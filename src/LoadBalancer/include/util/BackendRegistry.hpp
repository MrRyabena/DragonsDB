#pragma once

#include <cstddef>
#include <cstdint>
#include <string>
#include <vector>

#include <boost/asio.hpp>

namespace lb {

class BackendRegistry {
public:
    struct BackendEntry {
        boost::asio::ip::udp::endpoint endpoint;
        std::size_t weight = 1;
    };

    bool registerBackend(const std::string& host, std::uint16_t port, std::size_t weight = 1);
    bool registerBackendSpec(const std::string& spec, std::string* error = nullptr);

    std::size_t loadFromFile(const std::string& filePath, std::string* error = nullptr);
    std::size_t loadFromArgs(
            int argc,
            const char* const argv[],
            const std::string& argPrefix = "--backend=",
            std::string* error = nullptr);

    bool empty() const;
    std::size_t size() const;

    std::vector<boost::asio::ip::udp::endpoint> endpoints() const;
    std::vector<std::size_t> weights() const;
    std::string summary() const;

private:
    std::vector<BackendEntry> m_backends;
};

} // namespace lb
