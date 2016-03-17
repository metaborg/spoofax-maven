package org.metaborg.spoofax.maven.plugin.mojo.manual;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.metaborg.core.language.LanguageVersion;
import org.metaborg.core.project.ProjectException;
import org.metaborg.spoofax.maven.plugin.AbstractSpoofaxMojo;
import org.metaborg.spoofax.maven.plugin.SpoofaxInit;
import org.metaborg.spoofax.meta.core.generator.language.AnalysisType;
import org.metaborg.spoofax.meta.core.generator.language.ContinuousLanguageSpecGenerator;
import org.metaborg.spoofax.meta.core.generator.language.GeneratorSettingsBuilder;
import org.metaborg.spoofax.meta.core.generator.language.GeneratorSettingsBuilder.FullGeneratorSettings;
import org.metaborg.spoofax.meta.core.generator.language.LanguageSpecGenerator;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.prompt.Prompter;

import com.google.common.base.Joiner;

@Mojo(name = "generate", requiresDirectInvocation = true, requiresProject = false)
public class GenerateProjectMojo extends AbstractSpoofaxMojo {
    private static final ILogger logger = LoggerUtils.logger(GenerateProjectMojo.class);

    private static String defaultVersionString = "0.1.0";
    private static AnalysisType defaultAnalysisType = AnalysisType.NaBL_TS;

    @Parameter(defaultValue = "${groupId}", required = false) private String groupId;
    @Parameter(defaultValue = "${id}", required = false) private String id;
    @Parameter(defaultValue = "${version}", required = false) private String version;
    @Parameter(defaultValue = "${name}", required = false) private String name;
    @Parameter(defaultValue = "${plugin.version}", required = false) private String metaborgVersion;
    @Parameter(defaultValue = "${extension}", required = false) private String[] extensions;
    @Parameter(defaultValue = "${analysisType}", required = false) private AnalysisType analysisType;


    @Override public void execute() throws MojoFailureException, MojoExecutionException {
        super.execute();

        final MavenProject project = mavenProject();

        if(project != null && project.getFile() != null) {
            final String message = logger.format("Found existing project {}, not continuing", project.getName());
            throw new MojoFailureException(message);
        }

        GeneratorSettingsBuilder settingsBuilder = new GeneratorSettingsBuilder()
                .withGroupId(groupId)
                .withId(id)
                .withVersion((version != null && LanguageVersion.valid(version)) ?
                        LanguageVersion.parse(version) : null)
                .withName(name)
                .withMetaborgVersion(metaborgVersion)
                .withExtensions(extensions)
                .withAnalysisType(analysisType)
                .withDefaultVersion(defaultVersionString)
                .withDefaultAnalysisType(defaultAnalysisType)
                ;
 
        if(!settingsBuilder.isComplete()) {
            Prompter prompter;
            try {
                prompter = Prompter.get();
            } catch(IOException e) {
                throw new MojoFailureException("Must run interactively", e);
            }
            settingsBuilder.configureFromPrompt(prompter);
        }
 
        generate(settingsBuilder);
    }


    private void generate(GeneratorSettingsBuilder settingsBuilder) throws MojoFailureException {
        if ( settingsBuilder.canBuild() ) {
            try {
                FullGeneratorSettings settings =
                        settingsBuilder.build(basedirLocation(),
                                SpoofaxInit.spoofaxMeta().languageSpecConfigBuilder());

                final LanguageSpecGenerator newGenerator =
                    new LanguageSpecGenerator(settings.generatorSettings,
                            settings.extensions, settings.analysisType);
                newGenerator.generateAll();

                final ContinuousLanguageSpecGenerator generator =
                        new ContinuousLanguageSpecGenerator(settings.generatorSettings);
                generator.generateAll();
            } catch(IOException ex) {
                throw new MojoFailureException("Failed to generate project files", ex);
            } catch(ProjectException ex) {
                throw new MojoFailureException("Invalid project settings", ex);
            }
        } else {
            throw new MojoFailureException("Missing required "+
                    Joiner.on(", ").join(settingsBuilder.stillRequired()));
        }
    }
}
