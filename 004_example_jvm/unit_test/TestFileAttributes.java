import info.guardianproject.iocipher.File;

public class TestFileAttributes {
    public static void run() {
        System.out.println("\n--- Test: File Attributes ---");
        try {
            File f = new File("/attributes_test.txt");
            if (f.exists()) f.delete();
            f.createNewFile();

            // Default permissions (IOCipher always returns true for these as permissions aren't enforced)
            IOCipherUnitTest.assertCondition("File is readable by default", f.canRead());
            IOCipherUnitTest.assertCondition("File is writable by default", f.canWrite());
            IOCipherUnitTest.assertCondition("File is executable by default", f.canExecute());

            // lastModified
            long time1 = f.lastModified();
            IOCipherUnitTest.assertCondition("lastModified is > 0 after creation", time1 > 0);
            
            // IOCipher's SQLite backend does not support setting last modified time.
            // The method returns false and the time remains unchanged.
            long customTime = 1000000000000L; // Sep 9, 2001
            boolean setTimeResult = f.setLastModified(customTime);
            IOCipherUnitTest.assertCondition("setLastModified returns false (not supported)", !setTimeResult);
            IOCipherUnitTest.assertCondition("lastModified remains unchanged", f.lastModified() == time1);

            // setReadOnly
            // IOCipher may return true for setReadOnly but doesn't actually enforce read-only permissions.
            // canWrite() will still return true because the VFS doesn't track permission bits.
            boolean readOnlyResult = f.setReadOnly();
            IOCipherUnitTest.assertCondition("setReadOnly call succeeds (no-op)", readOnlyResult);
            IOCipherUnitTest.assertCondition("File is still writable (permissions not enforced)", f.canWrite());
            IOCipherUnitTest.assertCondition("File is still readable after setReadOnly", f.canRead());

            f.delete();
        } catch (Exception e) {
            IOCipherUnitTest.assertCondition("File Attributes test failed with exception", false);
            e.printStackTrace();
        }
    }
}
