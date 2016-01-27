package org.metaborg.spoofax.maven.plugin;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.metaborg.core.action.CompileGoal;
import org.metaborg.core.action.EndNamedGoal;
import org.metaborg.core.action.ITransformGoal;
import org.metaborg.core.build.BuildInput;
import org.metaborg.core.build.ConsoleBuildMessagePrinter;
import org.metaborg.core.build.BuildInputBuilder;
import org.metaborg.core.language.ILanguage;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.spoofax.core.resource.SpoofaxIgnoresSelector;
import org.metaborg.spoofax.maven.plugin.impl.FileSetSelector;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Lists;

@Mojo(name = "transform")
public class TransformMojo extends AbstractSpoofaxMojo {
    private static final ILogger logger = LoggerUtils.logger(TransformMojo.class);

    @Parameter(defaultValue = "false") boolean skip;
    @Parameter(required = true) private String language;
    @Parameter private String goal;
    @Parameter(defaultValue = "false") boolean includeSources;
    @Parameter(defaultValue = "true") boolean includeDependencies;
    @Parameter private List<FileSet> fileSets;
    @Parameter private List<FileSet> auxFileSets;
    @Parameter(defaultValue = "${basedir}", readonly = true, required = true) private File basedir;


    @Override public void execute() throws MojoFailureException, MojoExecutionException {
        if(skip || skipAll) {
            return;
        }
        super.execute();
        discoverLanguages();

        try {

            // GTODO: use language implementation id
            final ILanguage languageObj = languageService.getLanguage(language);
            if(languageObj == null) {
                final String message = String.format("Cannot find language %s", language);
                throw new MojoFailureException(message);
            }
            final ILanguageImpl languageImpl = languageObj.activeImpl();
            if(languageImpl == null) {
                final String message = String.format("Cannot find active language implementation for %s", language);
                throw new MojoFailureException(message);
            }

            final Iterable<FileObject> sources =
                filesFromFileSets(fileSets, includeSources,
                    languagePathService.sourcePaths(getLanguageSpec(), language));
            final Iterable<FileObject> includes =
                filesFromFileSets(auxFileSets, includeDependencies,
                    languagePathService.includePaths(getLanguageSpec(), language));
            final ITransformGoal goal = this.goal == null ? new CompileGoal() : new EndNamedGoal(this.goal);

            final BuildInputBuilder inputBuilder = new BuildInputBuilder(getLanguageSpec());
            // @formatter:off
            final BuildInput input = inputBuilder
                .addLanguage(languageImpl)
                .withDefaultIncludePaths(false)
                .withSources(sources)
                .withSelector(new SpoofaxIgnoresSelector())
                .withMessagePrinter(new ConsoleBuildMessagePrinter(sourceTextService, true, true, logger))
                // GTODO: are the includes here paths or files? if files, this will not work because the builder needs paths.
                .addIncludePaths(languageImpl, includes)
                .withThrowOnErrors(true)
                .addTransformGoal(goal)
                .build(dependencyService, languagePathService)
                ;
            // @formatter:on

            try {
                processorRunner.build(input, null, null).schedule().block();
            } catch(Exception e) {
                throw new MojoFailureException("Error generating sources", e);
            }
        } catch(Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private Iterable<FileObject> filesFromFileSets(Collection<FileSet> fileSets, boolean useDefault,
        Iterable<FileObject> defaultFiles) throws FileSystemException, MojoFailureException {
        List<FileObject> files = Lists.newArrayList();
        if(fileSets != null && !fileSets.isEmpty()) {
            for(FileSet fileSet : fileSets) {
                FileObject directory =
                    resourceService.resolve(fileSet.getDirectory() != null ? getAbsoluteFile(fileSet.getDirectory())
                        : basedir);
                if(directory.exists()) {
                    files.addAll(Arrays.asList(directory.findFiles(new FileSetSelector(fileSet.getIncludes(), fileSet
                        .getExcludes()))));
                }
            }
        }
        if(useDefault) {
            files.addAll(Lists.newArrayList(defaultFiles));
        }
        return files;
    }
}
