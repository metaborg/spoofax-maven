package org.metaborg.spoofax.maven.plugin.mojo.manual;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.metaborg.spoofax.maven.plugin.AbstractSpoofaxMojo;
import org.metaborg.spoofax.maven.plugin.SpoofaxInit;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.HybridInterpreter;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.StrategoException;

@Mojo(name = "run")
public class RunMojo extends AbstractSpoofaxMojo {
    @Parameter(defaultValue = "false") boolean skip;
    @Parameter(readonly = true, required = true) private String name;
    @Parameter(readonly = true) private String[] args;

    public String[] getArgs() {
        return args == null ? new String[0] : args;
    }

    @Override public void execute() throws MojoFailureException, MojoExecutionException {
        if(skip || skipAll) {
            return;
        }
        super.execute();

        getLog().info("Invoking strategy " + name + " [" + StringUtils.join(getArgs(), ", ") + "]");

        final HybridInterpreter runtime = SpoofaxInit.spoofax().strategoRuntimeService.genericRuntime();
        final ITermFactory factory = runtime.getFactory();
        final Context context = new Context(factory);
        try {
            context.invokeStrategyCLI(name, name, getArgs());
        } catch(StrategoException ex) {
            throw new MojoFailureException(ex.getMessage(), ex);
        }
    }
}
