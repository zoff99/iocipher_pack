/**
 * IOCipher
 * Copyright (C) 2023-2024 Zoff <zoff@zoff.cc>
 * <p>
 */

package info.guardianproject.iocipher;

import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public class NativeLibLoader
{
    private static final String LOCK_EXT = ".lck";
    private static boolean extracted = false;

    private static final String NativeLibraryPath = "/jnilibs/linux_amd64";
    private static final String NativeLibraryName = "libiocipher2.so";


    /**
     * Loads native library.
     *
     * @return True if native library is successfully loaded; false otherwise.
     */
    public static synchronized boolean initialize() throws Exception
    {
        // only cleanup before the first extract
        if (!extracted)
        {
            cleanup();
        }
        loadNativeLibrary();
        return extracted;
    }

    private static java.io.File getTempDir()
    {
        java.io.File f = new java.io.File(System.getProperty("java.io.tmpdir"));
        System.out.println("getTempDir: " + f.getAbsolutePath());
        return f;
    }

    /**
     * Deleted old native libraries
     */
    static void cleanup()
    {
        String searchPattern = "libiocipher2.so";
        try (Stream<Path> dirList = Files.list(getTempDir().toPath()))
        {
            dirList.filter(
                            path ->
                                    !path.getFileName().toString().endsWith(LOCK_EXT)
                                            && path.getFileName()
                                                    .toString()
                                                    .startsWith(searchPattern))
                    .forEach(
                            nativeLib -> {
                                Path lckFile = Paths.get(nativeLib + LOCK_EXT);
                                if (Files.notExists(lckFile))
                                {
                                    try
                                    {
                                        System.out.println("trying to delete: " + nativeLib.toString());
                                        Files.delete(nativeLib);
                                        System.out.println("trying to delete: " + nativeLib.toString() + " -> DONE");
                                    }
                                    catch (Exception e)
                                    {
                                        System.out.println("Failed to delete old native lib" + e.getLocalizedMessage());
                                    }
                                }
                            });
        }
        catch (java.io.IOException e)
        {
            System.out.println("Failed to open directory" + e.getLocalizedMessage());
        }
    }

    /**
     * Loads native library using the given path and name of the library.
     *
     * @param path Path of the native library.
     * @param name Name of the native library.
     * @return True for successfully loading; false otherwise.
     */
    private static boolean loadNativeLibrary(String path, String name)
    {
        java.io.File libPath = new java.io.File(path, name);
        if (libPath.exists())
        {
            try
            {
                System.load(new java.io.File(path, name).getAbsolutePath());
                System.out.println("native library loaded OK " + (new java.io.File(path, name).getAbsolutePath()));
                return true;
            }
            catch(UnsatisfiedLinkError e)
            {
                System.out.println("Failed to load native library. UnsatisfiedLinkError " + (new java.io.File(path, name).getAbsolutePath()));
                return false;
            }
            catch(Exception e)
            {
                System.out.println("Failed to load native library. other ERROR. " + (new java.io.File(path, name).getAbsolutePath()));
                return false;
            }
        }
        else
        {
            System.out.println("Failed to load native library. library file not found. " + (new java.io.File(path, name).getAbsolutePath()));
            return false;
        }
    }

    private static boolean contentsEquals(java.io.InputStream in1, java.io.InputStream in2) throws java.io.IOException
    {
        if (!(in1 instanceof java.io.BufferedInputStream))
        {
            in1 = new java.io.BufferedInputStream(in1);
        }
        if (!(in2 instanceof java.io.BufferedInputStream))
        {
            in2 = new java.io.BufferedInputStream(in2);
        }

        int ch = in1.read();
        while (ch != -1)
        {
            int ch2 = in2.read();
            if (ch != ch2)
            {
                return false;
            }
            ch = in1.read();
        }
        int ch2 = in2.read();
        return ch2 == -1;
    }

    /**
     * Extracts and loads the specified library file to the target folder
     *
     * @param libFolderForCurrentOS Library path.
     * @param libraryFileName Library name.
     * @param targetFolder Target folder.
     * @return
     */
    private static boolean extractAndLoadLibraryFile(String libFolderForCurrentOS, String libraryFileName, String targetFolder)
            throws java.io.FileNotFoundException
    {
        String nativeLibraryFilePath = libFolderForCurrentOS + "/" + libraryFileName;
        // Include architecture name in temporary filename in order to avoid conflicts
        // when multiple JVMs with different architectures running at the same time
        String uuid = UUID.randomUUID().toString();
        String extractedLibFileName =
                String.format("iocipher-%s-%s", uuid, libraryFileName);
        String extractedLckFileName = extractedLibFileName + LOCK_EXT;

        Path extractedLibFile = Paths.get(targetFolder, extractedLibFileName);
        Path extractedLckFile = Paths.get(targetFolder, extractedLckFileName);

        try
        {
            // Extract a native library file into the target directory
            try (java.io.InputStream reader = (NativeLibLoader.class.getResourceAsStream("/jnilibs/linux_amd64/libiocipher2.so")))
            {
                System.out.println("reader=" + reader);
                if (Files.notExists(extractedLckFile))
                {
                    Files.createFile(extractedLckFile);
                }

                Files.copy(reader, extractedLibFile, StandardCopyOption.REPLACE_EXISTING);
            }
            finally
            {
                // Delete the extracted lib file on JVM exit.
                extractedLibFile.toFile().deleteOnExit();
                extractedLckFile.toFile().deleteOnExit();
            }

            // Set executable (x) flag to enable Java to load the native library
            extractedLibFile.toFile().setReadable(true);
            extractedLibFile.toFile().setWritable(true, true);
            extractedLibFile.toFile().setExecutable(true);

            // Check whether the contents are properly copied from the resource folder
            {
                try (java.io.InputStream nativeIn = NativeLibLoader.class.getResourceAsStream(nativeLibraryFilePath);
                java.io.InputStream extractedLibIn = Files.newInputStream(extractedLibFile))
                {
                    if (!contentsEquals(nativeIn, extractedLibIn))
                    {
                        throw new java.io.FileNotFoundException(
                                String.format(
                                        "Failed to write a native library file at %s",
                                        extractedLibFile));
                    }
                    else
                    {
                        System.out.println("extracted file matches the file inside the jar archive");
                    }
                }
            }
            return loadNativeLibrary(targetFolder, extractedLibFileName);
        } catch (java.io.IOException e) {
            System.out.println("Unexpected IOException: " + e.getLocalizedMessage());
            return false;
        }
    }

    /**
     * Loads native library using given path and name of the library.
     *
     * @throws
     */
    private static void loadNativeLibrary() throws Exception
    {
        if (extracted)
        {
            return;
        }

        List<String> triedPaths = new LinkedList<>();
        // temporary library folder
        String tempFolder = getTempDir().getAbsolutePath();
        // Try extracting the library from jar
        if (extractAndLoadLibraryFile(NativeLibraryPath, NativeLibraryName, tempFolder))
        {
            extracted = true;
            return;
        }
        else
        {
            triedPaths.add(NativeLibraryPath);
        }
    }
}