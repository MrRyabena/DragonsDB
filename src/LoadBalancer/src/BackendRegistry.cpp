#include "util/BackendRegistry.hpp"

#include <algorithm>
#include <fstream>
#include <sstream>
#include <stdexcept>

namespace lb {

namespace {

std::string trim(const std::string& value) {
    const auto first = value.find_first_not_of(" \t\r\n");
    if (first == std::string::npos) {
        return "";
    }

    const auto last = value.find_last_not_of(" \t\r\n");
    return value.substr(first, last - first + 1);
}

bool parsePort(const std::string& raw, std::uint16_t& outPort) {
    try {
        const auto parsed = std::stoul(raw);
        if (parsed > 65535) {
            return false;
        }
        outPort = static_cast<std::uint16_t>(parsed);
        return true;
    } catch (...) {
        return false;
    }
}

bool parseWeight(const std::string& raw, std::size_t& outWeight) {
    try {
        const auto parsed = std::stoull(raw);
        if (parsed == 0) {
            return false;
        }
        outWeight = static_cast<std::size_t>(parsed);
        return true;
    } catch (...) {
        return false;
    }
}

} // namespace

bool BackendRegistry::registerBackend(const std::string& host, std::uint16_t port, std::size_t weight) {
    if (host.empty() || port == 0 || weight == 0) {
        return false;
    }

    boost::system::error_code ec;
    const auto address = boost::asio::ip::make_address(trim(host), ec);
    if (ec) {
        return false;
    }

    m_backends.push_back(BackendEntry{boost::asio::ip::udp::endpoint(address, port), weight});
    return true;
}

bool BackendRegistry::registerBackendSpec(const std::string& spec, std::string* error) {
    const auto input = trim(spec);
    if (input.empty()) {
        if (error) {
            *error = "Empty backend specification";
        }
        return false;
    }

    std::vector<std::string> parts;
    std::stringstream ss(input);
    std::string token;
    while (std::getline(ss, token, ':')) {
        parts.push_back(trim(token));
    }

    if (parts.size() < 2 || parts.size() > 3) {
        if (error) {
            *error = "Backend format must be host:port or host:port:weight";
        }
        return false;
    }

    std::uint16_t port = 0;
    if (!parsePort(parts[1], port)) {
        if (error) {
            *error = "Invalid backend port: " + parts[1];
        }
        return false;
    }

    std::size_t weight = 1;
    if (parts.size() == 3 && !parseWeight(parts[2], weight)) {
        if (error) {
            *error = "Invalid backend weight: " + parts[2];
        }
        return false;
    }

    if (!registerBackend(parts[0], port, weight)) {
        if (error) {
            *error = "Failed to register backend: " + input;
        }
        return false;
    }

    return true;
}

std::size_t BackendRegistry::loadFromFile(const std::string& filePath, std::string* error) {
    std::ifstream input(filePath);
    if (!input.is_open()) {
        if (error) {
            *error = "Cannot open backend config file: " + filePath;
        }
        return 0;
    }

    std::size_t loaded = 0;
    std::size_t lineNo = 0;
    std::string line;
    while (std::getline(input, line)) {
        ++lineNo;
        const auto cleaned = trim(line);
        if (cleaned.empty() || cleaned.starts_with('#')) {
            continue;
        }

        std::string parseError;
        if (!registerBackendSpec(cleaned, &parseError)) {
            if (error) {
                *error = "Line " + std::to_string(lineNo) + ": " + parseError;
            }
            continue;
        }

        ++loaded;
    }

    return loaded;
}

std::size_t BackendRegistry::loadFromArgs(
        int argc,
        const char* const argv[],
        const std::string& argPrefix,
        std::string* error) {
    std::size_t loaded = 0;

    for (int i = 1; i < argc; ++i) {
        const std::string arg = argv[i] == nullptr ? "" : std::string(argv[i]);
        if (!arg.starts_with(argPrefix)) {
            continue;
        }

        const auto spec = arg.substr(argPrefix.size());
        std::string parseError;
        if (!registerBackendSpec(spec, &parseError)) {
            if (error) {
                *error = "Argument '" + arg + "': " + parseError;
            }
            continue;
        }

        ++loaded;
    }

    return loaded;
}

bool BackendRegistry::empty() const {
    return m_backends.empty();
}

std::size_t BackendRegistry::size() const {
    return m_backends.size();
}

std::vector<boost::asio::ip::udp::endpoint> BackendRegistry::endpoints() const {
    std::vector<boost::asio::ip::udp::endpoint> out;
    out.reserve(m_backends.size());

    for (const auto& backend : m_backends) {
        out.push_back(backend.endpoint);
    }

    return out;
}

std::vector<std::size_t> BackendRegistry::weights() const {
    std::vector<std::size_t> out;
    out.reserve(m_backends.size());

    for (const auto& backend : m_backends) {
        out.push_back(backend.weight);
    }

    return out;
}

std::string BackendRegistry::summary() const {
    std::ostringstream ss;
    ss << "Backends(" << m_backends.size() << "):";
    for (const auto& backend : m_backends) {
        ss << " [" << backend.endpoint << ", w=" << backend.weight << "]";
    }
    return ss.str();
}

} // namespace lb
