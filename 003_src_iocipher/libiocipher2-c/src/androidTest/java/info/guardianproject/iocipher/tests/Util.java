package info.guardianproject.iocipher.tests;

import android.app.Instrumentation;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Formatter;
import java.util.Random;

public class Util {
    private final static String TAG = "Util";

    static String randomFileName(File root, String testName) {
        String name = null;
        do {
            name = root.getAbsolutePath()
                    + "/"
                    + testName
                    + "."
                    + Integer.toString((int) (Math.random() * Integer.MAX_VALUE));
        } while ((new File(name)).exists());
        return name;
    }

    static String toHex(byte[] digest) {
        Formatter formatter = new Formatter();
        for (byte b : digest) {
            formatter.format("%02x", b);
        }
        String ret = formatter.toString();
        formatter.close();
        return ret;
    }

    static boolean nativeWriteRandomBytes(int bytes, String filename) {
        try {
            File f = new File(filename);
            FileOutputStream out = new FileOutputStream(f);

            Random prng = new Random();
            byte[] random_buf = new byte[bytes];
            prng.nextBytes(random_buf);

            out.write(random_buf);
            out.close();

        } catch (IOException e) {
            Log.e(TAG, e.getCause().toString());
            return false;
        }
        return true;
    }

    static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    static byte[] cipherWriteRandomByteReturnBuf(int bytes, String filename) {
        byte[] random_buf;
        try {
            info.guardianproject.iocipher.File f = new info.guardianproject.iocipher.File(filename);
            info.guardianproject.iocipher.FileOutputStream out = new info.guardianproject.iocipher.FileOutputStream(f);

            Random prng = new Random();
            random_buf = new byte[bytes];
            prng.nextBytes(random_buf);

            out.write(random_buf);
            out.close();

        } catch (IOException e) {
            Log.e(TAG, e.getCause().toString());
            return null;
        }
        return random_buf;
    }

    static boolean cipherWriteRandomBytes(int bytes, String filename) {
        try {
            info.guardianproject.iocipher.File f = new info.guardianproject.iocipher.File(filename);
            info.guardianproject.iocipher.FileOutputStream out = new info.guardianproject.iocipher.FileOutputStream(f);

            Random prng = new Random();
            byte[] random_buf = new byte[bytes];
            prng.nextBytes(random_buf);

            out.write(random_buf);
            out.close();

        } catch (IOException e) {
            Log.e(TAG, e.getCause().toString());
            return false;
        }
        return true;
    }

    static void deleteDirectory(File dir) {
        for (File child : dir.listFiles()) {
            if (child.isDirectory()) {
                deleteDirectory(child);
                child.delete();
            } else
                child.delete();
        }
        dir.delete();
    }

    /**
     * Prefer internal over external storage, because external tends to be FAT filesystems,
     * which don't support symlinks (which we test using this method).
     */
    static java.io.File getWriteableDir(Instrumentation instrumentation) {
        Context context = instrumentation.getContext();
        Context targetContext = instrumentation.getTargetContext();

        java.io.File[] dirsToTry = new java.io.File[]{
                context.getCacheDir(),
                context.getFilesDir(),
                targetContext.getCacheDir(),
                targetContext.getFilesDir(),
                context.getExternalCacheDir(),
                context.getExternalFilesDir(null),
                targetContext.getExternalCacheDir(),
                targetContext.getExternalFilesDir(null),
                Environment.getExternalStorageDirectory(),
        };

        return getWriteableDir(dirsToTry);
    }

    private static java.io.File getWriteableDir(java.io.File[] dirsToTry) {
        for (java.io.File dir : dirsToTry) {
            if (dir != null && dir.canWrite()) {
                return dir;
            }
        }

        return null;
    }
}
