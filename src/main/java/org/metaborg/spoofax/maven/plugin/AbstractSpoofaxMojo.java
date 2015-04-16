package org.metaborg.spoofax.maven.plugin;

import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.File;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.metaborg.spoofax.core.language.ILanguage;
import org.metaborg.spoofax.core.language.ILanguageDiscoveryService;
import org.metaborg.spoofax.core.resource.IResourceService;

public abstract class AbstractSpoofaxMojo extends AbstractMojo {

    public enum Format {
        ctree,
        jar
    }

    @Parameter(defaultValue = "${project.name}")
    private String languageName;

    @Parameter(defaultValue = "ctree")
    private Format languageFormat;

    @Parameter(defaultValue = "${basedir}", readonly = true, required = true)
    private File basedir;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${plugin}", readonly = true, required = true)
    private PluginDescriptor plugin;

    @Parameter(defaultValue = "${project.build.directory}")
    private File buildDirectory;

    @Parameter(defaultValue = "${project.build.outputDirectory}")
    private File javaOutputDirectory;

    public String getLanguageName() {
        return languageName;
    }

    public Format getLanguageFormat() {
        return languageFormat;
    }

    public File getBasedir() {
        return basedir;
    }

    public MavenProject getProject() {
        return project;
    }

    public PluginDescriptor getPlugin() {
        return plugin;
    }

    public File getBuildDirectory() {
        return buildDirectory;
    }

    public File getGeneratedSourceDirectory() {
        return new File(basedir, "src-gen");
    }

    public File getOutputDirectory() {
        return new File(basedir, "include");
    }

    public File getIconsDirectory() {
        return new File(basedir, "icons");
    }

    public File getJavaOutputDirectory() {
        return javaOutputDirectory;
    }

    public File getDependencyDirectory() {
        return new File(buildDirectory, "spoofax/dependency");
    }

    public File getDependencyMarkersDirectory() {
        return new File(buildDirectory, "spoofax/dependency-markers");
    }

    public File getAntDirectory() {
        return new File(buildDirectory, "spoofax/ant");
    }

    public File getNativeDirectory() throws MojoExecutionException {
        File dependencyDirectory = getDependencyDirectory();
        if ( SystemUtils.IS_OS_WINDOWS ) {
            return new File(dependencyDirectory, "native/cygwin");
        } else if ( SystemUtils.IS_OS_MAC_OSX ) {
            return new File(dependencyDirectory, "native/macosx");
        } else if ( SystemUtils.IS_OS_LINUX ) {
            return new File(dependencyDirectory, "native/linux");
        } else {
            throw new MojoExecutionException("Unsupported platform "+SystemUtils.OS_NAME);
        }
    }

    public File getDistDirectory() {
        return new File(getDependencyDirectory(), "dist");
    }

    public File getLibDirectory() {
        return new File(basedir, "lib");
    }

    public Injector getSpoofax() {
        Injector spoofax;
        if ( (spoofax = (Injector) project.getContextValue("spoofax")) == null ) {
            getLog().info("Initialising Spoofax core");
            project.setContextValue("spoofax",
                    spoofax = Guice.createInjector(new SpoofaxMavenModule(project)));
            discoverLanguages(spoofax);
        } else {
            getLog().info("Using cached Spoofax core");
        }
        return spoofax;
    }

    private void discoverLanguages(Injector spoofax) {
        IResourceService resourceService =
                spoofax.getInstance(IResourceService.class);
        ILanguageDiscoveryService languageDiscoveryService =
                spoofax.getInstance(ILanguageDiscoveryService.class);
        for ( Artifact artifact : getPlugin().getArtifacts() ) {
            if ( !artifact.getType().equals("spoofax-language") ) {
                continue;
            }
            try {
                FileObject artifactFile = resourceService.resolve("zip:"+artifact.getFile());
                for ( ILanguage language : languageDiscoveryService.discover(artifactFile) ) {
                    getLog().info(String.format("Discovered Spoofax language %s", language.name()));
                }
            } catch (Exception ex) {
                getLog().error("Error during language discovery.",ex);
            }
        }
    }

    public File getSyntaxDirectory() {
        return new File(basedir, "syntax");
    }

    public File getGeneratedSyntaxDirectory() {
        return new File(getGeneratedSourceDirectory(), "syntax");
    }

    public File getTransDirectory() {
        return new File(basedir, "trans");
    }

    @Override
    public void execute() throws MojoExecutionException {
        // this doesn't work sometimes, looks like another implementation
        // of StaticLoggerBinder is pulled in when the plugin is run?
        // StaticLoggerBinder.getSingleton().setMavenLog(getLog());
    }

}
