package org.metaborg.spoofax.maven.plugin.mojo;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.spoofax.maven.plugin.AbstractSpoofaxLifecycleMojo;
import org.metaborg.spoofax.maven.plugin.SpoofaxInit;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.resource.FileSelectorUtils;

import com.google.common.collect.Iterables;

@Mojo(name = "verify", defaultPhase = LifecyclePhase.VERIFY, requiresDependencyCollection = ResolutionScope.TEST,
    requiresDependencyResolution = ResolutionScope.TEST)
public class VerifyMojo extends AbstractSpoofaxLifecycleMojo {
    private static final ILogger logger = LoggerUtils.logger(VerifyMojo.class);

    @Parameter(property = "spoofax.test.skip", defaultValue = "false") boolean skip;


    @Override public void execute() throws MojoFailureException, MojoExecutionException {
        if(skip || skipAll) {
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
            SpoofaxInit.spoofax().languageService.getAllImpls("org.metaborg", "org.metaborg.meta.lang.spt");
        if(Iterables.isEmpty(sptLangs)) {
            logger.info(
                "Skipping tests because SPT language implementation (org.metaborg:org.metaborg.meta.lang.spt) is not a dependency");
            return;
        }

        try {
            logger.info("Running SPT tests");
            SpoofaxInit.spoofaxMeta().metaBuilder.test(buildInput());
        } catch(MetaborgException e) {
            throw new MojoFailureException("Error testing", e);
        }
    }
}
