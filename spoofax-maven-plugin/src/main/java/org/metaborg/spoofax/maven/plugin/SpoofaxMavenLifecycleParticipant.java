package org.metaborg.spoofax.maven.plugin;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.component.annotations.Component;

@Component(role = AbstractMavenLifecycleParticipant.class, hint = Constants.languageSpecType)
public class SpoofaxMavenLifecycleParticipant extends AbstractMavenLifecycleParticipant {
    @Override public void afterSessionEnd(MavenSession session) {
        SpoofaxInit.deinit();
    }
}
