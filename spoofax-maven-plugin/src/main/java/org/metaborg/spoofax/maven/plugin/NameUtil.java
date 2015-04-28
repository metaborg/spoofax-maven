package org.metaborg.spoofax.maven.plugin;

public class NameUtil {
    
    public static boolean isValidIdentifier(String name) {
        if ( name == null || name.isEmpty() ) {
            return false;
        }
        char[] cs = name.toCharArray();
        if ( !Character.isJavaIdentifierStart(cs[0]) ) {
            return false;
        }
        for ( int i = 1; i < cs.length; i++ ) {
            if ( !Character.isJavaIdentifierPart(cs[i]) ) {
                return false;
            }
        }
        return true;
    }

    public static boolean isValidPackage(String name) {
        if ( name == null || name.isEmpty() ) {
            return false;
        }
        for ( String part : name.split("\\.") ) {
            if ( !isValidIdentifier(part) ) {
                return false;
            }
        }
        return true;
    }

    private NameUtil() {
    }

}
