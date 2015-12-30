package org.metaborg.spoofax.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.utils.io.FileUtils;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.ILanguageComponent;
import org.metaborg.spoofax.core.project.settings.SpoofaxProjectSettings;
import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.Iterables;

@Mojo(name = "package", defaultPhase = LifecyclePhase.PACKAGE)
public class PackageMojo extends AbstractSpoofaxLifecycleMojo {
    @Component(role = Archiver.class, hint = "zip") private ZipArchiver zipArchiver;

    @Parameter(defaultValue = "${project.build.finalName}") private String finalName;
    @Parameter(property = "spoofax.package.skip", defaultValue = "false") private boolean skip;


    @Override public void execute() throws MojoFailureException, MojoExecutionException {
        if(skip || skipAll) {
            getProject().getArtifact().setFile(archiveFile());
            return;
        }
        super.execute();

        getLog().info("Packaging Spoofax language");
        final File archive = createPackage();

        final FileObject archiveResource = resourceService.resolve("zip://" + archive.getAbsolutePath());
        getLog().info("Reloading language from: " + archiveResource);
        try {
            final Iterable<ILanguageComponent> components = languageDiscoveryService.discover(archiveResource);
            if(Iterables.isEmpty(components)) {
                throw new MojoExecutionException("Failed to reload language, no components were discovered");
            }
        } catch(MetaborgException e) {
            throw new MojoExecutionException("Failed to reload language", e);
        }
    }

    private File archiveFile() {
        return new File(getBuildDirectory(), finalName + "." + getProject().getPackaging());
    }

    private File createPackage() throws MojoFailureException {
        final File languageArchive = archiveFile();
        getLog().info("Creating " + languageArchive);
        zipArchiver.setDestFile(languageArchive);
        zipArchiver.setForced(true);
        try {
            final SpoofaxProjectSettings settings = getProjectSettings();
            addDirectory(settings.getIncludeDirectory());
            addDirectory(settings.getIconsDirectory());
            // TODO: Get these filenames and paths from the ISpoofaxLanguageSpecPaths object.
            addFiles(getJavaOutputDirectory(), "", Iterables2.<String>empty(), Iterables2.from("trans/**"));
            addFiles(new File(getProject().getFile().getParentFile(), "src-gen"), "src-gen",
                Iterables2.from("metaborg.generated.yaml"), Iterables2.<String>empty());
            addFiles(getProject().getFile().getParentFile(), "", Iterables2.from("metaborg.yml"), Iterables2.<String>empty());
            for(Resource resource : getProject().getResources()) {
                addResource(resource);
            }
            zipArchiver.createArchive();
        } catch(ArchiverException | IOException ex) {
            throw new MojoFailureException("Error creating archive", ex);
        }
        getProject().getArtifact().setFile(languageArchive);
        return languageArchive;
    }

    private void addDirectory(FileObject directory) throws IOException {
        addDirectory(directory, Iterables2.<String>empty(), Iterables2.<String>empty());
    }

    private void addDirectory(FileObject directory, Iterable<String> includes, Iterable<String> excludes)
        throws IOException {
        final File localDirectory = resourceService.localPath(directory);
        addFiles(localDirectory, localDirectory.getName(), includes, excludes);
    }

    private void addResource(Resource resource) throws IOException {
        final File directory = new File(resource.getDirectory());
        final String target = resource.getTargetPath() != null ? resource.getTargetPath() : "";
        addFiles(directory, target, resource.getIncludes(), resource.getExcludes());
    }

    @SuppressWarnings("unused") private void addFiles(File directory, String target) throws IOException {
        addFiles(directory, target, Iterables2.<String>empty(), Iterables2.<String>empty());
    }

    private void addFiles(File directory, String target, Iterable<String> includes, Iterable<String> excludes)
        throws IOException {
        if(directory.exists()) {
            if(!(target.isEmpty() || target.endsWith("/"))) {
                target += "/";
            }
            final String include = Iterables.isEmpty(includes) ? "**" : StringUtils.join(includes, ", ");
            final String exclude = StringUtils.join(excludes, ", ");
            final List<String> fileNames = FileUtils.getFileNames(directory, include, exclude, false);

            getLog().info("Adding " + directory + (target.isEmpty() ? "" : " as " + target));
            for(String fileName : fileNames) {
                zipArchiver.addFile(new File(directory, fileName), target + fileName);
            }
        } else {
            getLog().info("Ignored non-existing " + directory);
        }
    }
}
