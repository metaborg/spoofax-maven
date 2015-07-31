package org.metaborg.spoofax.maven.plugin;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.metaborg.core.project.ProjectException;
import org.metaborg.spoofax.core.project.settings.Format;
import org.metaborg.spoofax.core.project.settings.SpoofaxProjectSettings;

public abstract class AbstractSpoofaxLifecycleMojo extends AbstractSpoofaxMojo {
    @Parameter(defaultValue = "${project.artifactId}") private String id;
    @Parameter(defaultValue = "${project.version}") private String version;
    @Parameter(defaultValue = "${project.name}") private String name;

    /* Not using these parameters to create project settings, but they generate XML schema for POM files */
    @Parameter private List<LanguageContribution> languageContributions;
    @Parameter private List<String> pardonedLanguages;
    @Parameter private Format format;
    @Parameter private List<String> sdfArgs;
    @Parameter private List<String> strategoArgs;
    @Parameter private File externalDef;
    @Parameter private String externalJar;
    @Parameter private String externalJarFlags;

    private SpoofaxProjectSettings projectSettings;


    @Override public void execute() throws MojoFailureException, MojoExecutionException {
        super.execute();

        try {
            projectSettings = projectSettingsService.get(getSpoofaxProject());
        } catch(ProjectException e) {
            throw new MojoExecutionException("Could not retrieve project settings", e);
        }
    }


    public SpoofaxProjectSettings getProjectSettings() {
        return projectSettings;
    }
}
