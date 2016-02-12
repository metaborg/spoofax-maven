package org.metaborg.spoofax.maven.plugin;

import org.metaborg.core.MetaborgException;
import org.metaborg.core.project.ISimpleProjectService;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.core.project.ISimpleLegacyMavenProjectService;
import org.metaborg.spoofax.meta.core.SpoofaxMeta;

public class SpoofaxInit {
    private static Spoofax spoofax;
    private static SpoofaxMeta spoofaxMeta;

    private static ISimpleProjectService projectService;
    private static ISimpleLegacyMavenProjectService mavenProjectService;


    public static Spoofax spoofax() {
        return spoofax;
    }

    public static SpoofaxMeta spoofaxMeta() {
        return spoofaxMeta;
    }


    public static ISimpleProjectService projectService() {
        return projectService;
    }

    public static ISimpleLegacyMavenProjectService mavenProjectService() {
        return mavenProjectService;
    }


    public static boolean shouldInit() {
        return spoofax == null || spoofaxMeta == null;
    }

    public static void init() throws MetaborgException {
        if(shouldInit()) {
            spoofax = new Spoofax(new MavenSpoofaxModule());
            spoofaxMeta = new SpoofaxMeta(spoofax);

            projectService = spoofax.injector.getInstance(ISimpleProjectService.class);
            mavenProjectService = spoofax.injector.getInstance(ISimpleLegacyMavenProjectService.class);
        }
    }
}
