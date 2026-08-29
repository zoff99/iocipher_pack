import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileInputStream;
import info.guardianproject.iocipher.FileOutputStream;

public class TestLargeFileReadWrite {
    public static void run() {
        System.out.println("\n--- Test: Large File Read and Write ---");
        File f = new File("/largefile.bin");
        if (f.exists()) f.delete();

        int size = 10 * 1024 * 1024; // 10 MB
        byte[] data = new byte[size];
        for (int i = 0; i < size; i++) {
            data[i] = (byte) (i % 256);
        }

        try {
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(data);
            fos.close();

            IOCipherUnitTest.assertCondition("Large file length is correct (10MB)", f.length() == size);

            FileInputStream fis = new FileInputStream(f);
            byte[] readData = new byte[size];
            int bytesRead = 0;
            while (bytesRead < size) {
                int read = fis.read(readData, bytesRead, size - bytesRead);
                if (read == -1) break;
                bytesRead += read;
            }
            fis.close();

            IOCipherUnitTest.assertCondition("Read full 10MB from large file", bytesRead == size);
            
            boolean dataMatches = true;
            for (int i = 0; i < size; i++) {
                if (readData[i] != data[i]) {
                    dataMatches = false;
                    break;
                }
            }
            IOCipherUnitTest.assertCondition("Large file data integrity check", dataMatches);

        } catch (Exception e) {
            IOCipherUnitTest.assertCondition("Large File Read/Write test failed with exception", false);
            e.printStackTrace();
        } finally {
            f.delete();
        }
    }
}
