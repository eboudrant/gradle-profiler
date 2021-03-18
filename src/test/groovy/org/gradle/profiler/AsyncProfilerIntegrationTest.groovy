package org.gradle.profiler


import spock.lang.Requires

class AsyncProfilerIntegrationTest extends AbstractProfilerIntegrationTest {
    @Requires({ !OperatingSystem.isWindows() })
    def "profiles build CPU usage using async-profiler with tooling API and warm daemon"() {
        given:
        instrumentedBuildScript()

        when:
        new Main().run("--project-dir", projectDir.absolutePath, "--output-dir", outputDir.absolutePath, "--gradle-version", latestSupportedGradleVersion, "--profile", "async-profiler", "assemble")

        then:
        logFile.find("<daemon: true").size() == 4
        logFile.containsOne("<invocations: 3>")

        and:
        assertGraphsGenerated()
    }

    void assertGraphsGenerated(String event = "cpu") {
        ["raw", "simplified"].each { type ->
            assert new File(outputDir, "${latestSupportedGradleVersion}-${event}-${type}-flames.svg").file
            assert new File(outputDir, "${latestSupportedGradleVersion}-${event}-${type}-icicles.svg").file
        }

    }

    @Requires({ !OperatingSystem.isWindows() })
    def "profiles heap allocation using async-profiler with tooling API and warm daemon"() {
        given:
        instrumentedBuildScript()

        when:
        new Main().run("--project-dir", projectDir.absolutePath, "--output-dir", outputDir.absolutePath, "--gradle-version", latestSupportedGradleVersion, "--profile", "async-profiler-heap", "assemble")

        then:
        logFile.find("<daemon: true").size() == 4
        logFile.containsOne("<invocations: 3>")

        and:
        assertGraphsGenerated("allocation")
    }

    @Requires({ !OperatingSystem.isWindows() })
    def "profiles multiple iterations using async-profiler with tooling API and warm daemon"() {
        given:
        instrumentedBuildScript()

        when:
        new Main().run("--project-dir", projectDir.absolutePath, "--output-dir", outputDir.absolutePath, "--gradle-version", latestSupportedGradleVersion, "--profile", "async-profiler", "--iterations", "2", "assemble")

        then:
        logFile.find("<daemon: true").size() == 5
        logFile.containsOne("<invocations: 4>")

        and:
        assertGraphsGenerated()
    }

    @Requires({ !OperatingSystem.isWindows() })
    def "profiles build using async-profiler with tooling API and cold daemon"() {
        given:
        instrumentedBuildScript()

        when:
        new Main().run("--project-dir", projectDir.absolutePath, "--output-dir", outputDir.absolutePath, "--gradle-version", latestSupportedGradleVersion, "--profile", "async-profiler", "--cold-daemon", "assemble")

        then:
        logFile.find("<daemon: true").size() == 3
        logFile.find("<invocations: 1>").size() == 3

        and:
        assertGraphsGenerated()
    }

    @Requires({ !OperatingSystem.isWindows() })
    def "profiles build using async-profiler with tooling API and no daemon"() {
        given:
        instrumentedBuildScript()

        when:
        new Main().run("--project-dir", projectDir.absolutePath, "--output-dir", outputDir.absolutePath, "--gradle-version", latestSupportedGradleVersion, "--profile", "async-profiler", "--no-daemon", "assemble")

        then:
        logFile.find("<daemon: true").size() == 1
        logFile.find("<daemon: false").size() == 2
        logFile.find("<invocations: 1>").size() == 3

        and:
        assertGraphsGenerated()
    }

    @Requires({ !OperatingSystem.isWindows() })
    def "cannot profile using async-profiler with multiple iterations and cold daemon"() {
        given:
        instrumentedBuildScript()

        when:
        new Main().run("--project-dir", projectDir.absolutePath, "--output-dir", outputDir.absolutePath, "--gradle-version", latestSupportedGradleVersion, "--profile", "async-profiler", "--iterations", "2", "--cold-daemon", "assemble")

        then:
        thrown(IllegalArgumentException)

        and:
        output.contains("Scenario using Gradle ${latestSupportedGradleVersion}: Profiler async profiler does not support profiling multiple daemons.")
    }

    @Requires({ !OperatingSystem.isWindows() })
    def "cannot profile using async-profiler with multiple iterations and no daemon"() {
        given:
        instrumentedBuildScript()

        when:
        new Main().run("--project-dir", projectDir.absolutePath, "--output-dir", outputDir.absolutePath, "--gradle-version", latestSupportedGradleVersion, "--profile", "async-profiler", "--iterations", "2", "--no-daemon", "assemble")

        then:
        thrown(IllegalArgumentException)

        and:
        output.contains("Scenario using Gradle ${latestSupportedGradleVersion}: Profiler async profiler does not support profiling multiple daemons.")
    }

    @Requires({ !OperatingSystem.isWindows() })
    def "scenario name can contain reserved characters"() {
        given:
        instrumentedBuildScript()
        def scenarios = file("performance.scenarios")
        scenarios << """
            a/b {
            }
        """

        when:
        new Main().run("--project-dir", projectDir.absolutePath, "--output-dir", outputDir.absolutePath, "--gradle-version", latestSupportedGradleVersion, "--profile", "async-profiler", "--scenario-file", scenarios.absolutePath, "a/b")

        then:
        logFile.find("<daemon: true").size() == 4
        logFile.containsOne("<invocations: 3>")

        and:
        ["raw", "simplified"].each { type ->
            assert new File(outputDir, "a-b/a-b-${latestSupportedGradleVersion}-cpu-${type}-flames.svg").file
            assert new File(outputDir, "a-b/a-b-${latestSupportedGradleVersion}-cpu-${type}-icicles.svg").file
        }
    }
}
