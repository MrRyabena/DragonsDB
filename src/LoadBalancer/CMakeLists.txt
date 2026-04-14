cmake_minimum_required(VERSION 3.20)
project(LoadBalancer LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if(POLICY CMP0167)
    cmake_policy(SET CMP0167 NEW)
endif()

find_package(Boost REQUIRED CONFIG)
find_package(Threads REQUIRED)

add_executable(LoadBalancer
    src/main.cpp
    src/UdpServer.cpp
    src/BackendRegistry.cpp
    src/CommandLineOptions.cpp
    src/StrategyFactory.cpp
    src/PacketHandler.cpp
    src/RoundRobinStrategy.cpp
    src/IpHashStrategy.cpp
    src/WeightedRoundRobinStrategy.cpp
    src/LeastConnectionsStrategy.cpp
    src/WireHeader.cpp
)

target_include_directories(LoadBalancer PRIVATE include)

if(TARGET Boost::headers)
    target_link_libraries(LoadBalancer PRIVATE Boost::headers)
elseif(TARGET Boost::boost)
    target_link_libraries(LoadBalancer PRIVATE Boost::boost)
elseif(DEFINED Boost_INCLUDE_DIRS)
    target_include_directories(LoadBalancer PRIVATE ${Boost_INCLUDE_DIRS})
endif()

if(TARGET Boost::system)
    target_link_libraries(LoadBalancer PRIVATE Boost::system)
endif()

target_link_libraries(LoadBalancer PRIVATE Threads::Threads)
