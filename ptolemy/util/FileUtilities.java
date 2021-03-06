/* Utilities used to manipulate files

 Copyright (c) 2004-2017 The Regents of the University of California.
 All rights reserved.
 Permission is hereby granted, without written agreement and without
 license or royalty fees, to use, copy, modify, and distribute this
 software and its documentation for any purpose, provided that the above
 copyright notice and the following two paragraphs appear in all copies
 of this software.

 IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
 THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
 SUCH DAMAGE.

 THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 ENHANCEMENTS, OR MODIFICATIONS.

 PT_COPYRIGHT_VERSION_2
 COPYRIGHTENDKEY

 */
package ptolemy.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

// Avoid importing any packages from ptolemy.* here so that we
// can ship Ptplot.
///////////////////////////////////////////////////////////////////
//// FileUtilities

/**
 A collection of utilities for manipulating files
 These utilities do not depend on any other ptolemy.* packages.

 @author Christopher Brooks
 @version $Id$
 @since Ptolemy II 4.0
 @Pt.ProposedRating Green (cxh)
 @Pt.AcceptedRating Green (cxh)
 */
public class FileUtilities {
    /** Instances of this class cannot be created.
     */
    private FileUtilities() {
    }

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** Copy sourceURL to destinationFile without doing any byte conversion.
     *  @param sourceURL The source URL
     *  @param destinationFile The destination File.
     *  @return true if the file was copied, false if the file was not
     *  copied because the sourceURL and the destinationFile refer to the
     *  same file.
     *  @exception IOException If the source file does not exist.
     */
    public static boolean binaryCopyURLToFile(URL sourceURL,
            File destinationFile) throws IOException {
        URL destinationURL = destinationFile.getCanonicalFile().toURI().toURL();

        if (sourceURL.sameFile(destinationURL)) {
            return false;
        }

        // If sourceURL is of the form file:./foo, then we need to try again.
        File sourceFile = new File(sourceURL.getFile());

        // If the sourceURL is not a jar URL, then check to see if we
        // have the same file.
        // FIXME: should we check for !/ and !\ everywhere?
        if (sourceFile.getPath().indexOf("!/") == -1
                && sourceFile.getPath().indexOf("!\\") == -1) {
            try {
                if (sourceFile.getCanonicalFile().toURI().toURL()
                        .sameFile(destinationURL)) {
                    return false;
                }
            } catch (IOException ex) {
                // JNLP Jar urls sometimes throw an exception here.
                // IOException constructor does not take a cause
                IOException ioException = new IOException(
                        "Cannot find canonical file name of '" + sourceFile
                                + "'");
                ioException.initCause(ex);
                throw ioException;
            }
        }

        URLConnection sourceURLConnection = null;
        InputStream sourceURLInputStream = null;
        try {
            sourceURLConnection = sourceURL.openConnection();
            if (sourceURLConnection == null) {
                throw new IOException(
                        "Failed to open a connection on " + sourceURL);
            }
            sourceURLInputStream = sourceURLConnection.getInputStream();
            if (sourceURLInputStream == null) {
                throw new IOException(
                        "Failed to open a stream on " + sourceURLConnection);
            }

            if (!(sourceURLConnection instanceof JarURLConnection)) {
                _binaryCopyStream(sourceURLInputStream, destinationFile);
            } else {
                JarURLConnection jarURLConnection = (JarURLConnection) sourceURLConnection;
                JarEntry jarEntry = jarURLConnection.getJarEntry();
                if (jarEntry != null && !jarEntry.isDirectory()) {
                    // Simply copying a file.
                    _binaryCopyStream(sourceURLInputStream, destinationFile);
                } else {
                    // It is a directory if jarEntry == null, a Jar file.
                    _binaryCopyDirectory(jarURLConnection, destinationFile);
                }
            }
        } finally {
            if (sourceURLConnection != null) {
                // Work around
                // "JarUrlConnection.getInputStream().close() throws
                // NPE when entry is a directory"
                // https://bugs.openjdk.java.net/browse/JDK-8080094
                if (sourceURLConnection instanceof JarURLConnection) {
                    JarURLConnection jar = (JarURLConnection) sourceURLConnection;
                    if (jar.getUseCaches()) {
                        jar.getJarFile().close();
                    }
                } else {
                    if (sourceURLInputStream != null) {
                        sourceURLInputStream.close();
                    }
                }
            }
        }

        return true;
    }

    /** Read a sourceURL without doing any byte conversion.
     *  @param sourceURL The source URL
     *  @return The array of bytes read from the URL.
     *  @exception IOException If the source URL does not exist.
     */
    public static byte[] binaryReadURLToByteArray(URL sourceURL)
            throws IOException {
        return _binaryReadStream(sourceURL.openStream());
    }

