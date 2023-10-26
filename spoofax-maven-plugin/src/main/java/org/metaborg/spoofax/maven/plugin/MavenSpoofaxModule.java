package org.metaborg.spoofax.maven.plugin;

import org.metaborg.core.MetaborgModule;
import org.metaborg.core.editor.IEditorRegistry;
import org.metaborg.core.editor.NullEditorRegistry;
import org.metaborg.spoofax.core.SpoofaxModule;

import com.google.inject.Singleton;

public class MavenSpoofaxModule extends SpoofaxModule {
    /**
     * Overrides {@link MetaborgModule#bindEditor()} for null implementation of editor registry.
     */
    @Override protected void bindEditor() {
        bind(IEditorRegistry.class).to(NullEditorRegistry.class).in(Singleton.class);
    }
}
