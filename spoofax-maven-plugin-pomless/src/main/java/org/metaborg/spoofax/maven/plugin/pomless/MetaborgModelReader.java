package org.metaborg.spoofax.maven.plugin.pomless;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.commons.vfs2.FileObject;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Resource;
import org.apache.maven.model.io.DefaultModelWriter;
import org.apache.maven.model.io.ModelParseException;
import org.apache.maven.model.io.ModelReader;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.metaborg.core.MetaborgConstants;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.config.ConfigRequest;
import org.metaborg.core.config.IExportConfig;
import org.metaborg.core.config.IExportVisitor;
import org.metaborg.core.config.LangDirExport;
import org.metaborg.core.config.LangFileExport;
import org.metaborg.core.config.ResourceExport;
import org.metaborg.core.language.LanguageIdentifier;
import org.metaborg.core.messages.StreamMessagePrinter;
import org.metaborg.meta.core.config.ILanguageSpecConfig;
import org.metaborg.spoofax.maven.plugin.Constants;
import org.metaborg.spoofax.maven.plugin.SpoofaxInit;
import org.metaborg.spoofax.meta.core.SpoofaxExtensionModule;
import org.metaborg.spoofax.meta.core.config.ISpoofaxLanguageSpecConfig;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.sonatype.maven.polyglot.PolyglotModelUtil;
import org.sonatype.maven.polyglot.io.ModelReaderSupport;
import org.sonatype.maven.polyglot.mapping.Mapping;

import com.google.common.collect.Lists;

import mb.pie.task.archive.ArchiveCommon;
import mb.pie.task.archive.ArchiveDirectory;
import mb.resource.DefaultResourceService;
import mb.resource.fs.FSPath;
import mb.resource.fs.FSResource;
import mb.resource.fs.FSResourceRegistry;

@Component(role = ModelReader.class, hint = Constants.languageSpecType)
public class MetaborgModelReader extends ModelReaderSupport {
    private static final ILogger logger = LoggerUtils.logger(MetaborgModelReader.class);

    @Requirement(hint = "xml") private Mapping xmlMapping;


