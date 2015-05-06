package org.metaborg.spoofax.maven.plugin;

import java.io.File;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.metaborg.spoofax.generator.project.ProjectException;
import org.metaborg.spoofax.generator.project.ProjectSettings;
import org.metaborg.spoofax.generator.project.ProjectSettings.Format;
import org.metaborg.spoofax.maven.plugin.impl.FileHelper;

public abstract class AbstractSpoofaxMojo extends AbstractMojo {

    public final static String TYPE_SPOOFAX_LANGUAGE = "spoofax-language";

    @Parameter(defaultValue = "${project.name}")
    private String name;

    @Parameter(defaultValue = "${project.artifactId}")
    private String id;

    @Parameter
    private Format format;

    @Parameter
    private String[] sdfArgs;

    @Parameter
    private String[] strategoArgs;

    @Parameter
    private File externalDef;

    @Parameter
    private String externalJar;

    @Parameter
    private String externalJarFlags;

    @Parameter
    private List<File> additionalSources;

    @Parameter
    private List<String> pardonedLanguages;

    @Parameter(defaultValue = "${basedir}", readonly = true, required = true)
    private File basedir;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${plugin}", readonly = true, required = true)
    private PluginDescriptor plugin;

    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    private File buildDirectory;

    @Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true)
    private File javaOutputDirectory;

    private ProjectSettings projectSettings;

    @Override
    public void execute() throws MojoFailureException {
        // this doesn't work sometimes, looks like another implementation
        // of StaticLoggerBinder is pulled in when the plugin is run?
        // StaticLoggerBinder.getSingleton().setMavenLog(getLog());
        try {
        projectSettings = new ProjectSettings(name, basedir);
        projectSettings.setFormat(format);
        projectSettings.setId(id);
        } catch (ProjectException ex) {
            throw new MojoFailureException(ex.getMessage(), ex);
        }
    }

    public MavenProject getProject() {
        return project;
    }

    public PluginDescriptor getPlugin() {
        return plugin;
    }

    public ProjectSettings getProjectSettings() {
        return projectSettings;
    }

    public File getBuildDirectory() {
        return FileHelper.getAbsoluteFile(buildDirectory,basedir);
    }

    public File getJavaOutputDirectory() {
        return FileHelper.getAbsoluteFile(javaOutputDirectory,basedir);
    }

    public File getDependencyDirectory() {
        return new File(getBuildDirectory(), "spoofax/dependency");
    }

    public File getDependencyMarkersDirectory() {
        return new File(getBuildDirectory(), "spoofax/dependency-markers");
    }

    public File getAntDirectory() {
        return new File(getBuildDirectory(), "spoofax/ant");
    }

    public File getNativeDirectory() throws MojoFailureException {
        File dependencyDirectory = getDependencyDirectory();
        if ( SystemUtils.IS_OS_WINDOWS ) {
            return new File(dependencyDirectory, "native/cygwin");
        } else if ( SystemUtils.IS_OS_MAC_OSX ) {
            return new File(dependencyDirectory, "native/macosx");
        } else if ( SystemUtils.IS_OS_LINUX ) {
            return new File(dependencyDirectory, "native/linux");
        } else {
            throw new MojoFailureException("Unsupported platform "+SystemUtils.OS_NAME);
        }
    }

    public File getDistDirectory() {
        return new File(getDependencyDirectory(), "dist");
    }

    public String[] getSdfArgs() {
        return sdfArgs == null ? new String[0] : sdfArgs;
    }

    public String[] getStrategoArgs() {
        return strategoArgs == null ? new String[0] : strategoArgs;
    }

    @Nullable
    public File getExternalDef() {
        return externalDef;
    }

    @Nullable
    public String getExternalJar() {
        return externalJar;
    }

    @Nullable
    public String getExternalJarFlags() {
        return externalJarFlags;
    }

    public List<String> getPardonedLanguages() {
        return pardonedLanguages != null ?
                pardonedLanguages : Collections.EMPTY_LIST;
    }

    public List<File> getAdditionalSources() {
        return additionalSources != null ?
                additionalSources : Collections.EMPTY_LIST;
    }

}
