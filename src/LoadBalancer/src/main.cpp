#include <cstdint>
#include <exception>
#include <iostream>

#include "net/UdpServer.hpp"

int main() {
    try {
        lb::UdpServer server("0.0.0.0", static_cast<std::uint16_t>(5000));
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
