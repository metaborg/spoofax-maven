package org.metaborg.spoofax.maven.plugin;

import java.io.File;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.metaborg.core.build.BuildInput;
import org.metaborg.core.build.BuildInputBuilder;
import org.metaborg.core.build.ConsoleBuildMessagePrinter;
import org.metaborg.core.build.paths.ILanguagePathService;
import org.metaborg.core.language.ILanguage;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.language.ILanguageService;
import org.metaborg.core.processing.IProcessorRunner;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.core.source.ISourceTextService;
import org.metaborg.core.transform.CompileGoal;
import org.metaborg.core.transform.ITransformerGoal;
import org.metaborg.core.transform.NamedGoal;
import org.metaborg.spoofax.core.resource.SpoofaxIgnoresSelector;
import org.metaborg.spoofax.maven.plugin.impl.FileSetSelector;
import org.metaborg.util.log.LoggingOutputStream;

import com.google.common.collect.Lists;
import com.google.inject.Injector;

@Mojo(name = "transform")
public class TransformMojo extends AbstractSpoofaxMojo {
    @Parameter(defaultValue = "false") boolean skip;
    @Parameter(required = true) private String language;
    @Parameter private String goal;
    @Parameter(defaultValue = "false") boolean includeSources;
    @Parameter(defaultValue = "true") boolean includeDependencies;
    @Parameter private List<FileSet> fileSets;
    @Parameter private List<FileSet> auxFileSets;
    @Parameter(defaultValue = "${basedir}", readonly = true, required = true) private File basedir;


    @Override public void execute() throws MojoFailureException {
        if(skip) {
            return;
        }

        final Injector spoofax = getSpoofax();
        final ILanguagePathService languagePathService = spoofax.getInstance(ILanguagePathService.class);
        final ILanguageService languageService = spoofax.getInstance(ILanguageService.class);

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
                    languagePathService.sourcePaths(getSpoofaxProject(), language));
            final Iterable<FileObject> includes =
                filesFromFileSets(auxFileSets, includeDependencies,
                    languagePathService.includePaths(getSpoofaxProject(), language));
            final ITransformerGoal goal = this.goal == null ? new CompileGoal() : new NamedGoal(this.goal);

            final ISourceTextService sourceTextService = spoofax.getInstance(ISourceTextService.class);
            final OutputStream logOutputStream =
                new LoggingOutputStream(org.slf4j.LoggerFactory.getLogger(GenerateSourcesMojo.class), false);

            final BuildInputBuilder inputBuilder = new BuildInputBuilder(getSpoofaxProject());
            // @formatter:off
            final BuildInput input = inputBuilder
                .addLanguage(languageImpl)
                .withDefaultIncludePaths(false)
                .withSources(sources)
                .withSelector(new SpoofaxIgnoresSelector())
                .withMessagePrinter(new ConsoleBuildMessagePrinter(sourceTextService, logOutputStream, true, true))
                // GTODO: are the includes here paths or files? if files, this will not work because the builder needs paths.
                .addIncludePaths(languageImpl, includes)
                .withThrowOnErrors(true)
                .addTransformGoal(goal)
                .build(spoofax)
                ;
            // @formatter:on

            final IProcessorRunner<?, ?, ?> processor = getSpoofax().getInstance(IProcessorRunner.class);
            try {
                processor.build(input, null, null).schedule().block();
            } catch(Exception e) {
                throw new MojoFailureException("Error generating sources", e);
            }
        } catch(Exception ex) {
            throw new MojoFailureException(ex.getMessage(), ex);
        }
    }

    private Iterable<FileObject> filesFromFileSets(Collection<FileSet> fileSets, boolean useDefault,
        Iterable<FileObject> defaultFiles) throws FileSystemException, MojoFailureException {
        IResourceService resourceService = getSpoofax().getInstance(IResourceService.class);
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
