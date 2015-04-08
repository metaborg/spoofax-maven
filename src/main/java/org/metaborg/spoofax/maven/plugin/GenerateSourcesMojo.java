package org.metaborg.spoofax.maven.plugin;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.io.CharStreams;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.FileTypeSelector;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.metaborg.spoofax.core.analysis.AnalysisException;
import org.metaborg.spoofax.core.analysis.AnalysisFileResult;
import org.metaborg.spoofax.core.analysis.AnalysisResult;
import org.metaborg.spoofax.core.analysis.IAnalysisService;
import org.metaborg.spoofax.core.context.ContextException;
import org.metaborg.spoofax.core.context.IContext;
import org.metaborg.spoofax.core.context.IContextService;
import org.metaborg.spoofax.core.language.ILanguage;
import org.metaborg.spoofax.core.language.ILanguageIdentifierService;
import org.metaborg.spoofax.core.resource.IResourceService;
import org.metaborg.spoofax.core.syntax.ISyntaxService;
import org.metaborg.spoofax.core.syntax.ParseException;
import org.metaborg.spoofax.core.syntax.ParseResult;
import org.metaborg.spoofax.core.transform.CompileGoal;
import org.metaborg.spoofax.core.transform.ITransformer;
import org.metaborg.spoofax.core.transform.ITransformerGoal;
import org.metaborg.spoofax.core.transform.TransformerException;
import org.spoofax.interpreter.terms.IStrategoTerm;

@Mojo(name="generate-sources",
        defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateSourcesMojo extends AbstractSpoofaxMojo {

    @Parameter(property = "spoofax.generate-sources.skip", defaultValue = "false")
    private boolean skip;

    private static final ITransformerGoal COMPILE_GOAL = new CompileGoal();

    private IResourceService resourceService;
    private ILanguageIdentifierService languageIdentifierService;
    private ISyntaxService<IStrategoTerm> syntaxService;
    private IAnalysisService<IStrategoTerm,IStrategoTerm> analysisService;
    private IContextService contextService;
    private ITransformer<IStrategoTerm, IStrategoTerm, IStrategoTerm> transformer;

    public void execute() throws MojoExecutionException {
        if ( skip ) return;
        super.execute();
        getLog().info("Generating Spoofax sources");
        getProject().addCompileSourceRoot(getGeneratedSourceDirectory().getAbsolutePath());
        initSpoofax();
        compile();
    }

    private void initSpoofax() {
        Injector spoofax = getSpoofax();
        resourceService = spoofax.getInstance(IResourceService.class);
        languageIdentifierService = spoofax.getInstance(ILanguageIdentifierService.class);
        syntaxService = spoofax.getInstance(
                Key.get(new TypeLiteral<ISyntaxService<IStrategoTerm>>(){}));
        analysisService = spoofax.getInstance(
                Key.get(new TypeLiteral<IAnalysisService<IStrategoTerm,IStrategoTerm>>(){}));
        contextService = spoofax.getInstance(IContextService.class);
        transformer = spoofax.getInstance(
                Key.get(new TypeLiteral<ITransformer<IStrategoTerm, IStrategoTerm, IStrategoTerm>>() {}));
    }

    private void compile() throws MojoExecutionException {
        String[] compileSourceRoots = getProject().getCompileSourceRoots().toArray(new String[]{});
        File[] sourceDirectories = new File[compileSourceRoots.length];
        for ( int i = 0; i < compileSourceRoots.length; i++) {
            sourceDirectories[i] = new File(compileSourceRoots[i]);
        }
        compileDirectory(getBasedir(), sourceDirectories);
        compileDirectory(getBasedir(), new File[] { getGeneratedSourceDirectory() });
    }

    private void compileDirectory(File basedir, File[] directories)
            throws MojoExecutionException {
        final FileObject basedirFO = resourceService.resolve(basedir);
        try {
            final Collection<ParseResult<IStrategoTerm>> allParseResults = Lists.newArrayList();
            for ( File directory : directories ) {
                FileObject directoryFO = resourceService.resolve(directory);
                if ( directoryFO == null || !directoryFO.exists() ) {
                    getLog().warn("Ignoring missing source directory "+directory);
                    continue;
                }
                for ( FileObject fo : directoryFO.findFiles(new FileTypeSelector(FileType.FILE))) {
                    ILanguage language = languageIdentifierService.identify(fo);
                    if ( language != null ) {
                        getLog().debug(String.format("Identified %s as %s file",
                                basedirFO.getName().getRelativeName(fo.getName()),
                                language.name()));
                        try {
                            String text = CharStreams.toString(
                                    new InputStreamReader(fo.getContent().getInputStream()));
                            ParseResult<IStrategoTerm> parseResult =
                                    syntaxService.parse(text, fo, language);
                            allParseResults.add(parseResult);
                        } catch (IOException | ParseException ex) {
                            getLog().error("Error during parsing.",ex);
                            ex.printStackTrace();
                        }
                    }
                }
            }

            final Multimap<IContext, ParseResult<IStrategoTerm>> allParseResultsPerContext = ArrayListMultimap.create();
            for(ParseResult<IStrategoTerm> parseResult : allParseResults) {
                final FileObject resource = parseResult.source;
                try {
                    final IContext context = contextService.get(resource, parseResult.parsedWith);
                    allParseResultsPerContext.put(context, parseResult);
                } catch(ContextException ex) {
                    final String message = String.format("Could not retrieve context for parse result of %s", resource);
                    getLog().error(message, ex);
                }
            }

            final Map<IContext, AnalysisResult<IStrategoTerm, IStrategoTerm>> allAnalysisResults =
                Maps.newHashMapWithExpectedSize(allParseResultsPerContext.keySet().size());
            for(Entry<IContext, Collection<ParseResult<IStrategoTerm>>> entry : allParseResultsPerContext.asMap().entrySet()) {
                final IContext context = entry.getKey();
                final Iterable<ParseResult<IStrategoTerm>> parseResults = entry.getValue();
                try {
                    synchronized(context) {
                        final AnalysisResult<IStrategoTerm, IStrategoTerm> analysisResult =
                            analysisService.analyze(parseResults, context);
                        allAnalysisResults.put(context, analysisResult);
                    }
                } catch(AnalysisException ex) {
                    getLog().error("Analysis failed", ex);
                }
            }

            for(Entry<IContext, AnalysisResult<IStrategoTerm, IStrategoTerm>> entry : allAnalysisResults.entrySet()) {
                final IContext context = entry.getKey();
                if(!transformer.available(COMPILE_GOAL, context)) {
                    getLog().debug(String.format("No compilation required for %s", context.language().name()));
                    continue;
                }
                final AnalysisResult<IStrategoTerm, IStrategoTerm> analysisResult = entry.getValue();
                synchronized(context) {
                    for(AnalysisFileResult<IStrategoTerm, IStrategoTerm> fileResult : analysisResult.fileResults) {
                        try {
                            transformer.transform(fileResult, context, COMPILE_GOAL);
                        } catch(TransformerException ex) {
                            getLog().error("Compilation failed", ex);
                        }
                    }
                }
            }
        } catch (FileSystemException ex) {
            throw new MojoExecutionException("",ex);
        }
    }

}
