package com.phyzicsz.rocket.reflection.vfs;

import com.phyzicsz.rocket.reflection.Reflections;
import java.io.IOException;
import java.util.jar.JarFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * an implementation of Vfs.Dir for {@link java.util.zip.ZipFile}
 */
public class ZipDir implements Vfs.Dir {

    private static final Logger logger = LoggerFactory.getLogger(Reflections.class);
    final java.util.zip.ZipFile jarFile;

    public ZipDir(JarFile jarFile) {
        this.jarFile = jarFile;
    }

    @Override
    public String getPath() {
        if (jarFile == null) {
            return "/NO-SUCH-DIRECTORY/";
        }
        return jarFile.getName().replace("\\", "/");
    }

    @Override
    public Iterable<Vfs.File> getFiles() {
        return () -> jarFile.stream()
                .filter(entry -> !entry.isDirectory())
                .map(entry -> (Vfs.File) new ZipFile(ZipDir.this, entry))
                .iterator();
    }

    @Override
    public void close() {
        try {
            jarFile.close();
        } catch (IOException e) {
            logger.warn("Could not close JarFile", e);
        }
    }

    @Override
    public String toString() {
        return jarFile.getName();
    }
}
