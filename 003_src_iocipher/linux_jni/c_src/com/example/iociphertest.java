/*
 *
 * IOCipher Linux C example
 * (C) Zoff in 2024
 *
 */

package com.example;

import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.VirtualFileSystem;

import static info.guardianproject.iocipher.VirtualFileSystem.IOCIPHER_JNI_VERSION;

public class iociphertest
{
    private final static String TAG = "VirtualFileSystemTest";

    private static VirtualFileSystem vfs;
    private static String path;
    final static String dbfilename = "./test.db";
    final static String goodPassword = "this is the right password";
    private static String badPassword = "this soooo not the right password, its wrong";
    private static byte[] goodKey = {
            (byte) 0x2a, (byte) 0xfc, (byte) 0x69, (byte) 0xa1, (byte) 0x16, (byte) 0x40,
            (byte) 0x4f, (byte) 0x7d, (byte) 0x7f, (byte) 0x1b, (byte) 0x1d, (byte) 0xb9,
            (byte) 0x5e, (byte) 0x18, (byte) 0x11, (byte) 0x2e, (byte) 0x6b, (byte) 0x3c,
            (byte) 0xf7, (byte) 0x1e, (byte) 0x78, (byte) 0xaf, (byte) 0x88, (byte) 0x3c,
            (byte) 0xb1, (byte) 0x90, (byte) 0x51, (byte) 0x15, (byte) 0xbf, (byte) 0xc3,
            (byte) 0xb2, (byte) 0x8d,
    };
    private static byte[] tooLongKey = {
            (byte) 0x2a, (byte) 0xfc, (byte) 0x69, (byte) 0xa1, (byte) 0x16, (byte) 0x40,
            (byte) 0x4f, (byte) 0x7d, (byte) 0x7f, (byte) 0x1b, (byte) 0x1d, (byte) 0xb9,
            (byte) 0x5e, (byte) 0x18, (byte) 0x11, (byte) 0x2e, (byte) 0x6b, (byte) 0x3c,
            (byte) 0xf7, (byte) 0x1e, (byte) 0x78, (byte) 0xaf, (byte) 0x88, (byte) 0x3c,
            (byte) 0xb1, (byte) 0x90, (byte) 0x51, (byte) 0x15, (byte) 0xbf, (byte) 0xc3,
            (byte) 0xb2, (byte) 0x8d, (byte) 0x00
    };
    private static byte[] tooShortKey = {
            (byte) 0x2a, (byte) 0xfc, (byte) 0x69, (byte) 0xa1, (byte) 0x16, (byte) 0x40,
            (byte) 0x4f, (byte) 0x7d, (byte) 0x7f, (byte) 0x1b, (byte) 0x1d, (byte) 0xb9,
            (byte) 0x5e, (byte) 0x18, (byte) 0x11, (byte) 0x2e, (byte) 0x6b, (byte) 0x3c,
            (byte) 0xf7, (byte) 0x1e, (byte) 0x78, (byte) 0xaf, (byte) 0x88, (byte) 0x3c,
            (byte) 0xb1, (byte) 0x90, (byte) 0x51, (byte) 0x15, (byte) 0xbf, (byte) 0xc3,
    };
    private static byte[] badKey = {
            'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B',
            'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B',
    };

    public static void setUp() {
        path = dbfilename;
        java.io.File db = new java.io.File(path);
        if (db.exists())
            db.delete();
        vfs = VirtualFileSystem.get();
    }

    public static void tearDown() {
        try
        {
            if (vfs.isMounted()) {
                vfs.unmount();
            }
        }
        catch(Exception e)
        {
        }

    }

    public static void testVersionSqlfs() {
        System.out.println("sqlfs version: " + vfs.sqlfsVersion());
        assertNotNull(vfs.sqlfsVersion());
    }

    public static void testVersionIOcipher() {
        System.out.println("iocipher version: " + vfs.iocipherVersion());
        assertNotNull(vfs.iocipherVersion());
    }

