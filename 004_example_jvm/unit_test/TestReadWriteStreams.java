import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileInputStream;
import info.guardianproject.iocipher.FileOutputStream;

public class TestReadWriteStreams {
    public static void run() {
        System.out.println("\n--- Test: Read and Write Streams ---");
        File f = new File("/streamtest.txt");
        if (f.exists()) f.delete();

        String content = "Hello IOCipher! 🚀\nLine 2.";
        try {
            // Write
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(content.getBytes("UTF-8"));
            fos.close();

            IOCipherUnitTest.assertCondition("File length matches written bytes", f.length() == content.getBytes("UTF-8").length);

            // Read
            FileInputStream fis = new FileInputStream(f);
            byte[] buffer = new byte[(int) f.length()];
            int bytesRead = fis.read(buffer);
            fis.close();

            String readContent = new String(buffer, "UTF-8");
            IOCipherUnitTest.assertCondition("Read content matches written content", content.equals(readContent));
            IOCipherUnitTest.assertCondition("Bytes read matches file length", bytesRead == f.length());

        } catch (Exception e) {
            IOCipherUnitTest.assertCondition("Read/Write Streams test failed with exception", false);
            e.printStackTrace();
        } finally {
            f.delete();
        }
    }
}
