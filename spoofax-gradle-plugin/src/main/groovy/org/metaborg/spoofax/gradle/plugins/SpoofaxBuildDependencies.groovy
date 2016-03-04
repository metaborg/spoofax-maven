package org.metaborg.spoofax.gradle.plugins

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.plugins.ExtraPropertiesExtension.UnknownPropertyException
import org.metaborg.core.language.LanguageIdentifier

class SpoofaxBuildDependencies /*implements ExtraPropertiesExtension*/ {

    private final Project project
    private final SpoofaxBasePlugin basePlugin
 
    SpoofaxBuildDependencies(final Project project,
            SpoofaxBasePlugin basePlugin) {
        this.project = project
        this.basePlugin = basePlugin
    }

    final def configurationsById = [:].withDefault { languageId ->
        def buildConfiguration = basePlugin.createBuildConfiguration(languageId)
        basePlugin.loadLanguages(buildConfiguration.incoming.files)
        def languageImpl = basePlugin.spoofax.languageService.getImpl(languageId)
        def languageName = languageImpl.belongsTo().name()
        def configuration = new SpoofaxLanguageConfiguration(project,languageImpl)
        this.metaClass[languageName] = configuration
        this.metaClass[languageName] = { configuration.with it }
        // configurationsByName[languageName] = configuration
        configuration
    }
    final private def configurationsByName = [:]

    void language(String id) {
        configurationsById[LanguageIdentifier.parse(id)]
    }

    void language(String id, Closure closure) {
        configurationsById[LanguageIdentifier.parse(id)].with closure
    }

    /*
    @Override
    public SpoofaxLanguageConfiguration get(String name) throws UnknownPropertyException {
        def configuration = configurationsByName[name]
        if ( configuration == null ) {
            throw new UnknownPropertyException(this,name)
        }
        configuration
    }

    @Override
    public Map<String, Object> getProperties() {
        configurationsByName
    }

    @Override
    public boolean has(String name) {
        configurationsByName[name] != null
    }

    @Override
    public void set(String name, Object obj) {
        throw new GradleException("Cannot add languages by name.")
    }
    */
 
}