    public static void testVersionIOjnicipher() {
        System.out.println("iocipher JNI version: " + vfs.iocipherJNIVersion());
        assertEquals(vfs.iocipherJNIVersion(), IOCIPHER_JNI_VERSION);
    }

    public static void testInitMountUnmount() {
        vfs.setContainerPath(path);
        vfs.createNewContainer(goodPassword);
        vfs.mount(goodPassword);
        if (vfs.isMounted()) {
            Log.i(TAG, "vfs is mounted");
        } else {
            Log.i(TAG, "vfs is NOT mounted");
        }
        assertTrue(vfs.isMounted());
        vfs.unmount();
    }

    public static void testInitMountMkdirUnmount() {
        vfs.setContainerPath(path);
        vfs.createNewContainer(goodPassword);
        vfs.mount(goodPassword);
        if (vfs.isMounted()) {
            Log.i(TAG, "vfs is mounted");
        } else {
            Log.i(TAG, "vfs is NOT mounted");
        }
        File d = new File("/test");
        assertTrue(d.mkdir());
        vfs.unmount();
    }

    public static void testCreateMountUnmountMountExists() {
        vfs.setContainerPath(path);
        vfs.createNewContainer(goodPassword);
        vfs.mount(goodPassword);
        File f = new File("/testCreateMountUnmountMountExists."
                + Integer.toString((int) (Math.random() * 1024)));
        try {
            f.createNewFile();
        } catch (Exception e) {
            Log.e(TAG, "cannot create " + f.getPath());
            assertFalse(true);
        }
        vfs.unmount();
        vfs.mount(goodPassword);
        assertTrue(f.exists());
        vfs.unmount();
    }

    public static void testMountPasswordWithBadPassword() {
        vfs.createNewContainer(path, goodPassword);
        vfs.mount(goodPassword);
        File d = new File("/");
        for (String f : d.list()) {
            Log.v(TAG, "file: " + f);
        }
        vfs.unmount();
        vfs.mount(badPassword);
    }

    public static void testMountKeyWithBadKey() {
        vfs.setContainerPath(path);
        vfs.createNewContainer(goodKey);
        Log.i(TAG, "goodKey length: " + goodKey.length);
        Log.i(TAG, "badKey length: " + badKey.length);
        vfs.mount(goodKey);
        File d = new File("/");
        for (String f : d.list()) {
            Log.v(TAG, "file: " + f);
        }
        vfs.unmount();
        vfs.mount(badKey);
    }

    public static void testMountKeyWithTooLongKey() {
        vfs.createNewContainer(path, goodKey);
        Log.i(TAG, "goodKey length: " + goodKey.length);
        Log.i(TAG, "tooLongKey length: " + tooLongKey.length);
        vfs.mount(goodKey);
        File d = new File("/");
        for (String f : d.list()) {
            Log.v(TAG, "file: " + f);
        }
        vfs.unmount();
        try {
            vfs.mount(tooLongKey);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        } finally {
            try {
                vfs.unmount();
            } catch (IllegalStateException e) {
                // was not mounted, ignore
            }
        }
        fail();
    }

    public static void testMountKeyWithTooShortKey() {
        vfs.createNewContainer(path, goodKey);
        Log.i(TAG, "goodKey length: " + goodKey.length);
        Log.i(TAG, "tooShortKey length: " + tooShortKey.length);
        vfs.mount(goodKey);
        File d = new File("/");
        for (String f : d.list()) {
            Log.v(TAG, "file: " + f);
        }
        vfs.unmount();
        try {
            vfs.mount(tooShortKey);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        } finally {
            try {
                vfs.unmount();
            } catch (IllegalStateException e) {
                // was not mounted, ignore
            }
        }
        fail();
    }