    @Override public Model read(Reader input, Map<String, ?> options) throws IOException, ModelParseException {
        if(SpoofaxInit.shouldInit()) {
            logger.info("Initialising Spoofax core");
            try {
                // HACK: Maven core extensions does not work with Java service loaders for some reason. Load all the
                // extension modules explicitly instead. New extensions need to be added here unfortunately.
                SpoofaxInit.init(new SpoofaxExtensionModule(), new org.metaborg.mbt.core.SpoofaxExtensionModule(), new org.metaborg.spt.core.SpoofaxExtensionModule());
            } catch(MetaborgException e) {
                throw new IOException("Cannot initialize Spoofax", e);
            }
        }

        final File configFile = PolyglotModelUtil.getLocationFile(options);
        final File root = configFile.getParentFile();
        final FileObject rootDir = SpoofaxInit.spoofax().resourceService.resolve(root);
        final ConfigRequest<ISpoofaxLanguageSpecConfig> configRequest =
            SpoofaxInit.spoofaxMeta().languageSpecConfigService.get(rootDir);
        if(!configRequest.valid()) {
            logger.error(
                "Errors occurred when retrieving language specification configuration from project location {}",
                rootDir);
            configRequest
                .reportErrors(new StreamMessagePrinter(SpoofaxInit.spoofax().sourceTextService, false, false, logger));
            throw new ModelParseException("Configuration for language specification at " + rootDir + " is invalid", -1,
                -1, null);
        }

        final ILanguageSpecConfig config = configRequest.config();
        if(config == null) {
            logger.error("Could not retrieve language specification configuration from project location {}", rootDir);
            throw new IOException("Could not read project configuration.");
        }

        if(config.useBuildSystemSpec()) {
            final File pom = xmlMapping.locatePom(root);
            logger.info("Using build system specification at {} instead of {}", pom, configFile);

            final ModelReader xmlReader = xmlMapping.getReader();
            final Model model = xmlReader.read(pom, options);
            return model;
        }

        final String metaborgVersion = config.metaborgVersion();

        Model model = new Model();
        model.setModelVersion("4.0.0");
        model.setGroupId(config.identifier().groupId);
        model.setArtifactId(config.identifier().id);
        model.setVersion(config.identifier().version.toString());
        model.setPackaging(Constants.languageSpecType);

        final Parent parent = new Parent();
        parent.setGroupId(MetaborgConstants.METABORG_GROUP_ID);
        parent.setArtifactId(Constants.languageParentId);
        parent.setVersion(metaborgVersion);
        parent.setRelativePath("");
        model.setParent(parent);

        boolean stratego2Project = false;

        for(LanguageIdentifier dep : config.compileDeps()) {
            model.addDependency(createDep(dep, Constants.languageSpecType, "provided"));
            stratego2Project |= dep.groupId.equals(MetaborgConstants.METABORG_GROUP_ID) && dep.id.equals(Constants.stratego2id);
        }
        for(LanguageIdentifier dep : config.sourceDeps()) {
            model.addDependency(createDep(dep, Constants.languageSpecType, "provided"));
        }
        for(LanguageIdentifier dep : config.javaDeps()) {
            model.addDependency(createDep(dep, "jar", "compile"));
        }

        if(stratego2Project) {
            final Path str2libsReplicatePath = root.toPath().resolve("target/replicate/str2libs");
            final String str2libsJarRelativePath = "target/replicate/str2libs.jar";
            final Path str2libsJarPath = root.toPath().resolve(str2libsJarRelativePath);
            Files.createDirectories(str2libsReplicatePath);

            final Dependency dep = new Dependency();
            dep.setGroupId(MetaborgConstants.METABORG_GROUP_ID);
            dep.setArtifactId(Constants.str2libs);
            dep.setVersion(MetaborgConstants.METABORG_VERSION);
            dep.setScope("system");
            dep.setSystemPath("${pom.basedir}/" + str2libsJarRelativePath);
            model.addDependency(dep);

            final Model depModel = new Model();
            depModel.setModelVersion("4.0.0");
            depModel.setGroupId(MetaborgConstants.METABORG_GROUP_ID);
            depModel.setArtifactId(Constants.str2libs);
            depModel.setVersion(MetaborgConstants.METABORG_VERSION);
            depModel.setPackaging("jar");
            new DefaultModelWriter().write(str2libsReplicatePath.resolve("str2libs.pom").toFile(), Collections.emptyMap(), depModel);

            final Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            ArchiveCommon.archiveToJar(new DefaultResourceService(new FSResourceRegistry()),
                new FSResource(str2libsJarPath),
                Collections.singletonList(ArchiveDirectory.ofDirectory(new FSPath(str2libsReplicatePath))),
                manifest);
        }

        final Build build = new Build();

        final List<Resource> resources = Lists.newArrayList();
        for(IExportConfig export : config.exports()) {
            export.accept(new IExportVisitor() {
                @Override public void visit(LangDirExport export) {
                    final Resource resource = new Resource();
                    resource.setDirectory(export.directory);
                    resource.setTargetPath(export.directory);
                    resource.setIncludes(Lists.newArrayList(export.includes));
                    resource.setExcludes(Lists.newArrayList(export.excludes));
                    resources.add(resource);
                }

                @Override public void visit(LangFileExport export) {
                    final Resource resource = new Resource();
                    resource.setDirectory(".");
                    resource.setIncludes(Lists.newArrayList(export.file));
                    resources.add(resource);
                }

                @Override public void visit(ResourceExport export) {
                    final Resource resource = new Resource();
                    resource.setDirectory(export.directory);
                    resource.setIncludes(Lists.newArrayList(export.includes));
                    resource.setExcludes(Lists.newArrayList(export.excludes));
                    resources.add(resource);
                }
            });
        }
        build.setResources(resources);

        final Plugin metaborgPlugin = new Plugin();
        metaborgPlugin.setGroupId(MetaborgConstants.METABORG_GROUP_ID);
        metaborgPlugin.setArtifactId(Constants.pluginId);
        metaborgPlugin.setVersion(metaborgVersion);
        metaborgPlugin.setExtensions(true);
        build.addPlugin(metaborgPlugin);

        model.setBuild(build);

        return model;
    }


    private Dependency createDep(LanguageIdentifier id, String type, String scope) {
        final Dependency dep = new Dependency();
        dep.setGroupId(id.groupId);
        dep.setArtifactId(id.id);
        dep.setVersion(id.version.toString());
        dep.setType(type);
        dep.setScope(scope);
        return dep;
    }
}
