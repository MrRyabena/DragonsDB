#include "strategy/RoundRobinStrategy.hpp"

namespace lb {

std::optional<std::size_t> RoundRobinStrategy::selectBackendIndex(
        const WireHeader&,
        const std::vector<std::uint8_t>&,
        const boost::asio::ip::udp::endpoint&,
        const std::vector<boost::asio::ip::udp::endpoint>& backends) const {
    if (backends.empty()) {
        return std::nullopt;
    }

    const auto index = m_nextIndex.fetch_add(1, std::memory_order_relaxed) % backends.size();
    return static_cast<std::size_t>(index);
}

} // namespace lb
