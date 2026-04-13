cmake_minimum_required(VERSION 3.20)
project(LoadBalancer LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(Boost REQUIRED COMPONENTS system)

add_executable(LoadBalancer
    src/main.cpp
    src/UdpServer.cpp
    src/PacketHandler.cpp
    src/RoundRobinStrategy.cpp
    src/IpHashStrategy.cpp
    src/WeightedRoundRobinStrategy.cpp
    src/LeastConnectionsStrategy.cpp
    src/WireHeader.cpp
)

target_include_directories(LoadBalancer PRIVATE include)
target_link_libraries(LoadBalancer PRIVATE Boost::system)
