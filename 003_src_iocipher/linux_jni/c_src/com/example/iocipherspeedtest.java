package com.example;

import java.util.Arrays;
import java.util.Random;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.VirtualFileSystem;

import static info.guardianproject.iocipher.VirtualFileSystem.IOCIPHER_JNI_VERSION;

public class iocipherspeedtest
{
    private final static String TAG = "SpeedTest";

    private static VirtualFileSystem vfs;
    private static String path;
    final static String dbfilename = "./speedtest.db";
    final static String goodPassword = "this is the right password";

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

    public static void testSpeed() {
        vfs.setContainerPath(path);
        vfs.createNewContainer(goodPassword);
        vfs.mount(goodPassword);
        if (vfs.isMounted()) {
            Log.i(TAG, "vfs is mounted");
        } else {
            Log.i(TAG, "vfs is NOT mounted");
        }
        assertTrue(vfs.isMounted());
        // -------------------
        try
        {
            final String filename = "/speedtest01";
            final long one_kb = 1024;
            final long one_mb = 1024 * one_kb;
            final long one_gb = 1024 * one_mb;
            // --------------------------------------
            final int bytes = (int)(8 * one_kb * 120);
            final long full_filesize = one_gb * 8;
            // --------------------------------------
            final int loops = (int)(full_filesize / (long)bytes);
            info.guardianproject.iocipher.File f = new info.guardianproject.iocipher.File(filename);
            info.guardianproject.iocipher.FileOutputStream out = new info.guardianproject.iocipher.FileOutputStream(f);
            MessageDigest md_write = MessageDigest.getInstance("MD5");
            Random prng = new Random();
            byte[] random_buf = new byte[bytes];
            prng.nextBytes(random_buf);
        
            long startTime = System.nanoTime(); 
            int i=0;
            for (i=0;i<loops;i++)
            {
                out.write(random_buf);
                //HASH slows down//md_write.update(random_buf);
                if ((i % 500) == 0)
                {
                    try
                    {
                        long bytescount = (long)i * (long)bytes;
                        long endTime = System.nanoTime();
                        float bytesPerSec = bytescount / ((System.nanoTime() - startTime) / 1000000000);
                        float kbPerSec = bytesPerSec / one_kb;
                        System.out.println(String.format("%.1f", kbPerSec) + " KBps : size MB=" + (float)(bytescount / one_mb));
                        // HINT: update random bytes only a few times. otherwise it slows down write speed
                        //       and we want to measure the max speed possible.
                        prng.nextBytes(random_buf);
                    }
                    catch(Exception e)
                    {
                    }
                }
            }
            out.flush();
            out.close();
            long bytescount = (long)i * (long)bytes;
            long endTime = System.nanoTime();
            float bytesPerSec = bytescount / (1 + ((System.nanoTime() - startTime) / 1000000000));
            float kbPerSec = bytesPerSec / one_kb;
            System.out.println(String.format("%.1f", kbPerSec) + " KBps : size MB=" + (float)(bytescount / one_mb));


            byte[] digest = md_write.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest)
            {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            //HASH slows down//System.out.println("Hash: " + hexString.toString());


            System.out.println("" + ((System.nanoTime() - startTime) / 1000000000) + " seconds write");


            // ------------ now read the vfs file ------------

            info.guardianproject.iocipher.File fin = new info.guardianproject.iocipher.File(filename);
            info.guardianproject.iocipher.FileInputStream in = new info.guardianproject.iocipher.FileInputStream(fin);
            MessageDigest md_read = MessageDigest.getInstance("MD5");
            byte[] bytebuf_in = new byte[bytes];
            long startTime2 = System.nanoTime();
            int i2 = 0;
            int nRead = 0;
            bytescount = 0;
            while ((nRead = in.read(bytebuf_in, 0, bytebuf_in.length)) != -1)
            {
                i2++;
                //HASH slows down//md_read.update(bytebuf_in);
                bytescount = bytescount + (long)nRead;
                if ((i2 % 500) == 0)
                {
                    try
                    {
                        // System.out.println("bc="+bytescount);
                        endTime = System.nanoTime();
                        bytesPerSec = bytescount / (1 + ((System.nanoTime() - startTime2) / 1000000000));
                        kbPerSec = bytesPerSec / one_kb;
                        System.out.println(String.format("%.1f", kbPerSec) + " KBps : size MB=" + (float)(bytescount / one_mb));
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }

            in.close();


            byte[] digest2 = md_read.digest();
            StringBuilder hexString2 = new StringBuilder();
            for (byte b : digest2)
            {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString2.append('0');
                hexString2.append(hex);
            }
            //HASH slows down//System.out.println("Hash: " + hexString2.toString());


            System.out.println("" + ((System.nanoTime() - startTime2) / 1000000000) + " seconds read");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.e(TAG, e.getCause().toString());
            fail();
        }
        // -------------------
        vfs.unmount();
    }

    public static void main(String[] args)
    {
        // -------------------
        System.out.println("-test-");

        // -------------------
        setUp();
        testVersionSqlfs();
        testVersionIOcipher();
        testVersionIOjnicipher();
        // -------------------
        testSpeed();
        // -------------------
        tearDown();
        // -------------------
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

