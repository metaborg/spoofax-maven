package org.metaborg.spoofax.maven.plugin.pomless;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;

import org.apache.commons.vfs2.FileObject;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.ModelParseException;
import org.apache.maven.model.io.ModelReader;
import org.codehaus.plexus.component.annotations.Component;
import org.metaborg.core.MetaborgConstants;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.LanguageIdentifier;
import org.metaborg.meta.core.config.ILanguageSpecConfig;
import org.metaborg.spoofax.maven.plugin.Constants;
import org.metaborg.spoofax.maven.plugin.SpoofaxInit;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.sonatype.maven.polyglot.PolyglotModelUtil;
import org.sonatype.maven.polyglot.io.ModelReaderSupport;

@Component(role = ModelReader.class, hint = Constants.languageSpecType)
public class MetaborgModelReader extends ModelReaderSupport {
    private static final ILogger logger = LoggerUtils.logger(MetaborgModelReader.class);


    @Override public Model read(Reader input, Map<String, ?> options) throws IOException, ModelParseException {
        if(SpoofaxInit.shouldInit()) {
            logger.info("Initialising Spoofax core");
            try {
                SpoofaxInit.init();
            } catch(MetaborgException e) {
                throw new IOException("Cannot instantiate Spoofax", e);
            }
        }

        final File root = PolyglotModelUtil.getLocationFile(options).getParentFile();
        final FileObject rootDir = SpoofaxInit.spoofax().resourceService.resolve(root);
        final ILanguageSpecConfig config = SpoofaxInit.spoofaxMeta().languageSpecConfigService.get(rootDir);
        final String metaborgVersion = config.metaborgVersion();

        Model model = new Model();
        model.setModelVersion("4.0.0");
        model.setName(config.name());
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

        for(LanguageIdentifier dep : config.compileDeps()) {
            model.addDependency(createDep(dep, Constants.languageSpecType, "provided"));
        }
        for(LanguageIdentifier dep : config.sourceDeps()) {
            model.addDependency(createDep(dep, Constants.languageSpecType, "provided"));
        }
        for(LanguageIdentifier dep : config.javaDeps()) {
            model.addDependency(createDep(dep, "jar", "compile"));
        }
        
        // TODO: exports

        final Build build = new Build();
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
