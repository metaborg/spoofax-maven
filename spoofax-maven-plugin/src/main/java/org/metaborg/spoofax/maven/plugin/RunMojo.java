package org.metaborg.spoofax.maven.plugin;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.metaborg.spoofax.core.stratego.IStrategoRuntimeService;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.HybridInterpreter;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.StrategoException;

@Mojo(name = "run")
public class RunMojo extends AbstractSpoofaxMojo {

    @Parameter(defaultValue = "false")
    boolean skip;

    @Parameter(readonly = true, required = true)
    private String name;

    @Parameter(readonly = true)
    private String[] args;

    public String[] getArgs() {
        return args == null ? new String[0] : args;
    }

    @Override
    public void execute() throws MojoFailureException {
        if ( skip ) { return; }
        getLog().info("Invoking strategy "+name+" ["+StringUtils.join(getArgs(), ", ")+"]");
        IStrategoRuntimeService strategoRuntimeService = getSpoofax().getInstance(IStrategoRuntimeService.class);
        HybridInterpreter runtime = strategoRuntimeService.genericRuntime();
        ITermFactory factory = runtime.getFactory();
        Context context = new Context(factory);
        try {
            context.invokeStrategyCLI(name, name, getArgs());
        } catch (StrategoException ex) {
            throw new MojoFailureException(ex.getMessage(), ex);
        }
    }
 
}
