package com.phyzicsz.rocket.reflection.vfs;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;

/**
 * Jar file implementation of Vfs.File
 * 
 * @author phyzicsz <phyzics.z@gmail.com>
 */
public class JarInputFile implements Vfs.File {
    private final ZipEntry entry;
    private final JarInputDir jarInputDir;
    private final long fromIndex;
    private final long endIndex;

    public JarInputFile(ZipEntry entry, JarInputDir jarInputDir, long cursor, long nextCursor) {
        this.entry = entry;
        this.jarInputDir = jarInputDir;
        fromIndex = cursor;
        endIndex = nextCursor;
    }

    @Override
    public String getName() {
        String name = entry.getName();
        return name.substring(name.lastIndexOf("/") + 1);
    }

    @Override
    public String getRelativePath() {
        return entry.getName();
    }

    @SuppressWarnings("InputStreamSlowMultibyteRead")
    @Override
    public InputStream openInputStream() throws IOException {
        return new InputStream() {
            @Override
            public int read() throws IOException {
                if (jarInputDir.cursor >= fromIndex && jarInputDir.cursor <= endIndex) {
                    int read = jarInputDir.jarInputStream.read();
                    jarInputDir.cursor++;
                    return read;
                } else {
                    return -1;
                }
            }
        };
    }
}
