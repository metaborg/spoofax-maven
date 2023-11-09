package org.metaborg.spoofax.maven.plugin.mojo.language;

import jakarta.annotation.Nullable;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.metaborg.core.config.ConfigException;
import org.metaborg.spoofax.maven.plugin.AbstractSpoofaxMojo;
import org.metaborg.spoofax.maven.plugin.SpoofaxInit;
import org.metaborg.spoofax.meta.core.build.LanguageSpecBuildInput;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpec;

public abstract class AbstractSpoofaxLanguageMojo extends AbstractSpoofaxMojo {
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
