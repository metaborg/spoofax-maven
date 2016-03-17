package org.metaborg.spoofax.gradle.internals

import org.metaborg.core.editor.IEditorRegistry
import org.metaborg.core.editor.NullEditorRegistry
import org.metaborg.core.project.IProjectService
import org.metaborg.core.project.ISimpleProjectService
import org.metaborg.core.project.SimpleProjectService
import org.metaborg.spoofax.core.SpoofaxModule

import com.google.inject.Singleton

public class GradleSpoofaxModule extends SpoofaxModule {

    @Override protected void bindProject() {
        bind(SimpleProjectService).in(Singleton)
        bind(ISimpleProjectService).to(SimpleProjectService)
        bind(IProjectService).to(SimpleProjectService)
    }

    /*
    @Override protected void bindLanguageSpecConfig() {
        bind(ILanguageSpecConfig)
            .to(ConfigurationBasedLanguageComponentConfig).in(Singleton)
        bind(ISpoofaxLanguageSpecConfig)
            .to(ConfigurationBasedSpoofaxLanguageSpecConfig).in(Singleton)
        bind(ILanguageSpecConfigWriter)
            .to(ConfigurationBasedLanguageSpecConfig).in(Singleton)
        bind(ISpoofaxLanguageSpecConfigWriter)
            .to(ConfigurationBasedSpoofaxLanguageSpecConfig).in(Singleton)
        bind(ISpoofaxLanguageSpecConfigBuilder)
            .to(ConfigurationBasedSpoofaxLanguageSpecConfig).in(Singleton)
        bind(ILanguageSpecConfigBuilder)
            .to(ConfigurationBasedSpoofaxLanguageSpecConfig).in(Singleton)
    }
    */
    
    @Override protected void bindEditor() {
        bind(IEditorRegistry).to(NullEditorRegistry).in(Singleton)
    }

}
