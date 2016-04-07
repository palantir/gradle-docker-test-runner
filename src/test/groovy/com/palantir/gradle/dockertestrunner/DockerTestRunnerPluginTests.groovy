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

import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermissions

class DockerTestRunnerPluginTests extends Specification {

    @Rule
    TemporaryFolder temporaryFolder = new TemporaryFolder(new File("."))

    File projectDir
    File buildFile
    List<File> pluginClasspath

    def setup() {
        projectDir = temporaryFolder.root
        buildFile = temporaryFolder.newFile('build.gradle')

        temporaryFolder.newFile('settings.gradle') << '''
            rootProject.name = 'dockerTestRunnerPluginTests'
        '''.stripIndent()

        def pluginClasspathResource = getClass().classLoader.findResource("plugin-classpath.txt")
        if (pluginClasspathResource == null) {
            throw new IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
        }

        pluginClasspath = pluginClasspathResource.readLines()
                .collect { it.replace('\\', '\\\\') } // escape backslashes in Windows paths
                .collect { new File(it) }
    }

    /**
     * Copies the Gradle resources needed to run Gradle within Docker for the tests. When the unit tests run, a
     * temporary directory with the simple test Gradle project is created. When the Docker-based tasks are run,
     * the container mounts that directory and runs './gradlew' within it, so it is necessary to ensure that minimum
     * resources needed to run Docker within that container with this plugin being tested is present.
     */
    private void setupDockerGradleResources() {
        // copy gradle wrapper and set run permissions
        FileUtils.copyFile(Paths.get('gradlew').toFile(), temporaryFolder.newFile('gradlew'))
        java.nio.file.Files.setPosixFilePermissions(
                temporaryFolder.getRoot().toPath().resolve('gradlew'),
                PosixFilePermissions.fromString('rwxr-xr-x'))

        // copy gradle directory
        FileUtils.copyDirectory(Paths.get('gradle').toFile(), temporaryFolder.newFolder('gradle'))

        // plugin needs to be available to the Gradle that runs within Docker. Copy the source files
        // to 'buildSrc' to ensure that the plugin that runs within the Docker container is the same
        // as the one being tested by these tests.
        FileUtils.copyDirectory(Paths.get('src', 'main').toFile(), temporaryFolder.newFolder('buildSrc', 'src', 'main'))
        File buildSrcBuildGradle = temporaryFolder.root.toPath().resolve('buildSrc/build.gradle').toFile();
        buildSrcBuildGradle.createNewFile();
        buildSrcBuildGradle << '''
            repositories {
                mavenCentral()
            }

            dependencies {
                compile 'com.google.guava:guava-jdk5:17.0'
            }
        '''
    }

    private GradleRunner run(String... tasks) {
        GradleRunner.create()
                .withPluginClasspath(pluginClasspath)
                .withProjectDir(projectDir)
                .withArguments(tasks)
                .withDebug(true)
    }

    def 'verify that tasks not present without Dockerfile'() {
        given:
        buildFile << '''
            plugins {
                id 'java'
                id 'com.palantir.docker-test-runner'
            }
        '''.stripIndent()

        when:
        BuildResult buildResult = run('tasks').build()

        then:
        buildResult.task(':tasks').outcome == TaskOutcome.SUCCESS
        buildResult.output !=~ ('buildDockerTestRunner')
        buildResult.output !=~ ('jacocoTestReportDockerTestRunner')
        buildResult.output !=~ ('testDockerTestRunner')
    }

    def 'verify that tasks present with Dockerfiles'() {
        given:
        String openJdk7 = 'open-jdk-7'
        File openJdk7Dir = temporaryFolder.newFolder(openJdk7)
        openJdk7Dir.toPath().resolve("Dockerfile").toFile().createNewFile()

        String oracleJdk8 = 'oracle-jdk-8'
        File oracleJdk8Dir = temporaryFolder.newFolder(oracleJdk8)
        oracleJdk8Dir.toPath().resolve("Dockerfile").toFile().createNewFile();

        buildFile << '''
            plugins {
                id 'java'
                id 'jacoco'
                id 'com.palantir.docker-test-runner'
            }

            dockerTestRunner {
                dockerFiles = fileTree(project.rootDir) {
                    include '**/Dockerfile'
                }
            }
        '''.stripIndent()

        when:
        BuildResult buildResult = run('tasks').build()

        then:
        buildResult.task(':tasks').outcome == TaskOutcome.SUCCESS

        // verify per Dockerfile tasks exist
        [ openJdk7, oracleJdk8 ].each {
            buildResult.output =~ ("Docker Test Runner: ${it}/dockerfile tasks")
            buildResult.output =~ ("buildDockerTestRunner-${it}/dockerfile")
            buildResult.output =~ ("runJacocoTestReportDockerTestRunner-${it}/dockerfile")
            buildResult.output =~ ("runTestDockerTestRunner-${it}/dockerfile")
        }

        // verify combined tasks exist
        buildResult.output =~ ('buildDockerTestRunner')
        buildResult.output =~ ('jacocoTestReportDockerTestRunner')
        buildResult.output =~ ('testDockerTestRunner')
    }

