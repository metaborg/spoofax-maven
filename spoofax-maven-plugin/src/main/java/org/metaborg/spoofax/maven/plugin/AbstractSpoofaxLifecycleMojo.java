package org.metaborg.spoofax.maven.plugin;

import java.io.File;
import java.util.List;

import org.apache.maven.plugins.annotations.Parameter;
import org.metaborg.spoofax.core.project.settings.StrategoFormat;
import org.metaborg.spoofax.meta.core.SpoofaxLanguageSpecBuildInput;

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


    protected SpoofaxLanguageSpecBuildInput createBuildInput() {
        return new SpoofaxLanguageSpecBuildInput(getLanguageSpec());
    }
}
