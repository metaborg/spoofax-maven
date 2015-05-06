package org.metaborg.spoofax.maven.plugin.impl;

import com.google.common.collect.Lists;
import java.io.File;
import java.util.Collection;
import java.util.List;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;

public class FileHelper {
 
    public static FileSet getFileSet(File directory) {
        FileSet fileSet = new FileSet();
        fileSet.setDirectory(directory.getPath());
        return fileSet;
    }

    public static List<File> getFiles(FileSet fileSets, File basedir) {
        FileSetManager fsm = new FileSetManager();
        List<File> files = Lists.newArrayList();
        getFiles(fileSets, basedir, fsm, files);
        return files;
    }

    public static List<File> getFiles(Collection<FileSet> fileSets, File basedir) {
        FileSetManager fsm = new FileSetManager();
        List<File> files = Lists.newArrayList();
        for ( FileSet fileSet : fileSets ) {
            getFiles(fileSet, basedir, fsm, files);
        }
        return files;
    }

    private static void getFiles(FileSet fileSet, File basedir, FileSetManager fsm, List<File> files) {
        String path = fileSet.getDirectory();
        File directory = getAbsoluteFile(path != null ? path : "", basedir);
        fileSet.setDirectory(directory.getPath());
        for ( String file : fsm.getIncludedFiles(fileSet) ) {
            files.add(new File(directory, file));
        }
    }

    public static File getAbsoluteFile(File file, File basedir) {
        return file.isAbsolute() ? file : new File(basedir, file.getPath());
    }

    public static File getAbsoluteFile(String path, File basedir) {
        return getAbsoluteFile(new File(path), basedir);
    }

    private FileHelper() {
    }

}
