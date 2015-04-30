package org.metaborg.spoofax.maven.plugin.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;

public class FileSetHelper {
 
    public static List<File> getFiles(FileSet fileSets) {
        FileSetManager fsm = new FileSetManager();
        List<File> files = new ArrayList<>();
        getFiles(fileSets, fsm, files);
        return files;
    }

    public static List<File> getFiles(List<FileSet> fileSets) {
        FileSetManager fsm = new FileSetManager();
        List<File> files = new ArrayList<>();
        for ( FileSet fileSet : fileSets ) {
            getFiles(fileSet, fsm, files);
        }
        return files;
    }

    private static void getFiles(FileSet fileSet, FileSetManager fsm, List<File> files) {
        String directory = fileSet.getDirectory();
        for ( String file : fsm.getIncludedFiles(fileSet) ) {
            files.add(new File(directory, file));
        }
    }

    private FileSetHelper() {
    }

}
