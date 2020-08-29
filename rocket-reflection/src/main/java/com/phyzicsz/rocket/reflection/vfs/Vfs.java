package com.phyzicsz.rocket.reflection.vfs;

import com.phyzicsz.rocket.reflection.Reflections;
import com.phyzicsz.rocket.reflection.ReflectionsException;
import com.phyzicsz.rocket.reflection.util.ClasspathHelper;
import com.phyzicsz.rocket.reflection.util.Utils;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Vfs {

    private static final Logger logger = LoggerFactory.getLogger(Reflections.class);
    private static List<UrlType> defaultUrlTypes = new ArrayList<>(Arrays.asList(DefaultUrlTypes.values()));

    /**
     * an abstract vfs dir
     */
    public interface Dir {

        String getPath();

        Iterable<File> getFiles();

        void close();
    }

    /**
     * an abstract vfs file
     */
    public interface File {

        String getName();

        String getRelativePath();

        InputStream openInputStream() throws IOException;
    }

    /**
     * a matcher and factory for a url
     */
    public interface UrlType {

        boolean matches(URL url) throws Exception;

        Dir createDir(URL url) throws Exception;
    }

    /**
     * the default url types that will be used when issuing
     *
     * @return the list of URLTypes
     */
    public static List<UrlType> getDefaultUrlTypes() {
        return defaultUrlTypes;
    }

    /**
     * sets the static default url types.can be used to statically plug in
     * urlTypes
     *
     * @param urlTypes default URL type
     */
    public static void setDefaultURLTypes(final List<UrlType> urlTypes) {
        defaultUrlTypes = urlTypes;
    }

    /**
     * * add a static default url types to the beginning of the default url
     * types list.can be used to statically plug in urlTypes
     *
     * @param urlType default URL type
     */
    public static void addDefaultURLTypes(UrlType urlType) {
        defaultUrlTypes.add(0, urlType);
    }

    /**
     * tries to create a Dir from the given url, using the defaultUrlTypes
     *
     * @param url URL
     * @return Dir for a given URL
     */
    public static Dir fromURL(final URL url) {
        return fromURL(url, defaultUrlTypes);
    }

    /**
     * tries to create a Dir from the given url, using the given urlType
     *
     * @param url URL
     * @param urlTypes the URL type
     * @return a Dir for the URL
     */
    public static Dir fromURL(final URL url, final List<UrlType> urlTypes) {
        for (UrlType type : urlTypes) {
            try {
                if (type.matches(url)) {
                    Dir dir = type.createDir(url);
                    if (dir != null) {
                        return dir;
                    }
                }
            } catch (Exception e) {
                logger.warn("could not create Dir using {} from url. skipping.", type, url.toExternalForm(), e);
            }
        }

        throw new ReflectionsException("could not create Vfs.Dir from url, no matching UrlType was found [" + url.toExternalForm() + "]\n"
                + "either use fromURL(final URL url, final List<UrlType> urlTypes) or "
                + "use the static setDefaultURLTypes(final List<UrlType> urlTypes) or addDefaultURLTypes(UrlType urlType) "
                + "with your specialized UrlType.");
    }

    /**
     * tries to create a Dir from the given url, using the given urlType
     *
     * @param url URL
     * @param urlTypes the URL type
     * @return a dir for the URL
     */
    public static Dir fromURL(final URL url, final UrlType... urlTypes) {
        return fromURL(url, Arrays.asList(urlTypes));
    }

    /**
     * return an iterable of all Vfs.File in given urls, starting with given
     * packagePrefix and matching nameFilter
     *
     * @param inUrls the URLs
     * @param packagePrefix a package prefix
     * @param nameFilter a name filter
     * @return An iterable for the files in the URLs
     */
    public static Iterable<File> findFiles(final Collection<URL> inUrls, final String packagePrefix, final Predicate<String> nameFilter) {
        Predicate<File> fileNamePredicate = file -> {
            String path = file.getRelativePath();
            if (path.startsWith(packagePrefix)) {
                String filename = path.substring(path.indexOf(packagePrefix) + packagePrefix.length());
                return !Utils.isEmpty(filename) && nameFilter.test(filename.substring(1));
            } else {
                return false;
            }
        };

        return findFiles(inUrls, fileNamePredicate);
    }

    /**
     * return an iterable of all Vfs.File in given urls, matching filePredicate
     *
     * @param urls URLs
     * @param filePredicate a matching predicate
     * @return and Iterable for the Files
     */
    public static Iterable<File> findFiles(final Collection<URL> urls, final Predicate<File> filePredicate) {
        return () -> urls.stream()
                .flatMap(url -> {
                    try {
                        return StreamSupport.stream(fromURL(url).getFiles().spliterator(), false);
                    } catch (Throwable e) {
                        logger.error("could not findFiles for url. continuing. [{}]", url, e);
                        return Stream.of();
                    }
                })
                .filter(filePredicate).iterator();
    }

    /**
     * try to get {@link java.io.File} from ur
     *
     * @param url a URL
     * @return a File handle for the URL
     */
    public static java.io.File getFile(URL url) {
        java.io.File file;
        String path;

        try {
            path = url.toURI().getSchemeSpecificPart();
            if ((file = new java.io.File(path)).exists()) {
                return file;
            }
        } catch (URISyntaxException ex) {
            logger.warn("uri syntax exception", ex);
        }

        try {
            path = URLDecoder.decode(url.getPath(), "UTF-8");
            if (path.contains(".jar!")) {
                path = path.substring(0, path.lastIndexOf(".jar!") + ".jar".length());
            }
            if ((file = new java.io.File(path)).exists()) {
                return file;
            }

        } catch (UnsupportedEncodingException ex) {
             logger.warn("unsupported encoding exception", ex);
        }

        try {
            path = url.toExternalForm();
            if (path.startsWith("jar:")) {
                path = path.substring("jar:".length());
            }
            if (path.startsWith("wsjar:")) {
                path = path.substring("wsjar:".length());
            }
            if (path.startsWith("file:")) {
                path = path.substring("file:".length());
            }
            if (path.contains(".jar!")) {
                path = path.substring(0, path.indexOf(".jar!") + ".jar".length());
            }
            if (path.contains(".war!")) {
                path = path.substring(0, path.indexOf(".war!") + ".war".length());
            }
            if ((file = new java.io.File(path)).exists()) {
                return file;
            }

            path = path.replace("%20", " ");
            if ((file = new java.io.File(path)).exists()) {
                return file;
            }

        } catch (Exception ex) {
            logger.warn("path error", ex);
        }

        return null;
    }

    private static boolean hasJarFileInPath(URL url) {
        return url.toExternalForm().matches(".*\\.jar(\\!.*|$)");
    }

    /**
     * default url types used by Vfs#fromURL(java.net.URL)
     */
    public enum DefaultUrlTypes implements UrlType {
        jarFile {
            @Override
            public boolean matches(URL url) {
                return url.getProtocol().equals("file") && hasJarFileInPath(url);
            }

            @Override
            public Dir createDir(final URL url) throws Exception {
                return new ZipDir(new JarFile(getFile(url)));
            }
        },
        jarUrl {
            @Override
            public boolean matches(URL url) {
                return "jar".equals(url.getProtocol()) || "zip".equals(url.getProtocol()) || "wsjar".equals(url.getProtocol());
            }

            @Override
            public Dir createDir(URL url) throws Exception {
                try {
                    URLConnection urlConnection = url.openConnection();
                    if (urlConnection instanceof JarURLConnection) {
                        urlConnection.setUseCaches(false);
                        return new ZipDir(((JarURLConnection) urlConnection).getJarFile());
                    }
                } catch (IOException e) {
                    /*fallback*/ }
                java.io.File file = getFile(url);
                if (file != null) {
                    return new ZipDir(new JarFile(file));
                }
                return null;
            }
        },
        directory {
            @Override
            public boolean matches(URL url) {
                if (url.getProtocol().equals("file") && !hasJarFileInPath(url)) {
                    java.io.File file = getFile(url);
                    return file != null && file.isDirectory();
                } else {
                    return false;
                }
            }

            @Override
            public Dir createDir(final URL url) throws Exception {
                return new SystemDir(getFile(url));
            }
        },
        jboss_vfs {
            @Override
            public boolean matches(URL url) {
                return url.getProtocol().equals("vfs");
            }

            @Override
            public Vfs.Dir createDir(URL url) throws Exception {
                return JbossDir.createDir(url);
            }
        },
        jboss_vfsfile {
            @Override
            public boolean matches(URL url) throws Exception {
                return "vfszip".equals(url.getProtocol()) || "vfsfile".equals(url.getProtocol());
            }

            @Override
            public Dir createDir(URL url) throws Exception {
                return new UrlTypeVFS().createDir(url);
            }
        },
        bundle {
            @Override
            public boolean matches(URL url) throws Exception {
                return url.getProtocol().startsWith("bundle");
            }

            @Override
            public Dir createDir(URL url) throws Exception {
                return fromURL((URL) ClasspathHelper.contextClassLoader().
                        loadClass("org.eclipse.core.runtime.FileLocator").getMethod("resolve", URL.class).invoke(null, url));
            }
        },
        jarInputStream {
            @Override
            public boolean matches(URL url) throws Exception {
                return url.toExternalForm().contains(".jar");
            }

            @Override
            public Dir createDir(final URL url) throws Exception {
                return new JarInputDir(url);
            }
        }
    }
}
