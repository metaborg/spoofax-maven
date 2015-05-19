package org.metaborg.spoofax.maven.plugin;

import org.metaborg.spoofax.maven.plugin.impl.Prompter;
import java.io.File;
import java.io.IOException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.metaborg.spoofax.generator.project.NameUtil;
import org.metaborg.spoofax.generator.eclipse.EclipseProjectGenerator;
import org.metaborg.spoofax.generator.eclipse.NewEclipseProjectGenerator;
import org.metaborg.spoofax.generator.project.ProjectException;
import org.metaborg.spoofax.generator.project.ProjectSettings;

@Mojo(name = "generate-eclipse", requiresDirectInvocation = true, requiresProject = false)
public class GenerateEclipseProjectMojo extends AbstractMojo {

    @Parameter(defaultValue = "${basedir}", readonly = true, required = true)
    private File basedir;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoFailureException {
        if ( project.getFile() != null ) {
            getLog().error("Found existing project "+project.getName());
            return;
        }

        Prompter prompter;
        try {
            prompter = Prompter.get();
        } catch (IOException ex) {
            throw new MojoFailureException("Must run interactively.", ex);
        }

        System.out.println("The name, id and extensions you enter must be the"
                + "same as for the language project.");

        String name = null;
        while ( name == null ) {
            name = prompter.readString("Name");
            if ( !NameUtil.isValidName(name) ) {
                System.err.println("Please enter a valid name.");
                name = null;
            }
        }
 
        String id = null;
        while ( id == null ) {
            id = prompter.readString("Id");
            if ( !NameUtil.isValidId(id) ) {
                System.err.println("Please enter a valid id.");
                id = null;
            }
        }

        String[] exts = null;
        while ( exts == null ) {
            exts = prompter.readString("File extensions (space separated)")
                    .split("[\\ \t\n]+");
            if ( exts.length == 0 ||
                    (exts.length == 1 && exts[0].isEmpty() ) ) {
                exts = null;
            } else {
                for ( String ext : exts ) {
                    if ( !NameUtil.isValidFileExtension(ext) ) {
                        System.err.println("Please enter valid file extensions. Invalid: "+ext);
                        exts = null;
                    }
                }
            }
        }
 
        try {
            ProjectSettings ps = new ProjectSettings(name, basedir);
            ps.setId(id);
            NewEclipseProjectGenerator pg = new NewEclipseProjectGenerator(ps, exts);
            pg.generateAll();

            EclipseProjectGenerator cg = new EclipseProjectGenerator(ps);
            cg.generateAll();
        } catch (IOException ex) {
            throw new MojoFailureException("Failed to generate project files.",ex);
        } catch (ProjectException ex) {
            throw new MojoFailureException("Invalid project settings.",ex);
        }
    }

}
