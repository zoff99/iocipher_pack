import info.guardianproject.iocipher.File;

public class TestRenameAndDelete {
    public static void run() {
        System.out.println("\n--- Test: Rename and Delete ---");
        try {
            File original = new File("/original.txt");
            File renamed = new File("/renamed.txt");
            
            if (original.exists()) original.delete();
            if (renamed.exists()) renamed.delete();

            original.createNewFile();
            IOCipherUnitTest.assertCondition("Original file created", original.exists());

            boolean renameResult = original.renameTo(renamed);
            IOCipherUnitTest.assertCondition("renameTo returns true", renameResult);
            IOCipherUnitTest.assertCondition("Original file no longer exists", !original.exists());
            IOCipherUnitTest.assertCondition("Renamed file exists", renamed.exists());

            boolean deleteResult = renamed.delete();
            IOCipherUnitTest.assertCondition("delete returns true", deleteResult);
            IOCipherUnitTest.assertCondition("Renamed file no longer exists", !renamed.exists());

        } catch (Exception e) {
            IOCipherUnitTest.assertCondition("Rename and Delete test failed with exception", false);
            e.printStackTrace();
        }
    }
}
