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
package com.palantir.gradle.dockertestrunner

import org.gradle.api.tasks.Exec

class BuildTask extends Exec {

    /**
     * Configures the task to build the Docker image specified by the provided Dockerfile. The image is tagged with
     * the provided name and is built in the directory in which the provided image file resides.
     */
    public void configure(File dockerFile, String imageName) {
        workingDir(project.rootDir)

        commandLine('docker',
                'build',
                '-f', dockerFile.absolutePath,
                '-t', imageName,
                dockerFile.parentFile.absolutePath)
    }

}