    /** Create a link.
     *  @param newLink the link to be created
     *  @param temporary the path to the temporary location where the directory to be replaced by the link should be placed.
     *  @param target the target of the link to be created.
     *  @exception IOException If there are problems creating the link
     *  @return A status message
     */
    public static String createLink(Path newLink, Path temporary, Path target)
            throws IOException {
        //
        //     Path currentRelativePath = Paths.get(".");
        //     throw new IOException(newLink + " does not exist?  That directory should be "
        //                           + " in the jar file so that we can move it aside.  "
        //                           + "The current relative path is "
        //                           + currentRelativePath.toAbsolutePath());
        // }

        String results = "";
        if (Files.isSymbolicLink(newLink)) {
            if (Files.isSameFile(target, Files.readSymbolicLink(newLink))) {
                return "FileUtilities.java: createLink(): " + target + " and "
                        + Files.readSymbolicLink(newLink) + " are the same.";
            }
            results = "FileUtilities.java: createLink(): " + target + " and "
                    + Files.readSymbolicLink(newLink) + " were not the same.";
        } else {
            results = "FileUtilities.java: createLink(): " + newLink
                    + " is not a link.";
        }

        boolean moveBack = false;
        if (Files.isReadable(newLink)) {
            try {
                // Save the directory that will be replaced by the link.
                // System.out.println("Moving " + newLink + " to " + temporary);
                Files.move(newLink, temporary);
            } catch (Throwable throwable) {
                IOException exception = new IOException(
                        "Could not move " + newLink + " to " + temporary);
                exception.initCause(throwable);
                throw exception;
            }
            moveBack = true;
        }

        try {
            Files.createSymbolicLink(newLink, target);
        } catch (IOException ex) {
            String message = "Failed to create symbolic link from " + newLink
                    + " to " + target + ": " + ex;
            if (moveBack) {
                try {
                    // System.out.println("Moving " + temporary + " to " + newLink);
                    Files.move(temporary, newLink);

                } catch (Throwable throwable) {
                    message += " In addition, could not move " + temporary
                            + " back to " + newLink + ": " + throwable;
                }
            }
            IOException exception = new IOException(message);
            exception.initCause(ex);
            throw exception;
        } catch (UnsupportedOperationException ex2) {
            try {
                // System.out.println("Creating link from " + newLink + " to " + temporary);
                Files.createLink(newLink, target);
            } catch (Throwable ex3) {
                String message = "Failed to create a hard link from " + newLink
                        + " to " + target + ": " + ex3;

                if (moveBack) {
                    try {
                        // System.out.println("Moving " + temporary + " to " + newLink);
                        Files.move(temporary, newLink);
                    } catch (Throwable throwable) {
                        message += " In addition, could not move " + temporary
                                + " back to " + newLink + ": " + throwable;
                    }
                }

                IOException exception = new IOException(message);
                exception.initCause(ex3);
                throw exception;
            }
        }

        if (moveBack) {
            try {
                // System.out.println("Deleting " + temporary);
                FileUtilities.deleteDirectory(temporary.toFile());
            } catch (Throwable throwable) {
                IOException exception = new IOException(
                        "Failed to delete " + temporary);
                exception.initCause(throwable);
                throw exception;
            }
        }
        return results + "  Link successfully created.";
    }

    /** Delete a directory.
     *  @param directory the File naming the directory.
     *  @return true if the toplevel directory was deleted or does not
     *  exist.
     */
    static public boolean deleteDirectory(File directory) {
        boolean deletedAllFiles = true;
        if (!directory.exists()) {
            return true;
        } else {
            if (Files.isSymbolicLink(directory.toPath())) {
                if (!directory.delete()) {
                    deletedAllFiles = false;
                }
            } else {
                File[] files = directory.listFiles();
                if (files != null) {
                    for (int i = 0; i < files.length; i++) {
                        if (files[i].isDirectory()
                                && !Files.isSymbolicLink(files[i].toPath())) {
                            deleteDirectory(files[i]);
                        } else {
                            if (!files[i].delete()) {
                                deletedAllFiles = false;
                            }
                        }
                    }
                }
            }
        }
        return directory.delete() && deletedAllFiles;
    }

    /**
     * Delete a directory and all of its content.
     * @param filepath The path for the directory or file to be deleted.
     * @return false if one or more files or directories cannot be deleted.
     */
    public static boolean deleteDirectory(String filepath) {
        return FileUtilities.deleteDirectory(new File(filepath));
    }

