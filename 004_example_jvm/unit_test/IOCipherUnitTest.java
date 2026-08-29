import info.guardianproject.iocipher.VirtualFileSystem;
import info.guardianproject.iocipher.File;

/**
 * IOCipher Plain Java Unit Test Runner
 *
 * This harness manages the lifecycle of the IOCipher Virtual File System (VFS)
 * for testing purposes. It mounts the VFS, runs all test suites, and ensures
 * proper unmounting and host-file cleanup.
 */
public class IOCipherUnitTest {
    public static int passed = 0;
    public static int failed = 0;
    public static VirtualFileSystem vfs;
    public static String dbPath = "./unit_test_iocipher.db";
    public static String password = "secure_test_password_123!";

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println(" IOCipher Plain Java Unit Tests");
        System.out.println("========================================\n");

        // Use java.io.File to delete the HOST OS container file before starting
        new java.io.File(dbPath).delete();

        try {
            vfs = VirtualFileSystem.get();
            vfs.setContainerPath(dbPath);
            vfs.createNewContainer(password);
            vfs.mount(password);
            
            if (!vfs.isMounted()) {
                System.out.println("[FATAL] Failed to mount VFS. Aborting tests.");
                System.exit(1);
            }
            System.out.println(">> VFS Mounted successfully.\n");
        } catch (Exception e) {
            System.out.println("[FATAL] Failed to initialize VFS: " + getRootCauseMessage(e));
            e.printStackTrace();
            System.exit(1);
        }

        // =============================================
        // Run all test suites
        // =============================================
        TestBasicFileOps.run();
        TestDirectoryOps.run();
        TestReadWriteStreams.run();
        TestRenameAndDelete.run();
        TestListFiles.run();
        TestSpecialCharactersInPaths.run();
        TestLargeFileReadWrite.run();

        // =============================================
        // Security and Threading Tests
        // =============================================
        TestPathTraversal.run();
        TestNullByteInjection.run();
        TestLongPathNames.run();

        TestConcurrentReadWrite.run();
        TestConcurrentCreateDelete.run();
        TestOpenFileDeleteParent.run();
        TestThreadBombing.run();

        // Shutdown and cleanup
        try {
            if (vfs != null && vfs.isMounted()) {
                vfs.unmount();
            }
        } catch (Exception e) {
            System.out.println("Warning during unmount: " + getRootCauseMessage(e));
        }
        
        // Delete the HOST OS container file after testing
        new java.io.File(dbPath).delete();

        // Print final summary
        System.out.println("\n========================================");
        System.out.println(" TEST SUMMARY");
        System.out.println("========================================");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);

        if (failed > 0) {
            System.exit(1);
        }
    }

    // =========================================================================
    // HELPER: Simple assertion without external libraries
    // =========================================================================
    public static void assertCondition(String testName, boolean condition) {
        if (condition) {
            System.out.println("[PASS] " + testName);
            passed++;
        } else {
            System.out.println("[FAIL] " + testName);
            failed++;
        }
    }

    // =========================================================================
    // HELPER: Get root cause message from an exception chain
    // =========================================================================
    public static String getRootCauseMessage(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() != null ? cause.getMessage() : "";
    }
}