    public static void testMountKeyWithZeroedKey() {
        vfs.setContainerPath(path);
        byte[] keyCopy = new byte[goodKey.length];
        for (int i = 0; i < goodKey.length; i++)
            keyCopy[i] = goodKey[i];
        vfs.createNewContainer(keyCopy);
        vfs.mount(keyCopy);
        File d = new File("/");
        for (String f : d.list()) {
            Log.v(TAG, "file: " + f);
        }
        vfs.unmount();
        for (int i = 0; i < keyCopy.length; i++)
            keyCopy[i] = 0;
        try {
            vfs.mount(keyCopy);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        } finally {
            try {
                vfs.unmount();
            } catch (IllegalStateException e) {
                // was not mounted, ignore
            }
        }
        fail();
    }

    public static void testNoWritePermsInDir() {
        vfs.setContainerPath("/file-to-create-here");
    }

    public static void testMountKeyNonExistentFile() {
        vfs.setContainerPath("/foo/bar/this/does/not/exist");
    }

    public static void testSetGetContainerPath() {
        vfs.setContainerPath(path);
        assertTrue(path.equals(vfs.getContainerPath()));
    }

    public static void testMountAfterFileDeleted() {
        vfs.setContainerPath(path);
        vfs.createNewContainer(goodKey);
        vfs.mount(goodKey);
        File d = new File("/testMountAfterFileDeleted");
        assertTrue(d.mkdir());
        vfs.unmount();
        java.io.File containerFile = new java.io.File(vfs.getContainerPath());
        assertTrue(containerFile.exists());
        containerFile.delete();
        assertFalse(containerFile.exists());
        vfs.mount(goodKey);
    }

    public static void testMountWithoutCreate() {
        vfs.setContainerPath(path);
        vfs.mount(goodKey);
    }

    public static void testMountWithoutCreateSeparat() {
        vfs.setContainerPath(path);
        vfs.mount(goodKey);
    }

    public static void testMountWithoutCreateAtOnce() {
        vfs.mount(path, goodKey);
    }


    // ==============================================


