package com.example.iocipherexampleapp;

import android.content.Context;

import java.io.IOException;

import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.VirtualFileSystem;

@SuppressWarnings("ALL")
public class vfsexample
{
    private VirtualFileSystem vfs = null;
    private String path;
    private final String goodPassword = "this is the right password !?%";
    private final String TAG = "IOCipher-Example:";

    void testme(Context c)
    {
        System.out.println(TAG + "starting ...");

        // define the path where the vfs container file will be located
        path = c.getExternalFilesDir(null).getAbsolutePath() + "/" + "text" + ".db";

        // here we need java.io.* classes since the container file is a "real" file
        java.io.File db = new java.io.File(path);

        // initialize the global vfs object
        vfs = VirtualFileSystem.get();

        // show some version information
        System.out.println(TAG + "sqlfsVersion: " + vfs.sqlfsVersion());
        System.out.println(TAG + "iocipherVersion: " + vfs.iocipherVersion());
        System.out.println(TAG + "iocipherJNIVersion: " + vfs.iocipherJNIVersion());

        // set the path to the vfs container file
        vfs.setContainerPath(path);

        // create the vfs container file
        vfs.createNewContainer(goodPassword);

        // mount the vfs container file with password
        vfs.mount(goodPassword);

        // show status of the mount command
        if (vfs.isMounted()) {
            System.out.println(TAG + "vfs is mounted");
        } else {
            System.out.println(TAG + "vfs is NOT mounted");
        }



        // now create a virtual directory
        File d = new File("/test");
        boolean res = d.mkdir();
        System.out.println(TAG + "create a virtual directory: " + res);

        // now create a text file
        File f1 = new File("/sometext.txt");
        try
        {
            boolean res1 = f1.createNewFile();
            System.out.println(TAG + "create a text file: " + res1);
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
            }
            else
            {
                System.out.println(TAG + "File: " + f);
            }
        }




        // unmount the vfs container file
        if (vfs.isMounted()) {
            vfs.unmount();
            System.out.println(TAG + "vfs is UN-mounted");
        }

        // all finished
        System.out.println(TAG + "finished.");
    }
}
