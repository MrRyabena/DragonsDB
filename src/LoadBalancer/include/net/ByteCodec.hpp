#pragma once

#include <cstddef>
#include <cstdint>
#include <vector>

namespace lb {

class ByteCodec {
public:
    template <typename T>
    static void appendBigEndian(std::vector<std::uint8_t>& out, T value) {
        for (int i = static_cast<int>(sizeof(T)) - 1; i >= 0; --i) {
            out.push_back(static_cast<std::uint8_t>((value >> (i * 8)) & 0xFF));
        }
    }

    template <typename T>
    static T readBigEndian(const std::vector<std::uint8_t>& bytes, std::size_t offset) {
        T value = 0;
        for (std::size_t i = 0; i < sizeof(T); ++i) {
            value = static_cast<T>((value << 8) | bytes[offset + i]);
        }
        return value;
    }

private:
    ByteCodec() = delete;
};

} // namespace lb
