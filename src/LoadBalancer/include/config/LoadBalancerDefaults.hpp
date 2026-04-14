#pragma once

#include <array>
#include <cstdint>
#include <string_view>

namespace lb {

    struct LoadBalancerDefaults {
        static constexpr std::string_view DEFAULT_BIND_ADDRESS = "0.0.0.0";
        static constexpr std::uint16_t LISTEN_PORT = 5000;

        static constexpr std::string_view DEFAULT_STRATEGY = "round_robin";

        static constexpr std::string_view HELP_ARG_LONG = "--help";
        static constexpr std::string_view HELP_ARG_SHORT = "-h";
        static constexpr std::string_view STRATEGY_ARG_PREFIX = "--strategy=";
        static constexpr std::string_view BACKEND_ARG_PREFIX = "--backend=";
        static constexpr std::string_view BACKENDS_FILE_ARG_PREFIX = "--backends-file=";

        static constexpr std::string_view DEFAULT_BACKEND_HOST = "127.0.0.1";
        static constexpr std::array<std::uint16_t, 2> DEFAULT_BACKEND_PORTS = { 5001, 5002 };
        static constexpr std::size_t DEFAULT_BACKEND_WEIGHT = 1;

    private:
        LoadBalancerDefaults() = delete;
    };

} // namespace lb
