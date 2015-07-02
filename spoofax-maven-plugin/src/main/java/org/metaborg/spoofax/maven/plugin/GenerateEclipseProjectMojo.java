package org.metaborg.spoofax.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.commons.vfs2.FileObject;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.spoofax.generator.eclipse.EclipseProjectGenerator;
import org.metaborg.spoofax.generator.project.NameUtil;
import org.metaborg.spoofax.generator.project.ProjectException;
import org.metaborg.spoofax.generator.project.ProjectSettings;
import org.metaborg.spoofax.maven.plugin.impl.Prompter;

@Mojo(name = "generate-eclipse", requiresDirectInvocation = true, requiresProject = false)
public class GenerateEclipseProjectMojo extends AbstractSpoofaxMojo {
    @Parameter(defaultValue = "${basedir}", readonly = true, required = true) private File basedir;
    @Parameter(defaultValue = "${project}", readonly = true) private MavenProject project;


    @Override public void execute() throws MojoFailureException {
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

        String name = null;
        while(name == null) {
            name = prompter.readString("Name  (e.g. 'SDF')");
            if(!NameUtil.isValidName(name)) {
                System.err.println("Please enter a valid name");
                name = null;
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

        final String version = prompter.readString("Maven version (e.g. '1.5.0-SNAPSHOT')");

        String groupId = null;
        while(groupId == null) {
            groupId = prompter.readString("Group ID (e.g. 'org.metaborg')");
            if(!NameUtil.isValidId(groupId)) {
                System.err.println("Please enter a valid id");
                groupId = null;
            }
        }

        final String metaborgVersion =
            prompter.readString("Maven version for MetaBorg artifacts (e.g. '1.5.0-SNAPSHOT')");

        try {
            final File newBaseDir = EclipseProjectGenerator.childBaseDir(basedir, id);
            final IResourceService resourceService = getSpoofax().getInstance(IResourceService.class);
            final FileObject newBaseDirLocation = resourceService.resolve(newBaseDir);
            final ProjectSettings settings = new ProjectSettings(groupId, id, version, name, newBaseDirLocation);
            settings.setMetaborgVersion(metaborgVersion);

            final EclipseProjectGenerator generator = new EclipseProjectGenerator(resourceService, settings);
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
            final File newBaseDir = EclipseProjectGenerator.childBaseDir(project.getBasedir().getParentFile(), id);
            final IResourceService resourceService = getSpoofax().getInstance(IResourceService.class);
            final FileObject newBaseDirLocation = resourceService.resolve(newBaseDir);
            final ProjectSettings settings =
                new ProjectSettings(project.getGroupId(), id, project.getVersion(), project.getName(),
                    newBaseDirLocation);
            settings.setMetaborgVersion(project.getParent().getVersion());

            final EclipseProjectGenerator generator = new EclipseProjectGenerator(resourceService, settings);
            generator.generateAll();
        } catch(IOException ex) {
            throw new MojoFailureException("Failed to generate project files", ex);
        } catch(ProjectException ex) {
            throw new MojoFailureException("Invalid project settings", ex);
        }
    }
}
