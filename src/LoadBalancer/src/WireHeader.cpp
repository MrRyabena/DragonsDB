#include "net/WireHeader.hpp"
#include "net/ByteCodec.hpp"

#include <stdexcept>
#include <string>

namespace lb {

    std::vector<std::uint8_t> WireHeader::toBytes() const
    {
        std::vector<std::uint8_t> out;
        out.reserve(SIZE);

        ByteCodec::appendBigEndian<std::uint32_t>(out, magic);
        ByteCodec::appendBigEndian<std::uint16_t>(out, version);
        ByteCodec::appendBigEndian<std::uint8_t>(out, static_cast<std::uint8_t>(kind));
        ByteCodec::appendBigEndian<std::uint8_t>(out, flags);
        ByteCodec::appendBigEndian<std::uint64_t>(out, requestId);
        ByteCodec::appendBigEndian<std::uint64_t>(out, sessionId);
        ByteCodec::appendBigEndian<std::uint32_t>(out, payloadLength);

        return out;
    }

    WireHeader WireHeader::fromBytes(const std::vector<std::uint8_t>& bytes)
    {
        if (bytes.size() < SIZE)
        {
            throw std::runtime_error("Wire header is too small");
        }

        WireHeader header;
        std::size_t offset = 0;

        header.magic = ByteCodec::readBigEndian<std::uint32_t>(bytes, offset);
        offset += sizeof(std::uint32_t);

        header.version = ByteCodec::readBigEndian<std::uint16_t>(bytes, offset);
        offset += sizeof(std::uint16_t);

        header.kind = static_cast<Kind>(ByteCodec::readBigEndian<std::uint8_t>(bytes, offset));
        offset += sizeof(std::uint8_t);

        header.flags = ByteCodec::readBigEndian<std::uint8_t>(bytes, offset);
        offset += sizeof(std::uint8_t);

        header.requestId = ByteCodec::readBigEndian<std::uint64_t>(bytes, offset);
        offset += sizeof(std::uint64_t);

        header.sessionId = ByteCodec::readBigEndian<std::uint64_t>(bytes, offset);
        offset += sizeof(std::uint64_t);

        header.payloadLength = ByteCodec::readBigEndian<std::uint32_t>(bytes, offset);

        return header;
    }

    std::vector<std::uint8_t> WireHeader::pack(const WireHeader& header, const std::vector<std::uint8_t>& payload)
    {
        WireHeader adjusted = header;
        adjusted.payloadLength = static_cast<std::uint32_t>(payload.size());

        std::vector<std::uint8_t> packet = adjusted.toBytes();
        packet.insert(packet.end(), payload.begin(), payload.end());
        return packet;
    }

    bool WireHeader::unpack(
            const std::vector<std::uint8_t>& packet,
            WireHeader& outHeader,
            std::vector<std::uint8_t>& outPayload,
            std::string& error)
    {
        if (packet.size() < SIZE)
        {
            error = "Packet is too small";
            return false;
        }

        try
        {
            outHeader = fromBytes(packet);
        }
        catch (const std::exception& e)
        {
            error = e.what();
            return false;
        }

        if (outHeader.magic != MAGIC)
        {
            error = "Invalid wire magic";
            return false;
        }

        if (outHeader.version != VERSION)
        {
            error = "Unsupported wire version";
            return false;
        }

        const std::size_t expectedSize = SIZE + static_cast<std::size_t>(outHeader.payloadLength);
        if (packet.size() < expectedSize)
        {
            error = "Packet payload is truncated";
            return false;
        }

        outPayload.assign(packet.begin() + static_cast<std::ptrdiff_t>(SIZE), packet.begin() + static_cast<std::ptrdiff_t>(expectedSize));
        return true;
    }

} // namespace lb
