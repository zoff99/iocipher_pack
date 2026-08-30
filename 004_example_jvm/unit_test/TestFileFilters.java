import info.guardianproject.iocipher.File;
// IMPORTANT: Use IOCipher's specific interfaces, not java.io.*
import info.guardianproject.iocipher.FilenameFilter;
import info.guardianproject.iocipher.FileFilter;

public class TestFileFilters {
    public static void run() {
        System.out.println("\n--- Test: File Filters ---");
        try {
            File dir = new File("/filter_test");
            dir.mkdirs();
            
            new File("/filter_test/file1.txt").createNewFile();
            new File("/filter_test/file2.txt").createNewFile();
            new File("/filter_test/file3.log").createNewFile();
            new File("/filter_test/file4.dat").createNewFile();

            FilenameFilter txtFilter = new FilenameFilter() {
                // Parameter MUST be info.guardianproject.iocipher.File
                public boolean accept(File d, String name) {
                    return name.endsWith(".txt");
                }
            };

            String[] list = dir.list(txtFilter);
            IOCipherUnitTest.assertCondition("FilenameFilter finds exactly 2 txt files", list != null && list.length == 2);

            FileFilter logFilter = new FileFilter() {
                // Parameter MUST be info.guardianproject.iocipher.File
                public boolean accept(File f) {
                    return f.isFile() && f.getName().endsWith(".log");
                }
            };

            File[] files = dir.listFiles(logFilter);
            IOCipherUnitTest.assertCondition("FileFilter finds exactly 1 log file", files != null && files.length == 1);

            // Cleanup
            new File("/filter_test/file1.txt").delete();
            new File("/filter_test/file2.txt").delete();
            new File("/filter_test/file3.log").delete();
            new File("/filter_test/file4.dat").delete();
            dir.delete();
        } catch (Exception e) {
            IOCipherUnitTest.assertCondition("File Filters test failed with exception", false);
            e.printStackTrace();
        }
    }
}
