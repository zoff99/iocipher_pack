import info.guardianproject.iocipher.File;

public class TestSpecialCharactersInPaths {
    public static void run() {
        System.out.println("\n--- Test: Special Characters in Paths ---");
        try {
            String weirdDirName = "weird n@mes! 🚀 #1?";
            File dir = new File("/" + weirdDirName);
            if (dir.exists()) {
                File[] existing = dir.listFiles();
                if (existing != null) {
                    for (File f : existing) f.delete();
                }
                dir.delete();
            }
            
            boolean mkdirResult = dir.mkdir();
            IOCipherUnitTest.assertCondition("Directory with emojis and symbols created", mkdirResult && dir.exists());

            String weirdFileName = "file with spaces.txt";
            File file = new File(dir, weirdFileName);
            boolean createResult = file.createNewFile();
            IOCipherUnitTest.assertCondition("File with spaces created", createResult && file.exists());

            // Cleanup
            file.delete();
            dir.delete();
            IOCipherUnitTest.assertCondition("Special characters files cleaned up", !dir.exists());

        } catch (Exception e) {
            IOCipherUnitTest.assertCondition("Special Characters test failed with exception", false);
            e.printStackTrace();
        }
    }
}
