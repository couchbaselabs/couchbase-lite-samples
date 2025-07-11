cmake_minimum_required(VERSION 3.25)

project(color)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_INSTALL_PREFIX ${CMAKE_CURRENT_BINARY_DIR}/out)

find_package(CouchbaseLite 3.2.0 REQUIRED PATHS lib/libcblite)

add_executable(color src/main.cpp src/DBManager.cpp src/DBManager.h src/RGB.h)

target_link_libraries(
        color PRIVATE
        cblite
)

file(COPY dataset DESTINATION ${CMAKE_CURRENT_BINARY_DIR})
file(COPY extension DESTINATION ${CMAKE_CURRENT_BINARY_DIR})

install(
        TARGETS color
        RUNTIME DESTINATION bin
)
