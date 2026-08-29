import info.guardianproject.iocipher.File;

public class TestDirectoryOps {
    public static void run() {
        System.out.println("\n--- Test: Directory Operations ---");
        try {
            File dir = new File("/testdir");
            if (dir.exists()) dir.delete();

            boolean mkdirResult = dir.mkdir();
            IOCipherUnitTest.assertCondition("mkdir returns true", mkdirResult);
            IOCipherUnitTest.assertCondition("Directory exists", dir.exists());
            IOCipherUnitTest.assertCondition("Created object is a directory", dir.isDirectory());
            IOCipherUnitTest.assertCondition("Created object is not a file", !dir.isFile());

            // Test mkdirs for nested directories
            File nestedDir = new File("/parent/child/grandchild");
            boolean mkdirsResult = nestedDir.mkdirs();
            IOCipherUnitTest.assertCondition("mkdirs returns true", mkdirsResult);
            IOCipherUnitTest.assertCondition("Nested directory exists", nestedDir.exists());
            IOCipherUnitTest.assertCondition("Parent directory exists", new File("/parent").exists());

            // Cleanup
            nestedDir.delete();
            new File("/parent/child").delete();
            new File("/parent").delete();
            dir.delete();
            
            IOCipherUnitTest.assertCondition("Directory deleted successfully", !dir.exists());

        } catch (Exception e) {
            IOCipherUnitTest.assertCondition("Directory Ops test failed with exception", false);
            e.printStackTrace();
        }
    }
}
