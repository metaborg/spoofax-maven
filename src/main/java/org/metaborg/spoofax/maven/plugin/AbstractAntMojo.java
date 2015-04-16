package org.metaborg.spoofax.maven.plugin;

import java.io.File;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;

public abstract class AbstractAntMojo extends AbstractSpoofaxMojo {

    private Project antProject;

    @Override
    public void execute() throws MojoExecutionException {
        super.execute();
        File buildFile = new File(getAntDirectory(), "build.main.xml");
        initProject(buildFile);
        setLogger();
        setProperties();
        parseBuildFile(buildFile);
    }

    private void initProject(File buildFile) throws BuildException {
        antProject = new Project();
        antProject.setBaseDir(getBasedir());
        antProject.setProperty("ant.file", buildFile.getAbsolutePath());
        antProject.init();
    }

    private void setLogger() {
        DefaultLogger consoleLogger = new DefaultLogger();
        consoleLogger.setErrorPrintStream(System.err);
        consoleLogger.setOutputPrintStream(System.out);
        consoleLogger.setMessageOutputLevel(Project.MSG_INFO);
        antProject.addBuildListener(consoleLogger);
    }

    private void setProperties() throws MojoExecutionException {
        antProject.setProperty("nativepath", getNativeDirectory().getAbsolutePath());
        antProject.setProperty("distpath", getDistDirectory().getAbsolutePath());

        antProject.setProperty("lang.name", getLanguageName());
        antProject.setProperty("lang.name.small", getLanguageName().toLowerCase());
        antProject.setProperty("lang.format", getLanguageFormat().name());
    }

    private void parseBuildFile(File buildFile) throws BuildException {
        ProjectHelper helper = ProjectHelper.getProjectHelper();
        antProject.addReference("ant.projectHelper", helper);
        helper.parse(antProject, buildFile);
    }

    public void executeTarget(String name) {
        antProject.executeTarget(name);
    }

}
