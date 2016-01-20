package org.metaborg.spoofax.maven.plugin;

import org.apache.commons.vfs2.FileSystemException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.metaborg.spoofax.meta.core.LanguageSpecBuildInput;

@Mojo(name = "initialize", defaultPhase = LifecyclePhase.INITIALIZE,
    requiresDependencyResolution = ResolutionScope.COMPILE)
public class InitializeMojo extends AbstractSpoofaxLifecycleMojo {
    @Parameter(property = "spoofax.initialise.skip", defaultValue = "false") boolean skip;


    @Override public void execute() throws MojoFailureException, MojoExecutionException {
        if(skip || skipAll) {
            return;
        }
        super.execute();

        final LanguageSpecBuildInput metaInput = createBuildInput();
//        final MetaBuildInput input = new MetaBuildInput(getMetaborgProject(), getProjectSettings());

        try {
            metaBuilder.initialize(metaInput);
        } catch(FileSystemException e) {
            throw new MojoFailureException("Error initializing", e);
        }
    }
}
