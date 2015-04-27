package org.metaborg.spoofax.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.util.FileUtils;

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
    public void execute() throws MojoFailureException {
        if ( skip ) { return; }
        super.execute();
        getLog().info("Packaging Spoofax language");
        createPackage();
    }

    private void createPackage() throws MojoFailureException {
        File languageArchive = new File(getBuildDirectory(),
                finalName+"."+getProject().getPackaging());
        getLog().info("Creating "+languageArchive);
        zipArchiver.setDestFile(languageArchive);
        zipArchiver.setForced(true);
        addDirectory(getOutputDirectory());
        addDirectory(getIconsDirectory());
        try {
            for ( Resource resource : getProject().getResources() ) {
                addResource(resource);
            }
            zipArchiver.createArchive();
        } catch (ArchiverException | IOException ex) {
            throw new MojoFailureException("Error creating archive.", ex);
        }
        getProject().getArtifact().setFile(languageArchive);
    }
 
    private void addDirectory(File directory) {
        addDirectory(directory, directory.getName());
    }

    private void addDirectory(File directory, String name) {
        if (directory.exists()) {
            if ( !name.isEmpty() && !name.endsWith("/") ) {
                name += "/";
            }
            getLog().info("Adding "+directory);
            zipArchiver.addDirectory(directory, name);
        } else {
            getLog().info("Ignored non-existing "+directory);
        }
    }

    private void addResource(Resource resource) throws IOException {
        File directory = new File(resource.getDirectory());
        if ( directory.exists() ) {
            String target = resource.getTargetPath() != null ?
                    resource.getTargetPath() : "";
            target += "/";
            List<String> fileNames = FileUtils.getFileNames(directory,
                    StringUtils.join(resource.getIncludes(), ", "),
                    StringUtils.join(resource.getExcludes(), ", "),
                    false);
            getLog().info("Adding "+directory);
            for ( String fileName : fileNames ) {
                zipArchiver.addFile(new File(directory, fileName), target+fileName);
            }
        } else {
            getLog().info("Ignored non-existing "+directory);
        }
    }

}
