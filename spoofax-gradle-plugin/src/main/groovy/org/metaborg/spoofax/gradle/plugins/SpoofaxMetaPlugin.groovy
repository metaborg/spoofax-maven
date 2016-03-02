package org.metaborg.spoofax.gradle.plugins

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.metaborg.core.MetaborgConstants
import org.metaborg.core.action.CompileGoal
import org.metaborg.core.build.BuildInput
import org.metaborg.core.build.BuildInputBuilder
import org.metaborg.core.messages.StreamMessagePrinter
import org.metaborg.core.project.IProject
import org.metaborg.spoofax.core.Spoofax
import org.metaborg.spoofax.core.resource.SpoofaxIgnoresSelector
import org.metaborg.spoofax.gradle.internals.GradleSpoofaxMetaModule
import org.metaborg.spoofax.meta.core.SpoofaxMeta
import org.metaborg.spoofax.meta.core.build.LanguageSpecBuildInput
import org.metaborg.spoofax.meta.core.generator.language.ContinuousLanguageSpecGenerator
import org.metaborg.spoofax.meta.core.generator.language.GeneratorSettingsBuilder
import org.metaborg.spoofax.meta.core.generator.language.LanguageSpecGenerator
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpec
import org.metaborg.util.file.FileAccess
import org.metaborg.util.log.LoggerUtils
import org.metaborg.util.prompt.Prompter

class SpoofaxMetaPlugin implements Plugin<Project> {
    private static final def logger = LoggerUtils.logger(SpoofaxMetaPlugin)

    private File rootDir
    private SpoofaxBasePlugin basePlugin
    private Configuration spoofaxSourceConfiguration
    private Configuration javaCompileConfiguration
    private SourceSet javaSourceSet

    @Override
    void apply(final Project project) {
        throwIfPluginConflict(project)
        rootDir = project.rootDir
        basePlugin = project.plugins.apply(SpoofaxBasePlugin)
        project.plugins.apply(JavaBasePlugin)
        createMetaExtension(project)
        createConfigurations(project)
        createTasks(project)
    }
 
    private def throwIfPluginConflict(final Project project) {
        if ( project.plugins.hasPlugin(SpoofaxPlugin) ) {
            throw new GradleException("Cannot apply plugin {} when plugin {} is already applied.",
                SpoofaxGradleConstants.SPOOFAX_META_PLUGIN_NAME,
                SpoofaxGradleConstants.SPOOFAX_PLUGIN_NAME)
        }
    }
    
    private def createMetaExtension(final Project project) {
        project.extensions.create(SpoofaxGradleConstants.SPOOFAX_EXTENSION_NAME,
            SpoofaxMetaPluginExtension)
    }
    
    private def createConfigurations(final Project project) {
        spoofaxSourceConfiguration = project.configurations.create(
                SpoofaxGradleConstants.SPOOFAX_SOURCE_CONFIGURATION_NAME)
        spoofaxSourceConfiguration.transitive = false
        spoofaxSourceConfiguration.visible = false
    }
    
    private def createTasks(final Project project) {
        project.task(SpoofaxGradleConstants.SPOOFAX_INITPROJECT_TASK_NAME,
                group: SpoofaxGradleConstants.SPOOFAX_GROUP_NAME,
                description: "Initialize Spoofax language project.") << {
            initProject()
        }
        
        project.task(SpoofaxGradleConstants.SPOOFAX_COMPILE_TASK_NAME,
                group: SpoofaxGradleConstants.SPOOFAX_GROUP_NAME,
                description: "Compile Spoofax sources.") << {
            generateSources()
        } 

        project.task(SpoofaxGradleConstants.SPOOFAX_METACOMPILE_PRE_TASK_NAME,
                description: "Compile Spoofax language (pre-Java).",
                dependsOn: SpoofaxGradleConstants.SPOOFAX_COMPILE_TASK_NAME) << {
            preCompile()
        }
        
        def javaConvention = project.convention.getPlugin(JavaPluginConvention)
        javaSourceSet = javaConvention.sourceSets.create(SpoofaxGradleConstants.SPOOFAX_JAVA_SOURCESET_NAME)
        project.tasks.getByName(javaSourceSet.compileJavaTaskName)
                .dependsOn(SpoofaxGradleConstants.SPOOFAX_METACOMPILE_PRE_TASK_NAME)
        javaCompileConfiguration = project.configurations.getByName(javaSourceSet.compileConfigurationName)

        project.task(SpoofaxGradleConstants.SPOOFAX_METACOMPILE_POST_TASK_NAME,
                description: "Compile Spoofax language (post-Java).",
                dependsOn: javaSourceSet.classesTaskName) << {
            postCompile()
        }

        project.task(SpoofaxGradleConstants.SPOOFAX_METACOMPILE_TASK_NAME,
                group: SpoofaxGradleConstants.SPOOFAX_GROUP_NAME,
                description: "Compile Spoofax language.",
                dependsOn: [
                    SpoofaxGradleConstants.SPOOFAX_METACOMPILE_PRE_TASK_NAME,
                    SpoofaxGradleConstants.SPOOFAX_METACOMPILE_POST_TASK_NAME
                ])

        project.tasks.findByName('compile')
                ?.dependsOn(SpoofaxGradleConstants.SPOOFAX_METACOMPILE_TASK_NAME)
                ?: logger.warn("Task 'compile' not found")

                
        project.task(SpoofaxGradleConstants.SPOOFAX_CREATEARCHIVE_TASK_NAME,
                group: SpoofaxGradleConstants.SPOOFAX_GROUP_NAME,
                description: "Compile Spoofax language.",
                dependsOn: SpoofaxGradleConstants.SPOOFAX_METACOMPILE_TASK_NAME) << {
            createArchive()
        }

        project.tasks.getByName('assemble')
                ?.dependsOn(SpoofaxGradleConstants.SPOOFAX_CREATEARCHIVE_TASK_NAME)
                ?: logger.warn("Task 'assemble' not found")

                
        project.task(SpoofaxGradleConstants.SPOOFAX_TEST_TASK_NAME,
                group: SpoofaxGradleConstants.SPOOFAX_GROUP_NAME,
                description: "Run Spoofax language tests.",
                dependsOn: SpoofaxGradleConstants.SPOOFAX_METACOMPILE_TASK_NAME) << {
            test()
        }

        project.tasks.getByName('check')
                ?.dependsOn(SpoofaxGradleConstants.SPOOFAX_TEST_TASK_NAME)
                ?: logger.warn("Task 'check' not found")
                

        project.task(SpoofaxGradleConstants.SPOOFAX_CLEAN_TASK_NAME,
                group: SpoofaxGradleConstants.SPOOFAX_GROUP_NAME,
                description: "Clean Spoofax build files.") << {
            clean()
        }

        project.tasks.getByName('clean')
                ?.dependsOn(SpoofaxGradleConstants.SPOOFAX_CLEAN_TASK_NAME)
                ?: logger.warn("Task 'clean' not found")
    }
 

