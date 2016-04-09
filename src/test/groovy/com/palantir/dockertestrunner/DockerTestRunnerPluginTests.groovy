package com.palantir.dockertestrunner

import com.google.common.collect.ImmutableSet
import org.gradle.api.Project
import org.gradle.api.ProjectState
import org.gradle.api.internal.project.DefaultProject
import org.gradle.api.internal.project.ProjectStateInternal
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class DockerTestRunnerPluginTests extends Specification {

    @Rule
    TemporaryFolder temporaryFolder = new TemporaryFolder(new File("."))

    private Project createRootProject() {
        return ProjectBuilder
                .builder()
                .withProjectDir(temporaryFolder.root)
                .build()
    }

    def 'verify that plugin fails if nonexistent Dockerfiles are specified'() {
        given:
        DefaultProject project = createRootProject()
        DockerTestRunnerPlugin plugin = new DockerTestRunnerPlugin()

        when:
        plugin.apply(project)
        project.dockerTestRunner {
            dockerFiles project.files('/foo/bar/nonexistent')
        }

        ProjectState state = new ProjectStateInternal()
        state.executed()
        project.getProjectEvaluationBroadcaster().afterEvaluate(project, state)

        then:
        IllegalStateException ex = thrown()
        ex.message =~ 'The following files were either nonexistent or were directories'
        ex.message =~ '/foo/bar/nonexistent'
    }

    def 'verify that Jacoco tasks are not added if Jacoco plugin is not applied'() {
        given:
        File parentFolder = temporaryFolder.newFolder("jdk7")
        File dockerFile = parentFolder.toPath().resolve("Dockerfile").toFile()
        dockerFile.createNewFile()

        DefaultProject project = createRootProject()
        project.apply plugin: 'java'

        DockerTestRunnerPlugin plugin = new DockerTestRunnerPlugin()

        when:
        plugin.apply(project)
        project.dockerTestRunner {
            dockerFiles project.files(dockerFile.absolutePath)
        }

        ProjectState state = new ProjectStateInternal()
        state.executed()
        project.getProjectEvaluationBroadcaster().afterEvaluate(project, state)

        then:
        // no Jacoco tasks should be present
        project.tasks.find { it.name =~ 'jacoco' } == null
    }

    def 'verify that Dockerfile name is derived from parent and file name'() {
        given:
        File parentFolder = temporaryFolder.newFolder("jdk7")
        File dockerFile = parentFolder.toPath().resolve("Dockerfile").toFile()
        dockerFile.createNewFile()

        DefaultProject project = createRootProject()
        project.apply plugin: 'java'
        project.apply plugin: 'jacoco'

        DockerTestRunnerPlugin plugin = new DockerTestRunnerPlugin()

        when:
        plugin.apply(project)
        project.dockerTestRunner {
            dockerFiles project.files(dockerFile.absolutePath)
        }

        ProjectState state = new ProjectStateInternal()
        state.executed()
        project.getProjectEvaluationBroadcaster().afterEvaluate(project, state)

        then:
        // taks should be added with name that's parent directory + child file name
        project.tasks.find {
            it.name =~ 'jdk7/dockerfile'
        }
    }

    def 'verify task added by dockerRunner has provided name'() {
        given:
        File parentFolder = temporaryFolder.newFolder("jdk7")
        File dockerFile = parentFolder.toPath().resolve("Dockerfile").toFile()
        dockerFile.createNewFile()

        DefaultProject project = createRootProject()
        project.apply plugin: 'java'
        project.apply plugin: 'jacoco'

        DockerTestRunnerPlugin plugin = new DockerTestRunnerPlugin()

        when:
        plugin.apply(project)
        project.dockerTestRunner {
            dockerRunner name: 'custom-jdk', dockerFile: dockerFile
        }

        ProjectState state = new ProjectStateInternal()
        state.executed()
        project.getProjectEvaluationBroadcaster().afterEvaluate(project, state)

        then:
        // taks should be added with specified name
        project.tasks.find {
            it.name =~ 'custom-jdk'
        }
    }

    def 'verify task added by dockerRunner without name uses generated name'() {
        given:
        File parentFolder = temporaryFolder.newFolder("jdk7")
        File dockerFile = parentFolder.toPath().resolve("Dockerfile").toFile()
        dockerFile.createNewFile()

        DefaultProject project = createRootProject()
        project.apply plugin: 'java'
        project.apply plugin: 'jacoco'

        DockerTestRunnerPlugin plugin = new DockerTestRunnerPlugin()

        when:
        plugin.apply(project)
        project.dockerTestRunner {
            dockerRunner dockerFile: dockerFile
        }

        ProjectState state = new ProjectStateInternal()
        state.executed()
        project.getProjectEvaluationBroadcaster().afterEvaluate(project, state)

        then:
        // taks should be added with name that's parent directory + child file name
        project.tasks.find {
            it.name =~ 'jdk7/dockerfile'
        }
    }

    def 'verify task added by dockerRunner must have dockerFile'() {
        given:
        File parentFolder = temporaryFolder.newFolder("jdk7")
        File dockerFile = parentFolder.toPath().resolve("Dockerfile").toFile()
        dockerFile.createNewFile()

        DefaultProject project = createRootProject()
        project.apply plugin: 'java'
        project.apply plugin: 'jacoco'

        DockerTestRunnerPlugin plugin = new DockerTestRunnerPlugin()

        when:
        plugin.apply(project)
        project.dockerTestRunner {
            dockerRunner name: 'custom-jdk'
        }

        ProjectState state = new ProjectStateInternal()
        state.executed()
        project.getProjectEvaluationBroadcaster().afterEvaluate(project, state)

        then:
        IllegalArgumentException ex = thrown()
        ex.message == 'dockerFile was not specified'
    }

    def 'verify project-level test configuration'() {
        given:
        File parentFolder = temporaryFolder.newFolder("jdk7")
        File dockerFile = parentFolder.toPath().resolve("Dockerfile").toFile()
        dockerFile.createNewFile()

        DefaultProject project = createRootProject()
        project.apply plugin: 'java'
        project.apply plugin: 'jacoco'

        DockerTestRunnerPlugin plugin = new DockerTestRunnerPlugin()
        Set<String> testIncludes = ImmutableSet.of('foobar')

        when:
        plugin.apply(project)
        project.dockerTestRunner {
            dockerRunner dockerFile: dockerFile
            testConfig = {
                includes = testIncludes
            }
        }

        ProjectState state = new ProjectStateInternal()
        state.executed()
        project.getProjectEvaluationBroadcaster().afterEvaluate(project, state)

        then:
        project.tasks.getByName('testDockerTestRunner-jdk7/dockerfile').includes == testIncludes
    }

    def 'verify test-level test configuration'() {
        given:
        File parentFolder = temporaryFolder.newFolder("jdk7")
        File dockerFile = parentFolder.toPath().resolve("Dockerfile").toFile()
        dockerFile.createNewFile()

        DefaultProject project = createRootProject()
        project.apply plugin: 'java'
        project.apply plugin: 'jacoco'

        DockerTestRunnerPlugin plugin = new DockerTestRunnerPlugin()
        Set<String> testIncludes = ImmutableSet.of('foobar')

        when:
        plugin.apply(project)
        project.dockerTestRunner {
            dockerRunner dockerFile: dockerFile, testConfig: {
                includes = testIncludes
            }
        }

        ProjectState state = new ProjectStateInternal()
        state.executed()
        project.getProjectEvaluationBroadcaster().afterEvaluate(project, state)

        then:
        project.tasks.getByName('testDockerTestRunner-jdk7/dockerfile').includes == testIncludes
    }

    def 'verify project-level and test-level configurations are both applied'() {
        given:
        File parentFolder = temporaryFolder.newFolder("jdk7")
        File dockerFile = parentFolder.toPath().resolve("Dockerfile").toFile()
        dockerFile.createNewFile()

        DefaultProject project = createRootProject()
        project.apply plugin: 'java'
        project.apply plugin: 'jacoco'

        DockerTestRunnerPlugin plugin = new DockerTestRunnerPlugin()
        String projectInclude = 'project-include'
        String testInclude = 'test-include'

        when:
        plugin.apply(project)
        project.dockerTestRunner {
            dockerRunner dockerFile: dockerFile, testConfig: {
                includes << testInclude
            }

            testConfig = {
                includes << projectInclude
            }
        }

        ProjectState state = new ProjectStateInternal()
        state.executed()
        project.getProjectEvaluationBroadcaster().afterEvaluate(project, state)

        then:
        project.tasks.getByName('testDockerTestRunner-jdk7/dockerfile').includes == ImmutableSet.of(projectInclude, testInclude)
    }

}
