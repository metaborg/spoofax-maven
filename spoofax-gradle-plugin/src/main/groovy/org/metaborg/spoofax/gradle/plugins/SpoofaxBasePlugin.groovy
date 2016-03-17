package org.metaborg.spoofax.gradle.plugins

import org.apache.commons.vfs2.FileObject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.file.FileCollection
import org.metaborg.core.language.ILanguageComponent
import org.metaborg.core.language.ILanguageDiscoveryRequest
import org.metaborg.core.language.LanguageIdentifier
import org.metaborg.core.language.LanguageImplementation
import org.metaborg.spoofax.core.Spoofax
import org.metaborg.spoofax.gradle.internals.GradleSpoofaxModule
import org.metaborg.util.log.LoggerUtils

class SpoofaxBasePlugin implements Plugin<Project> {
    private static final def logger = LoggerUtils.logger(SpoofaxBasePlugin)

    private Project project
 
    @Override
    void apply(final Project project) {
        this.project = project
    }
 
    
    @Lazy Spoofax spoofax =
            new Spoofax(new GradleSpoofaxModule())

    LanguageImplementation loadBuildLanguage(LanguageIdentifier languageId) {
        def languageImpl = spoofax.languageService.getImpl(languageId)
        if ( languageImpl == null ) {
            def configuration = createBuildConfiguration(languageId)
            loadLanguages(configuration)
            languageImpl = spoofax.languageService.getImpl(languageId)
        }
        languageImpl
    }
 
    Configuration createBuildConfiguration(final LanguageIdentifier language) {
        Configuration configuration = project.configurations.create(
            SpoofaxGradleConstants.SPOOFAX_BUILD_CONFIGURATION_BASENAME+language.toString())
        configuration.transitive = true
        configuration.visible = false
        configuration.dependencies.add(
            project.dependencies.create(createLanguageDependency(language)))
        configuration
    }
 
    private def createLanguageDependency(final LanguageIdentifier language) {
        def dependency = project.dependencies.create([
                group: language.groupId,
                name: language.id,
                version: language.version.toString()])
        if ( dependency instanceof ExternalModuleDependency ) {
            if ( dependency.version.endsWith("-SNAPSHOT") ) {
                dependency.changing = true
            }
        }
        dependency
    }

    def loadLanguages(final FileCollection files) {
        files.files.collectMany { file ->
            def url = (file.isDirectory() ? "file:" : "zip:") + file.getPath();
            FileObject location = spoofax.resourceService.resolve(url)
            spoofax.languageDiscoveryService.request(location).findResults { request  ->
                ILanguageComponent language =
                        spoofax.languageDiscoveryService.discover(request)
                if ( language == null ) {
                    logger.warn("Unable to load language from {}",url)
                }
                language
            }
        }
    }
 
}