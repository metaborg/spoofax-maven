package org.metaborg.spoofax.maven.plugin;

import com.google.common.collect.Lists;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.metaborg.spoofax.core.transform.CompileGoal;
import org.metaborg.spoofax.core.transform.ITransformerGoal;
import org.metaborg.spoofax.core.transform.NamedGoal;
import org.metaborg.spoofax.maven.plugin.impl.FileSetSelector;
import org.metaborg.spoofax.maven.plugin.impl.SpoofaxHelper;

@Mojo(name = "transform")
public class TransformMojo extends AbstractSpoofaxMojo {

    @Parameter(defaultValue = "false")
    boolean skip;

    @Parameter(required = true)
    private String language;

    @Parameter
    private String goal;

    @Parameter(defaultValue = "false")
    boolean includeSources;

    @Parameter(defaultValue = "true")
    boolean includeDependencies;

    @Parameter
    private List<FileSet> fileSets;

    @Parameter
    private List<FileSet> auxFileSets;

    @Parameter(defaultValue = "false")
    private boolean pardoned;

    @Parameter(defaultValue = "${basedir}", readonly = true, required = true)
    private File basedir;

    @Override
    public void execute() throws MojoFailureException {
        if ( skip ) { return; }
        SpoofaxHelper spoofax = getSpoofaxHelper();
        try {
            Iterable<FileObject> files =
                    filesFromFileSets(spoofax, fileSets, includeSources, spoofax.getLanguageSources(language));
            Iterable<FileObject> auxFiles =
                    filesFromFileSets(spoofax, auxFileSets, includeDependencies, spoofax.getLanguageIncludes(language));
            ITransformerGoal goal = this.goal == null ?
                    new CompileGoal() : new NamedGoal(this.goal);
            spoofax.transformFiles(goal, spoofax.getLanguage(language),
                    files, auxFiles, pardoned);
        } catch (FileSystemException ex) {
            throw new MojoFailureException(ex.getMessage(), ex);
        }
    }

    private Iterable<FileObject> filesFromFileSets(SpoofaxHelper spoofax,
            Collection<FileSet> fileSets, boolean useDefault, Iterable<FileObject> defaultFiles) throws FileSystemException {
        List<FileObject> files = Lists.newArrayList();
        if ( fileSets != null && !fileSets.isEmpty() ) {
            for ( FileSet fileSet : fileSets ) {
                FileObject directory = spoofax.getResourceService().resolve(
                        fileSet.getDirectory() != null ?
                                getAbsoluteFile(fileSet.getDirectory()) : basedir);
                files.addAll(Arrays.asList(
                        directory.findFiles(new FileSetSelector(fileSet.getIncludes(), fileSet.getExcludes()))));
            }
        }
        if ( useDefault ) {
            files.addAll(Lists.newArrayList(defaultFiles));
        }
        return files;
    }
    
}
