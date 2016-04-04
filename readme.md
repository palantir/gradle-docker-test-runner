Docker Test Runner Gradle Plugin
================================
This plugin provides a simple way to test Gradle projects in multiple
different environments specified in Dockerfiles as part of a build. Given a set
of Dockerfiles that contain the desired environments, this plugin adds tasks
for running the project's tests and coverage tasks in those containers. The
test and coverage output for each environment is persisted separately, and
it is possible to combine the coverage from the tests run in all of the
environments using [gradle-jacoco-coverage](https://github.com/palantir/gradle-jacoco-coverage).

This plugin makes it trivial to test existing projects against multiple
different environments in a lightweight manner. The ability to easily run tests
in different environments locally makes the development process faster and
allows environment verification to be part of the core build. It also allows
code coverage to properly account for code that will only execute in certain
environments.

An example use case of this plugin is testing a project against multiple
different versions or vendors of a JDK.

Usage
-----
Apply the plugin using standard gradle convention:

    plugins {
        id 'com.palantir.docker-test-runner'
    }

Because this plugin adds test and coverage tasks, it requires the 'java' and 'jacoco' plugins to already be applied.

Configure the 'dockerFiles' property of the dockerTestRunner configuration
object to be a FileCollection of the DockerFiles that should be used as the
test environments:

    dockerTestRunner {
        dockerFiles = fileTree(project.rootDir) {
            include '**/Dockerfile'
        }
    }

The `dockerTestRunner` block offers the following options:

 * `dockerFiles` the Dockerfiles to use as test environments.
 * (optional) `jacocoClassDirectories` is an optional closure that can be
  provided to control the output of the Jacoco reports. The closure is
  provided with a `FileCollection` that contains all of the classes that will
  be used for coverage and must return the classes that should be considered
  for coverage. This can be useful if there are certain classes that should
  be excluded for coverage purposes.
 * (optional) `customDockerRunArgs` is a [`Multimap`](http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/collect/Multimap.html)
   that can be provided with custom arguments that should be provided to the
   Docker `run` commmand that is executed by the task. The keys are the flags
   and the values are the values for the flag.

Tasks
-----
Each environment has an identifier that is derived from the parent directory
and name of its Dockerfile. For example, if
'environments/oraclejdk-7/dockerfile' and 'environments/openjdk-8/dockerfile' were specified as Dockerfiles, their environment identifiers would be
'oraclejdk-7/dockerfile' and 'openjdk-8/dockerfile', respectively. The
identifiers must be unique (the plugin will throw an exception if a set of
Dockerfiles that would result in identifier collisions are specified).

Each environment has its own task group that contains 3 tasks:
* `buildDockerTestRunner-{identifier}`
* `runJacocoTestReportDockerTestRunner-{identifier}`
* `runTestDockerTestRunner-{identifier}`

If there is at least one environment, then the following bulk tasks that run
the tasks for all of the environments are added:
* `buildDockerTestRunner`
* `jacocoTestReportDockerTestRunner`
* `testDockerTestRunner`

Build
-----
The build tasks run `docker build` to create the images for the environments.
The build tasks are invoked automatically as needed, but can be called directly
to build the images for caching purposes.

Test
----
The test tasks use `docker run` to run a Docker container of the image for the
specified environment and then runs a Gradle test task within that container.
The test task that is run within the container tests all of the classes in the
project's Gradle test output classes directory using the test runtime path.

The destination for the XML and HTML reports will be the same destination used
by the standard Gradle test task with the identifier appended to it. For
example, if the regular test tasks writes its output to 'build/reports/tests',
then this test task will write its output to
'build/reports/tests-{identifier}'.

The raw Jacoco execution output is written to
"${project.getBuildDir()}/jacoco/${identifier}.exec".

Jacoco Test Report
------------------
The Jacoco test report tasks are the same as the test tasks but generate a
coverage report. The output location of the coverage reports is the same
location used by the standard Gradle Jacoco reports task, but the name of
the directory within the reports directory will be the environment identifier.

Combining Coverage
------------------
The coverage outputs of multiple different test environments can be combined
using [gradle-jacoco-coverage](https://github.com/palantir/gradle-jacoco-coverage)
without any further modifications. Running that plugin's `jacocoFullReport`
task will create a report that includes the coverage outputs of the tests
run in the Docker environments.

In addition to creating these scripts, this plugin will merge the entire
contents of `${projectDir}/service` and `${projectDir}/var` into the package.

License
-------
This plugin is made available under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0).
