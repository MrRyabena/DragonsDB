#pragma once

#include <cstddef>
#include <cstdint>
#include <mutex>
#include <optional>
#include <vector>

#include "strategy/BalancingStrategy.hpp"

namespace lb {

class LeastConnectionsStrategy final : public BalancingStrategy {
public:
    std::optional<std::size_t> selectBackendIndex(
            const WireHeader& header,
            const std::vector<std::uint8_t>& payload,
            const boost::asio::ip::udp::endpoint& clientEndpoint,
            const std::vector<boost::asio::ip::udp::endpoint>& backends) const override;

    void onRequestAssigned(std::size_t backendIndex) override;
    void onResponseCompleted(std::size_t backendIndex) override;

private:
    void m_ensureState(std::size_t backendCount) const;

    mutable std::vector<std::size_t> m_inflight;
    mutable std::mutex m_mutex;
};

} // namespace lb
