package org.metaborg.spoofax.gradle.plugins

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil
import org.metaborg.spoofax.gradle.internals.SpoofaxGradleConstants
import org.metaborg.spoofax.gradle.tasks.SpoofaxCompileTask


class SpoofaxPluginExtension {

    final Project project
    private final SpoofaxBasePlugin basePlugin

    final SpoofaxBuildDependencies buildDependencies
    final NamedDomainObjectContainer<SpoofaxSourceSet> sourceSets

    SpoofaxPluginExtension(final Project project,
            final SpoofaxBasePlugin basePlugin) {
        this.project = project
        this.basePlugin = basePlugin
        this.buildDependencies =
                new SpoofaxBuildDependencies(project,basePlugin)
        this.sourceSets =
                project.container(SpoofaxSourceSet,{ name ->
                    new SpoofaxSourceSet(project, name)
                })
    }
 
    void buildDependencies(Closure configureClosure) {
        ConfigureUtil.configure(configureClosure, buildDependencies)
    }

    void sourceSets(Closure configureClosure) {
        sourceSets.configure(configureClosure)
    }
 
    void createDefaultSourceSets() {
        def main = sourceSets.maybeCreate(SpoofaxGradleConstants.SPOOFAX_MAIN_SOURCESET_NAME)
        buildDependencies.languages.each { language ->
            main.languages.maybeCreate(language.belongsTo.name())
        }
    }
    
    void createTasks() {
        sourceSets.each { SpoofaxSourceSet sourceSet ->
            def task = project.task(sourceSet.compileTaskName,
                    group: SpoofaxGradleConstants.SPOOFAX_GROUP_NAME,
                    type: SpoofaxCompileTask,
                    description: "Compile ${sourceSet.name} Spoofax sources.")
        }
    }
    
}