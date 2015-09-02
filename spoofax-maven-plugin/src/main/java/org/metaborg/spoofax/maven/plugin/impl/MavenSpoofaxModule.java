package org.metaborg.spoofax.maven.plugin.impl;

import org.metaborg.core.MetaborgModule;
import org.metaborg.core.editor.IEditorRegistry;
import org.metaborg.core.editor.NullEditorRegistry;
import org.metaborg.core.project.IProjectService;
import org.metaborg.core.project.ISimpleProjectService;
import org.metaborg.core.project.SimpleProjectService;
import org.metaborg.spoofax.core.SpoofaxModule;
import org.metaborg.spoofax.core.project.IMavenProjectService;
import org.metaborg.spoofax.core.project.ISimpleMavenProjectService;
import org.metaborg.spoofax.core.project.SimpleMavenProjectService;

import com.google.inject.Singleton;

public class MavenSpoofaxModule extends SpoofaxModule {
    /**
     * Overrides {@link MetaborgModule#bindProject()} for non-dummy implementation of project service.
     */
    @Override protected void bindProject() {
        bind(SimpleProjectService.class).in(Singleton.class);
        bind(ISimpleProjectService.class).to(SimpleProjectService.class);
        bind(IProjectService.class).to(SimpleProjectService.class);
    }

    /**
     * Overrides {@link SpoofaxModule#bindMavenProject()} for non-dummy implementation of Maven project service.
     */
    @Override protected void bindMavenProject() {
        bind(SimpleMavenProjectService.class).in(Singleton.class);
        bind(ISimpleMavenProjectService.class).to(SimpleMavenProjectService.class);
        bind(IMavenProjectService.class).to(SimpleMavenProjectService.class);
    }

    /**
     * Overrides {@link MetaborgModule#bindEditor()} for null implementation of editor registry.
     */
    @Override protected void bindEditor() {
        bind(IEditorRegistry.class).to(NullEditorRegistry.class).in(Singleton.class);
    }
}
