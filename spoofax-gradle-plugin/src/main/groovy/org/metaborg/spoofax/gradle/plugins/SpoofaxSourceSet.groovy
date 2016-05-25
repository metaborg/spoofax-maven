package org.metaborg.spoofax.gradle.plugins

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.metaborg.spoofax.gradle.internals.SpoofaxGradleConstants

class SpoofaxSourceSet {

    final NamedDomainObjectContainer<SpoofaxLanguageDirectories> languages

    final Project project
    final String name
    final String compileTaskName


    SpoofaxSourceSet(final Project project, final String name) {
        this.project = project
        this.name = name
        this.compileTaskName = SpoofaxGradleConstants.SPOOFAX_COMPILE_TASK_NAME + (
            SpoofaxGradleConstants.SPOOFAX_MAIN_SOURCESET_NAME == name ? "" : name.capitalize())
        this.languages = project.container(SpoofaxLanguageDirectories, { languageName ->
            new SpoofaxLanguageDirectories(project, languageName)
        })
    }
 
    void languages(Closure configureClosure) {
        languages.configure(configureClosure)
    }
    
}
