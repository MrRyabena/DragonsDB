#pragma once

#include <cstdint>
#include <memory>
#include <mutex>
#include <optional>
#include <string>
#include <unordered_map>
#include <vector>

#include <boost/asio.hpp>

#include "net/WireHeader.hpp"
#include "strategy/BalancingStrategy.hpp"

namespace lb {

    class UdpServer;

    class PacketHandler {
    public:
        using BackendEndpoint = boost::asio::ip::udp::endpoint;

        struct PendingRequest {
            BackendEndpoint clientEndpoint;
            std::size_t backendIndex = 0;
        };

        PacketHandler() = default;

        explicit PacketHandler(std::shared_ptr<BalancingStrategy> strategy);

        void setStrategy(std::shared_ptr<BalancingStrategy> strategy);
        void setBackends(std::vector<BackendEndpoint> backends);
        void addBackend(const BackendEndpoint& backend);

        void handle(
                UdpServer& server,
                const std::vector<std::uint8_t>& packet,
                const BackendEndpoint& remoteEndpoint);

    private:
        void m_handleRequest(
                UdpServer& server,
                const std::vector<std::uint8_t>& packet,
                const WireHeader& header,
                const std::vector<std::uint8_t>& payload,
                const BackendEndpoint& clientEndpoint);

        void m_handleResponse(
                UdpServer& server,
                const std::vector<std::uint8_t>& packet,
                const WireHeader& header,
                const BackendEndpoint& remoteEndpoint);

        std::shared_ptr<BalancingStrategy> m_strategy;
        std::vector<BackendEndpoint> m_backends;
        std::unordered_map<std::uint64_t, PendingRequest> m_pendingRequests;
        std::mutex m_mutex;
    };

} // namespace lb
