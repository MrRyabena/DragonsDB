#pragma once

#include <atomic>
#include <cstddef>
#include <cstdint>
#include <optional>
#include <vector>

#include "strategy/BalancingStrategy.hpp"

namespace lb {

    class RoundRobinStrategy final : public BalancingStrategy {
    public:
        std::optional<std::size_t> selectBackendIndex(
                const WireHeader& header,
                const std::vector<std::uint8_t>& payload,
                const boost::asio::ip::udp::endpoint& clientEndpoint,
                const std::vector<boost::asio::ip::udp::endpoint>& backends) const override;

    private:
        mutable std::atomic<std::uint64_t> m_nextIndex{ 0 };
    };

} // namespace lb
