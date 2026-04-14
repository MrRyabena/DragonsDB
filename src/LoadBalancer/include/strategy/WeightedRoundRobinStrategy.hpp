#pragma once

#include <cstddef>
#include <cstdint>
#include <optional>
#include <vector>

#include "strategy/BalancingStrategy.hpp"

namespace lb {

    class WeightedRoundRobinStrategy final : public BalancingStrategy {
    public:
        explicit WeightedRoundRobinStrategy(std::vector<std::size_t> weights = {});

        void setWeights(std::vector<std::size_t> weights);

        std::optional<std::size_t> selectBackendIndex(
                const WireHeader& header,
                const std::vector<std::uint8_t>& payload,
                const boost::asio::ip::udp::endpoint& clientEndpoint,
                const std::vector<boost::asio::ip::udp::endpoint>& backends) const override;

    private:
        void m_ensureState(std::size_t backendCount) const;

        mutable std::vector<std::size_t> m_weights;
        mutable std::vector<std::ptrdiff_t> m_currentWeights;
    };

} // namespace lb
