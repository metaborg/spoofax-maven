package org.metaborg.spoofax.maven.plugin;

import java.io.File;
import java.io.IOException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.archiver.util.ResourceUtils;

@Mojo(name = "initialize", defaultPhase = LifecyclePhase.INITIALIZE)
public class InitializeMojo extends AbstractSpoofaxMojo {

    @Parameter(property = "initialize.skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException {
        if ( skip ) { return; }
        super.execute();
        try {
            File antDirectory = getAntDirectory();
            if ( !antDirectory.exists() ) {
                antDirectory.mkdirs();
            }
            copyResource("build.main.xml", antDirectory);
            copyResource("build.generated.xml", antDirectory);
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to copy Ant files.", ex);
        }
    }
   
    private void copyResource(String name, File directory) throws IOException {
        ResourceUtils.copyFile(getClass().getResourceAsStream(name),
                new File(directory, name));
    }

}
