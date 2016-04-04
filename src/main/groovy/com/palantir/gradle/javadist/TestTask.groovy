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

import org.gradle.api.tasks.testing.Test

class TestTask extends Test {

    /**
     * Configures the task to run all of the tests for the current project. The task is configured to output XML and
     * HTML reports to directories that correspond to the "default" location concatenated with the sanitized container
     * name.
     */
    public void configure(String containerName) {
        testClassesDir = project.sourceSets.test.output.classesDir
        classpath = project.sourceSets.test.runtimeClasspath

        String sanitizedName = sanitize(containerName)

        println("debug TestTask configure: junitXml destination is ${reports.junitXml.destination}")
        println("debug TestTask configure: html destination is ${reports.html.destination}")

        reports.junitXml.destination("${project.buildDir.absolutePath}/test-results-${sanitizedName}")
        reports.html.destination("${project.buildDir.absolutePath}/reports/tests-${sanitizedName}")

        include("**/*")
    }

    /**
     * Returns the provided String with all '/' characters replaced with '_' so that the name can be used as part
     * of a path without causing directories to be created.
     */
    private static String sanitize(String name) {
        return name.replaceAll('/', '_')
    }

}
