#pragma once

#include <cstddef>
#include <cstdint>
#include <string>
#include <vector>

namespace lb {

    struct WireHeader {
        static constexpr std::uint32_t MAGIC = 0x44524231; // DRB1
        static constexpr std::uint16_t VERSION = 1;
        static constexpr std::size_t SIZE =
            sizeof(std::uint32_t) + sizeof(std::uint16_t) + sizeof(std::uint8_t)
            + sizeof(std::uint8_t) + sizeof(std::uint64_t) + sizeof(std::uint64_t)
            + sizeof(std::uint32_t);

        enum class Kind : std::uint8_t {
            Request = 1,
            Response = 2,
        };

        std::uint32_t magic = MAGIC;
        std::uint16_t version = VERSION;
        Kind kind = Kind::Request;
        std::uint8_t flags = 0;
        std::uint64_t requestId = 0;
        std::uint64_t sessionId = 0;
        std::uint32_t payloadLength = 0;

        std::vector<std::uint8_t> toBytes() const;
        static WireHeader fromBytes(const std::vector<std::uint8_t>& bytes);

        static std::vector<std::uint8_t> pack(const WireHeader& header, const std::vector<std::uint8_t>& payload);
        static bool unpack(
                const std::vector<std::uint8_t>& packet,
                WireHeader& outHeader,
                std::vector<std::uint8_t>& outPayload,
                std::string& error);
    };

} // namespace lb
