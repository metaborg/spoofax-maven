package org.metaborg.spoofax.maven.plugin.mojo.manual;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.metaborg.core.MetaborgConstants;
import org.metaborg.core.language.LanguageIdentifier;
import org.metaborg.core.language.LanguageVersion;
import org.metaborg.core.project.NameUtil;
import org.metaborg.core.project.ProjectException;
import org.metaborg.spoofax.generator.language.AnalysisType;
import org.metaborg.spoofax.generator.language.LanguageSpecGenerator;
import org.metaborg.spoofax.generator.language.NewLanguageSpecGenerator;
import org.metaborg.spoofax.maven.plugin.AbstractSpoofaxMojo;
import org.metaborg.spoofax.maven.plugin.SpoofaxInit;
import org.metaborg.spoofax.maven.plugin.misc.Prompter;
import org.metaborg.spoofax.meta.core.config.ISpoofaxLanguageSpecConfig;
import org.metaborg.spoofax.meta.core.project.GeneratorSettings;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpecPaths;
import org.metaborg.spoofax.meta.core.project.SpoofaxLanguageSpecPaths;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

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

        final MavenProject project = getProject();

        if(project != null && project.getFile() != null) {
            final String message = logger.format("Found existing project {}, not continuing", project.getName());
            throw new MojoFailureException(message);
        }

        if(groupId == null || id == null || version == null || name == null || extensions == null
            || analysisType == null) {
            generateFromPrompt();
        } else {
            generateFromParameters();
        }
    }

    private void generateFromPrompt() throws MojoFailureException {
        Prompter prompter;
        try {
            prompter = Prompter.get();
        } catch(IOException e) {
            throw new MojoFailureException("Must run interactively", e);
        }

        String groupId = this.groupId;
        while(groupId == null || groupId.isEmpty()) {
            groupId = prompter.readString("Group ID (e.g. 'org.metaborg')");
            if(!LanguageIdentifier.validId(groupId)) {
                System.err.println("Please enter a valid id");
                groupId = null;
            }
        }

        String name = this.name;
        while(name == null || name.isEmpty()) {
            name = prompter.readString("Name");
            if(!LanguageIdentifier.validId(name)) {
                System.err.println("Please enter a valid name");
                name = null;
            }
        }

        final String defaultId = name.toLowerCase();
        String id = this.id;
        while(id == null || id.isEmpty()) {
            id = prompter.readString("Id [" + defaultId + "]");
            id = id.isEmpty() ? defaultId : id;
            if(!LanguageIdentifier.validId(id)) {
                System.err.println("Please enter a valid id");
                id = null;
            }
        }

        LanguageVersion version = null;
        if(this.version != null && LanguageVersion.valid(this.version)) {
            version = LanguageVersion.parse(this.version);
        }
        while(version == null) {
            final String versionString = prompter.readString("Version [" + defaultVersionString + "]");
            if(versionString.isEmpty()) {
                version = LanguageVersion.parse(defaultVersionString);
            } else if(!LanguageVersion.valid(versionString)) {
                System.err.println("Please enter a valid version");
                version = null;
            } else {
                version = LanguageVersion.parse(versionString);
            }
        }

        String defaultExt = name.toLowerCase().substring(0, Math.min(name.length(), 3));
        String[] exts = this.extensions;
        while(exts == null) {
            exts = prompter.readString("File extensions (space separated) [" + defaultExt + "]").split("[\\ \t\n]+");
            if(exts.length == 0 || (exts.length == 1 && exts[0].isEmpty())) {
                exts = new String[] { defaultExt };
            }
            for(String ext : exts) {
                if(!NameUtil.isValidFileExtension(ext)) {
                    System.err.println("Please enter valid file extensions. Invalid: " + ext);
                    exts = null;
                }
            }
        }

        AnalysisType analysisType = this.analysisType;
        while(analysisType == null) {
            final String analysisTypeString = prompter.readString("Choose the type of analysis [" + defaultAnalysisType
                + "] (choose from:" + Joiner.on(", ").join(AnalysisType.values()) + ")");
            if(analysisTypeString.isEmpty()) {
                analysisType = defaultAnalysisType;
            } else {
                try {
                    analysisType = AnalysisType.valueOf(analysisTypeString);
                } catch(IllegalArgumentException e) {
                    System.err.println("Please enter a valid analysis type");
                    analysisType = null;
                }
            }
        }

        String metaborgVersion = this.metaborgVersion;
        while(metaborgVersion == null || metaborgVersion.isEmpty()) {
            metaborgVersion =
                prompter.readString("Version for MetaBorg artifacts [" + MetaborgConstants.METABORG_VERSION + "]");
            if(metaborgVersion.isEmpty()) {
                metaborgVersion = MetaborgConstants.METABORG_VERSION;
            }
        }

        final LanguageIdentifier identifier = new LanguageIdentifier(groupId, id, version);
        generate(identifier, name, metaborgVersion, exts, analysisType);
    }

    private void generateFromParameters() throws MojoFailureException {
        final LanguageVersion version = LanguageVersion.parse(this.version);
        final LanguageIdentifier identifier = new LanguageIdentifier(groupId, id, version);
        generate(identifier, name, metaborgVersion, extensions, analysisType);
    }


    private void generate(LanguageIdentifier identifier, String name, String metaborgVersion, String[] exts,
        AnalysisType analysisType) throws MojoFailureException {
        try {
            final ISpoofaxLanguageSpecConfig config = SpoofaxInit.spoofaxMeta().languageSpecConfigBuilder()
                .withIdentifier(identifier).withName(name).build(getBasedirLocation());
            final ISpoofaxLanguageSpecPaths paths = new SpoofaxLanguageSpecPaths(getBasedirLocation(), config);
            final GeneratorSettings generatorSettings = new GeneratorSettings(config, paths);
            generatorSettings.setMetaborgVersion(metaborgVersion);

            final NewLanguageSpecGenerator newGenerator =
                new NewLanguageSpecGenerator(generatorSettings, exts, analysisType);
            newGenerator.generateAll();
            final LanguageSpecGenerator generator = new LanguageSpecGenerator(generatorSettings);
            generator.generateAll();
        } catch(IOException ex) {
            throw new MojoFailureException("Failed to generate project files", ex);
        } catch(ProjectException ex) {
            throw new MojoFailureException("Invalid project settings", ex);
        }
    }
}
