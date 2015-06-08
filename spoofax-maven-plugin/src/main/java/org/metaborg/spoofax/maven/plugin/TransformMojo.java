package org.metaborg.spoofax.maven.plugin;

import com.google.common.collect.Lists;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.metaborg.spoofax.core.project.ILanguagePathService;
import org.metaborg.spoofax.core.resource.IResourceService;
import org.metaborg.spoofax.core.transform.CompileGoal;
import org.metaborg.spoofax.core.transform.ITransformerGoal;
import org.metaborg.spoofax.core.transform.NamedGoal;
import org.metaborg.spoofax.maven.plugin.impl.FileSetSelector;
import org.metaborg.spoofax.meta.core.SpoofaxBuilder;

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

    @Parameter(defaultValue = "${basedir}", readonly = true, required = true)
    private File basedir;

    @Override
    public void execute() throws MojoFailureException {
        if ( skip ) { return; }
        ILanguagePathService languagePathService = getSpoofax().getInstance(ILanguagePathService.class);
        SpoofaxBuilder builder = getSpoofax().getInstance(SpoofaxBuilder.class);
        try {
            Iterable<FileObject> sources =
                    filesFromFileSets(fileSets, includeSources,
                            languagePathService.sources(getSpoofaxProject(), language));
            Iterable<FileObject> includes =
                    filesFromFileSets(auxFileSets, includeDependencies,
                            languagePathService.includes(getSpoofaxProject(), language));
            ITransformerGoal goal = this.goal == null ?
                    new CompileGoal() : new NamedGoal(this.goal);
            builder.build(goal, sources, includes, Collections.EMPTY_LIST);
        } catch (Exception ex) {
            throw new MojoFailureException(ex.getMessage(), ex);
        }
    }

    private Iterable<FileObject> filesFromFileSets(Collection<FileSet> fileSets,
            boolean useDefault, Iterable<FileObject> defaultFiles) throws FileSystemException, MojoFailureException {
        IResourceService resourceService = getSpoofax().getInstance(IResourceService.class);
        List<FileObject> files = Lists.newArrayList();
        if ( fileSets != null && !fileSets.isEmpty() ) {
            for ( FileSet fileSet : fileSets ) {
                FileObject directory = resourceService.resolve(
                        fileSet.getDirectory() != null ?
                                getAbsoluteFile(fileSet.getDirectory()) : basedir);
                if ( directory.exists() ) {
                    files.addAll(Arrays.asList(
                            directory.findFiles(new FileSetSelector(fileSet.getIncludes(), fileSet.getExcludes()))));
                }
            }
        }
        if ( useDefault ) {
            files.addAll(Lists.newArrayList(defaultFiles));
        }
        return files;
    }
    
}
