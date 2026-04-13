#include "strategy/LeastConnectionsStrategy.hpp"

#include <limits>

namespace lb {

void LeastConnectionsStrategy::m_ensureState(std::size_t backendCount) const {
    if (m_inflight.size() != backendCount) {
        m_inflight.assign(backendCount, 0);
    }
}

std::optional<std::size_t> LeastConnectionsStrategy::selectBackendIndex(
        const WireHeader&,
        const std::vector<std::uint8_t>&,
        const boost::asio::ip::udp::endpoint&,
        const std::vector<boost::asio::ip::udp::endpoint>& backends) const {
    if (backends.empty()) {
        return std::nullopt;
    }

    std::lock_guard<std::mutex> lock(m_mutex);
    m_ensureState(backends.size());

    std::size_t bestIndex = 0;
    for (std::size_t i = 1; i < backends.size(); ++i) {
        if (m_inflight[i] < m_inflight[bestIndex]) {
            bestIndex = i;
        }
    }

    return bestIndex;
}

void LeastConnectionsStrategy::onRequestAssigned(std::size_t backendIndex) {
    std::lock_guard<std::mutex> lock(m_mutex);
    if (backendIndex < m_inflight.size()) {
        ++m_inflight[backendIndex];
    }
}

void LeastConnectionsStrategy::onResponseCompleted(std::size_t backendIndex) {
    std::lock_guard<std::mutex> lock(m_mutex);
    if (backendIndex < m_inflight.size() && m_inflight[backendIndex] > 0) {
        --m_inflight[backendIndex];
    }
}

} // namespace lb
