package org.metaborg.spoofax.gradle.plugins

import org.gradle.api.Project
import org.metaborg.core.language.LanguageIdentifier
import org.metaborg.core.language.LanguageImplementation

class SpoofaxBuildDependencies {

    private final Project project
    private final SpoofaxBasePlugin basePlugin
 
    // FIXME: Make sure to reject conflicting language versions in one project.

    SpoofaxBuildDependencies(final Project project,
            SpoofaxBasePlugin basePlugin) {
        this.project = project
        this.basePlugin = basePlugin
    }

    private final Map<LanguageIdentifier,LanguageImplementation> languageCache =
            [:].withDefault { id -> basePlugin.loadBuildLanguage(id) }

    void language(String id) {
        languageCache[LanguageIdentifier.parse(id)]
    }

    void languages(Iterable<String> ids) {
        ids.each { language(it) }
    }

    Iterable<LanguageImplementation> getLanguages() {
        languageCache.values()
    }

}