    def 'verify buildDockerTestRunner builds Docker image'() {
        given:
        String openJdk8 = 'openjdk-8'
        File openJdk8Dir = temporaryFolder.newFolder(openJdk8)
        File dockerFile = openJdk8Dir.toPath().resolve("Dockerfile").toFile()
        dockerFile.createNewFile();

        dockerFile << '''
            FROM nimmis/java-centos:openjdk-8-jdk
            ENV JAVA_HOME=/usr/lib/jvm/java
        '''.stripIndent()

        buildFile << '''
            plugins {
                id 'java'
                id 'jacoco'
                id 'com.palantir.docker-test-runner'
            }

            dockerTestRunner {
                dockerFiles = fileTree(project.rootDir) {
                    include '**/Dockerfile'
                }
            }
        '''.stripIndent()

        when:
        BuildResult buildResult = run('buildDockerTestRunner').build()

        then:
        buildResult.task(':buildDockerTestRunner').outcome == TaskOutcome.SUCCESS
        buildResult.output =~ ('FROM nimmis/java-centos:openjdk-8-jdk')
        buildResult.output =~ ('Successfully built')
    }

    def 'verify testDockerTestRunner runs in Docker container in root project'() {
        given:
        setupDockerGradleResources();

        String openJdk8 = 'openjdk-8'
        File openJdk8Dir = temporaryFolder.newFolder(openJdk8)
        File dockerFile = openJdk8Dir.toPath().resolve('Dockerfile').toFile()
        dockerFile.createNewFile();
        dockerFile << '''
            FROM nimmis/java-centos:openjdk-8-jdk
            ENV JAVA_HOME=/usr/lib/jvm/java
        '''.stripIndent()

        buildFile << '''
            plugins {
                id 'java'
                id 'jacoco'
            }

            apply plugin: 'com.palantir.docker-test-runner'

            dockerTestRunner {
                dockerFiles = fileTree(project.rootDir) {
                    include '**/Dockerfile'
                }
            }
        '''.stripIndent()

        when:
        BuildResult buildResult = run('testDockerTestRunner').build()

        then:
        buildResult.task(':testDockerTestRunner').outcome == TaskOutcome.SUCCESS
        buildResult.output =~ (':testDockerTestRunner-openjdk-8/dockerfile UP-TO-DATE')
    }

    def 'verify testDockerTestRunner runs in Docker container in subproject'() {
        given:
        setupDockerGradleResources();

        String openJdk8 = 'openjdk-8'
        File openJdk8Dir = temporaryFolder.newFolder(openJdk8)
        File dockerFile = openJdk8Dir.toPath().resolve('Dockerfile').toFile()
        dockerFile.createNewFile();
        dockerFile << '''
            FROM nimmis/java-centos:openjdk-8-jdk
            ENV JAVA_HOME=/usr/lib/jvm/java
        '''.stripIndent()

        buildFile << '''
            plugins {
                id 'java'
                id 'jacoco'
            }

            subprojects {
                apply plugin: 'java'
                apply plugin: 'jacoco'
                apply plugin: 'com.palantir.docker-test-runner'

                dockerTestRunner {
                    dockerFiles = fileTree(project.rootDir) {
                        include '**/Dockerfile'
                    }
                }
            }
        '''.stripIndent()

        // create subproject
        temporaryFolder.newFolder('test-subproject')
        File settings = temporaryFolder.root.toPath().resolve('settings.gradle').toFile()
        settings << '''
            include 'test-subproject'
        '''.stripIndent()

        when:
        BuildResult buildResult = run('testDockerTestRunner').build()

        then:
        buildResult.task(':test-subproject:testDockerTestRunner').outcome == TaskOutcome.SUCCESS
        buildResult.output =~ (':test-subproject:testDockerTestRunner-openjdk-8/dockerfile UP-TO-DATE')
    }

    def 'verify testDockerTestRunner runs tests in Docker container'() {
        given:
        setupDockerGradleResources();

        String openJdk8 = 'openjdk-8'
        File openJdk8Dir = temporaryFolder.newFolder(openJdk8)
        File dockerFile = openJdk8Dir.toPath().resolve('Dockerfile').toFile()
        dockerFile.createNewFile();
        dockerFile << '''
            FROM nimmis/java-centos:openjdk-8-jdk
            ENV JAVA_HOME=/usr/lib/jvm/java
            ENV FOO=bar
        '''.stripIndent()

        buildFile << '''
            plugins {
                id 'java'
                id 'jacoco'
            }

            apply plugin: 'java'
            apply plugin: 'com.palantir.docker-test-runner'

            dockerTestRunner {
                dockerFiles = fileTree(project.rootDir) {
                    include '**/Dockerfile'
                }
            }

            repositories {
                mavenCentral()
            }

            dependencies {
                compile 'junit:junit:4.12'
            }
        '''.stripIndent()

        // create test file that will run in container. The test asserts the presence of an environment variable that
        // is set in the container file.
        temporaryFolder.newFolder('src', 'test', 'java', 'foo')
        temporaryFolder.newFile('src/test/java/foo/FooTest.java') << '''
        package foo;
        import org.junit.Assert;
        import org.junit.Test;
        public class FooTest {
            @Test
            public void fooTest() {
                String foo = System.getenv("FOO");
                Assert.assertEquals("bar", foo);
            }
        }
        '''.stripIndent()

        when:
        BuildResult buildResult = run('testDockerTestRunner').build()

        then:
        println buildResult.output
        buildResult.task(':testDockerTestRunner').outcome == TaskOutcome.SUCCESS
        buildResult.output =~ (':testClasses')
    }

}
