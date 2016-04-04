package com.palantir.gradle.dockertestrunner

enum GradleCacheMode {
    // Gradle cache directory should be mounted in container
    MOUNT,
    // Gradle cache directory should be copied into container
    COPY,
    // Container starts with no Gradle cache
    NONE,
}
