/*
 * Copyright 2016 Palantir Technologies
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

package com.palantir.gradle.dockertestrunner

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import org.gradle.api.file.FileCollection


class DockerTestRunnerExtension {
    /**
     * Collection of Dockerfile files. Gradle tasks for building the Docker container, running the container
     * and running the tests for the environment will be generated for each file in the collection. Every
     * file in the collection must be a Dockerfile file (however, the files can be named something other than
     * "Dockerfile"). The context of each Dockerfile will be the directory in which it resides.
     */
    FileCollection dockerFiles

    /**
     * Map of supplemental flags provided to the 'docker run' command. The key is the full flag including any leading
     * dashes -- for example, '-v' or '--cidfile'. The values are the values that will be provided for the key flag.
     * A separate flag will be added for each value of the key -- for example, if the key '-e' has values 'COLOR=pink'
     * and 'FOO=bar', then '-e COLOR=pink' and '-e FOO=bar' are both specified separately. The run arguments in this
     * map are specified before the built-in ones, so if there are configuration values that conflict, the built-in
     * ones will take precedence.
     */
    Multimap<String, String> customDockerRunArgs = ArrayListMultimap.create();

    /**
     * Optional Closure that can be specified to customize the class files that are considered in Jacoco coverage
     * reports. The closure is supplied with a FileCollection that starts with all class files for the project and the
     * FileCollection returned by the closure should include only the class files that should be considered for
     * coverage.
     */
    Closure<FileCollection> jacocoClassDirectories

    /**
     * Optional parameter that controls how Gradle cache is handled. The default behavior is to mount the host's
     * Gradle cache directly into the container. This typically provides the highest performance, but in some instances
     * sharing the same Gradle cache between multiple concurrent Gradle builds can lead to locking issues ("Timeout
     * waiting to lock artifact cache").
     */
    GradleCacheMode gradleCacheMode = GradleCacheMode.MOUNT

}
