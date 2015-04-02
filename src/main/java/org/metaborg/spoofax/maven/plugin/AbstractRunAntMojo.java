package org.metaborg.spoofax.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.types.LogLevel;
import org.codehaus.plexus.archiver.util.ResourceUtils;
import org.codehaus.plexus.util.FileUtils;

public abstract class AbstractRunAntMojo extends AbstractSpoofaxMojo {

    public void executeAntTarget(String target) throws MojoExecutionException {
        super.execute();
        copyResource("build.main.xml", getBuildDirectory(), false);
        copyResource("build.generated.xml", getBuildDirectory(), false);
        runAnt(target);
    }

    private void copyResource(String name, File directory, boolean preservePath)
            throws MojoExecutionException {
        InputStream is = getClass().getResourceAsStream(name);
        if ( is == null ) {
            throw new MojoExecutionException("Resource "+name+" not found.");
        }
        try {
            File dst = new File(directory, preservePath ? name : FileUtils.basename(name)+FileUtils.extension(name) );
            dst.getParentFile().mkdirs();
            ResourceUtils.copyFile(is, dst);
        } catch (IOException ex) {
            throw new MojoExecutionException("", ex);
        }
    }

    private void runAnt(String target) throws MojoExecutionException {
        File buildFile = new File(getBuildDirectory(), "build.main.xml");
        Project p = new Project();
        p.init();
        p.setBaseDir(getBasedir());
        p.setName(getLangName());

        p.setUserProperty("ant.file", buildFile.getAbsolutePath());
        p.setUserProperty("nativepath", getNativePath().getAbsolutePath());
        p.setUserProperty("distpath", new File(getDependencyDirectory(), "dist").getAbsolutePath());

        p.setUserProperty("lang.name", getLangName());
        p.setUserProperty("lang.name.small", getLangName().toLowerCase());
        p.setUserProperty("lang.format", getLangFormat().name());

        ProjectHelper.configureProject(p, buildFile);

        DefaultLogger logger = new DefaultLogger();
        logger.setOutputPrintStream(System.out);
        logger.setErrorPrintStream(System.err);
        logger.setMessageOutputLevel(LogLevel.INFO.getLevel());
        p.addBuildListener(logger);

        p.executeTarget(target);
    }

    private File getNativePath() throws MojoExecutionException {
        File buildDepsDirectory = getDependencyDirectory();
        if ( SystemUtils.IS_OS_WINDOWS ) {
            return new File(buildDepsDirectory, "native/cygwin");
        } else if ( SystemUtils.IS_OS_MAC_OSX ) {
            return new File(buildDepsDirectory, "native/macosx");
        } else if ( SystemUtils.IS_OS_LINUX ) {
            return new File(buildDepsDirectory, "native/linux");
        } else {
            throw new MojoExecutionException("Unsupported platform "+SystemUtils.OS_NAME);
        }
    }

}
