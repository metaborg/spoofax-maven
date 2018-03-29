# Spoofax Maven Plugin

This repository contains the sources for a Maven plugin that allows
the compilation of Spoofax langauges using only Maven.

## Installing

Run `mvn install` to install this plugin.

## Generate a project

Run `mvn org.metaborg:spoofax-maven-plugin:generate` to generate an example project in the
current directory.

## Shorthand commands

To use shorthand commands like `mvn spoofax:generate`, add the following to `~/.m2/settings.xml`:

```
  <pluginGroups>
    <pluginGroup>org.metaborg</pluginGroup>
  </pluginGroups>
```

Or create the file with the following content if it does not yet exist:

```
<?xml version="1.0" ?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <pluginGroups>
    <pluginGroup>org.metaborg</pluginGroup>
  </pluginGroups>
</settings>
```

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
