package com.sourcemuse.gradle.plugin

import static com.sourcemuse.gradle.plugin.BuildScriptBuilder.buildScript
import static com.sourcemuse.gradle.plugin.MongoUtils.mongoInstanceRunning
import static TestHelpersPlugin.MONGO_RUNNING_FLAG
import static TestHelpersPlugin.TEST_START_MANAGED_MONGO_DB
import static TestHelpersPlugin.TEST_START_MONGO_DB
import static TestHelpersPlugin.TEST_STOP_MONGO_DB

import org.gradle.testkit.functional.ExecutionResult
import org.gradle.testkit.functional.GradleRunnerFactory

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class MongoPluginTasksSpec extends Specification {

    @Rule TemporaryFolder tmp
    def gradleRunner = GradleRunnerFactory.create()

    def buildScriptPluginDeclarations =
            """
            |apply plugin: ${GradleMongoPlugin.name}
            |apply plugin: ${TestHelpersPlugin.name}
            |
            """.stripMargin()

    def 'individual tasks can declare a dependency on a running mongo instance'() {
        given:
        generate(buildScriptPluginDeclarations +
            """
            |task A {
            |   withMongoDbRunning()
            |}
            """.stripMargin())
        gradleRunner.arguments << 'A'

        when:
        ExecutionResult result = gradleRunner.run()
        def mongoRunningDuringBuild = result.standardOutput.contains('mongo running!')

        then:
        mongoRunningDuringBuild
    }

    def 'mongo does not start when task is skipped'() {
        given:
        generate(buildScriptPluginDeclarations +
                """
            |task A {
            |   withMongoDbRunning()
            |   outputs.
            |}
            """.stripMargin())
        gradleRunner.arguments << 'A'

        when:
        ExecutionResult result = gradleRunner.run()
        def mongoRunningDuringBuild = result.standardOutput.contains('mongo running!')

        then:
        mongoRunningDuringBuild
    }

    def 'startManagedMongoDb starts a mongo instance, and then stops once the build has completed'() {
        given:
        generate(buildScript())
        gradleRunner.arguments << TEST_START_MANAGED_MONGO_DB

        when:
        ExecutionResult result = gradleRunner.run()
        def mongoRunningDuringBuild = result.standardOutput.contains(MONGO_RUNNING_FLAG)
        def mongoRunningAfterBuild = mongoInstanceRunning()

        then:
        mongoRunningDuringBuild
        !mongoRunningAfterBuild
    }

    def 'startMongoDb starts a mongo instance that continues running after the build has completed'() {
        given:
        generate(buildScript())
        gradleRunner.arguments << TEST_START_MONGO_DB

        when:
        ExecutionResult result = gradleRunner.run()
        def mongoRunningDuringBuild = result.standardOutput.contains(MONGO_RUNNING_FLAG)
        def mongoRunningAfterBuild = mongoInstanceRunning()

        then:
        mongoRunningDuringBuild
        mongoRunningAfterBuild
    }

    def 'stopMongoDb stops the mongo instance'() {
        given:
        generate(buildScript())
        gradleRunner.arguments << TEST_STOP_MONGO_DB

        when:
        gradleRunner.run()
        def mongoRunningAfterBuild = mongoInstanceRunning()

        then:
        !mongoRunningAfterBuild
    }


    def cleanup() {
        //ensureMongoIsStopped()
    }

    void generate(BuildScriptBuilder buildScriptBuilder) {
        generate(buildScriptBuilder.build())
    }

    void generate(String buildScriptContent) {
        tmp.newFile('build.gradle') << buildScriptContent
        gradleRunner.directory = tmp.root
    }
}
