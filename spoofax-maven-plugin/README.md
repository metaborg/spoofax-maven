# Spoofax Maven Plugin

THis repository contains the sources for a Maven plugin that allows
the compilation of Spoofax langauges using only Maven.

## Installing

Run `mvn install` to install this plugin.

## Generate a project

Run `mvn spoofax:generate` to generate an example project in the
current directory.

## Convert existing project

 * Change POM.
   - Set packaging to spoofax-language.
   - Set name and artifactId to name and id from *.main.esv.
   - Set sdfArgs, strategoArgs and format based on build.main.xml
   - Set resources based on build.properties.
 * Remove Eclipse specific Java files in
   editor/java/package.name/*.java.
 * If package name changes, rename
   editor/java/old.package.name/strategies to
   editor/java/new.package.name/strategies and replace
   old.package.name with new.package.name in *.java.
 * Delete build.*, plugin.xml, META-INF/, utils/, .classpath,
   .externalToolBuilders/, .project, .settings

## Limitations

 * The plugin does not support the standard Maven directory layout, but
   is hardcoded to use the classic Spoofax language directory layout.
