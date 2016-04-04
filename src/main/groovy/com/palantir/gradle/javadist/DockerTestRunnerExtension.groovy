/*
 * Copyright 2015 Palantir Technologies
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * <http://www.apache.org/licenses/LICENSE-2.0>
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.gradle.javadist

import org.gradle.api.file.FileCollection


class DockerTestRunnerExtension {
    /**
     * Collection of Dockerfile files. Gradle tasks for building the Docker container, running the container
     * and running the tests for the environment will be generated for each file in the collection. Every
     * file in the collection must be a Dockerfile file (however, the files can be named something other than
     * "Dockerfile"). The context of each Dockerfile will be the directory in which it resides.
     */
    FileCollection dockerFiles
}
