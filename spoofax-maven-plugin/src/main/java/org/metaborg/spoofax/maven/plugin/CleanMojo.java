package org.metaborg.spoofax.maven.plugin;

import java.io.File;
import java.io.IOException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;

@Mojo(name = "clean", defaultPhase = LifecyclePhase.CLEAN)
public class CleanMojo extends AbstractSpoofaxMojo {

    @Parameter(property = "clean.skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoFailureException {
        if ( skip ) { return; }
        super.execute();
        // remove editor/*.generated.esv
        cleanDirectory(getJavaTransDirectory());
        cleanDirectory(getOutputDirectory());
        cleanDirectory(getGeneratedSourceDirectory());
        cleanDirectory(getDependencyDirectory());
        cleanDirectory(getDependencyMarkersDirectory());
        cleanDirectory(getCacheDirectory());
    }

    private void cleanDirectory(File directory) throws MojoFailureException {
        if ( directory.exists() ) {
            getLog().info("Deleting "+directory);
            try {
                FileUtils.deleteDirectory(directory);
            } catch (IOException ex) {
                throw new MojoFailureException("",ex);
            }
        }
    }
    
}
