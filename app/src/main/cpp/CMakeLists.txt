cmake_minimum_required(VERSION 3.22.1)
project(karaoke_synth C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Ang tsf.h ay dina-download ng Gradle task na "fetchTsf" bago mag-build.
add_library(karaoke_synth SHARED synth.c)
# -O3 kahit debug build, dahil real-time ang synth
target_compile_options(karaoke_synth PRIVATE -O3)
target_link_libraries(karaoke_synth m)

# Mic echo/reverb (Oboe, galing sa prefab package)
find_package(oboe REQUIRED CONFIG)
add_library(karaoke_mic SHARED mic.cpp)
target_compile_options(karaoke_mic PRIVATE -O3)
target_link_libraries(karaoke_mic oboe::oboe)
