package org.metaborg.spoofax.maven.plugin;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.io.CharStreams;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.FileTypeSelector;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.metaborg.spoofax.core.analysis.AnalysisException;
import org.metaborg.spoofax.core.analysis.AnalysisFileResult;
import org.metaborg.spoofax.core.analysis.AnalysisResult;
import org.metaborg.spoofax.core.analysis.IAnalysisService;
import org.metaborg.spoofax.core.context.ContextException;
import org.metaborg.spoofax.core.context.IContext;
import org.metaborg.spoofax.core.context.IContextService;
import org.metaborg.spoofax.core.language.ILanguage;
import org.metaborg.spoofax.core.language.ILanguageDiscoveryService;
import org.metaborg.spoofax.core.language.ILanguageIdentifierService;
import org.metaborg.spoofax.core.resource.IResourceService;
import org.metaborg.spoofax.core.stratego.IStrategoRuntimeService;
import org.metaborg.spoofax.core.syntax.ISyntaxService;
import org.metaborg.spoofax.core.syntax.ParseException;
import org.metaborg.spoofax.core.syntax.ParseResult;
import org.metaborg.spoofax.core.transform.CompileGoal;
import org.metaborg.spoofax.core.transform.ITransformer;
import org.metaborg.spoofax.core.transform.ITransformerGoal;
import org.metaborg.spoofax.core.transform.TransformerException;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.HybridInterpreter;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.StrategoException;

class SpoofaxHelper {
    
    private static final ITransformerGoal COMPILE_GOAL = new CompileGoal();

    private final MavenProject project;
    private final PluginDescriptor plugin;
    private final Log log;
    private final IResourceService resourceService;
    private final ILanguageIdentifierService languageIdentifierService;
    private final ISyntaxService<IStrategoTerm> syntaxService;
    private final IAnalysisService<IStrategoTerm,IStrategoTerm> analysisService;
    private final IContextService contextService;
    private final ITransformer<IStrategoTerm, IStrategoTerm, IStrategoTerm> transformer;
    private final IStrategoRuntimeService strategoRuntimeService;

    public SpoofaxHelper(MavenProject project, PluginDescriptor plugin, Log log) {
        this.project = project;
        this.plugin = plugin;
        this.log = log;
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
        strategoRuntimeService = spoofax.getInstance(IStrategoRuntimeService.class);
    }

    private Injector getSpoofax() {
        Injector spoofax;
        if ( (spoofax = (Injector) project.getContextValue("spoofax")) == null ) {
            log.info("Initialising Spoofax core");
            project.setContextValue("spoofax",
                    spoofax = Guice.createInjector(new SpoofaxMavenModule(project)));
            discoverLanguages(spoofax, plugin.getArtifacts(), log);
        } else {
            log.info("Using cached Spoofax core");
        }
        return spoofax;
    }

    // static method to make clear that the *Service fields are not initialised
    // yet when languages are being discovered
    private static void discoverLanguages(Injector spoofax, List<Artifact> artifacts, Log log) {
        IResourceService resourceService = spoofax.getInstance(IResourceService.class);
        ILanguageDiscoveryService languageDiscoveryService = spoofax.getInstance(ILanguageDiscoveryService.class);
        for ( Artifact artifact : artifacts ) {
            if ( !artifact.getType().equals("spoofax-language") ) {
                continue;
            }
            try {
                FileObject artifactFile = resourceService.resolve("zip:"+artifact.getFile());
                for ( ILanguage language : languageDiscoveryService.discover(artifactFile) ) {
                    log.info(String.format("Discovered Spoofax language %s", language.name()));
                }
            } catch (Exception ex) {
                log.error("Error during language discovery.",ex);
            }
        }
    }

    public void compileDirectory(File[] directories)
            throws MojoFailureException {
        try {
            final Collection<ParseResult<IStrategoTerm>> allParseResults = Lists.newArrayList();
            for ( File directory : directories ) {
                FileObject directoryFO = resourceService.resolve(directory);
                if ( directoryFO == null || !directoryFO.exists() ) {
                    log.warn("Ignoring missing source directory "+directory);
                    continue;
                }
                for ( FileObject fo : directoryFO.findFiles(new FileTypeSelector(FileType.FILE))) {
                    ILanguage language = languageIdentifierService.identify(fo);
                    if ( language != null ) {
                        log.debug(String.format("Identified %s as %s file",
                                fo.getName(), language.name()));
                        try {
                            String text = CharStreams.toString(
                                    new InputStreamReader(fo.getContent().getInputStream()));
                            ParseResult<IStrategoTerm> parseResult =
                                    syntaxService.parse(text, fo, language);
                            allParseResults.add(parseResult);
                        } catch (IOException | ParseException ex) {
                            log.error("Error during parsing.",ex);
                        }
                    }
                }
            }

            final Multimap<IContext, ParseResult<IStrategoTerm>> allParseResultsPerContext = ArrayListMultimap.create();
            for(ParseResult<IStrategoTerm> parseResult : allParseResults) {
                final FileObject resource = parseResult.source;
                try {
                    final IContext context = contextService.get(resource, parseResult.language);
                    allParseResultsPerContext.put(context, parseResult);
                } catch(ContextException ex) {
                    final String message = String.format("Could not retrieve context for parse result of %s", resource);
                    log.error(message, ex);
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
                    log.error("Analysis failed", ex);
                }
            }

            for(Entry<IContext, AnalysisResult<IStrategoTerm, IStrategoTerm>> entry : allAnalysisResults.entrySet()) {
                final IContext context = entry.getKey();
                if(!transformer.available(COMPILE_GOAL, context)) {
                    log.debug(String.format("No compilation required for %s", context.language().name()));
                    continue;
                }
                final AnalysisResult<IStrategoTerm, IStrategoTerm> analysisResult = entry.getValue();
                synchronized(context) {
                    for(AnalysisFileResult<IStrategoTerm, IStrategoTerm> fileResult : analysisResult.fileResults) {
                        try {
                            transformer.transform(fileResult, context, COMPILE_GOAL);
                        } catch(TransformerException ex) {
                            log.error("Compilation failed", ex);
                        }
                    }
                }
            }
        } catch (FileSystemException ex) {
            throw new MojoFailureException("",ex);
        }
    }

    public void runStrategy(String name, String[] args) throws MojoFailureException {
        log.info("Invoking "+name+" ["+StringUtils.join(args, ", ")+"]");
        HybridInterpreter runtime = strategoRuntimeService.genericRuntime();
        ITermFactory factory = runtime.getFactory();
        Context context = new Context(factory);
        try {
            context.invokeStrategyCLI(name, name, args);
        } catch (StrategoException ex) {
            throw new MojoFailureException(ex.getMessage(), ex);
        }
    }

}
