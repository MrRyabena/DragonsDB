#pragma once

#include <memory>
#include <string>
#include <vector>

#include "strategy/BalancingStrategy.hpp"

namespace lb {

class StrategyFactory {
public:
    static std::shared_ptr<BalancingStrategy> create(
            const std::string& strategyName,
            const std::vector<std::size_t>& weights);

    static std::string normalizeName(const std::string& strategyName);
    static bool isSupported(const std::string& strategyName);
    static std::string supportedList();

private:
    StrategyFactory() = delete;
};

} // namespace lb
