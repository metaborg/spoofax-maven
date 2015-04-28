package org.metaborg.spoofax.maven.plugin;

import java.io.File;
import java.io.IOException;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.codehaus.plexus.archiver.util.ResourceUtils;

class AntHelper {

    private final AbstractSpoofaxMojo mojo;
    private final Project antProject;

    public AntHelper(AbstractSpoofaxMojo mojo) throws MojoFailureException {
        this.mojo = mojo;
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
        antProject.setBaseDir(mojo.getBasedir());
        antProject.setProperty("ant.file", buildFile.getAbsolutePath());
        antProject.init();
        return antProject;
    }

    private void setLogger() {
        DefaultLogger consoleLogger = new DefaultLogger();
        consoleLogger.setErrorPrintStream(System.err);
        consoleLogger.setOutputPrintStream(System.out);
        consoleLogger.setMessageOutputLevel(Project.MSG_INFO);
        antProject.addBuildListener(consoleLogger);
    }

    private void setProperties() throws MojoFailureException {
        antProject.setProperty("nativepath", mojo.getNativeDirectory().getAbsolutePath());
        antProject.setProperty("distpath", mojo.getDistDirectory().getAbsolutePath());

        antProject.setProperty("lang.name", mojo.getName());
        antProject.setProperty("lang.name.small", mojo.getName().toLowerCase());
        antProject.setProperty("lang.format", mojo.getFormat().name());
        antProject.setProperty("lang.package.name", mojo.getPackageName());
        antProject.setProperty("lang.package.path", mojo.getPackagePath());

        antProject.setProperty("sdf.args", formatArgs(mojo.getSdfArgs()));
        antProject.setProperty("stratego.args", formatArgs(mojo.getStrategoArgs()));
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
