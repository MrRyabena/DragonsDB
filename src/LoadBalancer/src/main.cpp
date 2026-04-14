#include <atomic>
#include <chrono>
#include <csignal>
#include <cstdint>
#include <exception>
#include <iostream>
#include <memory>
#include <thread>

#ifdef _WIN32
#include <io.h>
#define isatty _isatty
#define fileno _fileno
#else
#include <unistd.h>
#endif

#include "config/LoadBalancerDefaults.hpp"
#include "net/PacketHandler.hpp"
#include "net/UdpServer.hpp"
#include "util/BackendRegistry.hpp"
#include "util/CommandLineOptions.hpp"
#include "util/StrategyFactory.hpp"

namespace {

std::atomic_bool g_keep_running{true};

void onSignal(int)
{
    g_keep_running.store(false);
}

bool isInteractiveStdin()
{
    return isatty(fileno(stdin)) != 0;
}

} // namespace

int main(int argc, const char* argv[])
{
    try
    {
        const auto cli = lb::CommandLineOptions::parse(argc, argv);
        if (cli.showHelp)
        {
            lb::CommandLineOptions::printHelp(std::cout);
            return 0;
        }

        for (const auto& warning : cli.warnings)
        {
            std::cerr << warning << std::endl;
        }

        auto packetHandler = std::make_shared<lb::PacketHandler>();

        lb::BackendRegistry registry;
        lb::CommandLineOptions::applyBackends(cli, registry, std::cerr);

        if (registry.empty())
        {
            for (const auto port : lb::LoadBalancerDefaults::DEFAULT_BACKEND_PORTS)
            {
                registry.registerBackend(
                        std::string(lb::LoadBalancerDefaults::DEFAULT_BACKEND_HOST),
                        port,
                        lb::LoadBalancerDefaults::DEFAULT_BACKEND_WEIGHT);
            }
        }

        const auto selectedStrategy =
            lb::StrategyFactory::create(cli.strategyName, registry.weights());

        packetHandler->setStrategy(selectedStrategy);
        packetHandler->setBackends(registry.endpoints());

        std::cout << registry.summary() << std::endl;
        std::cout << "Strategy: " << cli.strategyName << std::endl;

        lb::UdpServer server(
            std::string(lb::LoadBalancerDefaults::DEFAULT_BIND_ADDRESS),
            lb::LoadBalancerDefaults::LISTEN_PORT,
            [packetHandler](lb::UdpServer& udpServer,
                const std::vector<std::uint8_t>& packet,
                const boost::asio::ip::udp::endpoint& remote) {
                    packetHandler->handle(udpServer, packet, remote);
                });
        server.start();

        if (isInteractiveStdin())
        {
            std::cout << "Press Enter to stop the load balancer..." << std::endl;
            std::cin.get();
        }
        else
        {
            std::signal(SIGTERM, onSignal);
            std::signal(SIGINT, onSignal);

            while (g_keep_running.load())
            {
                std::this_thread::sleep_for(std::chrono::milliseconds(200));
            }
        }

        server.stop();
        return 0;
    }
    catch (const std::exception& e)
    {
        std::cerr << "LoadBalancer failed: " << e.what() << std::endl;
        return 1;
    }
}
