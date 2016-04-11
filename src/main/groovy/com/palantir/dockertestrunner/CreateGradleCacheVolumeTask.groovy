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

package com.palantir.dockertestrunner

import com.google.common.collect.Multimap
import org.gradle.api.tasks.Exec

class CreateGradleCacheVolumeTask extends Exec {

    /**
     * Configures the task to create a Docker data volume that contains the Gradle cache files using the provided
     * container. A Gradle project will always have a single Gradle cache Docker data volume that is used by all
     * projects (including subprojects). The name for the volume is deterministic based on the name of the root project.
     * This command loads the Gradle cache Docker volume for the project (creating it if necessary) and copies the
     * contents of the Gradle cache (determined by the 'gradleUserHomeDir' of the running Gradle task) into the Docker
     * data volume. The container can be any container that supports the 'cp' operation (the container is only used to
     * create the volume and invoke the copy operation). Generally, using an image that is already part of the project
     * will be best for caching purposes, but any general image (including simple ones like 'busybox') can be used.
     */
    public void configure(String containerName) {
        workingDir(project.rootDir)

        String projectGradleDataVolume = DockerTestRunnerPlugin.getGradleDockerDataVolumeName(project)

        List<Object> arguments = []
        arguments << 'docker' << 'run'
        arguments << '--rm'
        arguments << '-v' << "${projectGradleDataVolume}:/dockerVolumeGradleData"
        arguments << '-v' << "${project.gradle.gradleUserHomeDir.absolutePath}:/hostGradleData"
        arguments << containerName
        arguments << 'cp' << '-r' << '/hostGradleData/. /dockerVolumeGradleData/'

        commandLine(arguments)
    }

}
