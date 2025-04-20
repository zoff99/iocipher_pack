package com.example.iocipherexampleapp;

import android.content.Context;
import android.os.Build;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileReader;
import info.guardianproject.iocipher.FileWriter;
import info.guardianproject.iocipher.VirtualFileSystem;

@SuppressWarnings("ALL")
public class vfsexample
{
    private VirtualFileSystem vfs = null;
    private String path;
    private final String goodPassword = "this is the right password !?%";
    private static final String TAG = "IOCipher-Example:";
    private static String ret = "";

    private static final String FILE_NAME = "/testfile.txt";
    private static final int THREAD_COUNT = 10;
    private static final int WRITE_DATA_MULTIPLIER = 5;
    private static final String data = "powkrwerjijwefj09jf09j09a09w3j09fdj09ewjfjefjiyjdäsaqwie2qieiqeip2qiepiü9qeiüqwieiqüeriq2eiü0peq";

    static class FileTask implements Runnable {
        private final Random random = new Random();

        @Override
        public void run() {
            try {
                //if (random.nextBoolean()) {
                //    writeFile();
                //} else {
                    readFile();
                //}
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void readFile() throws IOException {
            try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
                while (reader.readLine() != null) {
                    // Reading file
                }
                System.out.println(TAG + "reading from thread: " + Thread.currentThread().getName());
            }
        }
    }

    private void writeFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_NAME, true))) {
            for (int i = 0; i < WRITE_DATA_MULTIPLIER; i++)
            {
                writer.write(data + "\n");
            }
            // System.out.println(TAG + "Writing from thread: " + Thread.currentThread().getName());
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    private void heavy_load_test(VirtualFileSystem vfs)
    {
        long startTime = System.currentTimeMillis();

        File fw = new File(FILE_NAME);
        fw.delete();
        writeFile();

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.execute(new FileTask());
        }
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long endTime = System.currentTimeMillis();
        System.out.println(TAG + "Elapsed time: " + (endTime - startTime) + " ms");
        ret = ret + "\n" + "Elapsed time: " + (endTime - startTime) + " ms";
    }

    String testme(Context c)
    {
        long time_start = System.currentTimeMillis();

        try
        {
            System.out.println(TAG + "app version:" + BuildConfig.VERSION_NAME);
            ret = ret + "\n" + "app version:" + BuildConfig.VERSION_NAME;

            System.out.println(TAG + "git hash:" + BuildConfig.GIT_HASH);
            ret = ret + "\n" + "git hash:" + BuildConfig.GIT_HASH;

            System.out.println(TAG + "Android API:" + Build.VERSION.SDK_INT);
            ret = ret + "\n" + "Android API:" + Build.VERSION.SDK_INT;
        }
        catch(Exception e)
        {
            try
            {
                ret = ret + "\n" + "git hash:" + BuildConfig.GIT_HASH;
            }
            catch(Exception ignored)
            {
            }
        }

        System.out.println(TAG + "starting ...");
        ret = ret + "\n" + "starting ...";

        // define the path where the vfs container file will be located
        // path = c.getExternalFilesDir(null).getAbsolutePath() + "/" + "text" + ".db";
        path = c.getFilesDir().getAbsolutePath() + "/" + "text" + ".db";
        System.out.println(TAG + "path: " + path);
        ret = ret + "\n" + "path: " + path;

        // here we need java.io.* classes since the container file is a "real" file
        java.io.File db = new java.io.File(path);

        // initialize the global vfs object
        vfs = VirtualFileSystem.get();

        // show some version information
        System.out.println(TAG + "sqlfsVersion: " + vfs.sqlfsVersion());
        System.out.println(TAG + "iocipherVersion: " + vfs.iocipherVersion());
        System.out.println(TAG + "iocipherJNIVersion: " + vfs.iocipherJNIVersion());

        ret = ret + "\n" + "sqlfsVersion: " + vfs.sqlfsVersion();
        ret = ret + "\n" + "iocipherVersion: " + vfs.iocipherVersion();
        ret = ret + "\n" + "iocipherJNIVersion: " + vfs.iocipherJNIVersion();

        // set the path to the vfs container file
        vfs.setContainerPath(path);

        // create the vfs container file
        vfs.createNewContainer(goodPassword);

        // mount the vfs container file with password
        vfs.mount(goodPassword);

        // show status of the mount command
        if (vfs.isMounted()) {
            System.out.println(TAG + "vfs is mounted");
            ret = ret + "\n" + "vfs is mounted";
        } else {
            System.out.println(TAG + "vfs is NOT mounted");
            ret = ret + "\n" + "vfs is NOT mounted";
        }



        // now create a virtual directory
        File d = new File("/test");
        boolean res = d.mkdir();
        System.out.println(TAG + "create a virtual directory: " + res);
        ret = ret + "\n" + "create a virtual directory: " + res;

        // now create a text file
        File f1 = new File("/sometext.txt");
        try
        {
            boolean res1 = f1.createNewFile();
            System.out.println(TAG + "create a text file: " + res1);
            ret = ret + "\n" + "create a text file: " + res1;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        // now list files in the root directory of the vfs
        File ls = new File("/");
        for (String f : ls.list()) {
            if (new File("/" + f).isDirectory())
            {
                System.out.println(TAG + "DIR : " + f + "/");
                ret = ret + "\n" + "DIR : " + f + "/";
            }
            else
            {
                System.out.println(TAG + "File: " + f);
                ret = ret + "\n" + "File: " + f;
            }
        }


        // now try to do a heavy load test (mutlithreaded)
        heavy_load_test(vfs);

        // unmount the vfs container file
        if (vfs.isMounted()) {
            vfs.unmount();
            System.out.println(TAG + "vfs is UN-mounted");
            ret = ret + "\n" + "vfs is UN-mounted";
        }

        // all finished
        System.out.println(TAG + "finished.");
        ret = ret + "\n" + "finished";

        return ret;
    }
}
