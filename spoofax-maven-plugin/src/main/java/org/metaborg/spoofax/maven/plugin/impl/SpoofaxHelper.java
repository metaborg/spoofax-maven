package org.metaborg.spoofax.maven.plugin.impl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;
import com.google.common.io.CharStreams;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
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
import org.metaborg.spoofax.core.messages.IMessage;
import org.metaborg.spoofax.core.messages.ISourceRegion;
import static org.metaborg.spoofax.core.messages.MessageSeverity.*;
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

/** Helper to run Spoofax
 * 
 * Mojo's using this should require resolution of compile dependencies.
 * 
 * @author hendrik
 */
public class SpoofaxHelper {
 
    private final MavenProject project;
    private final PluginDescriptor plugin;
    private final Log log;
    private final List<ILanguage> compileLanguages;

    private final IResourceService resourceService;
    private final ILanguageIdentifierService languageIdentifierService;
    private final ISyntaxService<IStrategoTerm> syntaxService;
    private final IAnalysisService<IStrategoTerm,IStrategoTerm> analysisService;
    private final IContextService contextService;
    private final ITransformer<IStrategoTerm, IStrategoTerm, IStrategoTerm> transformer;
    private final IStrategoRuntimeService strategoRuntimeService;

    private SpoofaxHelper(MavenProject project, PluginDescriptor plugin, Log log)
            throws MojoFailureException {
        this.project = project;
        this.plugin = plugin;
        this.log = log;
        this.compileLanguages = Lists.newArrayList();

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

    private Injector getSpoofax() throws MojoFailureException {
        log.info("Initialising Spoofax core");
        Injector spoofax = Guice.createInjector(new SpoofaxMavenModule(project));

        DependencyHelper dh = new DependencyHelper(project, plugin);
        log.info("Discovering compile languages.");
        compileLanguages.addAll(discoverLanguages(spoofax, dh.getCompileArtifacts(), log));
        log.info("Discovering compile dependency languages.");
        discoverLanguages(spoofax, dh.getCompileDependencyArtifacts(), log);

        return spoofax;
    }

    // static method to make clear that the *Service fields are not initialised
    // yet when languages are being discovered
    private static Collection<ILanguage> discoverLanguages(Injector spoofax,
            Collection<Artifact> languageArtifacts, Log log) {
        List<ILanguage> discoveredLanguages = Lists.newArrayList();
        IResourceService resourceService = spoofax.getInstance(IResourceService.class);
        ILanguageDiscoveryService languageDiscoveryService =
                spoofax.getInstance(ILanguageDiscoveryService.class);
        for ( Artifact artifact : languageArtifacts ) {
            try {
                FileObject artifactFile = resourceService.resolve("zip:"+artifact.getFile());
                Collection<ILanguage> languages =
                        Lists.newArrayList(languageDiscoveryService.discover(artifactFile));
                if ( languages.isEmpty()) {
                    log.warn(String.format("No languages in %s",artifact.getId()));
                } else {
                    String msg = String.format("Languages in %s:",artifact.getId());
                    for ( ILanguage language : languages ) {
                        msg += " "+language.name();
                    }
                    log.info(msg);
                }
                discoveredLanguages.addAll(languages);
            } catch (Exception ex) {
                log.error(String.format("Error during language discovery in %s",artifact.getId()), ex);
            }
        }
        return discoveredLanguages;
    }

    public void compileDirectories(Collection<File> directories,
            List<String> pardonedLanguages)
            throws MojoFailureException {
        try {
            List<File> files = Lists.newArrayList();
            for ( File directory : directories ) {
                files.addAll(FileUtils.getFiles(directory, "**", ""));
            }
            transformFiles(new CompileGoal(), files, Collections.EMPTY_LIST,
                    pardonedLanguages);
        } catch (IOException ex) {
            throw new MojoFailureException(ex.getMessage(),ex);
        }
    }

    public void transformFiles(ITransformerGoal goal, Collection<File> files,
            Collection<File> auxFiles, List<String> pardonedLanguages) throws MojoFailureException {
        if ( !pardonedLanguages.isEmpty() ) {
            log.warn("Treating errors as warnings for "+
                    StringUtils.join(pardonedLanguages, " "));
        }
        ContextComparator contextComparator = new ContextComparator(compileLanguages);

        final Set<FileObject> allFiles = Sets.newHashSet();
        final Set<FileObject> targetFiles = Sets.newHashSet();
        for ( File file : auxFiles ) {
            FileObject fo = resourceService.resolve(file);
            allFiles.add(fo);
        }
        for ( File file : files ) {
            FileObject fo = resourceService.resolve(file);
            allFiles.add(fo);
            targetFiles.add(fo);
        }

        final Collection<ParseResult<IStrategoTerm>> allParseResults = Lists.newArrayList();
        final Multimap<ILanguage,IMessage> parseMessages = HashMultimap.create();
        for ( FileObject fo : allFiles ) {
            ILanguage language = languageIdentifierService.identify(fo);
            if ( language != null && compileLanguages.contains(language)) {
                log.debug(String.format("Parsing %s as %s", fo.getName(), language.name()));
                try {
                    String text = CharStreams.toString(
                            new InputStreamReader(fo.getContent().getInputStream()));
                    ParseResult<IStrategoTerm> parseResult =
                            syntaxService.parse(text, fo, language);
                    allParseResults.add(parseResult);
                    parseMessages.putAll(language,Lists.newArrayList(parseResult.messages));
                } catch (IOException | ParseException ex) {
                    throw new MojoFailureException("Error during parsing.",ex);
                }
            }
        }
        printMessages(parseMessages, "parse", Collections.EMPTY_LIST);

        final Multimap<IContext, ParseResult<IStrategoTerm>> allParseResultsPerContext =
                TreeMultimap.create(contextComparator, Ordering.arbitrary());
        for(ParseResult<IStrategoTerm> parseResult : allParseResults) {
            final FileObject fo = parseResult.source;
            try {
                final IContext context = contextService.get(fo, parseResult.language);
                allParseResultsPerContext.put(context, parseResult);
            } catch(ContextException ex) {
                final String message = String.format("Could not retrieve context for parse result of %s", fo);
                throw new MojoFailureException(message, ex);
            }
        }

        final Map<IContext, AnalysisResult<IStrategoTerm, IStrategoTerm>> allAnalysisResults =
                new TreeMap(contextComparator);
        final Multimap<ILanguage,IMessage> analysisMessages = HashMultimap.create();
        for(Entry<IContext, Collection<ParseResult<IStrategoTerm>>> entry : allParseResultsPerContext.asMap().entrySet()) {
            final IContext context = entry.getKey();
            final Iterable<ParseResult<IStrategoTerm>> parseResults = entry.getValue();
            log.debug(String.format("Analysing %s - %s", context.location(), context.language().name()));
            for ( ParseResult parseResult : parseResults ) {
                log.debug(String.format(" * %s", parseResult.source));
            }
            try {
                synchronized(context) {
                    final AnalysisResult<IStrategoTerm, IStrategoTerm> analysisResult =
                        analysisService.analyze(parseResults, context);
                    allAnalysisResults.put(context, analysisResult);
                    for ( AnalysisFileResult<IStrategoTerm,IStrategoTerm> fileResult : analysisResult.fileResults ) {
                        analysisMessages.putAll(context.language(), Lists.newArrayList(fileResult.messages));
                    }
                }
            } catch(AnalysisException ex) {
                throw new MojoFailureException("Analysis failed", ex);
            }
        }
        printMessages(analysisMessages, "analysis", pardonedLanguages);

        for(Entry<IContext, AnalysisResult<IStrategoTerm, IStrategoTerm>> entry : allAnalysisResults.entrySet()) {
            final IContext context = entry.getKey();
            if(!transformer.available(goal, context)) {
                log.debug(String.format("Transformer %s not available for %s", goal, context.language().name()));
                continue;
            }
            log.debug(String.format("Transforming %s - %s", context.location(), context.language().name()));
            final AnalysisResult<IStrategoTerm, IStrategoTerm> analysisResult = entry.getValue();
            synchronized(context) {
                for(AnalysisFileResult<IStrategoTerm, IStrategoTerm> fileResult : analysisResult.fileResults) {
                    if ( targetFiles.contains(fileResult.source) ) {
                        log.debug(String.format(" * %s", fileResult.source));
                        try {
                            transformer.transform(fileResult, context, goal);
                        } catch(TransformerException ex) {
                            throw new MojoFailureException("Transformation failed", ex);
                        }
                    }
                }
            }
        }
    }

    private void printMessages(Multimap<ILanguage,IMessage> messages, String type,
            List<String> pardonedLanguages) throws MojoFailureException {
        boolean hasErrors = false;
        for ( Entry<ILanguage,IMessage> lm : messages.entries() ) {
            IMessage message = lm.getValue();
            String messageText = message2string(lm.getValue());
            switch (message.severity()) {
            case ERROR:
                hasErrors = hasErrors || !pardonedLanguages.contains(lm.getKey().name());
                log.error(messageText);
                break;
            case WARNING:
                log.warn(messageText);
                break;
            case NOTE:
                log.info(messageText);
                break;
            }
        }
        if ( hasErrors ) {
            throw new MojoFailureException(String.format("There were %s errors.", type));
        }
    }

    private String message2string(IMessage message) {
        return String.format("%s[%s]: %s",
                message.source().getName(),
                region2string(message.region()),
                message.message());
    }

    private String region2string(ISourceRegion region) {
        return String.format("%s,%s",
                region.startRow()+1,
                region.startColumn()+1);
    }

    public void runStrategy(String name, String... args) throws MojoFailureException {
        log.info("Invoking strategy "+name+" ["+StringUtils.join(args, ", ")+"]");
        HybridInterpreter runtime = strategoRuntimeService.genericRuntime();
        ITermFactory factory = runtime.getFactory();
        Context context = new Context(factory);
        try {
            context.invokeStrategyCLI(name, name, args);
        } catch (StrategoException ex) {
            throw new MojoFailureException(ex.getMessage(), ex);
        }
    }

    public static SpoofaxHelper get(MavenProject project, PluginDescriptor plugin,
            Log log, boolean fresh)
            throws MojoFailureException {
        SpoofaxHelper spoofaxHelper;
        if ( fresh ) {
            log.info("Initialising standalone Spoofax core");
            spoofaxHelper = new SpoofaxHelper(project, plugin, log);
        } else {
            if ( (spoofaxHelper = (SpoofaxHelper) project.getContextValue("spoofax")) == null ) {
                log.info("Initialising shared Spoofax core");
                project.setContextValue("spoofax",
                        spoofaxHelper = new SpoofaxHelper(project, plugin, log));
            } else {
                log.info("Using shared Spoofax core");
            }
        }
        return spoofaxHelper;
    }

}
