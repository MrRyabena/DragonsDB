#include "strategy/IpHashStrategy.hpp"

#include <functional>

namespace lb {

std::optional<std::size_t> IpHashStrategy::selectBackendIndex(
        const WireHeader&,
        const std::vector<std::uint8_t>&,
        const boost::asio::ip::udp::endpoint& clientEndpoint,
        const std::vector<boost::asio::ip::udp::endpoint>& backends) const {
    if (backends.empty()) {
        return std::nullopt;
    }

    const auto key = clientEndpoint.address().to_string() + ":" + std::to_string(clientEndpoint.port());
    const auto hash = std::hash<std::string>{}(key);
    return static_cast<std::size_t>(hash % backends.size());
}

} // namespace lb
