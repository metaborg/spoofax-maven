package org.metaborg.spoofax.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.metaborg.spoofax.generator.common.CommonGenerator;
import org.metaborg.spoofax.generator.project.ProjectGenerator;
import static org.metaborg.spoofax.maven.plugin.AbstractSpoofaxMojo.Format.*;

@Mojo(name = "generate", requiresDirectInvocation = true, requiresProject = false)
public class GenerateProjectMojo extends AbstractMojo {

    @Parameter(defaultValue = "${basedir}", readonly = true, required = true)
    private File basedir;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(property = "spoofax.generate.minimal", defaultValue = "false", readonly = true)
    private boolean minimal;

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

        String name = prompter.readString("Name", true);

        String id = prompter.readString("Id", name.toLowerCase());
        if ( id.equals(name.toLowerCase()) ) {
            id = null;
        }

        String ext = name.toLowerCase().substring(0, Math.min(name.length(), 3));
        String[] exts = prompter.readString("File extensions (space separated)", ext)
                .split("[\\ \t\n]+");

        String defaultFormat = AbstractSpoofaxMojo.DEFAULT_FORMAT.name();
        String format = prompter.readStringFromList("Format",
                Arrays.asList(ctree.name(), jar.name()), defaultFormat);
        if ( format.equals(defaultFormat) ) {
            format = null;
        }

        ProjectGenerator pg = new ProjectGenerator(basedir, name, exts);
        pg.setFormat(format);
        pg.setMinimal(minimal);
        pg.setPackageName(id);
        try {
            pg.generateAll();
        } catch (IOException ex) {
            throw new MojoFailureException("Failed to generate project files.",ex);
        }

        CommonGenerator cg = new CommonGenerator(basedir, name);
        try {
            cg.generateAll();
        } catch (IOException ex) {
            throw new MojoFailureException("Failed to generate common files.",ex);
        }
    }

}
