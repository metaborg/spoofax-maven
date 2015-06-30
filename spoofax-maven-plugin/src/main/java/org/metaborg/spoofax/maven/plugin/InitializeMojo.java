package org.metaborg.spoofax.maven.plugin;

import org.apache.commons.vfs2.FileSystemException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.metaborg.spoofax.meta.core.SpoofaxMetaBuilder;

@Mojo(name = "initialize", defaultPhase = LifecyclePhase.INITIALIZE,
    requiresDependencyResolution = ResolutionScope.COMPILE)
public class InitializeMojo extends AbstractSpoofaxLifecycleMojo {
    @Parameter(property = "spoofax.initialise.skip", defaultValue = "false") boolean skip;


    @Override public void execute() throws MojoFailureException {
        if(skip) {
            return;
        }
        super.execute();

        final SpoofaxMetaBuilder metaBuilder = getSpoofax().getInstance(SpoofaxMetaBuilder.class);

        try {
            metaBuilder.initialize(getMetaBuildInput());
        } catch(FileSystemException e) {
            throw new MojoFailureException("Error initializing", e);
        }
    }
}
