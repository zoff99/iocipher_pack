package com.example.iocipherexampleapp;

import android.content.Context;
import android.database.Cursor;

import java.io.IOException;

import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.VirtualFileSystem;

import net.zetetic.database.sqlcipher.SQLiteConnection;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import net.zetetic.database.sqlcipher.SQLiteDatabaseHook;
import net.zetetic.database.sqlcipher.SQLiteStatement;

@SuppressWarnings("ALL")
public class vfsexample
{
    private VirtualFileSystem vfs = null;
    private String path;
    private final String goodPassword = "this is the right password !?%";
    private final String TAG = "IOCipher-Example:";
    private static String ret = "";

    String testme(Context c)
    {
        long time_start = System.currentTimeMillis();

        try
        {
            System.out.println(TAG + "app version:" + BuildConfig.VERSION_NAME);
            ret = ret + "\n" + "app version:" + BuildConfig.VERSION_NAME;

            System.out.println(TAG + "git hash:" + BuildConfig.GIT_HASH);
            ret = ret + "\n" + "git hash:" + BuildConfig.GIT_HASH;
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
        ret = "\nstarting ...";

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




        // unmount the vfs container file
        if (vfs.isMounted()) {
            vfs.unmount();
            System.out.println(TAG + "vfs is UN-mounted");
            ret = ret + "\n" + "vfs is UN-mounted";
        }

        // all finished
        System.out.println(TAG + "finished.");
        ret = ret + "\n" + "finished";


        System.loadLibrary("sqlcipher");
        final String pz = "/data/data/com.example.iocipherexampleapp/files/" + "demo.db";
        java.io.File databaseFile = new java.io.File(pz);

        SQLiteDatabaseHook hook = new SQLiteDatabaseHook() {
            public void preKey(SQLiteConnection connection) {}
            public void postKey(SQLiteConnection connection) {}
        };

        SQLiteDatabase.deleteDatabase(databaseFile);
        SQLiteDatabase database = SQLiteDatabase.openOrCreateDatabase(databaseFile, goodPassword, null, null, hook);
        database.enableWriteAheadLogging();
        try
        {
            database.execSQL("create table t1(a, b)");
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            database.execSQL("insert into t1(a, b) values(?, ?)", new Object[]{"row 1 column 1", "row 1 column 2"});
            database.execSQL("insert into t1(a, b) values(?, ?)", new Object[]{"row 2 column 1", "row 2 column 2"});
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        SQLiteStatement st;
        String res1;

        st = database.compileStatement("SELECT sqlite_version()");
        res1 = st.simpleQueryForString();
        ret = ret + "\n" + "sqlite_version: " + res1;

        st = database.compileStatement("SELECT * FROM t1");
        res1 = st.simpleQueryForString();
        ret = ret + "\n" + "" + res1;


        Cursor cursor = database.rawQuery("SELECT * FROM pragma_compile_options;", null);
        cursor.moveToFirst();
        do {
            // ret = ret + "\n" + "ds " + cursor.getString(0);
        } while(cursor.moveToNext());
        cursor.close();

        database.close();

        long time_end = System.currentTimeMillis();

        // all finished
        System.out.println(TAG + "finished (" + (long)((time_end - time_start) / 1000) + " s)");
        ret = ret + "\n" + "finished (" + (long)((time_end - time_start) / 1000) + " s)";

        return ret;
    }
}
