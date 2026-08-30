import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileOutputStream;
import info.guardianproject.iocipher.FileInputStream;

public class TestFileAppending {
    public static void run() {
        System.out.println("\n--- Test: File Appending ---");
        try {
            File f = new File("/append_test.txt");
            if (f.exists()) f.delete();

            String part1 = "Hello ";
            String part2 = "World!";

            FileOutputStream fos1 = new FileOutputStream(f);
            fos1.write(part1.getBytes("UTF-8"));
            fos1.close();

            FileOutputStream fos2 = new FileOutputStream(f, true); // append = true
            fos2.write(part2.getBytes("UTF-8"));
            fos2.close();

            IOCipherUnitTest.assertCondition("File length matches total bytes", f.length() == (part1 + part2).getBytes("UTF-8").length);

            FileInputStream fis = new FileInputStream(f);
            byte[] buffer = new byte[(int) f.length()];
            fis.read(buffer);
            fis.close();

            String readContent = new String(buffer, "UTF-8");
            IOCipherUnitTest.assertCondition("Appended content matches", (part1 + part2).equals(readContent));

            f.delete();
        } catch (Exception e) {
            IOCipherUnitTest.assertCondition("File Appending test failed with exception", false);
            e.printStackTrace();
        }
    }
}
