package org.metaborg.spoofax.gradle.plugins

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.metaborg.core.action.CompileGoal
import org.metaborg.core.build.BuildInputBuilder
import org.metaborg.core.language.LanguageFileSelector
import org.metaborg.core.messages.StreamMessagePrinter
import org.metaborg.core.project.IProject
import org.metaborg.spoofax.core.Spoofax
import org.metaborg.spoofax.core.resource.SpoofaxIgnoresSelector
import org.metaborg.spoofax.gradle.internals.SpoofaxGradleConstants
import org.metaborg.util.log.LoggerUtils

class SpoofaxPlugin implements Plugin<Project> {
    private static def logger = LoggerUtils.logger(SpoofaxPlugin)

    private Project gradleProject
    private SpoofaxBasePlugin basePlugin
    def SpoofaxPluginExtension extension
    
    @Override
    void apply(final Project project) {
        this.gradleProject = project
        throwIfPluginConflict()
        basePlugin = project.plugins.apply(SpoofaxBasePlugin)
        extension = createExtension()
        project.afterEvaluate {
            project.spoofax.createDefaultSourceSets()
            project.spoofax.createTasks()
        }
    }

    private def throwIfPluginConflict() {
        if ( gradleProject.plugins.hasPlugin(SpoofaxMetaPlugin) ) {
            throw new GradleException("Cannot apply plugin {} when plugin {} is already applied.",
                    SpoofaxGradleConstants.SPOOFAX_PLUGIN_NAME,
                    SpoofaxGradleConstants.SPOOFAX_META_PLUGIN_NAME)
        }
    }

    private def createExtension() {
        gradleProject.extensions.create(SpoofaxGradleConstants.SPOOFAX_EXTENSION_NAME,
                SpoofaxPluginExtension, gradleProject, basePlugin)
    }
 
    @Lazy Spoofax spoofax = basePlugin.spoofax

    @Lazy IProject spoofaxProject =
            spoofax.projectService.create(spoofax.resourceService.resolve(gradleProject.rootDir))


    def compile() {
        def inputBuilder = new BuildInputBuilder(spoofaxProject)
                .withSelector(new SpoofaxIgnoresSelector())
                .withMessagePrinter(new StreamMessagePrinter(spoofax.sourceTextService, true, true, logger))
                .withThrowOnErrors(true)
                .addTransformGoal(new CompileGoal())
        extension.buildDependencies.configurations.each { configuration ->
            inputBuilder.addLanguage(configuration.languageImpl)
            def files = configuration.main.srcFiles.files
                    .collectMany { srcDir ->
                        spoofax.resourceService.resolve(srcDir)
                                .findFiles(new LanguageFileSelector(spoofax.languageIdentifierService,
                                        configuration.languageImpl)) as List
                    }
            inputBuilder.withSources(files)
        }
        def input = inputBuilder.build(spoofax.dependencyService, spoofax.languagePathService)
        try {
            spoofax.processorRunner.build(input, null, null).schedule().block();
        } catch(Exception e) {
            throw new GradleException("Error compiling sources", e);
        }
    }
 
}