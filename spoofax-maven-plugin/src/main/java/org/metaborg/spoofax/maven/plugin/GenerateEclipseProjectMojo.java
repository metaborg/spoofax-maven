package org.metaborg.spoofax.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.commons.vfs2.FileObject;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.metaborg.core.language.LanguageIdentifier;
import org.metaborg.core.language.LanguageVersion;
import org.metaborg.core.project.NameUtil;
import org.metaborg.core.project.ProjectException;
import org.metaborg.spoofax.core.project.SpoofaxProjectSettings;
import org.metaborg.spoofax.generator.eclipse.EclipseProjectGenerator;
import org.metaborg.spoofax.generator.project.GeneratorProjectSettings;
import org.metaborg.spoofax.maven.plugin.impl.Prompter;

@Mojo(name = "generate-eclipse", requiresDirectInvocation = true, requiresProject = false)
public class GenerateEclipseProjectMojo extends AbstractSpoofaxMojo {
    @Parameter(defaultValue = "${basedir}", readonly = true, required = true) private File basedir;
    @Parameter(defaultValue = "${project}", readonly = true) private MavenProject project;


    @Override public void execute() throws MojoFailureException, MojoExecutionException {
        super.execute();

        if(project.getFile() == null) {
            generateFromPrompt();
        } else if("spoofax-language".equals(project.getPackaging())) {
            generateFromProject();
        } else {
            getLog().error(
                "Found existing project " + project.getName() + ", but it is not of packaging type 'spoofax-language'");
        }
    }

    private void generateFromPrompt() throws MojoFailureException {
        final PrintStream out = System.out;

        out.println("Generating Eclipse plugin project from scratch");

        final Prompter prompter;
        try {
            prompter = Prompter.get();
        } catch(IOException ex) {
            throw new MojoFailureException("Must run interactively", ex);
        }

        out.println("The language name, id, and version you enter must be the same as for the Spoofax language project");

        String groupId = null;
        while(groupId == null) {
            groupId = prompter.readString("Group ID (e.g. 'org.metaborg')");
            if(!NameUtil.isValidId(groupId)) {
                System.err.println("Please enter a valid id");
                groupId = null;
            }
        }

        String id = null;
        while(id == null) {
            id = prompter.readString("ID  (e.g. 'org.metaborg.meta.lang.sdf')");
            if(!NameUtil.isValidId(id)) {
                System.err.println("Please enter a valid id");
                id = null;
            }
        }

        LanguageVersion version = null;
        while(version == null) {
            final String versionString = prompter.readString("Maven version (e.g. '1.5.0-SNAPSHOT')");
            if(!LanguageVersion.valid(versionString)) {
                System.err.println("Please enter a valid id");
                version = null;
            } else {
                version = LanguageVersion.parse(versionString);
            }
        }

        String name = null;
        while(name == null) {
            name = prompter.readString("Name  (e.g. 'SDF')");
            if(!NameUtil.isValidName(name)) {
                System.err.println("Please enter a valid name");
                name = null;
            }
        }

        final String metaborgVersion =
            prompter.readString("Maven version for MetaBorg artifacts (e.g. '1.5.0-SNAPSHOT')");

        try {
            final LanguageIdentifier identifier = new LanguageIdentifier(id, groupId, version);
            final File newBaseDir = EclipseProjectGenerator.childBaseDir(basedir, id);
            final FileObject newBaseDirLocation = resourceService.resolve(newBaseDir);

            final SpoofaxProjectSettings settings = new SpoofaxProjectSettings(identifier, name, newBaseDirLocation);
            final GeneratorProjectSettings generatorSettings = new GeneratorProjectSettings(settings);
            generatorSettings.setMetaborgVersion(metaborgVersion);

            final EclipseProjectGenerator generator = new EclipseProjectGenerator(generatorSettings);
            generator.generateAll();
        } catch(IOException ex) {
            throw new MojoFailureException("Failed to generate project files", ex);
        } catch(ProjectException ex) {
            throw new MojoFailureException("Invalid project settings", ex);
        }
    }

    private void generateFromProject() throws MojoFailureException {
        System.out.println("Generating Eclipse plugin project from existing Spoofax language project");

        try {
            final String id = project.getArtifactId();
            final String groupId = project.getGroupId();
            final LanguageVersion version = LanguageVersion.parse(project.getVersion());
            final LanguageIdentifier identifier = new LanguageIdentifier(id, groupId, version);
            final String name = project.getName();
            final File newBaseDir = EclipseProjectGenerator.childBaseDir(project.getBasedir().getParentFile(), id);
            final FileObject newBaseDirLocation = resourceService.resolve(newBaseDir);

            final SpoofaxProjectSettings settings = new SpoofaxProjectSettings(identifier, name, newBaseDirLocation);
            final GeneratorProjectSettings generatorSettings = new GeneratorProjectSettings(settings);
            generatorSettings.setMetaborgVersion(project.getParent().getVersion());

            final EclipseProjectGenerator generator = new EclipseProjectGenerator(generatorSettings);
            generator.generateAll();
        } catch(IOException ex) {
            throw new MojoFailureException("Failed to generate project files", ex);
        } catch(ProjectException ex) {
            throw new MojoFailureException("Invalid project settings", ex);
        }
    }
}
