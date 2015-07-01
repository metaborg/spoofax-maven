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
import org.metaborg.spoofax.core.build.BuildInput;
import org.metaborg.spoofax.core.build.BuildInputBuilder;
import org.metaborg.spoofax.core.build.ConsoleBuildMessagePrinter;
import org.metaborg.spoofax.core.build.IBuilder;
import org.metaborg.spoofax.core.build.paths.ILanguagePathService;
import org.metaborg.spoofax.core.language.ILanguage;
import org.metaborg.spoofax.core.language.ILanguageService;
import org.metaborg.spoofax.core.resource.IResourceService;
import org.metaborg.spoofax.core.resource.SpoofaxIgnoredDirectories;
import org.metaborg.spoofax.core.source.ISourceTextService;
import org.metaborg.spoofax.core.transform.CompileGoal;
import org.metaborg.spoofax.core.transform.ITransformerGoal;
import org.metaborg.spoofax.core.transform.NamedGoal;
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
            final ILanguage languageObj = languageService.get(language);
            if(languageObj == null) {
                final String message = String.format("Cannot find language %s", language);
                throw new MojoFailureException(message);
            }

            final Iterable<FileObject> sources =
                filesFromFileSets(fileSets, includeSources, languagePathService.sources(getSpoofaxProject(), language));
            final Iterable<FileObject> includes =
                filesFromFileSets(auxFileSets, includeDependencies,
                    languagePathService.includes(getSpoofaxProject(), language));
            final ITransformerGoal goal = this.goal == null ? new CompileGoal() : new NamedGoal(this.goal);

            final ISourceTextService sourceTextService = spoofax.getInstance(ISourceTextService.class);
            final OutputStream logOutputStream =
                new LoggingOutputStream(org.slf4j.LoggerFactory.getLogger(GenerateSourcesMojo.class), false);

            final BuildInputBuilder inputBuilder = new BuildInputBuilder(getSpoofaxProject());
            // @formatter:off
            final BuildInput input = inputBuilder
                .addLanguage(languageObj)
                .withDefaultIncludeLocations(false)
                .withResources(sources)
                .withSelector(SpoofaxIgnoredDirectories.includeFileSelector())
                .withMessagePrinter(new ConsoleBuildMessagePrinter(sourceTextService, logOutputStream, true, true))
                .addIncludeLocations(languageObj, includes)
                .withThrowOnErrors(true)
                .addGoal(goal)
                .build(spoofax)
                ;
            // @formatter:on

            final IBuilder<?, ?, ?> builder = spoofax.getInstance(IBuilder.class);
            try {
                builder.build(input);
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
