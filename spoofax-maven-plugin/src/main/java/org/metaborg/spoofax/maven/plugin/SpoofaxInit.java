package org.metaborg.spoofax.maven.plugin;

import org.metaborg.core.MetaborgException;
import org.metaborg.core.project.ISimpleProjectService;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.meta.core.SpoofaxMeta;
import org.metaborg.spt.core.SPTModule;

import com.google.inject.Injector;

public class SpoofaxInit {
    private static class ShutdownHook extends Thread {
        public void run() {
            spoofaxMeta.close();
            spoofax.close();
        }
    }

    private static Spoofax spoofax;
    private static SpoofaxMeta spoofaxMeta;
    private static Injector sptInjector;

    private static ISimpleProjectService projectService;


    public static Spoofax spoofax() {
        return spoofax;
    }

    public static SpoofaxMeta spoofaxMeta() {
        return spoofaxMeta;
    }

    public static Injector sptInjector() {
        return sptInjector;
    }


    public static ISimpleProjectService projectService() {
        return projectService;
    }


    public static boolean shouldInit() {
        return spoofax == null || spoofaxMeta == null;
    }

    public static void init() throws MetaborgException {
        if(shouldInit()) {
            spoofax = new Spoofax(new MavenSpoofaxModule());
            spoofaxMeta = new SpoofaxMeta(spoofax);
            sptInjector = spoofaxMeta.injector.createChildInjector(new SPTModule());

            projectService = spoofax.injector.getInstance(ISimpleProjectService.class);

            Runtime.getRuntime().addShutdownHook(new ShutdownHook());
        }
    }
}