    @Lazy Spoofax spoofax = basePlugin.spoofax
 
    @Lazy SpoofaxMeta spoofaxMeta =
            new SpoofaxMeta(spoofax, new GradleSpoofaxMetaModule())
 
    @Lazy IProject project =
            spoofax.projectService.create(spoofax.resourceService.resolve(rootDir))

    @Lazy ISpoofaxLanguageSpec languageSpec =
            spoofaxMeta.languageSpecService.get(project)
 
    @Lazy LanguageSpecBuildInput buildInput =
            new LanguageSpecBuildInput(languageSpec)
 
    
    def initProject() {
        if ( new File(rootDir,MetaborgConstants.FILE_CONFIG).exists() ) {
            throw new GradleException("Project already initialized.")
        }
        
        Prompter prompter
        try {
            prompter = Prompter.get();
        } catch(IOException e) {
            throw new GradleException("Must run interactively (try 'gradle --no-daemon')", e);
        }

        def settings = new GeneratorSettingsBuilder()
            .configureFromPrompt(prompter)
            .build(spoofax.resourceService.resolve(rootDir),
                    spoofaxMeta.languageSpecConfigBuilder)
            
        def newGenerator = new LanguageSpecGenerator(settings.generatorSettings,
                    settings.extensions, settings.analysisType);
        newGenerator.generateAll();

        def generator = new ContinuousLanguageSpecGenerator(settings.generatorSettings);
        generator.generateAll();
    }
    
 
    @Lazy private def init = {
        initDependencies()
        initJavaSources()
        spoofaxMeta.metaBuilder.initialize(buildInput)
        true // return non-null, or `init` runs every time
    }()
 
    private def initDependencies() {
        def config = project.config()
        // build languages
        for ( def language : config.compileDeps() ) {
            Configuration configuration = basePlugin.createBuildConfiguration(language)
            basePlugin.loadLanguages(configuration.incoming.files)
        }
        
        // source libraries
        for ( def language : config.sourceDeps() ) {
            spoofaxSourceConfiguration.dependencies.add(basePlugin.createLanguageDependency(language))
        }
        basePlugin.loadLanguages(spoofaxSourceConfiguration.incoming.files)

        // classpath libraries
        for ( def language : config.javaDeps() ) {
            javaCompileConfiguration.dependencies.add(basePlugin.createLanguageDependency(language))
        }
    }
 
    private def initJavaSources() {
        def paths = languageSpec.paths()
        javaSourceSet.java.srcDirs = [spoofax.resourceService.localPath(paths.strJavaFolder())]
        javaSourceSet.resources.srcDirs = []
        javaSourceSet.output.classesDir = spoofax.resourceService.localPath(paths.outputClassesFolder())
    }
    

    private def generateSources() {
        init
        spoofaxMeta.metaBuilder.generateSources(buildInput, new FileAccess());
        final BuildInputBuilder inputBuilder = new BuildInputBuilder(languageSpec);
        // @formatter:off
        final BuildInput input = inputBuilder
                .withDefaultIncludePaths(true)
                .withSourcesFromDefaultSourceLocations(true)
                .withSelector(new SpoofaxIgnoresSelector())
                .withMessagePrinter(new StreamMessagePrinter(spoofax.sourceTextService, true, true, logger))
                .withThrowOnErrors(true)
                .withPardonedLanguageStrings(languageSpec.config().pardonedLanguages())
                .addTransformGoal(new CompileGoal())
                .build(spoofax.dependencyService, spoofax.languagePathService)
                ;
        // @formatter:on
        spoofax.processorRunner.build(input, null, null).schedule().block();
    }
 
    
    private def preCompile() {
        init
        spoofaxMeta.metaBuilder.compilePreJava(buildInput)
    }
 
    
    private def postCompile() {
        init
        spoofaxMeta.metaBuilder.compilePostJava(buildInput)
    }
 
    
    private def createArchive() {
        throw new GradleException("Packaging is not yet implemented.")
    }

    
    private def test() {
        throw new GradleException("Testing is not yet implemented.")
    }
    

    private def clean() {
        init
        spoofaxMeta.metaBuilder.clean(buildInput)
    }
 
}
