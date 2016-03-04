package org.metaborg.spoofax.gradle.plugins

import org.gradle.api.Project

class SpoofaxPluginExtension {

    private final Project project
    private final SpoofaxBasePlugin basePlugin

    final SpoofaxBuildDependencies buildDependencies

    SpoofaxPluginExtension(final Project project,
            final SpoofaxBasePlugin basePlugin) {
        this.project = project
        this.basePlugin = basePlugin
        this.buildDependencies =
                new SpoofaxBuildDependencies(project,basePlugin)
    }
    
    void buildDependencies(Closure closure) {
        buildDependencies.with closure
    }

}