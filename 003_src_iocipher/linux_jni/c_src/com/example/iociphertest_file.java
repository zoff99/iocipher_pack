/*
 *
 * IOCipher Linux Java example
 * (C) Zoff in 2024
 *
 */

package com.example;

import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.VirtualFileSystem;
import info.guardianproject.iocipher.FileWriter;
import info.guardianproject.iocipher.FileReader;
import info.guardianproject.iocipher.FileOutputStream;
import info.guardianproject.iocipher.FileInputStream;
import info.guardianproject.iocipher.IOCipherFileChannel;
import info.guardianproject.iocipher.RandomAccessFile;

import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.security.MessageDigest;
import java.security.DigestInputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class iociphertest_file
{
    private final static String TAG = "CipherFileTest";

    private static VirtualFileSystem vfs;
    private static String path;
    private static File ROOT = null;
    private final static String goodPassword = iociphertest.goodPassword;

    // @Before
    public static void setUp() {
        Log.v(TAG, "setUp:...");
        path = iociphertest.dbfilename;
        java.io.File db = new java.io.File(path);
        if (db.exists()) {
            Log.v(TAG, "Deleting existing database file: " + db.getAbsolutePath());
            db.delete();
        }
        if (db.exists()) {
            Log.v(TAG, "Deleted file exists: " + db.getAbsolutePath());
            fail();
        }
        Log.v(TAG, "Creating new database file: " + db.getAbsolutePath());
        vfs = VirtualFileSystem.get();
        vfs.setContainerPath(path);
        vfs.createNewContainer(goodPassword);
        Log.v(TAG, "Mounting:");
        vfs.mount(goodPassword);
        ROOT = new File("/");
        Log.v(TAG, "setUp:DONE");
    }

    // @After
    public static void tearDown() {
        vfs.unmount();
    }

    // // @Test
    public static void testExists() {
        File f = new File("");
        try {
            assertFalse(f.exists());
        } catch (ExceptionInInitializerError e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        }
    }

    // // @Test
    // public static void testGetFreeSpace() {
    // File f = new File(ROOT, "");
    // try {
    // long free = f.getFreeSpace();
    // Log.v(TAG, "f.getFreeSpace: " + Long.toString(free));
    // assertTrue(free > 0);
    // } catch (ExceptionInInitializerError e) {
    // Log.e(TAG, e.getCause().toString());
    // assertFalse(true);
    // }
    // }

    // // @Test
    // public static void testGetUsableSpace() {
    // File f = new File(ROOT, "");
    // try {
    // long total = f.getUsableSpace();
    // Log.v(TAG, "f.getUsableSpace: " + Long.toString(total));
    // assertTrue(total > 0);
    // } catch (ExceptionInInitializerError e) {
    // Log.e(TAG, e.getCause().toString());
    // assertFalse(true);
    // }
    // }

    // // @Test
    // public static void testGetTotalSpace() {
    // File f = new File(ROOT, "");
    // try {
    // long total = f.getTotalSpace();
    // Log.v(TAG, "f.getTotalSpace: " + Long.toString(total));
    // assertTrue(total > 0);
    // } catch (ExceptionInInitializerError e) {
    // Log.e(TAG, e.getCause().toString());
    // assertFalse(true);
    // }
    // }

    // @Test
    public static void testMkdirExists() {
        File f = new File(ROOT, "test.iocipher.dir."
                + Integer.toString((int) (Math.random() * 1024)));
        try {
            assertFalse(f.exists());
            assertTrue(f.mkdir());
            assertTrue(f.exists());
        } catch (ExceptionInInitializerError e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        }
    }

    // @Test
    public static void testMkdirs() {
        File f0 = new File(ROOT, Integer.toString((int) (Math.random() * Integer.MAX_VALUE)));
        File f1 = new File(f0,
                Integer.toString((int) (Math.random() * Integer.MAX_VALUE)));
        File f2 = new File(f1,
                Integer.toString((int) (Math.random() * Integer.MAX_VALUE)));
        Log.v(TAG, "f2: " + f2.getAbsolutePath());
        try {
            f2.mkdirs();
            for (String f : f0.list()) {
                Log.v(TAG, "file in f0: " + f);
            }
            for (String f : f1.list()) {
                Log.v(TAG, "file in f1: " + f);
            }
            assertTrue(f0.exists());
            assertTrue(f1.exists());
            assertTrue(f2.exists());
        } catch (ExceptionInInitializerError e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        }
    }

    // @Test
    public static void testSlashIsDirectory() {
        File f = ROOT;
        try {
            assertTrue(f.isDirectory());
        } catch (ExceptionInInitializerError e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        }
    }

    // @Test
    public static void testCanReadSlash() {
        File f = ROOT;
        try {
            assertTrue(f.isDirectory());
            assertTrue(f.canRead());
        } catch (ExceptionInInitializerError e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        }
    }

    // @Test
    public static void testCanWriteSlash() {
        File f = ROOT;
        try {
            assertTrue(f.isDirectory());
            assertTrue(f.canWrite());
        } catch (ExceptionInInitializerError e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        }
    }

    // @Test
    public static void testSlashIsFile() {
        File f = ROOT;
        try {
            assertFalse(f.isFile());
        } catch (ExceptionInInitializerError e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        }
    }

    // @Test
    public static void testSlashIsAbsolute() {
        File f = ROOT;
        try {
            assertTrue(f.isAbsolute());
        } catch (ExceptionInInitializerError e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        }
    }

    // @Test
    public static void testMkdirRemove() {
        File f = new File(ROOT, "mkdir-to-remove");
        try {
            assertTrue(f.mkdir());
            assertTrue(f.exists());
            assertTrue(f.delete());
            assertFalse(f.exists());
        } catch (ExceptionInInitializerError e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        }
    }

    // @Test
    public static void testRenameToExisting() {
        File d = new File(ROOT, "dir-to-rename");
        File d2 = new File(ROOT, "exists");
        try {
            d.mkdir();
            d2.mkdir();
            assertFalse(d.renameTo(new File(ROOT, "exists")));
            assertTrue(d.exists());
        } catch (ExceptionInInitializerError e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        }
    }

    // @Test
    public static void testMkdirRename() {
        String dir = "mkdir-to-rename";
        String newdir = "renamed";
        String firstfile = "first-file";
        File root = ROOT;
        File d = new File(root, dir);
        File newd = new File(root, newdir);
        try {
            d.mkdir();
            File f1 = new File(d, firstfile);
            f1.createNewFile();
            assertTrue(f1.exists());
            String[] files = d.list();
            assertEquals(files.length, 1);
            for (String filename : files) {
                Log.v(TAG, "testMkdirRename " + dir + ": " + filename);
            }
            assertTrue(d.renameTo(newd));
            assertTrue(new File(newd, firstfile).exists());
            File f2 = new File(newd, "second-file");
            f2.createNewFile();
            files = root.list();
            assertEquals(files.length, 1);
            for (String filename : files) {
                Log.v(TAG, "testMkdirRename root: " + filename);
            }
            files = newd.list();
            assertEquals(files.length, 2);
            for (String filename : files) {
                Log.v(TAG, "testMkdirRename " + newdir + ": " + filename);
            }
            assertFalse(d.exists());
            assertTrue(newd.exists());
            assertTrue(f2.exists());
        } catch (ExceptionInInitializerError | IOException e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        }
    }

    // @Test
    public static void testNewFileRename() {
        File root = ROOT;
        File f = new File(Util.randomFileName(ROOT, "testNewFileRename-NEW"));
        File newf = new File(Util.randomFileName(ROOT, "testNewFileRename-RENAMED"));
        try {
            f.createNewFile();
            assertTrue(f.renameTo(newf));
            final String[] files = root.list();
            for (String filename : files) {
                Log.v(TAG, "testNewFileRename file: " + filename);
            }
            assertFalse(f.exists());
            assertTrue(newf.exists());
        } catch (ExceptionInInitializerError | IOException e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        }
    }

    // @Test
    public static void testMkdirIsDirectory() {
        File f = new File(ROOT, "mkdir-to-test");
        try {
            f.mkdir();
            assertTrue(f.isDirectory());
        } catch (ExceptionInInitializerError e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        }
    }

    // @Test
    public static void testMkdirList() {
        File root = ROOT;
        File f = new File(ROOT, "mkdir-to-list");
        try {
            f.mkdir();
            final String[] files = root.list();
            for (String filename : files) {
                Log.v(TAG, "testMkdirList file: " + filename);
            }
            Log.v(TAG, "testMkdirList list: " + files.length);
            assertTrue(files.length == 1); // ".." and "." shouldn't be included
        } catch (ExceptionInInitializerError e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        }
    }

    /*
     * // TODO testMkdirLastModified fails public static void testMkdirLastModified() {
     * File root = ROOT; File f = new File(Util.randomFileName(ROOT,
     * "test.iocipher.dir")); try { long lasttime = root.lastModified();
     * Log.v(TAG, "f.lastModified: " + Long.toString(lasttime)); f.mkdir(); long
     * thistime = root.lastModified(); Log.i(TAG,
     * "f.lastModified after setting: " + Long.toString(thistime));
     * assertTrue(thistime > lasttime); } catch (ExceptionInInitializerError e)
     * { Log.e(TAG, e.getCause().toString()); assertFalse(true); } } // TODO
     * testMkdirMtime fails public static void testMkdirMtime() { File f = new
     * File("/mkdir-with-mtime"); long faketime = 1000000000L; try { f.mkdir();
     * Log.v(TAG, "f.lastModified: " + Long.toString(f.lastModified()));
     * f.setLastModified(faketime); long time = f.lastModified(); Log.v(TAG,
     * "f.lastModified after setting: " + Long.toString(time)); assertTrue(time
     * == faketime); } catch (ExceptionInInitializerError e) { Log.e(TAG,
     * e.getCause().toString()); assertFalse(true); } }
     */

    // @Test
    public static void testCreateNewFile() {
        File root = ROOT;
        File f = new File(Util.randomFileName(ROOT, "testCreateNewFile"));
        try {
            assertFalse(f.exists());
            f.createNewFile();
            assertTrue(f.exists());
            assertTrue(f.isFile());
            final String[] files = root.list();
            for (String filename : files) {
                Log.v(TAG, "testCreateNewFile file: " + filename);
            }
        } catch (ExceptionInInitializerError | IOException e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        }
    }

    // @Test
    public static void testWriteNewFile() {
        File root = ROOT;
        File f = new File(Util.randomFileName(ROOT, "testWriteNewFile"));
        try {
            assertTrue(root.isDirectory());
            assertFalse(f.exists());
            FileOutputStream out = new FileOutputStream(f);
            out.write(123);
            out.close();
            assertTrue(f.exists());
            assertTrue(f.isFile());
            final String[] files = root.list();
            for (String filename : files) {
                Log.v(TAG, "testWriteNewFile file: " + filename);
            }
        } catch (ExceptionInInitializerError | IOException e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        }
    }

    // @Test
    /*
    public static void testWriteNewFile12GB() {
        File root = ROOT;
        File f = new File(Util.randomFileName(ROOT, "testWriteNewFile12GB"));
        final long one_g = 1L * 1024 * 1024 * 1024;
        final long one_m = 1L * 1024 * 1024;
        final int buf_bytes = (int)one_m;
        final byte[] buf = new byte[buf_bytes];
        final long size = 12L * one_g; // 12 GBytes
        final long loops = (size / buf_bytes);
        try {
            assertTrue(root.isDirectory());
            assertFalse(f.exists());
            FileOutputStream out = new FileOutputStream(f);
            long last = 0;
            long i=0;
            Log.v(TAG, "testWriteNewFile12GB starting to write very large file ...");
            for(i=0;i<loops;i++)
            {
                out.write(buf);
                if (((i * buf_bytes) - last >= (100 * one_m)))
                {
                    Log.v(TAG, "testWriteNewFile12GB current size=" + (float)((i * buf_bytes) / (float)one_g) + " GB");
                    last = i * buf_bytes;
                }
            }
            Log.v(TAG, "testWriteNewFile12GB size at the end=" + (float)((i * buf_bytes) / (float)one_g) + " GB");
            out.close();
            assertTrue(f.exists());
            assertTrue(f.isFile());
            final String[] files = root.list();
            for (String filename : files) {
                Log.v(TAG, "testWriteNewFile12GB file: " + filename + " size=" + f.length());
            }
        } catch (ExceptionInInitializerError | IOException e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        }
    }
    */

    // @Test
    public static void testWriteByteInNewFileThenRead() {
        byte testValue = 43;
        File root = ROOT;
        File f = new File(Util.randomFileName(ROOT, "testWriteNewFile"));
        try {
            assertTrue(root.isDirectory());
            assertFalse(f.exists());
            FileOutputStream out = new FileOutputStream(f);
            out.write(testValue);
            out.close();
            assertTrue(f.exists());
            assertTrue(f.isFile());
            FileInputStream in = new FileInputStream(f);
            int b = in.read();
            Log.v(TAG, "read: " + Integer.toString(b));
            assertTrue(b == testValue);
            in.close();
        } catch (ExceptionInInitializerError | IOException e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        }
    }

    // @Test
    public static void testWriteTextInNewFileThenReadByByte() {
        String testString = "this is a test of IOCipher!";
        File f = new File(Util.randomFileName(ROOT, "testWriteTextInNewFileThenReadByByte"));
        try {
            assertFalse(f.exists());
            BufferedWriter out = new BufferedWriter(new FileWriter(f));
            out.write(testString);
            out.close();
            assertTrue(f.exists());
            assertTrue(f.isFile());
            FileInputStream in = new FileInputStream(f);
            byte[] data = new byte[testString.length()];
            in.read(data, 0, data.length);
            String dataString = new String(data);
            Log.v(TAG, "read: " + dataString);
            assertTrue(dataString.equals(testString));
            in.close();
        } catch (ExceptionInInitializerError e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        } catch (IOException e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        }
    }

    // @Test
    public static void testWriteTextInNewFileThenReadIntoByteArray() {
        String testString = "this is a test of IOCipher!";
        File f = new File(Util.randomFileName(ROOT, "testWriteTextInNewFileThenReadIntoByteArray"));
        try {
            assertFalse(f.exists());
            BufferedWriter out = new BufferedWriter(new FileWriter(f));
            out.write(testString);
            out.close();
            assertTrue(f.exists());
            assertTrue(f.isFile());
            FileInputStream in = new FileInputStream(f);
            byte[] data = new byte[testString.length()];
            int ret = in.read(data);
            assertTrue(ret == data.length);
            String dataString = new String(data);
            assertTrue(dataString.equals(testString));
            in.close();
        } catch (ExceptionInInitializerError e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        } catch (IOException e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        }
    }

    // @Test
    public static void testWriteTextInNewFileThenReadOneByteByByte() {
        String testString = "01234567890abcdefgh";
        File f = new File(Util.randomFileName(ROOT, "testWriteTextInNewFileThenReadOneByteByByte."));
        try {
            assertFalse(f.exists());
            BufferedWriter out = new BufferedWriter(new FileWriter(f));
            out.write(testString);
            out.close();
            assertTrue(f.exists());
            assertTrue(f.isFile());
            FileInputStream in = new FileInputStream(f);
            byte[] data = new byte[testString.length()];
            int ret = 0;
            int i = 0;
            while (i < data.length) {
                ret = in.read();
                if (ret != -1) {
                    data[i] = (byte) ret;
                    i++;
                } else {
                    break;
                }
            }
            String dataString = new String(data);
            Log.v(TAG, "read: " + dataString);
            assertTrue(dataString.equals(testString));
            in.close();
        } catch (ExceptionInInitializerError e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        } catch (IOException e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        }
    }

    // @Test
    public static void testWriteTextInNewFileThenCheckSize() {
        String testString = "01234567890abcdefgh";
        File f = new File(Util.randomFileName(ROOT, "testWriteTextInNewFileThenCheckSize"));
        try {
            assertFalse(f.exists());
            BufferedWriter out = new BufferedWriter(new FileWriter(f));
            out.write(testString);
            out.close();
            assertTrue(f.exists());
            assertTrue(f.isFile());
            FileInputStream in = new FileInputStream(f);
            IOCipherFileChannel channel = in.getChannel();
            assertTrue(channel.size() == testString.length());
            assertTrue(testString.length() == f.length());
            in.close();
        } catch (ExceptionInInitializerError e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        } catch (IOException e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        }
    }

    // @Test
    public static void testWriteTextInNewFileThenSkipAndRead() {
        String testString = "01234567890abcdefghijklmnopqrstuvxyz";
        File f = new File(Util.randomFileName(ROOT, "testWriteTextInNewFileThenSkipAndRead"));
        try {
            assertFalse(f.exists());
            BufferedWriter out = new BufferedWriter(new FileWriter(f));
            out.write(testString);
            out.close();
            assertTrue(f.exists());
            assertTrue(f.isFile());
            FileInputStream in = new FileInputStream(f);
            char c = (char) in.read();
            assertTrue(c == testString.charAt(0));
            in.skip(5);
            c = (char) in.read();
            Log.v(TAG, "c: " + c + "  testString.charAt(6): "
                    + testString.charAt(6));
            assertTrue(c == testString.charAt(6));
            in.skip(20);
            c = (char) in.read();
            Log.v(TAG, "c: " + c + "  testString.charAt(27): "
                    + testString.charAt(27));
            assertTrue(c == testString.charAt(27));
            in.close();
        } catch (ExceptionInInitializerError e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        } catch (IOException e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        }
    }

    // @Test
    public static void testWriteRepeat() {
        int i, repeat = 1000;
        String testString = "01234567890abcdefghijklmnopqrstuvxyzABCDEFGHIJKLMNOPQRSTUVWXYZ\n";
        File f = new File(Util.randomFileName(ROOT, "testWriteRepeat"));
        try {
            assertFalse(f.exists());
            BufferedWriter out = new BufferedWriter(new FileWriter(f));
            for (i = 0; i < repeat; i++)
                out.write(testString);
            out.close();
            assertTrue(f.exists());
            assertTrue(f.isFile());
            Log.v(TAG, f.toString() + ".length(): " + f.length() + " " + testString.length()
                    * repeat);
            assertTrue(f.length() == testString.length() * repeat);
        } catch (ExceptionInInitializerError e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        } catch (IOException e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        }
    }

    // @Test
    public static void testWriteSkipWrite() {
        int skip = 100;
        String testString = "the best of times\n";
        String testString2 = "the worst of times\n";
        File f = new File(Util.randomFileName(ROOT, "testWriteSkipWrite"));
        try {
            assertFalse(f.exists());
            RandomAccessFile inout = new RandomAccessFile(f, "rw");
            inout.writeBytes(testString);
            inout.seek(inout.getFilePointer() + skip);
            inout.writeBytes(testString2);
            inout.close();
            assertTrue(f.exists());
            assertTrue(f.isFile());
            int inputLength = testString.length() + skip + testString2.length();
            Log.v(TAG, "testWriteSkipWrite: " + f.toString() + ".length(): " + f.length() + " "
                    + inputLength);
            assertTrue(f.length() == inputLength);

            inout = new RandomAccessFile(f, "rw");
            byte[] best = new byte[testString.length()];
            byte[] worst = new byte[testString2.length()];
            inout.read(best, 0, testString.length());
            inout.seek(inout.getFilePointer() + skip);
            inout.read(worst, 0, testString2.length());
            inout.close();
            assertEquals(new String(best), testString);
            assertEquals(new String(worst), testString2);
        } catch (ExceptionInInitializerError e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        } catch (IOException e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        }
    }

    // @Test
    public static void testWriteTextInNewFileThenFileInputStream() {
        String testString = "01234567890abcdefgh";
        File f = new File(Util.randomFileName(ROOT, "testWriteTextInNewFileThenFileInputStream"));
        try {
            assertFalse(f.exists());
            BufferedWriter out = new BufferedWriter(new FileWriter(f));
            out.write(testString);
            out.close();
            assertTrue(f.exists());
            assertTrue(f.isFile());
            BufferedReader in = new BufferedReader(new FileReader(f));
            String tmp = in.readLine();
            Log.v(TAG, "in.readline(): " + tmp);
            assertTrue(testString.equals(tmp));
            in.close();
        } catch (ExceptionInInitializerError e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        } catch (IOException e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        }
    }

    // @Test
    public static void testWriteManyLinesInNewFileThenFileInputStream() {
        String testString = "01234567890abcdefghijklmnopqrstuvwxyz";
        File f = new File(Util.randomFileName(ROOT,
                "testWriteManyLinesInNewFileThenFileInputStream"));
        try {
            assertFalse(f.exists());
            BufferedWriter out = new BufferedWriter(new FileWriter(f));
            for (int i = 0; i < 1000; i++)
                out.write(testString + "\n");
            out.close();
            assertTrue(f.exists());
            assertTrue(f.isFile());
            BufferedReader in = new BufferedReader(new FileReader(f));
            for (int i = 0; i < 1000; i++) {
                String tmp = in.readLine();
                Log.v(TAG, "in.readline(): " + tmp);
                assertTrue(testString.equals(tmp));
            }
            in.close();
        } catch (ExceptionInInitializerError e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        } catch (IOException e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        }
    }

    // @Test
    public static void testWriteAndReadAfterAlreadyMountedException() {
        String testString = "01234567890abcdefghijklmnopqrstuvwxyz";
        File f = new File(Util.randomFileName(ROOT,
                "testWriteAndReadAfterAlreadyMountedException"));
        try {
            assertFalse(f.exists());
            BufferedWriter out = new BufferedWriter(new FileWriter(f));
            for (int i = 0; i < 100; i++) {
                out.write(testString + "\n");
                try {
                    vfs.mount(goodPassword);
                    fail();
                } catch (IllegalStateException e) {
                    // this is what we want, its already mounted
                }
                assertTrue(vfs.isMounted());
            }
            out.close();
            assertTrue(f.exists());
            assertTrue(f.isFile());
            BufferedReader in = new BufferedReader(new FileReader(f));
            for (int i = 0; i < 100; i++) {
                String tmp = in.readLine();
                Log.v(TAG, "in.readline(): " + tmp);
                try {
                    vfs.mount(goodPassword);
                    fail();
                } catch (IllegalStateException e) {
                    // this is what we want, its already mounted
                }
                assertTrue(vfs.isMounted());
                assertTrue(testString.equals(tmp));
            }
            in.close();
        } catch (ExceptionInInitializerError e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        } catch (IOException e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        }
    }

    private static byte[] digest(File f) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            FileInputStream fstr = new FileInputStream(f);
            DigestInputStream dstr = new DigestInputStream(fstr, md);

            // read to EOF, really Java? *le sigh*
            while (dstr.read() != -1)
            {
                ;
            }

            dstr.close();
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        } catch (IOException e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        }
        return null;
    }

    // @Test
    public static void testFileChannelTransferTo() {
        String input_name = "/testCopyFileChannels-input";
        String output_name = "/testCopyFileChannels-output";
        assertTrue(Util.cipherWriteRandomBytes(1000, input_name));
        File inputFile = new File(input_name);
        File outputFile = new File(output_name);

        try {
            assertTrue(inputFile.exists());
            assertTrue(inputFile.isFile());

            assertFalse(outputFile.exists());

            FileInputStream source = new FileInputStream(inputFile);
            FileOutputStream destination = new FileOutputStream(output_name);
            IOCipherFileChannel sourceFileChannel = source.getChannel();
            IOCipherFileChannel destinationFileChannel = destination.getChannel();

            sourceFileChannel.transferTo(0, sourceFileChannel.size(), destinationFileChannel);
            sourceFileChannel.close();
            destinationFileChannel.close();

            assertTrue(outputFile.exists());
            assertTrue(outputFile.isFile());
            assertEquals(inputFile.length(), outputFile.length());

            byte[] expected = digest(inputFile);
            byte[] actual = digest(outputFile);

            Log.i(TAG, "file hashes:" + Util.toHex(expected) + "    " + Util.toHex(actual));
            assertTrue(Arrays.equals(expected, actual));

            source.close();
            destination.close();
        } catch (ExceptionInInitializerError e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        } catch (IOException e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        }
    }

    // @Test
    public static void testFileChannelTransferFrom() {
        String input_name = "/testCopyFileChannels-input";
        String output_name = "/testCopyFileChannels-output";
        assertTrue(Util.cipherWriteRandomBytes(1000, input_name));
        File inputFile = new File(input_name);
        File outputFile = new File(output_name);

        try {
            assertTrue(inputFile.exists());
            assertTrue(inputFile.isFile());

            assertFalse(outputFile.exists());

            FileInputStream source = new FileInputStream(inputFile);
            FileOutputStream destination = new FileOutputStream(output_name);
            IOCipherFileChannel sourceFileChannel = source.getChannel();
            IOCipherFileChannel destinationFileChannel = destination.getChannel();

            destinationFileChannel.transferFrom(sourceFileChannel, 0, sourceFileChannel.size());
            sourceFileChannel.close();
            destinationFileChannel.close();

            assertTrue(outputFile.exists());
            assertTrue(outputFile.isFile());
            assertEquals(inputFile.length(), outputFile.length());

            byte[] expected = digest(inputFile);
            byte[] actual = digest(outputFile);

            Log.i(TAG, "file hashes:" + Util.toHex(expected) + "    " + Util.toHex(actual));
            assertTrue(Arrays.equals(expected, actual));

            source.close();
            destination.close();
        } catch (ExceptionInInitializerError e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        } catch (IOException e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        }
    }

    // @Test
    public static void testFileExistingTruncate() {
        String name = Util.randomFileName(ROOT, "testFileExistingTruncate");
        assertTrue(Util.cipherWriteRandomBytes(50000, name));

        File f = new File(name);
        assertEquals(50000, f.length());

        try {
            FileOutputStream out = new FileOutputStream(f);
            out.close();
            assertEquals(0, f.length());
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        } catch (IOException e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        }
    }

    // @Test
    public static void testFileManySizes() {
        try {
            for(int i=0;i<10000;i++)
            {
                String name = Util.randomFileName(ROOT, "testFileManySizes");
                File f = new File(name);
                byte[] bufrandom = Util.cipherWriteRandomByteReturnBuf(i, name);
                assertNotSame(bufrandom, null);
                Log.v(TAG, "write: bytes=" + i);

                f = new File(name);
                FileOutputStream out = new FileOutputStream(f, true);
                Log.v(TAG, "append: length:before=" + f.length());
                for(int k=0;k<2;k++)
                {
                    out.write(19);
                    Log.v(TAG, "append: length:+k=" + f.length());
                }
                out.close();

                f = new File(name);
                byte[] orig_in = new byte[i];
                FileInputStream in = new FileInputStream(f);
                in.read(orig_in, 0, i);
                assertEquals(i + 2, f.length());
                for(int j=0;j<i;j++)
                {
                    assertEquals(bufrandom[j], orig_in[j]);
                }
                Log.v(TAG, "CMP: " + Util.bytesToHex(bufrandom) + " <--> " + Util.bytesToHex(orig_in));
                Log.v(TAG, "read: bytes=" + i + " OK");
                in.close();
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        } catch (Exception e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        }
    }

    // @Test
    public static void testFileExistingAppend() {
        String name = Util.randomFileName(ROOT, "testFileExistingAppend");
        assertTrue(Util.cipherWriteRandomBytes(500, name));
        File f = new File(name);
        byte[] orig_buf = new byte[500];

        try {
            FileInputStream in = new FileInputStream(f);
            in.read(orig_buf, 0, 500);

            assertEquals(500, f.length());

            FileOutputStream out = new FileOutputStream(f, true);

            // write 2 bytes
            out.write(13);
            out.write(42);
            out.close();
            assertEquals(502, f.length());

            FileInputStream in2 = new FileInputStream(f);
            byte[] test_buf = new byte[500];
            in2.read(test_buf, 0, 500);
            assertTrue(Arrays.equals(orig_buf, test_buf));
            assertEquals(13, in2.read());
            assertEquals(42, in2.read());
            in.close();
            in2.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        } catch (IOException e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        }
    }

    // @Test
    public static void testWriteByteInExistingFileThenRead() {
        byte testValue = 43;
        byte secondTestValue = 100;
        File root = ROOT;
        File f = new File(Util.randomFileName(ROOT, "testWriteByteInExistingFileThenRead"));
        try {
            assertTrue(root.isDirectory());
            assertFalse(f.exists());
            FileOutputStream out = new FileOutputStream(f);
            out.write(testValue);
            out.close();
            assertTrue(f.exists());
            assertTrue(f.isFile());
            FileInputStream in = new FileInputStream(f);
            int b = in.read();
            in.close();
            Log.v(TAG, "read: " + Integer.toString(b));
            assertTrue(b == testValue);

            // now overwrite
            out = new FileOutputStream(f);
            out.write(secondTestValue);
            out.close();
            assertTrue(f.exists());
            assertTrue(f.isFile());
            in = new FileInputStream(f);
            b = in.read();
            in.close();
            assertTrue(b == secondTestValue);
        } catch (ExceptionInInitializerError e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        } catch (IOException e) {
            Log.e(TAG, e.getCause().toString());
            assertFalse(true);
        }
    }

    // @Test
    public static void testEqualsAndCompareTo() {
        String filename = "thisisafile";
        File f = new File(ROOT, filename);
        File dup = new File(ROOT, filename);
        File diff = new File(ROOT, "differentfile");
        assertTrue(f.equals(dup));
        assertTrue(f.compareTo(dup) == 0);
        assertFalse(f.equals(diff));
        assertTrue(f.compareTo(diff) != 0);
    }

    static void assertEquals(String a, String b)
    {
        if (!a.equals(b))
        {
            Log.e(TAG, "ERROR:assertEquals" + " " + Log.getCaller());
            System.exit(1);
        }
    }

    static void assertEquals(int a, int b)
    {
        if (a != b)
        {

            Log.e(TAG, "ERROR:assertEquals a=" + a + " b=" + b + " " + Log.getCaller());
            System.exit(1);
        }
    }

    static void assertEquals(long a, long b)
    {
        if (a != b)
        {
            Log.e(TAG, "ERROR:assertEquals a=" + a + " b=" + b + " " + Log.getCaller());
            System.exit(1);
        }
    }

    static void assertNotNull(Object a)
    {
        if (a == null)
        {
            Log.e(TAG, "ERROR:assertNotNull" + " " + Log.getCaller());
            System.exit(1);
        }
    }

    static void assertTrue(boolean b)
    {
        if (!b)
        {
            Log.e(TAG, "ERROR:assertTrue" + " " + Log.getCaller());
            System.exit(1);
        }
    }

    static void assertFalse(boolean b)
    {
        if (b)
        {
            Log.e(TAG, "ERROR:assertFalse" + " " + Log.getCaller());
            System.exit(1);
        }
    }

    static void fail()
    {
        Log.e(TAG, "ERROR:fail" + " " + Log.getCaller());
        System.exit(1);
    }

    static void assertNotSame(byte[] a, String dummy)
    {
        if (a == null)
        {
            Log.e(TAG, "ERROR:assertNotSame" + " " + Log.getCaller());
            System.exit(1);
        }
    }
}
