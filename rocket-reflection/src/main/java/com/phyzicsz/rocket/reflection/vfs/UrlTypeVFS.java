package com.phyzicsz.rocket.reflection.vfs;

import com.phyzicsz.rocket.reflection.RocketReflection;
import com.phyzicsz.rocket.reflection.exception.ReflectionException;
import com.phyzicsz.rocket.reflection.vfs.Vfs.Dir;
import com.phyzicsz.rocket.reflection.vfs.Vfs.UrlType;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UrlType to be used by Reflections library. This class handles the vfszip and
 * vfsfile protocol of JBOSS files.
 */
public class UrlTypeVFS implements UrlType {

    private static final Logger logger = LoggerFactory.getLogger(RocketReflection.class);

    private final static String[] REPLACE_EXTENSION = new String[]{".ear/", ".jar/", ".war/", ".sar/", ".har/", ".par/"};

    final String VFSZIP = "vfszip";
    final String VFSFILE = "vfsfile";

    @Override
    public boolean matches(URL url) {
        return VFSZIP.equals(url.getProtocol()) || VFSFILE.equals(url.getProtocol());
    }

    @Override
    public Dir createDir(final URL url) {
        try {
            URL adaptedUrl = adaptURL(url);
            return new ZipDir(new JarFile(adaptedUrl.getFile()));
        } catch (IOException e) {
            try {
                return new ZipDir(new JarFile(url.getFile()));
            } catch (IOException ex) {
                logger.warn("Could not get URL", ex);
            }
        }
        return null;
    }

    public URL adaptURL(URL url) throws MalformedURLException {
        if (VFSZIP.equals(url.getProtocol())) {
            return replaceZipSeparators(url.getPath(), realFile);
        } else if (VFSFILE.equals(url.getProtocol())) {
            return new URL(url.toString().replace(VFSFILE, "file"));
        } else {
            return url;
        }
    }

    URL replaceZipSeparators(String path, Predicate<File> acceptFile)
            throws MalformedURLException {
        int pos = 0;
        while (pos != -1) {
            pos = findFirstMatchOfDeployableExtention(path, pos);

            if (pos > 0) {
                File file = new File(path.substring(0, pos - 1));
                if (acceptFile.test(file)) {
                    return replaceZipSeparatorStartingFrom(path, pos);
                }
            }
        }

        throw new ReflectionException("Unable to identify the real zip file in path '" + path + "'.");
    }

    int findFirstMatchOfDeployableExtention(String path, int pos) {
        Pattern p = Pattern.compile("\\.[ejprw]ar/");
        Matcher m = p.matcher(path);
        if (m.find(pos)) {
            return m.end();
        } else {
            return -1;
        }
    }

    Predicate<File> realFile = file -> file.exists() && file.isFile();

    URL replaceZipSeparatorStartingFrom(String path, int pos)
            throws MalformedURLException {
        String zipFile = path.substring(0, pos - 1);
        String zipPath = path.substring(pos);

        int numSubs = 1;
        for (String ext : REPLACE_EXTENSION) {
            while (zipPath.contains(ext)) {
                zipPath = zipPath.replace(ext, ext.substring(0, 4) + "!");
                numSubs++;
            }
        }

        String prefix = "";
        for (int i = 0; i < numSubs; i++) {
            prefix += "zip:";
        }

        if (zipPath.trim().length() == 0) {
            return new URL(prefix + "/" + zipFile);
        } else {
            return new URL(prefix + "/" + zipFile + "!" + zipPath);
        }
    }
}
