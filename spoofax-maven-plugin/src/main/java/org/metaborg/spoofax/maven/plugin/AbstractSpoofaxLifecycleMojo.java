package org.metaborg.spoofax.maven.plugin;

import java.io.File;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.metaborg.core.config.ConfigException;
import org.metaborg.spoofax.meta.core.build.LanguageSpecBuildInput;
import org.metaborg.spoofax.meta.core.config.StrategoFormat;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpec;

public abstract class AbstractSpoofaxLifecycleMojo extends AbstractSpoofaxMojo {
    @Parameter(defaultValue = "${project.artifactId}") private String id;
    @Parameter(defaultValue = "${project.version}") private String version;
    @Parameter(defaultValue = "${project.name}") private String name;

    /* Not using these parameters to create project settings, but they generate XML schema for POM files */
    @Parameter private List<LanguageContribution> languageContributions;
    @Parameter private List<String> pardonedLanguages;
    @Parameter private StrategoFormat format;
    @Parameter private List<String> sdfArgs;
    @Parameter private List<String> strategoArgs;
    @Parameter private File externalDef;
    @Parameter private String externalJar;
    @Parameter private String externalJarFlags;

    @Nullable private ISpoofaxLanguageSpec languageSpec;


    @Override public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();

        try {
            languageSpec = SpoofaxInit.spoofaxMeta().languageSpecService.get(project());
        } catch(ConfigException e) {
            throw new MojoExecutionException("Cannot get language specification project", e);
        }

        if(languageSpec == null) {
            throw new MojoExecutionException("Project is not a language specification");
        }
    }


    public ISpoofaxLanguageSpec languageSpec() {
        return languageSpec;
    }

    public LanguageSpecBuildInput buildInput() {
        return new LanguageSpecBuildInput(languageSpec);
    }
}
