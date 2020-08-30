package com.phyzicsz.rocket.reflection.vfs;

import com.phyzicsz.rocket.reflection.ReflectionsException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class SystemDir implements Vfs.Dir {
    private final File file;

    public SystemDir(File file) {
        if (file != null && (!file.isDirectory() || !file.canRead())) {
            throw new RuntimeException("cannot use dir " + file);
        }

        this.file = file;
    }

    @Override
    public String getPath() {
        if (file == null) {
            return "/NO-SUCH-DIRECTORY/";
        }
        return file.getPath().replace("\\", "/");
    }

    @Override
    public Iterable<Vfs.File> getFiles() {
        if (file == null || !file.exists()) {
            return Collections.emptyList();
        }
        return () -> {
            try {
                try(Stream<Path> stream = Files.walk(file.toPath())) {
                    return stream.filter(Files::isRegularFile)
                        .map(path -> (Vfs.File) new SystemFile(SystemDir.this, path.toFile()))
                        .collect(Collectors.toList())
                        .iterator();  
                }
            } catch (IOException e) {
                throw new ReflectionsException("could not get files for " + file, e);
            }
        };
    }

    @Override
    public void close() {
    }

    @Override
    public String toString() {
        return getPath();
    }
}
