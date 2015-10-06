package org.metaborg.spoofax.maven.plugin;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.metaborg.core.language.LanguageIdentifier;
import org.metaborg.core.language.LanguageVersion;
import org.metaborg.core.project.NameUtil;
import org.metaborg.core.project.ProjectException;
import org.metaborg.core.project.settings.IProjectSettings;
import org.metaborg.core.project.settings.ProjectSettings;
import org.metaborg.spoofax.core.SpoofaxConstants;
import org.metaborg.spoofax.core.project.settings.SpoofaxProjectSettings;
import org.metaborg.spoofax.generator.NewProjectGenerator;
import org.metaborg.spoofax.generator.ProjectGenerator;
import org.metaborg.spoofax.generator.project.GeneratorProjectSettings;
import org.metaborg.spoofax.maven.plugin.impl.Prompter;

@Mojo(name = "generate", requiresDirectInvocation = true, requiresProject = false)
public class GenerateProjectMojo extends AbstractSpoofaxMojo {
    @Parameter(defaultValue = "${basedir}", readonly = true, required = true) private File basedir;
    @Parameter(defaultValue = "${project}", readonly = true) private MavenProject project;


    @Override public void execute() throws MojoFailureException, MojoExecutionException {
        super.execute();

        if(project.getFile() != null) {
            getLog().error("Found existing project " + project.getName());
            return;
        }

        Prompter prompter;
        try {
            prompter = Prompter.get();
        } catch(IOException ex) {
            throw new MojoFailureException("Must run interactively.", ex);
        }

        String name = null;
        while(name == null) {
            name = prompter.readString("Name");
            if(!LanguageIdentifier.validId(name)) {
                System.err.println("Please enter a valid name.");
                name = null;
            }
        }

        final String defaultId = name.toLowerCase();
        String id = null;
        while(id == null) {
            id = prompter.readString("Id [" + defaultId + "]");
            id = id.isEmpty() ? defaultId : id;
            if(!LanguageIdentifier.validId(id)) {
                System.err.println("Please enter a valid id.");
                id = null;
            }
        }

        String defaultExt = name.toLowerCase().substring(0, Math.min(name.length(), 3));
        String[] exts = null;
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

        try {
            final String groupId = SpoofaxConstants.METABORG_GROUP_ID;
            final LanguageVersion version = LanguageVersion.parse(SpoofaxConstants.METABORG_VERSION);
            final LanguageIdentifier identifier = new LanguageIdentifier(groupId, id, version);

            final IProjectSettings settings = new ProjectSettings(identifier, name);
            final SpoofaxProjectSettings spoofaxSettings = new SpoofaxProjectSettings(settings, getBasedirLocation());
            final GeneratorProjectSettings generatorSettings = new GeneratorProjectSettings(spoofaxSettings);

            final NewProjectGenerator newGenerator = new NewProjectGenerator(generatorSettings, exts);
            newGenerator.generateAll();
            final ProjectGenerator generator = new ProjectGenerator(generatorSettings);
            generator.generateAll();
        } catch(IOException ex) {
            throw new MojoFailureException("Failed to generate project files", ex);
        } catch(ProjectException ex) {
            throw new MojoFailureException("Invalid project settings", ex);
        }
    }
}
