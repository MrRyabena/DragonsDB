#pragma once

#include <ostream>
#include <string>
#include <vector>

#include "config/LoadBalancerDefaults.hpp"

namespace lb {

    class BackendRegistry;

    class CommandLineOptions {
    public:
        struct Parsed {
            bool showHelp = false;
            std::string strategyName = std::string(LoadBalancerDefaults::DEFAULT_STRATEGY);
            std::string strategyRaw;
            std::string backendsFile;
            std::vector<std::string> backendSpecs;
            std::vector<std::string> warnings;
        };

        static Parsed parse(int argc, const char* const argv[]);
        static void applyBackends(const Parsed& options, BackendRegistry& registry, std::ostream& err);
        static void printHelp(std::ostream& out);

    private:
        CommandLineOptions() = delete;
    };

} // namespace lb
