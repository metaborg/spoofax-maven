package org.metaborg.spoofax.maven.plugin;

import java.io.File;
import java.io.IOException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;

@Mojo(name="post-compile",
        defaultPhase = LifecyclePhase.COMPILE)
public class PostCompileMojo extends AbstractSpoofaxMojo {

    @Component(role = Archiver.class, hint = "jar")
    private JarArchiver jarArchiver;

    @Parameter(property = "spoofax.compile.skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException {
        if ( skip ) { return; }
        super.execute();
        getLog().info("Creating language JAR");
        createJAR();
    }

    private void createJAR() throws MojoExecutionException {
        File javaOutputDirectory = getJavaOutputDirectory();
        if ( !javaOutputDirectory.exists() ) {
            getLog().info("Skipping empty language JAR.");
            return;
        }
        File languageJAR = new File(getOutputDirectory(),
                getLanguageName().toLowerCase()+"-java.jar");
        getLog().info("Creating "+languageJAR);
        jarArchiver.setDestFile(languageJAR);
        jarArchiver.addDirectory(javaOutputDirectory, "");
        try {
            jarArchiver.createArchive();
        } catch (ArchiverException | IOException ex) {
            throw new MojoExecutionException("Error creating archive.", ex);
        }
    }
    
}
