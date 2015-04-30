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
import org.metaborg.spoofax.generator.ProjectGenerator;
import org.metaborg.spoofax.generator.NewProjectGenerator;
import org.metaborg.spoofax.generator.project.ProjectException;
import org.metaborg.spoofax.generator.project.ProjectSettings;

@Mojo(name = "generate", requiresDirectInvocation = true, requiresProject = false)
public class GenerateProjectMojo extends AbstractMojo {

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

        String name = null;
        while ( name == null ) {
            name = prompter.readString("Name");
            if ( !NameUtil.isValidName(name) ) {
                System.err.println("Please enter a valid name.");
                name = null;
            }
        }
        
        String defaultId = name.toLowerCase();
        String id = null;
        while ( id == null ) {
            id = prompter.readString("Id ["+defaultId+"]");
            id = id.isEmpty() ? defaultId : id;
            if ( !NameUtil.isValidId(id) ) {
                System.err.println("Please enter a valid id.");
                id = null;
            }
        }

        String defaultExt = name.toLowerCase().substring(0, Math.min(name.length(), 3));
        String[] exts = null;
        while ( exts == null ) {
            exts = prompter.readString("File extensions (space separated) ["+defaultExt+"]")
                    .split("[\\ \t\n]+");
            if ( exts.length == 0 ) {
                exts = new String[]{ defaultExt };
            }
            for ( String ext : exts ) {
                if ( !NameUtil.isValidFileExtension(ext) ) {
                    System.err.println("Please enter valid file extensions. Invalid: "+ext);
                    exts = null;
                }
            }
        }
        
        try {
            ProjectSettings ps = new ProjectSettings(name, basedir);
            ps.setId(id);
            NewProjectGenerator pg = new NewProjectGenerator(ps, exts);
            pg.generateAll();

            ProjectGenerator cg = new ProjectGenerator(ps);
            cg.generateAll();
        } catch (IOException ex) {
            throw new MojoFailureException("Failed to generate project files.",ex);
        } catch (ProjectException ex) {
            throw new MojoFailureException("Invalid project settings.",ex);
        }
    }

}