    /** Extract a jar file into a directory.  This is a trivial
     *  implementation of the <code>jar -xf</code> command.
     *  @param jarFileName The name of the jar file to extract
     *  @param directoryName The name of the directory.  If this argument
     *  is null, then the files are extracted in the current directory.
     *  @exception IOException If the jar file cannot be opened, or
     *  if there are problems extracting the contents of the jar file
     */
    public static void extractJarFile(String jarFileName, String directoryName)
            throws IOException {
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(jarFileName);
            Enumeration entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) entries.nextElement();
                File destinationFile = new File(directoryName,
                        jarEntry.getName());
                if (jarEntry.isDirectory()) {
                    if (!destinationFile.isDirectory()
                            && !destinationFile.mkdirs()) {
                        throw new IOException("Warning, failed to create "
                                + "directory for \"" + destinationFile + "\".");
                    }
                } else {
                    InputStream jarInputStream = null;
                    try {
                        jarInputStream = jarFile.getInputStream(jarEntry);
                        _binaryCopyStream(jarInputStream, destinationFile);
                    } finally {
                        if (jarInputStream != null) {
                            jarInputStream.close();
                        }
                    }
                }
            }
        } finally {
            if (jarFile != null) {
                jarFile.close();
            }
        }
    }

    /** If necessary, unjar the entire jar file that contains a target
     *  file.
     *
     *  @param targetFileName If the file exists relative to the
     *  directoryName, then do nothing.  Otherwise, look for the
     *  targetFile in the classpath and unjar the jar file in which it
     *  is found in the directory named by the <i>directoryName</i>
     *  parameter.
     *  @param directoryName The name of the directory in which to
     *  search for the file named by the <i>targetFileName</i>
     *  parameter and in which the jar file is possibly unjared.
     *  @exception IOException If there is problem finding the target
     *  file or extracting the jar file.
     */
    public static void extractJarFileIfNecessary(String targetFileName,
            String directoryName) throws IOException {
        File targetFile = new File(
                directoryName + File.separator + targetFileName);
        if (targetFile.exists()) {
            return;
        } else {
            URL targetFileURL = FileUtilities
                    .nameToURL("$CLASSPATH/" + targetFileName, null, null);
            if (targetFileURL == null) {
                throw new FileNotFoundException("Could not find "
                        + targetFileName + " as a file or in the CLASSPATH.");
            }

            String targetFileURLName = targetFileURL.toString();
            // Remove the jar:file: and everything after !/
            String jarFileName = targetFileURLName.substring(9,
                    targetFileURLName.indexOf("!/"));
            FileUtilities.extractJarFile(jarFileName, directoryName);
            targetFile = new File(
                    directoryName + File.separator + targetFileName);
            if (!targetFile.exists()) {
                throw new FileNotFoundException("Could not find "
                        + targetFileName + " after extracting " + jarFileName);
            }
        }
    }

    /** Given a URL, if it starts with http, the follow up to 10 redirects.
     *
     *  <p>If the URL is null or does not start with "http", then return the
     *  URL.</p>
     *
     *  @param url The URL to be followed.
     *  @return The new URL if any.
     *  @exception IOException If there is a problem opening the URL or
     *  if there are more than 10 redirects.
     */
    public static URL followRedirects(URL url) throws IOException {

        if (url == null || !url.getProtocol().startsWith("http")) {
            return url;
        }
        URL temporaryURL = url;
        int count;
        for (count = 0; count < 10; count++) {
            HttpURLConnection connection = (HttpURLConnection) temporaryURL
                    .openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setInstanceFollowRedirects(false);

            switch (connection.getResponseCode()) {
            case HttpURLConnection.HTTP_MOVED_PERM:
            case HttpURLConnection.HTTP_MOVED_TEMP:
                String location = connection.getHeaderField("Location");
                // Handle relative URLs.
                temporaryURL = new URL(temporaryURL, location);
                continue;
            }

            connection.disconnect();
            return temporaryURL;
        }
        throw new IOException("Failed to resolve " + url
                + " after 10 attempts.  The last url was " + temporaryURL);

    }

    /** Return the string contents of the file at the specified location.
     *  @param path The location.
     *  @return The contents as a string, assuming the default encoding of
     *   this JVM (probably utf-8).
     *  @exception IOException If the file cannot be read.
     */
    public static String getFileAsString(String path) throws IOException {
        // Use nameToURL so that we look in the classpath for jar files
        // that might contain the resource.
        URL url = FileUtilities.nameToURL(path, null, null);
        byte[] encoded = FileUtilities.binaryReadURLToByteArray(url);
        return new String(encoded, Charset.defaultCharset());
    }

    /** Return true if the command can be found in the directories
     *  listed in the directories contained in the PATH environment
     *  variable.
     *  @param command The command for which to search.
     *  @return True if the command can be found in $PATH
     */
    public static boolean inPath(String command) {
        String path = System.getenv("PATH");
        List<String> directories = Arrays
                .asList(path.split(File.pathSeparator));
        for (String directory : directories) {
            File file = new File(directory, command);
            if (file.exists() && file.canExecute()) {
                return true;
            }
        }
        return false;
    }

    /** Extract the contents of a jar file.
     *  @param args An array of arguments.  The first argument
     *  names the jar file to be extracted.  The first argument
     *  is required.  The second argument names the directory in
     *  which to extract the files from the jar file.  The second
     *  argument is optional.
     */
    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Usage: java -classpath $PTII "
                    + "ptolemy.util.FileUtilities jarFile [directory]\n"
                    + "where jarFile is the name of the jar file\n"
                    + "and directory is the optional directory in which to "
                    + "extract.");
            StringUtilities.exit(2);
        }
        String jarFileName = args[0];
        String directoryName = null;
        if (args.length >= 2) {
            directoryName = args[1];
        }
        try {
            extractJarFile(jarFileName, directoryName);
        } catch (Throwable throwable) {
            System.err.println("Failed to extract \"" + jarFileName + "\"");
            throwable.printStackTrace();
            StringUtilities.exit(3);
        }
    }

    /** Given a file name or URL, construct a java.io.File object that
     *  refers to the file name or URL.  This method
     *  first attempts to directly use the file name to construct the
     *  File. If the resulting File is a relative pathname, then
     *  it is resolved relative to the specified base URI, if
     *  there is one.  If there is no such base URI, then it simply
     *  returns the relative File object.  See the java.io.File
     *  documentation for a details about relative and absolute pathnames.
     *
     *  <p>If the name begins with
     *  "xxxxxxCLASSPATHxxxxxx" or "$CLASSPATH" then search for the
     *  file relative to the classpath.
     *
     *  <p>Note that "xxxxxxCLASSPATHxxxxxx" is the value of the
     *  globally defined constant $CLASSPATH available in the Ptolemy
     *  II expression language.
     *
     *  <p>If the name begins with $CLASSPATH or "xxxxxxCLASSPATHxxxxxx"
     *  but that name cannot be found in the classpath, the value
     *  of the ptolemy.ptII.dir property is substituted in.
     *  <p>
     *  The file need not exist for this method to succeed.  Thus,
     *  this method can be used to determine whether a file with a given
     *  name exists, prior to calling openForWriting(), for example.
     *
     *  <p>This method is similar to
     *  {@link #nameToURL(String, URI, ClassLoader)}
     *  except that in this method, the file or URL must be readable.
     *  Usually, this method is use for write a file and
     *  {@link #nameToURL(String, URI, ClassLoader)} is used for reading.
     *
     *  @param name The file name or URL.
     *  @param base The base for relative URLs.
     *  @return A File, or null if the filename argument is null or
     *   an empty string.
     *  @see #nameToURL(String, URI, ClassLoader)
     */
    public static File nameToFile(String name, URI base) {
        if (name == null || name.trim().equals("")) {
            return null;
        }

        if (name.startsWith(_CLASSPATH_VALUE)
                || name.startsWith("$CLASSPATH")) {
            URL result = null;
            try {
                result = _searchClassPath(name, null);
            } catch (IOException ex) {
                // Ignore.  In nameToFile(), it is ok if we don't find the variable
            }
            if (result != null) {
                return new File(result.getPath());
            } else {
                String ptII = StringUtilities.getProperty("ptolemy.ptII.dir");
                if (ptII != null && ptII.length() > 0) {
                    return new File(ptII, _trimClassPath(name));
                }
            }
        }

        File file = new File(name);

        if (!file.isAbsolute()) {
            // Try to resolve the base directory.
            if (base != null) {
                // Need to replace \ with /, otherwise resolve would fail even
                // if invoked in a windows OS. -- tfeng (02/27/2009)
                URI newURI = base.resolve(StringUtilities
                        .substitute(name, " ", "%20").replace('\\', '/'));

                //file = new File(newURI);
                String urlString = newURI.getPath();
                file = new File(
                        StringUtilities.substitute(urlString, "%20", " "));
            }
        }
        return file;
    }

    /** Given a file or URL name, return as a URL.  If the file name
     *  is relative, then it is interpreted as being relative to the
     *  specified base directory. If the name begins with
     *  "xxxxxxCLASSPATHxxxxxx" or "$CLASSPATH" then search for the
     *  file relative to the classpath.
     *
     *  <p>Note that "xxxxxxCLASSPATHxxxxxx" is the value of the
     *  globally defined constant $CLASSPATH available in the Ptolemy
     *  II expression language.
     *  II expression language.
     *
     *  <p>If no file is found, then throw an exception.
     *
     *  <p>This method is similar to {@link #nameToFile(String, URI)}
     *  except that in this method, the file or URL must be readable.
     *  Usually, this method is use for reading a file and
     *  is used for writing {@link #nameToFile(String, URI)}.
     *
     *  @param name The name of a file or URL.
     *  @param baseDirectory The base directory for relative file names,
     *   or null to specify none.
     *  @param classLoader The class loader to use to locate system
     *   resources, or null to use the system class loader that was used
     *   to load this class.
     *  @return A URL, or null if the name is null or the empty string.
     *  @exception IOException If the file cannot be read, or
     *   if the file cannot be represented as a URL (e.g. System.in), or
     *   the name specification cannot be parsed.
     *  @see #nameToFile(String, URI)
     */
    public static URL nameToURL(String name, URI baseDirectory,
            ClassLoader classLoader) throws IOException {
        if (name == null || name.trim().equals("")) {
            return null;
        }

        if (name.startsWith(_CLASSPATH_VALUE)
                || name.startsWith("$CLASSPATH")) {
            if (name.contains("#")) {
                name = name.substring(0, name.indexOf("#"));
            }

            URL result = _searchClassPath(name, classLoader);
            if (result == null) {
                throw new IOException("Cannot find file '"
                        + _trimClassPath(name) + "' in classpath");
            }

            return result;
        }

        File file = new File(name);

        // Be careful here, we need to be sure that we are reading
        // relative to baseDirectory if baseDirectory is not null.

        // The security tests rely on baseDirectory, to replicate:
        // (cd $PTII/ptII/ptolemy/actor/lib/security/test; rm rm foo.keystore auto/foo.keystore; make)

        if (file.isAbsolute() || (file.canRead() && baseDirectory == null)) {
            // If the URL has a "fragment" (also called a reference), which is
            // a pointer into the file, we have to strip that off before we
            // get the file, and the reinsert it before returning the URL.
            String fragment = null;
            if (!file.canRead()) {

                // FIXME: Need to strip off the fragment part
                // (the "reference") of the name (after the #),
                // if there is one, and add it in again by calling set()
                // on the URL at the end.
                String[] splitName = name.split("#");
                if (splitName.length > 1) {
                    name = splitName[0];
                    fragment = splitName[1];
                }

                // FIXME: This is a hack.
                // Expanding the configuration with Ptolemy II installed
                // in a directory with spaces in the name fails on
                // JAIImageReader because PtolemyII.jpg is passed in
                // to this method as C:\Program%20Files\Ptolemy\...
                file = new File(StringUtilities.substitute(name, "%20", " "));

                URL possibleJarURL = null;

                if (!file.canRead()) {
                    // ModelReference and FilePortParameters sometimes
                    // have paths that have !/ in them.
                    possibleJarURL = ClassUtilities.jarURLEntryResource(name);

                    if (possibleJarURL != null) {
                        file = new File(possibleJarURL.getFile());
                    }
                }

                if (!file.canRead()) {
                    throw new IOException("Cannot read file '" + name + "' or '"
                            + StringUtilities.substitute(name, "%20", " ") + "'"
                            + (possibleJarURL == null ? ""
                                    : " or '" + possibleJarURL.getFile() + ""));
                }
            }

            URL result = file.toURI().toURL();
            if (fragment != null) {
                result = new URL(result.toString() + "#" + fragment);
            }
            return result;
        } else {
            // Try relative to the base directory.
            if (baseDirectory != null) {
                // Try to resolve the URI.
                URI newURI;

                try {
                    newURI = baseDirectory.resolve(name);
                } catch (Exception ex) {
                    // FIXME: Another hack
                    // This time, if we try to open some of the JAI
                    // demos that have actors that have defaults FileParameters
                    // like "$PTII/doc/img/PtolemyII.jpg", then resolve()
                    // bombs.
                    String name2 = StringUtilities.substitute(name, "%20", " ");
                    try {
                        newURI = baseDirectory.resolve(name2);
                        name = name2;
                    } catch (Exception ex2) {
                        IOException io = new IOException(
                                "Problem with URI format in '" + name + "'. "
                                        + "and '" + name2 + "' "
                                        + "This can happen if the file name "
                                        + "is not absolute "
                                        + "and is not present relative to the "
                                        + "directory in which the specified model "
                                        + "was read (which was '"
                                        + baseDirectory + "')");
                        io.initCause(ex2);
                        throw io;
                    }
                }

                String urlString = newURI.toString();

                try {
                    // Adding another '/' for remote execution.
                    if (newURI.getScheme() != null
                            && newURI.getAuthority() == null) {
                        // Change from Efrat:
                        // "I made these change to allow remote
                        // execution of a workflow from within a web
                        // service."

                        // "The first modification was due to a URI
                        // authentication exception when trying to
                        // create a file object from a URI on the
                        // remote side. The second modification was
                        // due to the file protocol requirements to
                        // use 3 slashes, 'file:///' on the remote
                        // side, although it would be probably be a
                        // good idea to also make sure first that the
                        // url string actually represents the file
                        // protocol."
                        urlString = urlString.substring(0, 6) + "//"
                                + urlString.substring(6);

                        //} else {
                        // urlString = urlString.substring(0, 6) + "/"
                        // + urlString.substring(6);
                    }
                    // Unfortunately, between Java 1.5 and 1.6,
                    // The URL constructor changed.
                    // In 1.5, new URL("file:////foo").toString()
                    // returns "file://foo"
                    // In 1.6, new URL("file:////foo").toString()
                    // return "file:////foo".
                    // See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6561321
                    return new URL(urlString);
                } catch (Exception ex3) {
                    try {
                        // Under Webstart, opening
                        // hoc/demo/ModelReference/ModelReference.xml
                        // requires this because the URL is relative.
                        return new URL(baseDirectory.toURL(), urlString);
                    } catch (Exception ex4) {

                        try {
                            // Under Webstart, ptalon, EightChannelFFT
                            // requires this.
                            return new URL(baseDirectory.toURL(),
                                    newURI.toString());
                        } catch (Exception ex5) {
                            // Ignore
                        }

                        IOException io = new IOException(
                                "Problem with URI format in '" + urlString
                                        + "'. " + "This can happen if the '"
                                        + urlString + "' is not absolute"
                                        + " and is not present relative to the directory"
                                        + " in which the specified model was read"
                                        + " (which was '" + baseDirectory
                                        + "')");
                        io.initCause(ex3);
                        throw io;
                    }
                }
            }

            // As a last resort, try an absolute URL.

            URL url = new URL(name);

            // If we call new URL("http", null, /foo);
            // then we get "http:/foo", which should be "http://foo"
            // This change suggested by Dan Higgins and Kevin Kruland
            // See kepler/src/util/URLToLocalFile.java
            try {
                String fixedURLAsString = url.toString()
                        .replaceFirst("(https?:)//?", "$1//");
                url = new URL(fixedURLAsString);
            } catch (Exception e) {
                // Ignore
                url = new URL(name);
            }
            return url;
        }
    }

    /** Open the specified file for reading. If the specified name is
     *  "System.in", then a reader from standard in is returned. If
     *  the name begins with "$CLASSPATH" or "xxxxxxCLASSPATHxxxxxx",
     *  then the name is passed to {@link #nameToURL(String, URI, ClassLoader)}
     *  If the file name is not absolute, the it is assumed to be relative to
     *  the specified base URI.
     *  @see #nameToURL(String, URI, ClassLoader)
     *  @param name File name.
     *  @param base The base URI for relative references.
     *  @param classLoader The class loader to use to locate system
     *   resources, or null to use the system class loader that was used
     *   to load this class.
     *  @return If the name is null or the empty string,
     *  then null is returned, otherwise a buffered reader is returned.

     *  @exception IOException If the file cannot be opened.
     */
    public static BufferedReader openForReading(String name, URI base,
            ClassLoader classLoader) throws IOException {
        if (name == null || name.trim().equals("")) {
            return null;
        }

        if (name.trim().equals("System.in")) {
            if (STD_IN == null) {
                STD_IN = new BufferedReader(new InputStreamReader(System.in));
            }

            return STD_IN;
        }

        // Not standard input. Try URL mechanism.
        URL url = nameToURL(name, base, classLoader);

        if (url == null) {
            throw new IOException("Could not convert \"" + name
                    + "\" with base \"" + base + "\" to a URL.");
        }

        InputStreamReader inputStreamReader = null;
        try {
            inputStreamReader = new InputStreamReader(url.openStream());
        } catch (IOException ex) {
            // Try it as a jar url.
            // WebStart ptalon MapReduce needs this.
            try {
                URL possibleJarURL = ClassUtilities
                        .jarURLEntryResource(url.toString());
                if (possibleJarURL != null) {
                    inputStreamReader = new InputStreamReader(
                            possibleJarURL.openStream());
                }
                // If possibleJarURL is null, this throws an exception,
                // which we ignore and report the first exception (ex)
                if (inputStreamReader == null) {
                    throw new NullPointerException("Could not open " + url);
                } else {
                    return new BufferedReader(inputStreamReader);
                }
            } catch (Throwable throwable2) {
                try {
                    if (inputStreamReader != null) {
                        inputStreamReader.close();
                    }
                } catch (IOException ex3) {
                    // Ignore
                }
                IOException ioException = new IOException(
                        "Failed to open \"" + url + "\".");
                ioException.initCause(ex);
                throw ioException;
            }
        }

        return new BufferedReader(inputStreamReader);
    }

    /** Open the specified file for writing or appending. If the
     *  specified name is "System.out", then a writer to standard out
     *  is returned; otherwise, pass the name and base to {@link
     *  #nameToFile(String, URI)} and create a file writer.  If the
     *  file does not exist, then create it.  If the file name is not
     *  absolute, the it is assumed to be relative to the specified
     *  base directory.  If permitted, this method will return a
     *  Writer that will simply overwrite the contents of the file. It
     *  is up to the user of this method to check whether this is OK
     *  (by first calling {@link #nameToFile(String, URI)} and calling
     *  exists() on the returned value).
     *
     *  @param name File name.
     *  @param base The base URI for relative references.
     *  @param append If true, then append to the file rather than
     *   overwriting.
     *  @return If the name is null or the empty string,
     *  then null is returned, otherwise a writer is returned.
     *  @exception IOException If the file cannot be opened
     *   or created.
     */
    public static Writer openForWriting(String name, URI base, boolean append)
            throws IOException {
        if (name == null || name.trim().equals("")) {
            return null;
        }

        if (name.trim().equals("System.out")) {
            if (STD_OUT == null) {
                STD_OUT = new PrintWriter(System.out);
            }

            return STD_OUT;
        }

        File file = nameToFile(name, base);
        return new FileWriter(file, append);
    }

    /** Given a URL, open a stream.
     *
     *  <p>If the URL starts with "http", then follow up to 10 redirects
     *  and return the the final HttpURLConnection.</p>
     *
     *  <p>If the URL does not start with "http", then call
     *  URL.openStream().</p>
     *
     *  @param url The URL to be opened.
     *  @return The input stream
     *  @exception IOException If there is a problem opening the URL or
     *  if there are more than 10 redirects.
     */
    public static InputStream openStreamFollowingRedirects(URL url)
            throws IOException {
        return openStreamFollowingRedirectsReturningBoth(url).stream();
    }

    /** A class that contains an InputStream and a URL
     * so that we don't have to follow redirects twice.
     */
    public static class StreamAndURL {
        /** Create an object containing an InputStream
         *  and a URL.
         *  @param stream The stream.
         *  @param url The url.
         */
        public StreamAndURL(InputStream stream, URL url) {
            _stream = stream;
            _url = url;
        }

        /** Return the stream.
         *  @return The stream.
         */
        public InputStream stream() {
            return _stream;
        }

        /** Return the url.
         *  @return The url.
         */
        public URL url() {
            return _url;
        }

        private InputStream _stream;
        private URL _url;
    }

    /** Given a URL, open a stream and return an object containing
     *  both the inputStream and the possibly redirected URL.
     *
     *  <p>If the URL starts with "http", then follow up to 10 redirects
     *  and return the the final HttpURLConnection.</p>
     *
     *  <p>If the URL does not start with "http", then call
     *  URL.openStream().</p>
     *
     *  @param url The URL to be opened.
     *  @return The input stream
     *  @exception IOException If there is a problem opening the URL or
     *  if there are more than 10 redirects.
     */
    public static StreamAndURL openStreamFollowingRedirectsReturningBoth(
            URL url) throws IOException {

        if (!url.getProtocol().startsWith("http")) {
            return new StreamAndURL(url.openStream(), url);
        }

        // followRedirects() also calls openConnection() and then closes
        // the connection with disconnect().  We could duplicate the code
        // here, but it seems safer to avoid the duplication.
        URL redirectedURL = FileUtilities.followRedirects(url);
        return new StreamAndURL(redirectedURL.openStream(), redirectedURL);
    }

    /** Utility method to read a string from an input stream.
     *  @param stream The stream.
     *  @return The string.
     * @exception IOException If the stream cannot be read.
     */
    public static String readFromInputStream(InputStream stream)
            throws IOException {
        StringBuffer response = new StringBuffer();
        BufferedReader reader = null;
        try {
            String line = "";
            // Avoid Coverity Scan: "Dubious method used (FB.DM_DEFAULT_ENCODING)"
            reader = new BufferedReader(new InputStreamReader(stream,
                    java.nio.charset.Charset.defaultCharset()));

            String lineBreak = System.getProperty("line.separator");
            while ((line = reader.readLine()) != null) {
                response.append(line);
                if (!line.endsWith(lineBreak)) {
                    response.append(lineBreak);
                }
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return response.toString();
    }

    ///////////////////////////////////////////////////////////////////
    ////                         public members                   ////

    /** Standard in as a reader, which will be non-null
     *  only after a call to openForReading("System.in").
     */
    public static BufferedReader STD_IN = null;

    /** Standard out as a writer, which will be non-null
     *  only after a call to openForWriting("System.out").
     */
    public static PrintWriter STD_OUT = null;

    ///////////////////////////////////////////////////////////////////
    ////                         private methods                   ////

    /** Copy a directory in a jar file to a physical directory.
     *  @param jarURLConnection the connection to the jar file
     *  @param destinationDirectory The destination directory, which must already exist.
     *  @exception IOException If there are problems reading, writing or closing.
     */
    private static void _binaryCopyDirectory(JarURLConnection jarURLConnection,
            File destinationDirectory) throws IOException {
        // Get the path of the resource in the jar file
        String entryBaseName = jarURLConnection.getEntryName();
        JarFile jarFile = jarURLConnection.getJarFile();
        Enumeration<? extends ZipEntry> jarEntries = jarFile.entries();
        while (jarEntries.hasMoreElements()) {
            ZipEntry zipEntry = jarEntries.nextElement();
            String name = zipEntry.getName();
            if (!name.startsWith(entryBaseName)) {
                continue;
            }

            String entryFileName = name.substring(entryBaseName.length());
            File fileOrDirectory = new File(destinationDirectory,
                    entryFileName);
            if (zipEntry.isDirectory()) {
                if (!fileOrDirectory.mkdir()) {
                    throw new IOException(
                            "Could not create \"" + fileOrDirectory + "\"");
                }
            } else {
                InputStream inputStream = null;
                OutputStream outputStream = null;
                try {
                    inputStream = jarFile.getInputStream(zipEntry);
                    outputStream = new BufferedOutputStream(
                            new FileOutputStream(fileOrDirectory));
                    byte buffer[] = new byte[4096];
                    int readCount;
                    while ((readCount = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, readCount);
                    }
                } finally {
                    try {
                        if (outputStream != null) {
                            outputStream.close();
                        }
                    } finally {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                    }
                }
            }
        }
    }

    /** Copy files safely.  If there are problems, the streams are
     *  close appropriately.
     *  @param inputStream The input stream.
     *  @param destinationFile The destination File.
     *  @exception IOException If the input stream cannot be created
     *  or read, or * if there is a problem writing to the destination
     *  file.
     */
    private static void _binaryCopyStream(InputStream inputStream,
            File destinationFile) throws IOException {
        // Copy the source file.
        BufferedInputStream input = null;

        try {
            input = new BufferedInputStream(inputStream);

            if (input == null) {
                throw new IOException(
                        "Could not create a BufferedInputStream from \""
                                + inputStream
                                + "\".  This can happen if the input "
                                + "is a JarURL entry that refers to a directory "
                                + "in the jar file.");
            }

            BufferedOutputStream output = null;

            try {
                File parent = destinationFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    if (!parent.mkdirs()) {
                        throw new IOException("Failed to create directories "
                                + "for \"" + parent + "\".");
                    }
                }

                output = new BufferedOutputStream(
                        new FileOutputStream(destinationFile));

                int c;

                try {
                    while ((c = input.read()) != -1) {
                        output.write(c);
                    }
                } catch (NullPointerException ex) {
                    NullPointerException npe = new NullPointerException(
                            "While reading from \"" + input
                                    + "\" and writing to \"" + output
                                    + "\", a NullPointerException occurred.  "
                                    + "This can happen when attempting to read "
                                    + "from a JarURL entry that points to a directory.");
                    npe.initCause(ex);
                    throw npe;
                }
            } finally {
                if (output != null) {
                    try {
                        output.close();
                    } catch (Throwable throwable) {
                        throw new RuntimeException(throwable);
                    }
                }
            }
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (NullPointerException npe) {
                    // Ignore, see
                    // Work around
                    // "JarUrlConnection.getInputStream().close() throws
                    // NPE when entry is a directory"
                    // https://bugs.openjdk.java.net/browse/JDK-8080094
                } catch (Throwable throwable) {
                    throw new RuntimeException(throwable);
                }
            }
        }
    }

    /** Read a stream safely.  If there are problems, the streams are
     *  close appropriately.
     *  @param inputStream The input stream.
     *  @exception IOException If the input stream cannot be read.
     */
    private static byte[] _binaryReadStream(InputStream inputStream)
            throws IOException {
        // Copy the source file.
        BufferedInputStream input = null;

        ByteArrayOutputStream output = null;

        try {
            input = new BufferedInputStream(inputStream);

            try {
                output = new ByteArrayOutputStream();
                // Read the stream in 8k chunks
                final int BUFFERSIZE = 8192;
                byte[] buffer = new byte[BUFFERSIZE];
                int bytesRead = 0;
                while ((bytesRead = input.read(buffer, 0, BUFFERSIZE)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            } finally {
                if (output != null) {
                    try {
                        // ByteArrayOutputStream.close() has no
                        // effect, but we try it anyway for good form.
                        output.close();
                    } catch (Throwable throwable) {
                        throw new RuntimeException(throwable);
                    }
                }
            }
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Throwable throwable) {
                    throw new RuntimeException(throwable);
                }
            }
        }
        if (output != null) {
            return output.toByteArray();
        }
        return null;
    }

    /** Search the classpath.
     *  @param name The name to be searched
     *  @param classLoader The class loader to use to locate system
     *   resources, or null to use the system class loader that was used
     *   to load this class.
     *  @return null if name does not start with "$CLASSPATH"
     *  or _CLASSPATH_VALUE or if name cannot be found.
     */
    private static URL _searchClassPath(String name, ClassLoader classLoader)
            throws IOException {

        URL result = null;

        // If the name begins with "$CLASSPATH", or
        // "xxxxxxCLASSPATHxxxxxx",then attempt to open the file
        // relative to the classpath.
        // NOTE: Use the dummy variable constant set up in the constructor.
        if (name.startsWith(_CLASSPATH_VALUE)
                || name.startsWith("$CLASSPATH")) {
            // Try relative to classpath.
            String trimmedName = _trimClassPath(name);

            if (classLoader == null) {
                String referenceClassName = "ptolemy.util.FileUtilities";

                try {
                    // WebStart: We might be in the Swing Event thread, so
                    // Thread.currentThread().getContextClassLoader()
                    // .getResource(entry) probably will not work so we
                    // use a marker class.
                    Class referenceClass = Class.forName(referenceClassName);
                    classLoader = referenceClass.getClassLoader();
                } catch (Exception ex) {
                    // IOException constructor does not take a cause
                    IOException ioException = new IOException(
                            "Cannot look up class \"" + referenceClassName
                                    + "\" or get its ClassLoader.");
                    ioException.initCause(ex);
                    throw ioException;
                }
            }

            // Use Thread.currentThread()... for Web Start.
            result = classLoader.getResource(trimmedName);
        }
        return result;
    }

    /** Remove the value of _CLASSPATH_VALUE or "$CLASSPATH".
     */
    private static String _trimClassPath(String name) {
        String classpathKey;

        if (name.startsWith(_CLASSPATH_VALUE)) {
            classpathKey = _CLASSPATH_VALUE;
        } else {
            classpathKey = "$CLASSPATH";
        }

        return name.substring(classpathKey.length() + 1);
    }

    ///////////////////////////////////////////////////////////////////
    ////                         private members                   ////

    /** Tag value used by this class and registered as a parser
     *  constant for the identifier "CLASSPATH" to indicate searching
     *  in the classpath.  This is a hack, but it deals with the fact
     *  that Java is not symmetric in how it deals with getting files
     *  from the classpath (using getResource) and getting files from
     *  the file system.
     */
    private static String _CLASSPATH_VALUE = "xxxxxxCLASSPATHxxxxxx";
}
