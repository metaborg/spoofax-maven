package org.metaborg.spoofax.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.metaborg.spoofax.core.project.ISpoofaxLanguageSpecPaths;
import org.metaborg.spoofax.core.project.configuration.ISpoofaxLanguageSpecConfig;
import org.metaborg.spoofax.core.project.settings.Format;
import org.metaborg.spoofax.meta.core.LanguageSpecBuildInput;

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

//    private SpoofaxProjectSettings projectSettings;
    private ISpoofaxLanguageSpecConfig config;
    private ISpoofaxLanguageSpecPaths paths;


    @Override public void execute() throws MojoFailureException, MojoExecutionException {
        super.execute();
        
        try {
//            projectSettings = projectSettingsService.get(getMetaborgProject());
            this.config = configService.get(getLanguageSpec());
            if (this.config == null)
                throw new RuntimeException("Could not retrieve configuration.");

            this.paths = pathsService.get(getLanguageSpec());
            if (this.paths == null)
                throw new RuntimeException("Could not retrieve paths.");
        } catch(IOException e) {
            throw new MojoExecutionException("Could not retrieve project settings", e);
        }
    }


//    public SpoofaxProjectSettings getProjectSettings() {
//        return projectSettings;
//    }
    public ISpoofaxLanguageSpecConfig getLanguageSpecConfig() {
        return this.config;
    }
    public ISpoofaxLanguageSpecPaths getLanguageSpecPaths() {
        return this.paths;
    }
    protected LanguageSpecBuildInput createBuildInput() {
        return new LanguageSpecBuildInput(getLanguageSpec(), getLanguageSpecConfig(), getLanguageSpecPaths());
    }
}
