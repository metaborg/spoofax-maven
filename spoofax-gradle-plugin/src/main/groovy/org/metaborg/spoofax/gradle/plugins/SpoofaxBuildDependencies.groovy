package org.metaborg.spoofax.gradle.plugins

import org.gradle.api.Project
import org.metaborg.core.language.LanguageIdentifier

class SpoofaxBuildDependencies {

    private final Project project
    private final SpoofaxBasePlugin basePlugin
 
    SpoofaxBuildDependencies(final Project project,
            SpoofaxBasePlugin basePlugin) {
        this.project = project
        this.basePlugin = basePlugin
    }

    private final Map<LanguageIdentifier,SpoofaxLanguageConfiguration> configurationsById =
            [:].withDefault { languageId ->
                def languageImpl = basePlugin.loadBuildLanguage(languageId)
                def configuration = new SpoofaxLanguageConfiguration(project,languageImpl)
                def languageName = languageImpl.belongsTo().name()
                this.metaClass[languageName] = configuration
                this.metaClass[languageName] = { configuration.with it }
                configuration
            }

    void language(String id) {
        configurationsById[LanguageIdentifier.parse(id)]
    }

    void language(String id, Closure closure) {
        configurationsById[LanguageIdentifier.parse(id)].with closure
    }

    Iterable<LanguageIdentifier> getLanguages() {
        configurationsById.keySet()
    }
    
    Iterable<SpoofaxLanguageConfiguration> getConfigurations() {
        configurationsById.values()
    }

}