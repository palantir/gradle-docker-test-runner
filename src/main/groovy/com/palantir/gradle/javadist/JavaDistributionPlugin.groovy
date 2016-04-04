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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection

class JavaDistributionPlugin implements Plugin<Project> {

    private static final String GROUP_NAME = "Docker Test Runners"
    private static final String TASK_STRING = "docker-env"

    static String getContainerRunName(Project project, String containerName) {
        return "${project.name}-${sanitize(containerName)}"
    }

    static String getTestTaskName(String containerName) {
        return "test-${TASK_STRING}-${containerName}";
    }

    void apply(Project project) {
        DockerTestRunnerExtension ext = project.extensions.create('dockerTestRunner', DockerTestRunnerExtension)

        project.afterEvaluate {
            Map<String, File> dockerFiles = new HashMap<>()
            if (ext.dockerFiles != null) {
                dockerFiles = getDockerContainerFiles(ext.dockerFiles)
            }

            List<Task> buildDockerTasks = []
            List<Task> runDockerTasks = []

            dockerFiles.each({ containerName, dockerFile ->
                def buildTask = project.tasks.create("build-${TASK_STRING}-${containerName}", BuildTask, {
                    group = GROUP_NAME
                    description = "Build the Docker test environment container ${containerName}."
                })
                buildTask.configure(containerName, dockerFile)
                buildDockerTasks << buildTask

                def runTask = project.tasks.create("run-${TASK_STRING}-${containerName}", RunTask, {
                    group = GROUP_NAME
                    description = "Run tests in the Docker test environment container ${containerName}."
                })
                runTask.configure(containerName)
                runTask.dependsOn(buildTask)
                runDockerTasks << runTask

                def testTask = project.tasks.create(getTestTaskName(containerName), TestTask)
                testTask.configure(containerName)
            })

            project.task("build-${TASK_STRING}", {
                group = GROUP_NAME
                description = "Build all of the Docker test environment containers."
            }).setDependsOn(buildDockerTasks)

            project.task("test-${TASK_STRING}", {
                group = GROUP_NAME
                description = "Run all of the Docker test environment containers."
            }).setDependsOn(runDockerTasks)
        }
    }

    /**
     * Returns a map from the name of the Docker container to the Dockerfile for the container. All of the files must
     * exist and must be regular files (they cannot be directories). The keys in the returned map are Strings of the
     * form "parent/filename" and are sanitized for Docker use -- the "parent" and "filename" portion are all lowercase
     * and any unsupported character is replaced with an underscore ('_'). If the provided files do not have unique
     * names after the transformation is applied, an exception is thrown.
     */
    private static Map<String, File> getDockerContainerFiles(FileCollection collection) {
        def unsupportedFiles = collection.findAll({ file -> !file.exists() || file.isDirectory() })
        if (!unsupportedFiles.isEmpty()) {
            throw new IllegalStateException("The following files were either nonexistent or were directories: ${unsupportedFiles}")
        }

        def groupedByName = collection.groupBy({ file -> "${sanitize(file.parentFile.name)}/${sanitize(file.name)}" });

        def filesWithNameCollisions = groupedByName.findAll({ it.value.size() > 1 }).collect { it.value }
        if (!filesWithNameCollisions.isEmpty()) {
            throw new IllegalStateException("Multiple files had the \"parent/file\" name after being standardized: ${filesWithNameCollisions}");
        }

        return groupedByName.collectEntries({
            [(it.key): it.value.get(0)]
        })
    }

    /**
     * Sanitize string so that it can be used as Dockerfile identifier (A-Za-z0-9_-). All characters are converted to
     * lowercase and any characters that are not in the set of acceptable characters are replaced with an underscore
     * ('_').
     */
    private static String sanitize(String input) {
        StringBuilder builder = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == '.') {
                builder.append(Character.toLowerCase(c))
            } else {
                builder.append('_')
            }
        }

        return builder.toString()
    }

}
