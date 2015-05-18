package org.metaborg.spoofax.maven.plugin;

import java.io.File;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.metaborg.spoofax.generator.project.ProjectException;
import org.metaborg.spoofax.generator.project.ProjectSettings;
import org.metaborg.spoofax.generator.project.ProjectSettings.Format;

public abstract class AbstractSpoofaxLifecycleMojo extends AbstractSpoofaxMojo {

    public final static String TYPE_SPOOFAX_LANGUAGE = "spoofax-language";

    @Parameter(defaultValue = "${project.name}")
    private String name;

    @Parameter(defaultValue = "${project.artifactId}")
    private String id;

    @Parameter
    private Format format;

    @Parameter
    private List<String> sdfArgs;

    @Parameter
    private List<String> strategoArgs;

    @Parameter
    private File externalDef;

    @Parameter
    private String externalJar;

    @Parameter
    private String externalJarFlags;

    @Parameter
    private List<String> pardonedLanguages;

    private ProjectSettings projectSettings;

    @Override
    public void execute() throws MojoFailureException {
        // this doesn't work sometimes, looks like another implementation
        // of StaticLoggerBinder is pulled in when the plugin is run?
        // StaticLoggerBinder.getSingleton().setMavenLog(getLog());
        try {
        projectSettings = new ProjectSettings(name, getBasedir());
        projectSettings.setFormat(format);
        projectSettings.setId(id);
        } catch (ProjectException ex) {
            throw new MojoFailureException(ex.getMessage(), ex);
        }
    }

    public ProjectSettings getProjectSettings() {
        return projectSettings;
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

    public List<String> getSdfArgs() {
        return sdfArgs == null ? Collections.EMPTY_LIST : sdfArgs;
    }

    public List<String> getStrategoArgs() {
        return strategoArgs == null ? Collections.EMPTY_LIST : strategoArgs;
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

}
