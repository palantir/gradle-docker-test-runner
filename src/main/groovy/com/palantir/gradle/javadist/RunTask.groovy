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

import com.google.common.collect.Multimap
import org.gradle.api.tasks.Exec

class RunTask extends Exec {

    /**
     * Configures the task to run the Docker environment tests for the current project. Executes the 'docker run'
     * command. Loads the project root directory as 'workspace' in the Docker container and loads the Docker, Gradle
     * and Maven cache directories from the user's home directory into the container as well.
     */
    public void configure(String taskName, String containerName, Multimap<String, String> customArguments) {
        workingDir(project.rootDir)

        String homeDir = System.getProperty('user.home')

        List<Object> arguments = []
        arguments << 'docker' << 'run'

        // add custom arguments
        customArguments.asMap().each({ key, values ->
            values.each({ value ->
                arguments << key << value
            })
        })

        arguments << '--rm'
        arguments << '-w' << '/workspace'
        arguments << '-v' << "${project.rootDir.absolutePath}:/workspace"
        arguments << '-v' << "${homeDir}/.docker:/root/.docker"
        arguments << '-v' << "${homeDir}/.gradle:/root/.gradle"
        arguments << '-v' << "${homeDir}/.m2:/root/.m2"
        arguments << '--name' << JavaDistributionPlugin.getContainerRunName(project, containerName)
        arguments << containerName
        arguments << '/bin/bash' << '-c'
        arguments << "./gradlew --stacktrace :${project.name}:${taskName}"

        commandLine(arguments)
    }

}
