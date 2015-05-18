package org.metaborg.spoofax.maven.plugin.impl;

import com.google.inject.Singleton;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.metaborg.spoofax.core.SpoofaxModule;
import org.metaborg.spoofax.core.context.ILanguagePathService;
import org.metaborg.spoofax.core.project.IProjectService;

public class SpoofaxMavenModule extends SpoofaxModule {

    private final MavenProject project;
    private final PluginDescriptor plugin;
    private final MavenLanguagePathService languagePathService;

    public SpoofaxMavenModule(MavenProject project, PluginDescriptor plugin,
            MavenLanguagePathService languageComponentsService) {
        this.project = project;
        this.plugin = plugin;
        this.languagePathService = languageComponentsService;
    }

    @Override
    protected void bindLanguagePath() {
        bind(ILanguagePathService.class)
                .toInstance(languagePathService);
    }

    @Override
    protected void bindProject() {
        bind(IProjectService.class).to(MavenProjectService.class).in(Singleton.class);
    }

    @Override
    protected void bindOther() {
        super.bindOther();
        bind(MavenProject.class).toInstance(project);
        bind(PluginDescriptor.class).toInstance(plugin);
    }

}
