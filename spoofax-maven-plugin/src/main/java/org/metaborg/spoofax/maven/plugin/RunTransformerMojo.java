package org.metaborg.spoofax.maven.plugin;

import java.io.File;
import java.util.List;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.model.fileset.FileSet;
import org.metaborg.spoofax.core.transform.CompileGoal;
import org.metaborg.spoofax.core.transform.ITransformerGoal;
import org.metaborg.spoofax.core.transform.NamedGoal;
import org.metaborg.spoofax.maven.plugin.impl.FileSetHelper;
import org.metaborg.spoofax.maven.plugin.impl.SpoofaxHelper;

@Mojo(name = "run-transformer")
public class RunTransformerMojo extends AbstractMojo {

    @Parameter(readonly = true)
    String name;

    @Parameter(required = true, readonly = true)
    List<FileSet> fileSets;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${plugin}", readonly = true, required = true)
    private PluginDescriptor plugin;

    @Override
    public void execute() throws MojoFailureException {
        SpoofaxHelper spoofax = new SpoofaxHelper(project, plugin, getLog());
        List<File> files = FileSetHelper.getFiles(fileSets);
        ITransformerGoal goal = name == null ? new CompileGoal() : new NamedGoal(name);
        spoofax.runTransformer(goal, files);
    }
    
}
