package org.metaborg.spoofax.maven.plugin;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.metaborg.spoofax.generator.project.ProjectException;
import org.metaborg.spoofax.generator.project.ProjectSettings;
import org.metaborg.spoofax.generator.project.ProjectSettings.Format;
import org.metaborg.spoofax.meta.core.MetaBuildInput;

public abstract class AbstractSpoofaxLifecycleMojo extends AbstractSpoofaxMojo {
    @Parameter(defaultValue = "${project.name}") private String name;

    @Parameter(defaultValue = "${project.artifactId}") private String id;

    @Parameter(defaultValue = "${project.version}") private String version;

    @Parameter private Format format;

    @Parameter private List<String> sdfArgs;

    @Parameter private List<String> strategoArgs;

    @Parameter private File externalDef;

    @Parameter private String externalJar;

    @Parameter private String externalJarFlags;

    @Parameter private List<String> pardonedLanguages;

    private ProjectSettings projectSettings;
    private MetaBuildInput metaBuildInput;


    @Override public void execute() throws MojoFailureException {
        buildProjectSettings();
        buildMetaBuildInput();
    }


    private void buildProjectSettings() throws MojoFailureException {
        try {
            projectSettings = new ProjectSettings(name, getBasedir());
            projectSettings.setFormat(format);
            projectSettings.setId(id);
            projectSettings.setVersion(version);
        } catch(ProjectException ex) {
            throw new MojoFailureException(ex.getMessage(), ex);
        }
    }

    public ProjectSettings getProjectSettings() {
        return projectSettings;
    }

    private void buildMetaBuildInput() throws MojoFailureException {
        metaBuildInput =
            new MetaBuildInput(getSpoofaxProject(), getPardonedLanguages(), getProjectSettings(), getSdfArgs(),
                getStrategoArgs(), externalDef == null ? null : externalDef.getAbsolutePath(), externalJar,
                externalJarFlags);
    }

    public MetaBuildInput getMetaBuildInput() {
        return metaBuildInput;
    }

    private List<String> getSdfArgs() {
        return sdfArgs == null ? Collections.<String>emptyList() : sdfArgs;
    }

    private List<String> getStrategoArgs() {
        return strategoArgs == null ? Collections.<String>emptyList() : strategoArgs;
    }

    private List<String> getPardonedLanguages() {
        return pardonedLanguages != null ? pardonedLanguages : Collections.<String>emptyList();
    }
}
