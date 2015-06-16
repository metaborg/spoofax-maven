package org.metaborg.spoofax.maven.plugin;

import java.util.Collection;

import org.apache.commons.vfs2.FileObject;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.metaborg.spoofax.core.build.dependency.IDependencyService;
import org.metaborg.spoofax.core.build.paths.ILanguagePathService;
import org.metaborg.spoofax.core.language.ILanguage;
import org.metaborg.spoofax.core.transform.CompileGoal;
import org.metaborg.spoofax.meta.core.MetaBuildInput;
import org.metaborg.spoofax.meta.core.SpoofaxBuilder;
import org.metaborg.spoofax.meta.core.SpoofaxMetaBuilder;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.inject.Injector;

@Mojo(name = "generate-sources", defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    requiresDependencyResolution = ResolutionScope.COMPILE)
public class GenerateSourcesMojo extends AbstractSpoofaxLifecycleMojo {
    @Parameter(property = "spoofax.generate-sources.skip", defaultValue = "false") private boolean skip;

    @Override public void execute() throws MojoFailureException {
        if(skip) {
            return;
        }
        super.execute();

        getLog().info("Generating Spoofax sources");

        final Injector spoofax = getSpoofax();
        final MetaBuildInput input = getMetaBuildInput();
        
        final SpoofaxBuilder builder = spoofax.getInstance(SpoofaxBuilder.class);
        final IDependencyService dependencyService = spoofax.getInstance(IDependencyService.class);
        final ILanguagePathService languagePathService = spoofax.getInstance(ILanguagePathService.class);
        final Iterable<ILanguage> compileLanguages = dependencyService.compileDependencies(input.project);
        final Multimap<ILanguage, FileObject> sources = HashMultimap.create();
        final Multimap<ILanguage, FileObject> includes = HashMultimap.create();
        final Collection<ILanguage> pardonedLanguages = Lists.newArrayList();
        for(ILanguage language : compileLanguages) {
            sources.putAll(language, languagePathService.sources(input.project, language.name()));
            includes.putAll(language, languagePathService.includes(input.project, language.name()));
            if(input.pardonedLanguages.contains(language.name())) {
                pardonedLanguages.add(language);
            }
        }
        
        try {
            builder.build(new CompileGoal(), sources, includes, pardonedLanguages);
        } catch(Exception e) {
            throw new MojoFailureException(e.getMessage(), e);
        }

        final SpoofaxMetaBuilder metaBuilder = spoofax.getInstance(SpoofaxMetaBuilder.class);
        try {
            metaBuilder.generateSources(input);
        } catch(Exception e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
    }
}
