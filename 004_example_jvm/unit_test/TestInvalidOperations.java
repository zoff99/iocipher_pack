import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileInputStream;
import java.io.FileNotFoundException;

public class TestInvalidOperations {
    public static void run() {
        System.out.println("\n--- Test: Invalid Operations ---");
        try {
            File dir = new File("/invalid_ops_dir");
            dir.mkdirs();

            // 1. FileInputStream on a directory
            boolean fisExceptionThrown = false;
            try {
                new FileInputStream(dir);
            } catch (FileNotFoundException e) {
                fisExceptionThrown = true;
            }
            IOCipherUnitTest.assertCondition("FileInputStream on directory throws FileNotFoundException", fisExceptionThrown);

            // 2. createNewFile where a directory exists
            File sameNameAsDir = new File("/invalid_ops_dir");
            boolean createResult = sameNameAsDir.createNewFile();
            IOCipherUnitTest.assertCondition("createNewFile returns false when directory exists", !createResult);
            IOCipherUnitTest.assertCondition("Directory still exists", sameNameAsDir.isDirectory());

            // 3. delete on non-empty directory
            new File("/invalid_ops_dir/child.txt").createNewFile();
            boolean deleteResult = dir.delete();
            IOCipherUnitTest.assertCondition("delete() returns false for non-empty directory", !deleteResult);
            IOCipherUnitTest.assertCondition("Non-empty directory still exists", dir.exists());

            // 4. deleteOnExit specific to IOCipher
            boolean deleteOnExitExceptionThrown = false;
            try {
                dir.deleteOnExit();
            } catch (UnsupportedOperationException e) {
                deleteOnExitExceptionThrown = true;
            }
            IOCipherUnitTest.assertCondition("deleteOnExit throws UnsupportedOperationException", deleteOnExitExceptionThrown);

            // Cleanup
            new File("/invalid_ops_dir/child.txt").delete();
            dir.delete();
        } catch (Exception e) {
            IOCipherUnitTest.assertCondition("Invalid Operations test failed with exception", false);
            e.printStackTrace();
        }
    }
}
