package org.metaborg.spoofax.gradle.plugins

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

class SpoofaxPlugin implements Plugin<Project> {

    @Override
    void apply(final Project gradleProject) {
        throwIfPluginConflict(gradleProject)
        gradleProject.plugins.apply(SpoofaxBasePlugin)
        createTasks(gradleProject)
    }

    private def throwIfPluginConflict(final Project project) {
        if ( project.plugins.hasPlugin(SpoofaxMetaPlugin) ) {
            throw new GradleException("Cannot apply plugin {} when plugin {} is already applied.",
                SpoofaxGradleConstants.SPOOFAX_PLUGIN_NAME,
                SpoofaxGradleConstants.SPOOFAX_META_PLUGIN_NAME)
        }
    }

    private def createExtension(final Project project) {
        project.extensions.create(SpoofaxGradleConstants.SPOOFAX_EXTENSION_NAME,
            SpoofaxPluginExtension)
    }
 
    private def createTasks(final Project project) {
        project.task('compileSpoofax', type: CompileTask,
            group: SpoofaxGradleConstants.SPOOFAX_GROUP_NAME,
            description: "Compile Spoofax sources.")
    }

    @Lazy private def init = {
        // TODO: create configurations for buildDependencies
        // TODO: load build languages
        true // return non-null, or `init` runs every time
    }()
    
    def generateSources() {
        init
        // TODO: compile files in source directories
    }
 
}