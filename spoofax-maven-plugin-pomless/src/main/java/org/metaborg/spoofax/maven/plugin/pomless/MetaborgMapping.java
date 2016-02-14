package org.metaborg.spoofax.maven.plugin.pomless;

import org.codehaus.plexus.component.annotations.Component;
import org.metaborg.spoofax.maven.plugin.Constants;
import org.sonatype.maven.polyglot.mapping.Mapping;
import org.sonatype.maven.polyglot.mapping.MappingSupport;

@Component(role = Mapping.class, hint = Constants.languageSpecType)
public class MetaborgMapping extends MappingSupport {
    public MetaborgMapping() {
        super(Constants.languageSpecType);

        // Define model from the metaborg.yaml file if it exists.
        setPomNames("metaborg.yaml");
        setAcceptLocationExtensions(".yaml");

        // Lower priority than XmlMapping such that having a pom.xml file overrides the metaborg.yaml model.
        setPriority(-2);
    }
}
