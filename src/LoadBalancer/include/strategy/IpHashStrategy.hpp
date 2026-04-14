#pragma once

#include <cstddef>
#include <cstdint>
#include <optional>
#include <string>
#include <vector>

#include "strategy/BalancingStrategy.hpp"

namespace lb {

    class IpHashStrategy final : public BalancingStrategy {
    public:
        std::optional<std::size_t> selectBackendIndex(
                const WireHeader& header,
                const std::vector<std::uint8_t>& payload,
                const boost::asio::ip::udp::endpoint& clientEndpoint,
                const std::vector<boost::asio::ip::udp::endpoint>& backends) const override;
    };

} // namespace lb
