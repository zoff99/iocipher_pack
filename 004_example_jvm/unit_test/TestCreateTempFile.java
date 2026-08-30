import info.guardianproject.iocipher.File;

public class TestCreateTempFile {
    public static void run() {
        System.out.println("\n--- Test: Create Temp File ---");
        try {
            File tempDir = new File("/temp_dir");
            tempDir.mkdirs();

            File temp1 = File.createTempFile("prefix_", "_suffix", tempDir);
            IOCipherUnitTest.assertCondition("Temp file 1 created", temp1.exists());
            IOCipherUnitTest.assertCondition("Temp file 1 starts with prefix", temp1.getName().startsWith("prefix_"));
            IOCipherUnitTest.assertCondition("Temp file 1 ends with suffix", temp1.getName().endsWith("_suffix"));
            IOCipherUnitTest.assertCondition("Temp file 1 parent is tempDir", temp1.getParentFile().equals(tempDir));

            File temp2 = File.createTempFile("mytemp_", ".log", tempDir);
            IOCipherUnitTest.assertCondition("Temp file 2 created in dir", temp2.exists());
            IOCipherUnitTest.assertCondition("Temp file 2 ends with .log", temp2.getName().endsWith(".log"));

            temp1.delete();
            temp2.delete();
            tempDir.delete();
        } catch (Exception e) {
            IOCipherUnitTest.assertCondition("Create Temp File test failed with exception", false);
            e.printStackTrace();
        }
    }
}
