package org.metaborg.spoofax.maven.plugin.mojo.language;

import java.util.Collection;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.build.dependency.MissingDependencyException;
import org.metaborg.core.language.ILanguageComponent;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.spoofax.core.SpoofaxConstants;
import org.metaborg.spoofax.maven.plugin.SpoofaxInit;
import org.metaborg.spt.core.SPTRunner;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import mb.util.vfs2.resource.FileSelectorUtils;

@Mojo(name = "verify", defaultPhase = LifecyclePhase.VERIFY, requiresDependencyCollection = ResolutionScope.TEST,
    requiresDependencyResolution = ResolutionScope.TEST)
public class VerifyMojo extends AbstractSpoofaxLanguageMojo {
    private static final ILogger logger = LoggerUtils.logger(VerifyMojo.class);

    @Parameter(property = "spoofax.test.skip", defaultValue = "false") boolean skip;
    @Parameter(defaultValue = "${maven.test.skip}", readonly = true) private boolean mvnTestSkip;
    @Parameter(defaultValue = "${skipTests}", readonly = true) private boolean mvnSkipTests;

    @Override public void execute() throws MojoFailureException, MojoExecutionException {
        if(skip || mvnTestSkip || mvnSkipTests || skipAll) {
            return;
        }
        super.execute();

        try {
            final FileObject[] sptFiles = basedirLocation().findFiles(FileSelectorUtils.extension("spt"));
            if(sptFiles == null || sptFiles.length == 0) {
                // Skip silently
                return;
            }
        } catch(FileSystemException e) {
            throw new MojoExecutionException("Error determining files to test", e);
        }

        final Iterable<? extends ILanguageImpl> sptLangs =
            SpoofaxInit.spoofax().languageService.getAllImpls(SpoofaxConstants.GROUP_ID, SpoofaxConstants.LANG_SPT_ID);
        final int sptLangsSize = Iterables2.size(sptLangs);
        if(sptLangsSize == 0) {
            logger.info(
                "Skipping tests because SPT language implementation ({}:{}) is not a dependency",
                SpoofaxConstants.GROUP_ID, SpoofaxConstants.LANG_SPT_ID);
            return;
        }
        if(sptLangsSize > 1) {
            throw new MojoExecutionException("Multiple SPT language implementations were found");
        }
        final ILanguageImpl sptLang = sptLangs.iterator().next();

        try {
            Collection<ILanguageComponent> compileDeps =
                SpoofaxInit.spoofax().dependencyService.compileDeps(languageSpec());
            if(compileDeps.stream().noneMatch(dep -> Iterables2.contains(dep.contributesTo(), sptLang))) {
                logger.info(
                    "Skipping tests because SPT language implementation ({}:{}) is not a dependency",
                    SpoofaxConstants.GROUP_ID, SpoofaxConstants.LANG_SPT_ID);
                return;
            }
        } catch(MissingDependencyException e) {
            throw new MojoExecutionException("Could not determine language spec dependencies", e);
        }

        final ILanguageImpl testLang =
            SpoofaxInit.spoofax().languageService.getImpl(languageSpec().config().identifier());
        if(testLang == null) {
            logger.info("Skipping tests because language under test was not found");
            return;
        }

        try {
            logger.info("Running SPT tests");
            SpoofaxInit.sptInjector().getInstance(SPTRunner.class).test(languageSpec(), sptLang, testLang);
        } catch(MetaborgException e) {
            throw new MojoFailureException("Error testing", e);
        }
    }
}
