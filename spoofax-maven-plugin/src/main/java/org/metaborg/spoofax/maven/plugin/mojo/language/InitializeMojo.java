package org.metaborg.spoofax.maven.plugin.mojo.language;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.metaborg.core.MetaborgException;
import org.metaborg.spoofax.maven.plugin.SpoofaxInit;

@Mojo(name = "initialize", defaultPhase = LifecyclePhase.INITIALIZE,
    requiresDependencyResolution = ResolutionScope.COMPILE)
public class InitializeMojo extends AbstractSpoofaxLanguageMojo {
    @Parameter(property = "spoofax.initialise.skip", defaultValue = "false") boolean skip;


    @Override public void execute() throws MojoFailureException, MojoExecutionException {
        try {
            if(skip || skipAll) {
                return;
            }
            super.execute();
    
            try {
                SpoofaxInit.spoofaxMeta().metaBuilder.initialize(buildInput());
            } catch(MetaborgException e) {
                throw new MojoFailureException("Error initializing", e);
            }
        } finally {
            SpoofaxInit.close();
        }
    }
}
