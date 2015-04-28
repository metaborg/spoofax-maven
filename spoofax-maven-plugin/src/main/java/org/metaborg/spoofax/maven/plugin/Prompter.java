package org.metaborg.spoofax.maven.plugin;

import java.io.Console;
import java.io.IOException;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class Prompter {
    
    private final Console console;

    private Prompter() throws IOException {
        this.console = System.console();
        if ( console == null ) {
            throw new IOException("No console found.");
        }
    }

    public String readString(String prompt, boolean nonEmpty) {
        String value = console.readLine("%s%s: ", prompt,
                nonEmpty ? " [required]" : "");
        if ( !(nonEmpty && value.isEmpty() ) ) {
            return value;
        }
        System.err.println("Please enter a value.");
        return readString(prompt, nonEmpty);
    }

    public String readString(String prompt, String defaultValue) {
        String value = console.readLine("%s [default=%s]: ", prompt, defaultValue);
        return value.isEmpty() ? defaultValue : value;
    }

    public String readStringFromList(String prompt, List<String> values, boolean nonEmpty) {
        String value = console.readLine("%s [%s%s]: ", prompt,
                StringUtils.join(values, "/"), nonEmpty ? ",required" : "");
        if ( !(nonEmpty && value.isEmpty()) || values.contains(value) ) {
            return value;
        }
        System.err.println("Please enter a valid value.");
        return readStringFromList(prompt, values, nonEmpty);
    }

    public String readStringFromList(String prompt, List<String> values, String defaultValue) {
        String value = console.readLine("%s [%s,default=%s]: ", prompt,
                StringUtils.join(values, "/"), defaultValue);
        if ( value.isEmpty() ) {
            return defaultValue;
        } else if (values.contains(value) ) {
            return value;
        }
        System.err.println("Please enter a valid value.");
        return readStringFromList(prompt, values, defaultValue);
    }

    // SINGLETON STUFF

    private static Prompter prompter;

    public static synchronized Prompter get() throws IOException {
        if ( prompter == null ) {
            prompter = new Prompter();
        }
        return prompter;
    }

}
