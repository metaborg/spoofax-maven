package org.metaborg.spoofax.maven.plugin;

import java.io.File;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "initialize", defaultPhase = LifecyclePhase.INITIALIZE)
public class InitializeMojo extends AbstractSpoofaxMojo {

    @Parameter(property = "spoofax.initialise.skip", defaultValue = "false")
    boolean skip;

    @Override
    public void execute() throws MojoFailureException {
        if ( skip ) { return; }
        super.execute();
        mkdirs(getOutputDirectory());
        mkdirs(getGeneratedSourceDirectory());
        mkdirs(getGeneratedSyntaxDirectory());
    }
    
    private void mkdirs(File dir) {
        if ( !dir.exists() ) {
            getLog().info("Creating: "+dir.getPath());
            dir.mkdirs();
        }
    }

}
