/*
 *
 * IOCipher Linux Java minal example
 * (C) Zoff in 2024
 *
 */

import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileInputStream;
import info.guardianproject.iocipher.FileOutputStream;
import info.guardianproject.iocipher.IOCipherFileChannel;
import info.guardianproject.iocipher.FileFilter;
import info.guardianproject.iocipher.VirtualFileSystem;

class MinialExample {

    public static final String APP_TITLE = "IOCipher MinimalExample";
    private static final String VERSION = "1.0.0";

    private static VirtualFileSystem vfs;
    private static String vfspath;
    private static String dbfilename = "./min_example.db";
    private static String goodPassword = ""; // "secure pass!?§$";

    private static String OS = System.getProperty("os.name").toLowerCase();
    private static boolean isWindows() {
        return OS.contains("win");
    }

    public static void setUp() {
        vfs = VirtualFileSystem.get();
        vfs.setContainerPath(dbfilename);
        vfs.createNewContainer(goodPassword);
        vfs.mount(goodPassword);
        if (vfs.isMounted()) {
            System.out.println("vfs is mounted");
        } else {
            System.out.println("vfs is NOT mounted");
        }
    }

    public static void tearDown() {
        try {
            if (vfs.isMounted()) {
                System.out.println("unmounting vfs");
                vfs.unmount();
                System.out.println("vfs is unmounted");
            }
        } catch (Exception e) {
            System.out.println("tearDown Exception");
            e.printStackTrace();
        }
        System.out.println("tearDown finished");
    }

    public static void create_dummies()
    {
        try
        {
            info.guardianproject.iocipher.File f1 = new info.guardianproject.iocipher.File("/Images");
            f1.mkdirs();
            f1 = new info.guardianproject.iocipher.File("/Documents");
            f1.mkdirs();
            f1 = new info.guardianproject.iocipher.File("/Music");
            f1.mkdirs();
            f1 = new info.guardianproject.iocipher.File("/listy zakupów");
            f1.mkdirs();
            f1 = new info.guardianproject.iocipher.File("/My Shopping.docx");
            f1.createNewFile();
            info.guardianproject.iocipher.RandomAccessFile fra = new info.guardianproject.iocipher.RandomAccessFile(f1, "rw");
            fra.write(new byte[45847]);
            fra.close();

            for (int i=0;i<30;i++)
            {
                f1 = new info.guardianproject.iocipher.File("/Photo_" + i + ".png");
                f1.createNewFile();
                fra = new info.guardianproject.iocipher.RandomAccessFile(f1, "rw");
                fra.write(new byte[25841 + (int) (Math.random() * 4096)]);
                fra.close();
            }
        }
        catch(Exception e)
        {
        }
    }

    public static void main(String[] args) {

        System.out.println("" + APP_TITLE + "version: " + VERSION);
        System.out.println("setup db ...");
        setUp();
        System.out.println("setup db ... OK");
        System.out.println("sqlfs version: " + vfs.sqlfsVersion());
        create_dummies();
        tearDown();
    }
}
