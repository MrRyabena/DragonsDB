#pragma once

#include <array>
#include <atomic>
#include <cstdint>
#include <functional>
#include <string>
#include <thread>
#include <vector>

#include <boost/asio.hpp>

namespace lb {

    class UdpServer {
    public:
        using PacketCallback = std::function<void(
                UdpServer& server,
                const std::vector<std::uint8_t>& payload,
                const boost::asio::ip::udp::endpoint& remoteEndpoint)>;

        UdpServer(std::string bindAddress, std::uint16_t port, PacketCallback onPacket = {});
        ~UdpServer();

        void start();
        void stop();
        bool isRunning() const;
        void sendTo(
            const std::vector<std::uint8_t>& payload,
            const boost::asio::ip::udp::endpoint& remoteEndpoint);

    private:
        void m_doReceive();
        void m_handleReceive(const boost::system::error_code& error, std::size_t bytesReceived);

        boost::asio::io_context m_io_context;
        boost::asio::ip::udp::socket m_socket;
        boost::asio::ip::udp::endpoint m_remote_endpoint;
        std::array<std::uint8_t, 64 * 1024> m_buffer{};
        std::array<std::uint8_t, 64 * 1024> m_send_buffer{};
        std::thread m_worker_thread;
        std::atomic_bool m_running{ false };
        PacketCallback m_on_packet;
        std::string m_bind_address;
        std::uint16_t m_port;
    };

}   // namespace lb
