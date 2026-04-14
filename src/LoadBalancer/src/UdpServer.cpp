#include "net/UdpServer.hpp"

#include <iostream>
#include <stdexcept>
#include <utility>

namespace lb {

    namespace net = boost::asio;
    using ErrorCode = boost::system::error_code;

    UdpServer::UdpServer(std::string bind_address, std::uint16_t port, PacketCallback on_packet)
        : m_socket(m_io_context),
        m_on_packet(std::move(on_packet)),
        m_bind_address(std::move(bind_address)),
        m_port(port)
    {
        if (!m_on_packet)
        {
            m_on_packet = [](UdpServer&, const std::vector<std::uint8_t>& payload,
                                             const boost::asio::ip::udp::endpoint& remoteEndpoint) {
                                             std::cout << "Received " << payload.size() << " bytes from " << remoteEndpoint
                                                 << std::endl;
                };
        }
    }

    UdpServer::~UdpServer()
    {
        stop();
    }

    void UdpServer::start()
    {
        if (m_running.exchange(true))
        {
            return;
        }

        const auto address = net::ip::make_address(m_bind_address);
        const auto endpoint = net::ip::udp::endpoint(address, m_port);

        ErrorCode error_code;
        m_socket.open(endpoint.protocol(), error_code);
        if (error_code)
        {
            m_running = false;
            throw std::runtime_error("Failed to open UDP socket: " + error_code.message());
        }

        m_socket.set_option(net::socket_base::reuse_address(true), error_code);
        if (error_code)
        {
            stop();
            throw std::runtime_error("Failed to set socket option: " + error_code.message());
        }

        m_socket.bind(endpoint, error_code);
        if (error_code)
        {
            stop();
            throw std::runtime_error("Failed to bind UDP socket: " + error_code.message());
        }

        m_doReceive();

        m_worker_thread = std::thread([this]() {
            std::cout << "UDP server listening on " << m_bind_address << ':' << m_port << std::endl;
            m_io_context.run();
        });
    }

    void UdpServer::stop()
    {
        if (!m_running.exchange(false))
        {
            return;
        }

        ErrorCode ignored;
        m_socket.close(ignored);
        m_io_context.stop();

        if (m_worker_thread.joinable())
        {
            m_worker_thread.join();
        }
    }

    bool UdpServer::isRunning() const
    {
        return m_running.load();
    }

    void UdpServer::sendTo(
            const std::vector<std::uint8_t>& payload,
            const boost::asio::ip::udp::endpoint& remoteEndpoint)
    {
        if (!m_running.load())
        {
            return;
        }

        if (payload.size() > m_send_buffer.size())
        {
            throw std::runtime_error("UDP payload exceeds max datagram size");
        }

        std::copy(payload.begin(), payload.end(), m_send_buffer.begin());

        ErrorCode error_code;
        const auto sent =
            m_socket.send_to(net::buffer(m_send_buffer.data(), payload.size()), remoteEndpoint, 0, error_code);
        if (error_code)
        {
            throw std::runtime_error("Failed to send UDP packet: " + error_code.message());
        }

        if (sent != payload.size())
        {
            throw std::runtime_error("Failed to send full UDP payload");
        }
    }

    void UdpServer::m_doReceive()
    {
        m_socket.async_receive_from(
                net::buffer(m_buffer),
                m_remote_endpoint,
                [this](const ErrorCode& error, std::size_t bytesReceived) {
                        m_handleReceive(error, bytesReceived);
                });
    }

    void UdpServer::m_handleReceive(const ErrorCode& error, std::size_t bytesReceived)
    {
        if (!m_running.load())
        {
            return;
        }

        if (error)
        {
            if (error != net::error::operation_aborted)
            {
                std::cerr << "UDP receive error: " << error.message() << std::endl;
            }
            if (m_running.load())
            {
                m_doReceive();
            }
            return;
        }

        std::vector<std::uint8_t> payload(m_buffer.begin(), m_buffer.begin() + bytesReceived);
        m_on_packet(*this, payload, m_remote_endpoint);

        if (m_running.load())
        {
            m_doReceive();
        }
    }

} // namespace lb
