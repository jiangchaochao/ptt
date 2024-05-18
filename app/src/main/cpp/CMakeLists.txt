# cmake版本
cmake_minimum_required(VERSION 3.22.1)

# 项目名称
project("ptt")

# 导入预编译库
# opus
add_library(opus SHARED IMPORTED)
# 设置目标的属性，这里设置opus的属性是导入一个本地库，会根据当前的CMAKE_ANDROID_ARCH_ABI替换成不同的路径
set_target_properties(opus PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/../../../libs/${CMAKE_ANDROID_ARCH_ABI}/libopus.so)

# 包含头文件目录
include_directories(include)

add_library(${CMAKE_PROJECT_NAME} SHARED
        # List C/C++ source files with relative paths to this CMakeLists.txt.
        native-lib.cpp)

target_link_libraries(${CMAKE_PROJECT_NAME}
        # List libraries link to the target library
        android
        log
        opus  # 链接opus库
        )