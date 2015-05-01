package org.metaborg.spoofax.maven.plugin.impl;

import org.metaborg.spoofax.maven.plugin.AbstractSpoofaxMojo;
import java.io.File;
import java.io.IOException;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.PropertyHelper;
import org.codehaus.plexus.archiver.util.ResourceUtils;
import org.metaborg.spoofax.generator.project.ProjectSettings;

public class AntHelper {

    private final AbstractSpoofaxMojo mojo;
    private final ProjectSettings projectSettings;
    private final Project antProject;

    public AntHelper(AbstractSpoofaxMojo mojo) throws MojoFailureException {
        this.mojo = mojo;
        this.projectSettings = mojo.getProjectSettings();
        prepareAntFiles();
        File buildFile = new File(mojo.getAntDirectory(), "build.main.xml");
        this.antProject = initProject(buildFile);
        setLogger();
        setProperties();
        parseBuildFile(buildFile);
    }

    private void prepareAntFiles() throws MojoFailureException {
        File antDirectory = mojo.getAntDirectory();
        try {
            if ( !antDirectory.exists() ) {
                antDirectory.mkdirs();
            }
            copyResource("build.main.xml", antDirectory);
            copyResource("build.generated.xml", antDirectory);
        } catch (IOException ex) {
            throw new MojoFailureException("Failed to copy Ant files.", ex);
        }
    }

    private void copyResource(String name, File directory) throws IOException {
        ResourceUtils.copyFile(getClass().getResourceAsStream(name),
                new File(directory, name));
    }

    private Project initProject(File buildFile) throws BuildException {
        Project antProject = new Project();
        antProject.setBaseDir(projectSettings.getBaseDir());
        antProject.setProperty("ant.file", buildFile.getAbsolutePath());
        antProject.init();
        return antProject;
    }

    private void setLogger() {
        DefaultLogger consoleLogger = new DefaultLogger();
        consoleLogger.setErrorPrintStream(System.err);
        consoleLogger.setOutputPrintStream(System.out);
        if ( mojo.getLog().isDebugEnabled() ) {
            consoleLogger.setMessageOutputLevel(Project.MSG_DEBUG);
        } else if ( mojo.getLog().isInfoEnabled() ) {
            consoleLogger.setMessageOutputLevel(Project.MSG_INFO);
        } else if ( mojo.getLog().isWarnEnabled()) {
            consoleLogger.setMessageOutputLevel(Project.MSG_WARN);
        } else if ( mojo.getLog().isErrorEnabled()) {
            consoleLogger.setMessageOutputLevel(Project.MSG_ERR);
        }
        antProject.addBuildListener(consoleLogger);
    }

    private void setProperties() throws MojoFailureException {
        PropertyHelper ph = PropertyHelper.getPropertyHelper(antProject);

        ph.setUserProperty("nativepath", mojo.getNativeDirectory().getAbsolutePath());
        ph.setUserProperty("distpath", mojo.getDistDirectory().getAbsolutePath());

        ph.setUserProperty("lang.name", projectSettings.name());
        ph.setUserProperty("lang.strname", projectSettings.strategoName());
        ph.setUserProperty("lang.format", projectSettings.format().name());
        ph.setUserProperty("lang.package.name", projectSettings.packageName());
        ph.setUserProperty("lang.package.path", projectSettings.packagePath());

        ph.setUserProperty("sdf.args", formatArgs(mojo.getSdfArgs()));
        ph.setUserProperty("stratego.args", formatArgs(mojo.getStrategoArgs()));

        if ( mojo.getExternalDef() != null ) {
            ph.setUserProperty("externaldef", mojo.getExternalDef());
        }
        if ( mojo.getExternalJar() != null ) {
            ph.setUserProperty("externaljar", mojo.getExternalJar());
        }
        if ( mojo.getExternalJarFlags() != null ) {
            ph.setUserProperty("externaljarflags", mojo.getExternalJarFlags());
        }
        if ( mojo.getJavaJarIncludes() != null ) {
            ph.setUserProperty("javajar-includes",
                    StringUtils.join(mojo.getJavaJarIncludes(),","));
        }
    }

    private void parseBuildFile(File buildFile) throws BuildException {
        ProjectHelper helper = ProjectHelper.getProjectHelper();
        antProject.addReference("ant.projectHelper", helper);
        helper.parse(antProject, buildFile);
    }

    public void executeTarget(String name) {
        antProject.executeTarget(name);
    }

    private String formatArgs(String[] args) {
        String ret = "";
        for ( String arg : args ) {
            if ( StringUtils.containsWhitespace(arg) ) {
                ret += " \""+arg+"\"";
            } else {
                ret += " "+arg;
            }
        }
        return ret;
    }

}
