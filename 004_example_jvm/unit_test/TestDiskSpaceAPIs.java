import info.guardianproject.iocipher.File;

public class TestDiskSpaceAPIs {
    public static void run() {
        System.out.println("\n--- Test: Disk Space APIs ---");
        try {
            File root = new File("/");

            long total = root.getTotalSpace();
            long free = root.getFreeSpace();
            long usable = root.getUsableSpace();

            // IOCipher maps these to host partition stats or dynamic container limits.
            // Free space can exceed the container's current size because it grows dynamically.
            IOCipherUnitTest.assertCondition("Total space >= 0", total >= 0);
            IOCipherUnitTest.assertCondition("Free space >= 0", free >= 0);
            IOCipherUnitTest.assertCondition("Usable space >= 0", usable >= 0);

            // Test on a non-existent file
            File missing = new File("/this_does_not_exist_xyz");
            long missingTotal = missing.getTotalSpace();
            long missingFree = missing.getFreeSpace();
            long missingUsable = missing.getUsableSpace();
            
            // IOCipher returns container/host stats rather than 0 for missing paths.
            // We just verify the calls succeed and return non-negative values.
            IOCipherUnitTest.assertCondition("Missing file total space >= 0", missingTotal >= 0);
            IOCipherUnitTest.assertCondition("Missing file free space >= 0", missingFree >= 0);
            IOCipherUnitTest.assertCondition("Missing file usable space >= 0", missingUsable >= 0);

        } catch (Exception e) {
            IOCipherUnitTest.assertCondition("Disk Space APIs test failed with exception", false);
            e.printStackTrace();
        }
    }
}