    public static void main(String[] args)
    {
        // -------------------
        System.out.println("-test-");

        if (1==2-1)
        {
            // -------------------
            setUp();
            testVersionSqlfs();
            testVersionIOcipher();
            testVersionIOjnicipher();
            tearDown();
            // -------------------
            Log.i(TAG, "001");
            setUp();
            testInitMountUnmount();
            tearDown();

            Log.i(TAG, "002");
            setUp();
            testInitMountMkdirUnmount();
            tearDown();

            Log.i(TAG, "004");
            setUp();
            try
            {
                testMountPasswordWithBadPassword();
                fail();
            }
            catch(IllegalArgumentException e)
            {
            }
            tearDown();

            Log.i(TAG, "005");
            setUp();
            try
            {
                testMountKeyWithBadKey();
                fail();
            }
            catch(IllegalArgumentException e)
            {
            }
            tearDown();

            Log.i(TAG, "006");
            setUp();
            testMountKeyWithTooLongKey();
            tearDown();

            Log.i(TAG, "007");
            setUp();
            testMountKeyWithTooShortKey();
            tearDown();

            Log.i(TAG, "008");
            setUp();
            testMountKeyWithZeroedKey();
            tearDown();

            Log.i(TAG, "009");
            setUp();
            try
            {
                testNoWritePermsInDir();
                fail();
            }
            catch(IllegalArgumentException e)
            {
            }
            tearDown();

            Log.i(TAG, "010");
            setUp();
            try
            {
                testMountKeyNonExistentFile();
                fail();
            }
            catch(IllegalArgumentException e)
            {
            }
            tearDown();

            Log.i(TAG, "011");
            setUp();
            testSetGetContainerPath();
            tearDown();

            Log.i(TAG, "012");
            setUp();
            try
            {
                testMountAfterFileDeleted();
                fail();
            }
            catch(IllegalArgumentException e)
            {
            }
            tearDown();

            Log.i(TAG, "013");
            setUp();
            try
            {
                testMountWithoutCreate();
                fail();
            }
            catch(IllegalArgumentException e)
            {
            }
            tearDown();

            Log.i(TAG, "014");
            setUp();
            try
            {
                testMountWithoutCreateSeparat();
                fail();
            }
            catch(IllegalArgumentException e)
            {
            }
            tearDown();

            Log.i(TAG, "015");
            setUp();
            try
            {
                testMountWithoutCreateAtOnce();
                fail();
            }
            catch(IllegalArgumentException e)
            {
            }
            tearDown();

            Log.i(TAG, "003");
            setUp();
            testCreateMountUnmountMountExists();
            tearDown();
            // -------------------

        }

        if (1==2-1)
        {
            Log.i(TAG, "=========================== T2:" + Log.getLineNumber());
            iociphertest_file.setUp();iociphertest_file.testExists();iociphertest_file.tearDown();
            Log.i(TAG, "=========================== T2:" + Log.getLineNumber());
            iociphertest_file.setUp();iociphertest_file.testExists();iociphertest_file.tearDown();
            Log.i(TAG, "=========================== T2:" + Log.getLineNumber());
            iociphertest_file.setUp();iociphertest_file.testMkdirExists();iociphertest_file.tearDown();
            Log.i(TAG, "=========================== T2:" + Log.getLineNumber());
            iociphertest_file.setUp();iociphertest_file.testMkdirs();iociphertest_file.tearDown();
            Log.i(TAG, "=========================== T2:" + Log.getLineNumber());
            iociphertest_file.setUp();iociphertest_file.testSlashIsDirectory();iociphertest_file.tearDown();
            Log.i(TAG, "=========================== T2:" + Log.getLineNumber());
            iociphertest_file.setUp();iociphertest_file.testCanReadSlash();iociphertest_file.tearDown();
            Log.i(TAG, "=========================== T2:" + Log.getLineNumber());
            iociphertest_file.setUp();iociphertest_file.testCanWriteSlash();iociphertest_file.tearDown();
            Log.i(TAG, "=========================== T2:" + Log.getLineNumber());
            iociphertest_file.setUp();iociphertest_file.testSlashIsFile();iociphertest_file.tearDown();
            Log.i(TAG, "=========================== T2:" + Log.getLineNumber());
            iociphertest_file.setUp();iociphertest_file.testSlashIsAbsolute();iociphertest_file.tearDown();
            Log.i(TAG, "=========================== T2:" + Log.getLineNumber());
            iociphertest_file.setUp();iociphertest_file.testMkdirRemove();iociphertest_file.tearDown();
            Log.i(TAG, "=========================== T2:" + Log.getLineNumber());
            iociphertest_file.setUp();iociphertest_file.testRenameToExisting();iociphertest_file.tearDown();
            Log.i(TAG, "=========================== T2:" + Log.getLineNumber());
            iociphertest_file.setUp();iociphertest_file.testMkdirRename();iociphertest_file.tearDown();
            Log.i(TAG, "=========================== T2:" + Log.getLineNumber());
            iociphertest_file.setUp();iociphertest_file.testNewFileRename();iociphertest_file.tearDown();
            Log.i(TAG, "=========================== T2:" + Log.getLineNumber());
            iociphertest_file.setUp();iociphertest_file.testMkdirIsDirectory();iociphertest_file.tearDown();
            Log.i(TAG, "=========================== T2:" + Log.getLineNumber());
            iociphertest_file.setUp();iociphertest_file.testMkdirList();iociphertest_file.tearDown();
            Log.i(TAG, "=========================== T2:" + Log.getLineNumber());
            iociphertest_file.setUp();iociphertest_file.testCreateNewFile();iociphertest_file.tearDown();
            Log.i(TAG, "=========================== T2:" + Log.getLineNumber());
            iociphertest_file.setUp();iociphertest_file.testWriteNewFile();iociphertest_file.tearDown();
            Log.i(TAG, "=========================== T2:" + Log.getLineNumber());
            // iociphertest_file.setUp();iociphertest_file.testWriteNewFile12GB();iociphertest_file.tearDown();
            Log.i(TAG, "=========================== T2:" + Log.getLineNumber());
            iociphertest_file.setUp();iociphertest_file.testWriteByteInNewFileThenRead();iociphertest_file.tearDown();
            Log.i(TAG, "=========================== T2:" + Log.getLineNumber());
            iociphertest_file.setUp();iociphertest_file.testWriteTextInNewFileThenReadByByte();iociphertest_file.tearDown();
            Log.i(TAG, "=========================== T2:" + Log.getLineNumber());
            iociphertest_file.setUp();iociphertest_file.testWriteTextInNewFileThenReadIntoByteArray();iociphertest_file.tearDown();
            Log.i(TAG, "=========================== T2:" + Log.getLineNumber());
            iociphertest_file.setUp();iociphertest_file.testWriteTextInNewFileThenReadOneByteByByte();iociphertest_file.tearDown();
            Log.i(TAG, "=========================== T2:" + Log.getLineNumber());
            iociphertest_file.setUp();iociphertest_file.testWriteTextInNewFileThenCheckSize();iociphertest_file.tearDown();
            Log.i(TAG, "=========================== T2:" + Log.getLineNumber());
            iociphertest_file.setUp();iociphertest_file.testWriteTextInNewFileThenSkipAndRead();iociphertest_file.tearDown();
            Log.i(TAG, "=========================== T2:" + Log.getLineNumber());
            iociphertest_file.setUp();iociphertest_file.testWriteRepeat();iociphertest_file.tearDown();
            Log.i(TAG, "=========================== T2:" + Log.getLineNumber());
            iociphertest_file.setUp();iociphertest_file.testWriteSkipWrite();iociphertest_file.tearDown();
            Log.i(TAG, "=========================== T2:" + Log.getLineNumber());
            iociphertest_file.setUp();iociphertest_file.testWriteTextInNewFileThenFileInputStream();iociphertest_file.tearDown();
            Log.i(TAG, "=========================== T2:" + Log.getLineNumber());
            iociphertest_file.setUp();iociphertest_file.testWriteManyLinesInNewFileThenFileInputStream();iociphertest_file.tearDown();
            Log.i(TAG, "=========================== T2:" + Log.getLineNumber());
            iociphertest_file.setUp();iociphertest_file.testWriteAndReadAfterAlreadyMountedException();iociphertest_file.tearDown();
            Log.i(TAG, "=========================== T2:" + Log.getLineNumber());
            iociphertest_file.setUp();iociphertest_file.testFileChannelTransferTo();iociphertest_file.tearDown();
            Log.i(TAG, "=========================== T2:" + Log.getLineNumber());
            iociphertest_file.setUp();iociphertest_file.testFileChannelTransferFrom();iociphertest_file.tearDown();
            Log.i(TAG, "=========================== T2:" + Log.getLineNumber());
            iociphertest_file.setUp();iociphertest_file.testFileExistingTruncate();iociphertest_file.tearDown();
            Log.i(TAG, "=========================== T2:" + Log.getLineNumber());
            iociphertest_file.setUp();iociphertest_file.testFileManySizes();iociphertest_file.tearDown();
        }
        Log.i(TAG, "=========================== T2:" + Log.getLineNumber());
        iociphertest_file.setUp();iociphertest_file.testFileExistingAppend();iociphertest_file.tearDown();
        System.out.flush();
        if (1==2-1)
        {
            Log.i(TAG, "=========================== T2:" + Log.getLineNumber());
            iociphertest_file.setUp();iociphertest_file.testWriteByteInExistingFileThenRead();iociphertest_file.tearDown();
            Log.i(TAG, "=========================== T2:" + Log.getLineNumber());
            iociphertest_file.setUp();iociphertest_file.testEqualsAndCompareTo();iociphertest_file.tearDown();
            Log.i(TAG, "=========================== T2:" + Log.getLineNumber());
        }

        Log.i(TAG, "XXX:=== TESTS OK ==:XXX");
    }

    static void assertEquals(String a, String b)
    {
        if (!a.equals(b))
        {
            Log.e(TAG, "ERROR:assertEquals" + " " + Log.getCaller());
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
}

