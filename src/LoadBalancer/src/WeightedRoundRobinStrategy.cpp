#include "strategy/WeightedRoundRobinStrategy.hpp"

#include <algorithm>
#include <limits>
#include <stdexcept>
#include <utility>

namespace lb {

    WeightedRoundRobinStrategy::WeightedRoundRobinStrategy(std::vector<std::size_t> weights)
        : m_weights(std::move(weights))
    {
    }

    void WeightedRoundRobinStrategy::setWeights(std::vector<std::size_t> weights)
    {
        m_weights = std::move(weights);
        m_currentWeights.clear();
    }

    void WeightedRoundRobinStrategy::m_ensureState(std::size_t backendCount) const
    {
        if (m_weights.size() < backendCount)
        {
            m_weights.resize(backendCount, 1);
        }
        if (m_currentWeights.size() != backendCount)
        {
            m_currentWeights.assign(backendCount, 0);
        }
    }

    std::optional<std::size_t> WeightedRoundRobinStrategy::selectBackendIndex(
            const WireHeader&,
            const std::vector<std::uint8_t>&,
        const boost::asio::ip::udp::endpoint&,
        const std::vector<boost::asio::ip::udp::endpoint>& backends) const
    {
        if (backends.empty())
        {
            return std::nullopt;
        }

        m_ensureState(backends.size());

        std::ptrdiff_t totalWeight = 0;
        for (std::size_t i = 0; i < backends.size(); ++i)
        {
            m_currentWeights[i] += static_cast<std::ptrdiff_t>(m_weights[i]);
            totalWeight += static_cast<std::ptrdiff_t>(m_weights[i]);
        }

        std::size_t bestIndex = 0;
        for (std::size_t i = 1; i < backends.size(); ++i)
        {
            if (m_currentWeights[i] > m_currentWeights[bestIndex])
            {
                bestIndex = i;
            }
        }

        m_currentWeights[bestIndex] -= totalWeight;
        return bestIndex;
    }

} // namespace lb
