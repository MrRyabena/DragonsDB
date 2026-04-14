#include "util/CommandLineOptions.hpp"

#include <string>

#include "config/LoadBalancerDefaults.hpp"
#include "util/BackendRegistry.hpp"
#include "util/StrategyFactory.hpp"

namespace lb {

CommandLineOptions::Parsed CommandLineOptions::parse(int argc, const char* const argv[]) {
    Parsed options;

    for (int i = 1; i < argc; ++i) {
        const std::string arg = argv[i] == nullptr ? "" : std::string(argv[i]);

        if (arg == LoadBalancerDefaults::HELP_ARG_LONG || arg == LoadBalancerDefaults::HELP_ARG_SHORT) {
            options.showHelp = true;
            continue;
        }

        if (arg.starts_with(LoadBalancerDefaults::STRATEGY_ARG_PREFIX)) {
            options.strategyRaw =
                    arg.substr(std::string(LoadBalancerDefaults::STRATEGY_ARG_PREFIX).size());
            continue;
        }

        if (arg.starts_with(LoadBalancerDefaults::BACKENDS_FILE_ARG_PREFIX)) {
            options.backendsFile =
                    arg.substr(std::string(LoadBalancerDefaults::BACKENDS_FILE_ARG_PREFIX).size());
            continue;
        }

        if (arg.starts_with(LoadBalancerDefaults::BACKEND_ARG_PREFIX)) {
            options.backendSpecs.push_back(
                    arg.substr(std::string(LoadBalancerDefaults::BACKEND_ARG_PREFIX).size()));
            continue;
        }

        options.warnings.push_back("Unknown argument: " + arg);
    }

    options.strategyName = StrategyFactory::normalizeName(options.strategyRaw);
    if (!StrategyFactory::isSupported(options.strategyName)) {
        options.warnings.push_back(
                "Unknown strategy '"
                + options.strategyRaw
                + "', fallback to round_robin. Supported: "
                + StrategyFactory::supportedList());
        options.strategyName = std::string(LoadBalancerDefaults::DEFAULT_STRATEGY);
    }

    return options;
}

void CommandLineOptions::applyBackends(
        const Parsed& options,
        BackendRegistry& registry,
        std::ostream& err) {
    std::string parseError;

    if (!options.backendsFile.empty()) {
        registry.loadFromFile(options.backendsFile, &parseError);
        if (!parseError.empty()) {
            err << "Backend file warning: " << parseError << std::endl;
        }
    }

    for (const auto& spec : options.backendSpecs) {
        parseError.clear();
        if (!registry.registerBackendSpec(spec, &parseError)) {
            err << "Backend arg warning: " << parseError << std::endl;
        }
    }
}

void CommandLineOptions::printHelp(std::ostream& out) {
    out << "LoadBalancer usage:\n"
        << "  LoadBalancer [options]\n\n"
        << "Options:\n"
        << "  --help, -h\n"
        << "      Show this help and exit.\n\n"
        << "  --strategy=<name>\n"
        << "      Select balancing strategy. Supported: "
        << StrategyFactory::supportedList() << "\n"
        << "      Default: " << LoadBalancerDefaults::DEFAULT_STRATEGY << "\n\n"
        << "  --backend=<host:port[:weight]>\n"
        << "      Register one backend endpoint.\n"
        << "      Repeat this option to register multiple backends.\n"
        << "      Example: --backend=127.0.0.1:5001:2\n\n"
        << "  --backends-file=<path>\n"
        << "      Load backend endpoints from file.\n"
        << "      Format: host:port[:weight], one backend per line.\n"
        << "      Lines starting with '#' are treated as comments.\n\n"
        << "Behavior:\n"
        << "  If no backend is provided, defaults are used:\n"
        << "    " << LoadBalancerDefaults::DEFAULT_BACKEND_HOST << ":"
        << LoadBalancerDefaults::DEFAULT_BACKEND_PORTS[0] << " and "
        << LoadBalancerDefaults::DEFAULT_BACKEND_HOST << ":"
        << LoadBalancerDefaults::DEFAULT_BACKEND_PORTS[1] << " (weight="
        << LoadBalancerDefaults::DEFAULT_BACKEND_WEIGHT << ").\n";
}

} // namespace lb
