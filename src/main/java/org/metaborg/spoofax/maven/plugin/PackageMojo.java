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
import org.codehaus.plexus.archiver.zip.ZipArchiver;

@Mojo(name="package",
        defaultPhase = LifecyclePhase.PACKAGE)
public class PackageMojo extends AbstractRunAntMojo {

    @Component(role = Archiver.class, hint = "zip")
    private ZipArchiver archiver;

    @Parameter(defaultValue = "${project.build.finalName}", required = true)
    private String finalName;

    @Override
    public void execute() throws MojoExecutionException {
        super.execute();
        executeAntTarget("package");
        createPackage();
    }

    private void createPackage() throws MojoExecutionException {
        File languageFile = new File(getBuildDirectory(), finalName+"."+getProject().getPackaging());
        getLog().info("Creating "+languageFile);
        getLog().info("Creating "+getProject().getArtifact());

        archiver.setDestFile(languageFile);
        archiver.addDirectory(getIncludeDirectory(), "include/");
        try {
            archiver.createArchive();
        } catch (ArchiverException | IOException ex) {
            throw new MojoExecutionException("Error creating archive.", ex);
        }

        getProject().getArtifact().setFile(languageFile);
    }
 
}
