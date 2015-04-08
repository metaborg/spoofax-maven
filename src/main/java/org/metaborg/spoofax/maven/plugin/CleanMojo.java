package org.metaborg.spoofax.maven.plugin;

import java.io.File;
import java.io.IOException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;

@Mojo(name = "clean", defaultPhase = LifecyclePhase.CLEAN)
public class CleanMojo extends AbstractSpoofaxMojo {

    @Parameter(property = "clean.skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException {
        if ( skip ) { return; }
        super.execute();
        cleanDirectory(getDependencyDirectory());
        cleanDirectory(getDependencyMarkersDirectory());
        cleanDirectory(getGeneratedSourceDirectory());
        cleanDirectory(getOutputDirectory());
    }

    private void cleanDirectory(File directory) throws MojoExecutionException {
        if ( directory.exists() ) {
            getLog().info("Deleting "+directory);
            try {
                FileUtils.cleanDirectory(directory);
            } catch (IOException ex) {
                throw new MojoExecutionException("",ex);
            }
        }
    }
    
}
