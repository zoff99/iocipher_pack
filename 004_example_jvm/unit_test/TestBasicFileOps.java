import info.guardianproject.iocipher.File;

public class TestBasicFileOps {
    public static void run() {
        System.out.println("\n--- Test: Basic File Operations ---");
        try {
            File f = new File("/testfile.txt");
            if (f.exists()) {
                f.delete();
            }
            
            boolean created = f.createNewFile();
            IOCipherUnitTest.assertCondition("File creation returns true", created);
            IOCipherUnitTest.assertCondition("File exists after creation", f.exists());
            IOCipherUnitTest.assertCondition("Created object is a file", f.isFile());
            IOCipherUnitTest.assertCondition("Created object is not a directory", !f.isDirectory());
            IOCipherUnitTest.assertCondition("File length is 0 initially", f.length() == 0);

            f.delete();
            IOCipherUnitTest.assertCondition("File deleted successfully", !f.exists());

        } catch (Exception e) {
            IOCipherUnitTest.assertCondition("Basic File Ops test failed with exception", false);
            e.printStackTrace();
        }
    }
}
