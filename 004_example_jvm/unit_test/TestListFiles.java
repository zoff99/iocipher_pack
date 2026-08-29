import info.guardianproject.iocipher.File;

public class TestListFiles {
    public static void run() {
        System.out.println("\n--- Test: List Files ---");
        try {
            File dir = new File("/listdir");
            if (dir.exists()) {
                File[] existing = dir.listFiles();
                if (existing != null) {
                    for (File f : existing) {
                        f.delete();
                    }
                }
                dir.delete();
            }
            dir.mkdir();

            new File("/listdir/file1.txt").createNewFile();
            new File("/listdir/file2.txt").createNewFile();
            new File("/listdir/file3.txt").createNewFile();
            new File("/listdir/subdir").mkdir();

            File[] files = dir.listFiles();
            IOCipherUnitTest.assertCondition("listFiles returns correct number of items", files != null && files.length == 4);
            
            // Manual filter check to avoid FilenameFilter lambda issues in different Java versions
            int txtCount = 0;
            if (files != null) {
                for (File f : files) {
                    if (f.getName().endsWith(".txt")) {
                        txtCount++;
                    }
                }
            }
            IOCipherUnitTest.assertCondition("Count of text files is 3", txtCount == 3);

            // Cleanup
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
            dir.delete();
            IOCipherUnitTest.assertCondition("Directory cleaned up and deleted", !dir.exists());

        } catch (Exception e) {
            IOCipherUnitTest.assertCondition("List Files test failed with exception", false);
            e.printStackTrace();
        }
    }
}
