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
public class PackageMojo extends AbstractSpoofaxMojo {

    @Component(role = Archiver.class, hint = "zip")
    private ZipArchiver zipArchiver;

    @Parameter(defaultValue = "${project.build.finalName}")
    private String finalName;

    @Parameter(property = "spoofax.package.skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException {
        if ( skip ) { return; }
        super.execute();
        getLog().info("Packaging Spoofax language");
        createPackage();
    }

    private void createPackage() throws MojoExecutionException {
        File languageArchive = new File(getBuildDirectory(),
                finalName+"."+getProject().getPackaging());
        getLog().info("Creating "+languageArchive);
        zipArchiver.setDestFile(languageArchive);
        addDirectory(getOutputDirectory());
        addDirectory(getLibDirectory());
        try {
            zipArchiver.createArchive();
        } catch (ArchiverException | IOException ex) {
            throw new MojoExecutionException("Error creating archive.", ex);
        }
        getProject().getArtifact().setFile(languageArchive);
    }
 
    private void addDirectory(File directory) {
        if (directory.exists()) {
            getLog().info("Adding "+directory);
            zipArchiver.addDirectory(directory, directory.getName()+"/");
        } else {
            getLog().info("Ignored non-existing "+directory);
        }
    }

}
