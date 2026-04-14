#include "util/StrategyFactory.hpp"

#include <algorithm>
#include <array>
#include <cctype>

#include "strategy/IpHashStrategy.hpp"
#include "strategy/LeastConnectionsStrategy.hpp"
#include "strategy/RoundRobinStrategy.hpp"
#include "strategy/WeightedRoundRobinStrategy.hpp"

namespace lb {

    namespace {

        constexpr std::array<const char*, 4> kSupported = {
                "round_robin",
                "weighted_round_robin",
                "least_connections",
                "ip_hash",
        };

    } // namespace

    std::shared_ptr<BalancingStrategy> StrategyFactory::create(
            const std::string& strategyName,
            const std::vector<std::size_t>& weights)
    {
        const auto normalized = normalizeName(strategyName);

        if (normalized == "ip_hash")
        {
            return std::make_shared<IpHashStrategy>();
        }
        if (normalized == "least_connections")
        {
            return std::make_shared<LeastConnectionsStrategy>();
        }
        if (normalized == "weighted_round_robin")
        {
            return std::make_shared<WeightedRoundRobinStrategy>(weights);
        }

        return std::make_shared<RoundRobinStrategy>();
    }

    std::string StrategyFactory::normalizeName(const std::string& strategyName)
    {
        std::string normalized = strategyName;
        std::transform(
                normalized.begin(),
                normalized.end(),
                normalized.begin(),
                [](unsigned char c) { return static_cast<char>(std::tolower(c)); });

        if (normalized.empty())
        {
            return "round_robin";
        }

        return normalized;
    }

    bool StrategyFactory::isSupported(const std::string& strategyName)
    {
        const auto normalized = normalizeName(strategyName);
        for (const auto* value : kSupported)
        {
            if (normalized == value)
            {
                return true;
            }
        }
        return false;
    }

    std::string StrategyFactory::supportedList()
    {
        return "round_robin, weighted_round_robin, least_connections, ip_hash";
    }

} // namespace lb
