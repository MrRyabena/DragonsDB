#include "net/PacketHandler.hpp"

#include <iostream>
#include <sstream>
#include <utility>

#include "net/UdpServer.hpp"

namespace lb {

    namespace {

    std::string endpointKey(const PacketHandler::BackendEndpoint& endpoint)
    {
        std::ostringstream out;
        out << endpoint.address().to_string() << ':' << endpoint.port();
        return out.str();
    }

    } // namespace

    PacketHandler::PacketHandler(std::shared_ptr<BalancingStrategy> strategy)
        : m_strategy(std::move(strategy))
    {
    }

    void PacketHandler::setStrategy(std::shared_ptr<BalancingStrategy> strategy)
    {
        std::lock_guard<std::mutex> lock(m_mutex);
        m_strategy = std::move(strategy);
    }

    void PacketHandler::setBackends(std::vector<BackendEndpoint> backends)
    {
        std::lock_guard<std::mutex> lock(m_mutex);
        m_backends = std::move(backends);
    }

    void PacketHandler::addBackend(const BackendEndpoint& backend)
    {
        std::lock_guard<std::mutex> lock(m_mutex);
        m_backends.push_back(backend);
    }

    void PacketHandler::handle(
            UdpServer& server,
            const std::vector<std::uint8_t>& packet,
            const BackendEndpoint& remoteEndpoint)
    {
        WireHeader header;
        std::vector<std::uint8_t> payload;
        std::string error;
        if (!WireHeader::unpack(packet, header, payload, error))
        {
            std::cerr << "Invalid packet from " << remoteEndpoint << ": " << error << std::endl;
            return;
        }

        if (header.kind == WireHeader::Kind::Request)
        {
            m_handleRequest(server, packet, header, payload, remoteEndpoint);
            return;
        }

        if (header.kind == WireHeader::Kind::Response)
        {
            m_handleResponse(server, packet, header, remoteEndpoint);
            return;
        }

        std::cerr << "Unsupported frame kind from " << remoteEndpoint << std::endl;
    }

    void PacketHandler::m_handleRequest(
            UdpServer& server,
            const std::vector<std::uint8_t>& packet,
            const WireHeader& header,
            const std::vector<std::uint8_t>& payload,
            const BackendEndpoint& clientEndpoint)
    {
        std::vector<BackendEndpoint> backendsCopy;
        std::shared_ptr<BalancingStrategy> strategyCopy;
        std::optional<std::size_t> stickyIndex;
        bool usedSticky = false;
        std::string clientKey;
        {
            std::lock_guard<std::mutex> lock(m_mutex);
            backendsCopy = m_backends;
            strategyCopy = m_strategy;
            clientKey = endpointKey(clientEndpoint);

            const auto stickyIt = m_clientAffinity.find(clientKey);
            if (stickyIt != m_clientAffinity.end() && stickyIt->second < backendsCopy.size())
            {
                stickyIndex = stickyIt->second;
            }
        }

        if (backendsCopy.empty())
        {
            std::cout << "No backends configured, echoing requestId=" << header.requestId
                << " back to client " << clientEndpoint << std::endl;
            auto responseHeader = header;
            responseHeader.kind = WireHeader::Kind::Response;
            server.sendTo(WireHeader::pack(responseHeader, payload), clientEndpoint);
            return;
        }

        std::optional<std::size_t> selectedIndex;
        if (stickyIndex.has_value())
        {
            selectedIndex = stickyIndex;
            usedSticky = true;
        }
        else if (strategyCopy)
        {
            selectedIndex = strategyCopy->selectBackendIndex(
                    header, payload, clientEndpoint, backendsCopy);
        }

        if (!selectedIndex.has_value() || *selectedIndex >= backendsCopy.size())
        {
            std::cout << "Strategy did not select backend for requestId=" << header.requestId
                << ", echoing to client " << clientEndpoint << std::endl;
            auto responseHeader = header;
            responseHeader.kind = WireHeader::Kind::Response;
            server.sendTo(WireHeader::pack(responseHeader, payload), clientEndpoint);
            return;
        }

        const auto& backend = backendsCopy[*selectedIndex];
        {
            std::lock_guard<std::mutex> lock(m_mutex);
            m_pendingRequests[header.requestId] = PendingRequest{ clientEndpoint, *selectedIndex };
            m_clientAffinity[clientKey] = *selectedIndex;
        }

        if (strategyCopy)
        {
            strategyCopy->onRequestAssigned(*selectedIndex);
        }

        if (usedSticky)
        {
            std::cout << "Sticky route for client " << clientEndpoint << " -> backend " << backend
                << std::endl;
        }

        std::cout << "Forwarding requestId=" << header.requestId << " to backend " << backend
            << std::endl;
        server.sendTo(packet, backend);
    }

    void PacketHandler::m_handleResponse(
            UdpServer& server,
            const std::vector<std::uint8_t>& packet,
            const WireHeader& header,
            const BackendEndpoint& remoteEndpoint)
    {
        BackendEndpoint clientEndpoint;
        bool found = false;
        {
            std::lock_guard<std::mutex> lock(m_mutex);
            const auto it = m_pendingRequests.find(header.requestId);
            if (it != m_pendingRequests.end())
            {
                clientEndpoint = it->second.clientEndpoint;
                const auto backendIndex = it->second.backendIndex;
                m_pendingRequests.erase(it);
                found = true;

                if (backendIndex < m_backends.size() && m_strategy)
                {
                    m_strategy->onResponseCompleted(backendIndex);
                }
            }
        }

        if (!found)
        {
            std::cout << "Unknown response requestId=" << header.requestId << " from "
                << remoteEndpoint << std::endl;
            return;
        }

        std::cout << "Returning response requestId=" << header.requestId << " to client "
            << clientEndpoint << std::endl;
        server.sendTo(packet, clientEndpoint);
    }

} // namespace lb
