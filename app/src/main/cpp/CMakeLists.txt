cmake_minimum_required(VERSION 3.22.1)
project(karaoke_synth C)

# Ang tsf.h ay dina-download ng Gradle task na "fetchTsf" bago mag-build.
add_library(karaoke_synth SHARED synth.c)

# -O3 kahit debug build, dahil real-time ang synth
target_compile_options(karaoke_synth PRIVATE -O3)
target_link_libraries(karaoke_synth m)
