package org.metaborg.spoofax.maven.plugin;

import com.google.common.collect.Lists;
import java.io.File;
import java.util.Collections;
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
import org.metaborg.spoofax.maven.plugin.impl.FileHelper;
import org.metaborg.spoofax.maven.plugin.impl.SpoofaxHelper;

@Mojo(name = "transform")
public class TransformMojo extends AbstractMojo {

    @Parameter
    private String goal;

    @Parameter
    private File file;

    @Parameter
    private List<FileSet> fileSets;

    @Parameter
    private List<FileSet> auxFileSets;

    @Parameter(defaultValue = "false")
    private boolean standalone;

    @Parameter(defaultValue = "${basedir}", readonly = true, required = true)
    private File basedir;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${plugin}", readonly = true, required = true)
    private PluginDescriptor plugin;

    @Override
    public void execute() throws MojoFailureException {
        SpoofaxHelper spoofax = SpoofaxHelper.get(project, plugin, getLog(), standalone);
        List<File> files;
        if ( file != null ) {
            files = Lists.newArrayList(FileHelper.getAbsoluteFile(file, basedir));
            if ( fileSets != null ) {
                getLog().warn("Ignoring fileSets because file is specified.");
            }
        } else if ( fileSets != null ) {
            files = FileHelper.getFiles(fileSets, basedir);
        } else {
            throw new MojoFailureException("Setting one of file or fileSets is required.");
        }
        List<File> auxFiles = auxFileSets != null ?
                FileHelper.getFiles(auxFileSets, basedir) : Collections.EMPTY_LIST;
        ITransformerGoal goal = this.goal == null ? new CompileGoal() : new NamedGoal(this.goal);
        spoofax.transformFiles(goal, files, auxFiles, Collections.EMPTY_LIST);
    }
    
}
