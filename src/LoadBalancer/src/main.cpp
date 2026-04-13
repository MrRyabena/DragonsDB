#include <cstdint>
#include <exception>
#include <iostream>
#include <memory>

#include "net/PacketHandler.hpp"
#include "net/UdpServer.hpp"

int main() {
    try {
    auto packetHandler = std::make_shared<lb::PacketHandler>();
        lb::UdpServer server(
                "0.0.0.0",
                static_cast<std::uint16_t>(5000),
        [packetHandler](lb::UdpServer& udpServer,
                 const std::vector<std::uint8_t>& packet,
                 const boost::asio::ip::udp::endpoint& remote) {
            packetHandler->handle(udpServer, packet, remote);
                });
        server.start();

        std::cout << "Press Enter to stop the load balancer..." << std::endl;
        std::cin.get();

        server.stop();
        return 0;
    } catch (const std::exception& e) {
        std::cerr << "LoadBalancer failed: " << e.what() << std::endl;
        return 1;
    }
}
