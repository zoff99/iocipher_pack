package com.example;

import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.VirtualFileSystem;

import static info.guardianproject.iocipher.VirtualFileSystem.IOCIPHER_JNI_VERSION;

public class iociphertest
{
    private final static String TAG = "VirtualFileSystemTest";

    private static VirtualFileSystem vfs;
    private static String path;
    private static String goodPassword = "this is the right password";
    private String badPassword = "this soooo not the right password, its wrong";
    private byte[] goodKey = {
            (byte) 0x2a, (byte) 0xfc, (byte) 0x69, (byte) 0xa1, (byte) 0x16, (byte) 0x40,
            (byte) 0x4f, (byte) 0x7d, (byte) 0x7f, (byte) 0x1b, (byte) 0x1d, (byte) 0xb9,
            (byte) 0x5e, (byte) 0x18, (byte) 0x11, (byte) 0x2e, (byte) 0x6b, (byte) 0x3c,
            (byte) 0xf7, (byte) 0x1e, (byte) 0x78, (byte) 0xaf, (byte) 0x88, (byte) 0x3c,
            (byte) 0xb1, (byte) 0x90, (byte) 0x51, (byte) 0x15, (byte) 0xbf, (byte) 0xc3,
            (byte) 0xb2, (byte) 0x8d,
    };
    private byte[] tooLongKey = {
            (byte) 0x2a, (byte) 0xfc, (byte) 0x69, (byte) 0xa1, (byte) 0x16, (byte) 0x40,
            (byte) 0x4f, (byte) 0x7d, (byte) 0x7f, (byte) 0x1b, (byte) 0x1d, (byte) 0xb9,
            (byte) 0x5e, (byte) 0x18, (byte) 0x11, (byte) 0x2e, (byte) 0x6b, (byte) 0x3c,
            (byte) 0xf7, (byte) 0x1e, (byte) 0x78, (byte) 0xaf, (byte) 0x88, (byte) 0x3c,
            (byte) 0xb1, (byte) 0x90, (byte) 0x51, (byte) 0x15, (byte) 0xbf, (byte) 0xc3,
            (byte) 0xb2, (byte) 0x8d, (byte) 0x00
    };
    private byte[] tooShortKey = {
            (byte) 0x2a, (byte) 0xfc, (byte) 0x69, (byte) 0xa1, (byte) 0x16, (byte) 0x40,
            (byte) 0x4f, (byte) 0x7d, (byte) 0x7f, (byte) 0x1b, (byte) 0x1d, (byte) 0xb9,
            (byte) 0x5e, (byte) 0x18, (byte) 0x11, (byte) 0x2e, (byte) 0x6b, (byte) 0x3c,
            (byte) 0xf7, (byte) 0x1e, (byte) 0x78, (byte) 0xaf, (byte) 0x88, (byte) 0x3c,
            (byte) 0xb1, (byte) 0x90, (byte) 0x51, (byte) 0x15, (byte) 0xbf, (byte) 0xc3,
    };
    private byte[] badKey = {
            'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B',
            'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B',
    };

    public static void setUp() {
        path = "./test.db";
        java.io.File db = new java.io.File(path);
        if (db.exists())
            db.delete();
        vfs = VirtualFileSystem.get();
    }

    public static void tearDown() {
        if (vfs.isMounted()) {
            vfs.unmount();
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
        assertEquals(vfs.iocipherVersion(), IOCIPHER_JNI_VERSION);
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

    public static void main(String[] args)
    {
        // -------------------
        System.out.println("-test-");
        // -------------------
        setUp();
        // -------------------
        testVersionSqlfs();
        testVersionIOcipher();
        testVersionIOjnicipher();
        // -------------------
        testInitMountUnmount();

        // -------------------
        tearDown();
        // -------------------
    }

    static void assertEquals(String a, String b)
    {
        /*
        if (a.compareTo(b) == XXXXX)
        {
            Log.e(TAG, "ERROR:assertEquals");
            System.exit(1);
        }
        */
    }

    static void assertNotNull(Object a)
    {
        if (a == null)
        {
            Log.e(TAG, "ERROR:assertNotNull");
            System.exit(1);
        }
    }

    static void assertTrue(boolean b)
    {
        if (!b)
        {
            Log.e(TAG, "ERROR:assertTrue");
            System.exit(1);
        }
    }
}

