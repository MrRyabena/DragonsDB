#pragma once

#include <cstddef>
#include <cstdint>
#include <optional>
#include <vector>

#include <boost/asio.hpp>

#include "net/WireHeader.hpp"

namespace lb {

class BalancingStrategy {
public:
    virtual ~BalancingStrategy() = default;

    virtual std::optional<std::size_t> selectBackendIndex(
            const WireHeader& header,
            const std::vector<std::uint8_t>& payload,
            const boost::asio::ip::udp::endpoint& clientEndpoint,
            const std::vector<boost::asio::ip::udp::endpoint>& backends) const = 0;

    virtual void onRequestAssigned(std::size_t backendIndex) {
        (void)backendIndex;
    }

    virtual void onResponseCompleted(std::size_t backendIndex) {
        (void)backendIndex;
    }
};

} // namespace lb