package org.metaborg.spoofax.maven.plugin.impl;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.CharStreams;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
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
import org.metaborg.spoofax.core.language.ILanguageService;
import org.metaborg.spoofax.core.language.LanguageFileSelector;
import org.metaborg.spoofax.core.messages.IMessage;
import org.metaborg.spoofax.core.messages.ISourceRegion;
import static org.metaborg.spoofax.core.messages.MessageSeverity.*;
import org.metaborg.spoofax.core.resource.IResourceService;
import org.metaborg.spoofax.core.stratego.IStrategoRuntimeService;
import org.metaborg.spoofax.core.syntax.ISyntaxService;
import org.metaborg.spoofax.core.syntax.ParseException;
import org.metaborg.spoofax.core.syntax.ParseResult;
import org.metaborg.spoofax.core.transform.ITransformer;
import org.metaborg.spoofax.core.transform.ITransformerGoal;
import org.metaborg.spoofax.core.transform.TransformerException;
import org.metaborg.spoofax.generator.project.ProjectException;
import org.metaborg.spoofax.generator.project.ProjectSettings;
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
    private final Log log;
    private final List<ILanguage> compileLanguages;
    private final ProjectSettings projectSettings;

    private final IResourceService resourceService;
    private final ILanguageIdentifierService languageIdentifierService;
    private final ILanguageService languageService;
    private final ISyntaxService<IStrategoTerm> syntaxService;
    private final IAnalysisService<IStrategoTerm,IStrategoTerm> analysisService;
    private final IContextService contextService;
    private final ITransformer<IStrategoTerm, IStrategoTerm, IStrategoTerm> transformer;
    private final IStrategoRuntimeService strategoRuntimeService;
    private final ILanguageDiscoveryService languageDiscoveryService;
    private final MavenLanguagePathService mavenLanguagePathService;

    public SpoofaxHelper(MavenProject project, PluginDescriptor plugin,
            File dependencyDirectory, Log log)
            throws MojoFailureException {
        this.project = project;
        this.log = log;
        this.compileLanguages = Lists.newArrayList();
        try {
            this.projectSettings = new ProjectSettings(project.getName(), project.getBasedir());
        } catch (ProjectException ex) {
            throw new MojoFailureException(ex.getMessage(), ex);
        }

        DependencyHelper dh = new DependencyHelper(project, plugin);

        log.info("Initialising Spoofax core");
        mavenLanguagePathService = new MavenLanguagePathService();
        Injector spoofax = Guice.createInjector(new SpoofaxMavenModule(project,
                plugin, mavenLanguagePathService));
        resourceService = spoofax.getInstance(IResourceService.class);
        languageIdentifierService = spoofax.getInstance(ILanguageIdentifierService.class);
        languageService = spoofax.getInstance(ILanguageService.class);
        languageDiscoveryService = spoofax.getInstance(ILanguageDiscoveryService.class);
        syntaxService = spoofax.getInstance(
                Key.get(new TypeLiteral<ISyntaxService<IStrategoTerm>>(){}));
        analysisService = spoofax.getInstance(
                Key.get(new TypeLiteral<IAnalysisService<IStrategoTerm,IStrategoTerm>>(){}));
        contextService = spoofax.getInstance(IContextService.class);
        transformer = spoofax.getInstance(
                Key.get(new TypeLiteral<ITransformer<IStrategoTerm, IStrategoTerm, IStrategoTerm>>() {}));
        strategoRuntimeService = spoofax.getInstance(IStrategoRuntimeService.class);

        final FileObject basedir = resourceService.resolve(project.getBasedir());

        log.info("Discovering compile languages.");
        Collection<ILanguage> cl = discoverLanguages(dh.getCompileArtifacts());
        compileLanguages.addAll(cl);
        mavenLanguagePathService.addSources(cl, basedir);

        log.info("Discovering compile dependency languages.");
        discoverLanguages(dh.getCompileDependencyArtifacts());

        log.info("Unpacking language dependencies.");
        Collection<Artifact> dependencyArtifacts =
                DependencyHelper.unpack(dh.getDependencyArtifacts(),
                        dependencyDirectory, log);

        log.info("Discovering language dependencies.");
        Collection<ILanguage> languageDependencies = discoverLanguages(dependencyArtifacts);
        mavenLanguagePathService.addIncludes(languageDependencies);

        log.info("Registering source folders.");
        registerSources();

    }

    private void registerSources() throws MojoFailureException {
        FileObject trans = resourceService.resolve(projectSettings.getTransDirectory());
        FileObject syntax = resourceService.resolve(projectSettings.getSyntaxDirectory());
        FileObject include = resourceService.resolve(projectSettings.getBaseDir());
        mavenLanguagePathService.addSources(Languages.ESV,
                resourceService.resolve(projectSettings.getEditorDirectory()));
        mavenLanguagePathService.addSources(Languages.SDF3, syntax);
        mavenLanguagePathService.addSources(Languages.SDF, syntax);
        mavenLanguagePathService.addSources(Languages.Stratego, trans);
        mavenLanguagePathService.addSources(Languages.Stratego,
                resourceService.resolve(projectSettings.getLibDirectory()));
        mavenLanguagePathService.addIncludes(Languages.Stratego, include);
        mavenLanguagePathService.addSources(Languages.DynSem, trans);
    }

    // Spoofax Core initialisation

    private Collection<ILanguage> discoverLanguages(Collection<Artifact> languageArtifacts)
            throws MojoFailureException {
        List<ILanguage> discoveredLanguages = Lists.newArrayList();
        for ( Artifact artifact : languageArtifacts ) {
            try {
                File artifactFile = artifact.getFile();
                FileObject artifactFileObject = resourceService.resolve(
                        artifactFile.isDirectory() ?
                                artifactFile.getPath() : "zip:"+artifactFile);
                Collection<ILanguage> languages =
                        Lists.newArrayList(languageDiscoveryService.discover(artifactFileObject));
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
                throw new MojoFailureException(String.format("Error during language discovery in %s",artifact.getId()), ex);
            }
        }
        return discoveredLanguages;
    }

    public ILanguage getLanguage(String name) throws MojoFailureException {
        ILanguage language = languageService.get(name);
        if ( language == null ) {
            throw new MojoFailureException("Cannot find language "+name);
        }
        return language;
    }

    public Iterable<FileObject> getLanguageSources(String languageName) {
        return mavenLanguagePathService.getSources(languageName);
    }

    public Iterable<FileObject> getLanguageIncludes(String languageName) {
        return mavenLanguagePathService.getIncludes(languageName);
    }

    public Iterable<FileObject> getLanguageSourcesAndIncludes(String languageName) {
        return Iterables.concat(getLanguageSources(languageName), getLanguageIncludes(languageName));
    }

    public IResourceService getResourceService() {
        return resourceService;
    }

    // Transformations

    public void transformFiles(final ITransformerGoal goal,
            final List<String> pardonedLanguages)
            throws MojoFailureException {
        if ( !pardonedLanguages.isEmpty() ) {
            log.warn("Treating errors as warnings for "+
                    StringUtils.join(pardonedLanguages, " "));
        }

        for ( ILanguage language : compileLanguages ) {
            transformFiles(goal, language,
                    mavenLanguagePathService.getSources(language.name()), 
                    mavenLanguagePathService.getIncludes(language.name()),
                    pardonedLanguages.contains(language.name()));
        }
    }

    public void transformFiles(final ITransformerGoal goal,
            final ILanguage language, final Iterable<FileObject> files,
            final Iterable<FileObject> auxFiles, final boolean pardoned)
            throws MojoFailureException {
        FileObject basedir = resourceService.resolve(project.getBasedir());

        // create context
        IContext context;
        try {
            context = contextService.get(basedir, language);
        } catch (ContextException ex) {
            throw new MojoFailureException(ex.getMessage(), ex);
        }
        
        // build files
        final Set<FileObject> analysisFiles = Sets.newHashSet();
        final Set<FileObject> transformFiles = Sets.newHashSet();
        if ( files != null ) {
            transformFiles.addAll(Lists.newArrayList(files));
        }
        final LanguageFileSelector languageFileSelector = new LanguageFileSelector(languageIdentifierService, language);
        try {
            for ( FileObject file : files ) {
                if ( file.exists() ) {
                    List<FileObject> sources = Arrays.asList(file.findFiles(languageFileSelector));
                    analysisFiles.addAll(sources);
                    transformFiles.addAll(sources);
                }
            }
            for ( FileObject auxFile : auxFiles ) {
                if ( auxFile.exists() ) {
                    List<FileObject> sources = Arrays.asList(auxFile.findFiles(languageFileSelector));
                    analysisFiles.addAll(sources);
                }
            }
        } catch (FileSystemException ex) {
            throw new MojoFailureException(ex.getMessage(), ex);
        }
        
        // parse, analyse and transform
        Collection<ParseResult<IStrategoTerm>> parseResults =
                parseFiles(analysisFiles, language);
        if ( !parseResults.isEmpty() ) {
            log.info(String.format("Processing %s files", language.name()));
            AnalysisResult<IStrategoTerm, IStrategoTerm> analysisResult =
                    analyseFiles(context, parseResults, pardoned);
            transformFiles(context, goal, analysisResult, transformFiles);
        }
    }

    private Collection<ParseResult<IStrategoTerm>> parseFiles(
            final Collection<FileObject> files, final ILanguage language)
            throws MojoFailureException {
        final Collection<ParseResult<IStrategoTerm>> parseResults = Lists.newArrayList();
        final Collection<IMessage> parseMessages = Lists.newArrayList();
        for ( FileObject file : files ) {
            log.debug(String.format("Parsing %s as %s", file.getName(), language.name()));
            try {
                String text = CharStreams.toString(new InputStreamReader(file.getContent().getInputStream()));
                ParseResult<IStrategoTerm> parseResult =
                        syntaxService.parse(text, file, language);
                parseResults.add(parseResult);
                parseMessages.addAll(Lists.newArrayList(parseResult.messages));
            } catch (IOException | ParseException ex) {
                throw new MojoFailureException("Error during parsing.",ex);
            }
        }
        if ( printMessages(parseMessages, false) ) {
            throw new MojoFailureException("There were parse errors.");
        }
        return parseResults;
    }

    private AnalysisResult<IStrategoTerm, IStrategoTerm> analyseFiles(
            final IContext context, 
            final Iterable<ParseResult<IStrategoTerm>> parseResults,
            final boolean pardoned) throws MojoFailureException {
        AnalysisResult<IStrategoTerm, IStrategoTerm> analysisResult;
        log.debug(String.format("Analysing %s - %s", context.location(),
                context.language().name()));
        for ( ParseResult parseResult : parseResults ) {
            log.debug(String.format(" * %s", parseResult.source));
        }
        final Collection<IMessage> analysisMessages = Lists.newArrayList();
        try {
            synchronized(context) {
                analysisResult = analysisService.analyze(parseResults, context);
            }
            for ( AnalysisFileResult<IStrategoTerm,IStrategoTerm> fileResult : analysisResult.fileResults ) {
                analysisMessages.addAll(Lists.newArrayList(fileResult.messages));
            }
        } catch(AnalysisException ex) {
            throw new MojoFailureException("Analysis failed", ex);
        }
        if ( printMessages(analysisMessages, pardoned) ) {
            throw new MojoFailureException("There were analysis errors.");
        }
        return analysisResult;
    }
    
    private void transformFiles(final IContext context,
            final ITransformerGoal goal, 
            final AnalysisResult<IStrategoTerm, IStrategoTerm> analysisResult,
            final Collection<FileObject> targetFiles) throws MojoFailureException {
        if (!transformer.available(goal, context)) {
            log.debug(String.format("Transformer %s not available for %s", goal, context.language().name()));
            return;
        }
        log.debug(String.format("Transforming %s - %s", context.location(), context.language().name()));
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

    // Message printing

    private boolean printMessages(Collection<IMessage> messages,
            boolean errorsAsWarnings) throws MojoFailureException {
        boolean hasErrors = false;
        for ( IMessage message : messages ) {
            String messageText = message2string(message);
            switch (message.severity()) {
            case ERROR:
                if ( !errorsAsWarnings ) {
                    hasErrors = true;
                }
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
        return hasErrors;
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

    // Strategy stuff
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

}
