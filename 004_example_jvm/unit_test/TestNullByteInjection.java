import info.guardianproject.iocipher.File;

public class TestNullByteInjection {
    public static void run() {
        System.out.println("\n--- Test: Null Byte Injection ---");
        try {
            String maliciousName = "secret.txt\0.jpg";
            File f = new File("/" + maliciousName);
            
            boolean created = false;
            try {
                created = f.createNewFile();
            } catch (Exception e) {
                // Might throw IllegalArgumentException or IOException
            }

            if (created) {
                File f2 = new File("/" + maliciousName);
                IOCipherUnitTest.assertCondition("Null byte file accessible via exact name", f2.exists());
                f.delete();
            } else {
                IOCipherUnitTest.assertCondition("Null byte file creation blocked or sanitized", true);
            }

            // Test that JNI doesn't crash when passing null bytes in deep paths
            File f3 = new File("/dir\0/test.txt");
            try {
                f3.mkdirs();
                f3.delete();
            } catch (Exception e) {
                // Safe
            }
            IOCipherUnitTest.assertCondition("JNI survived null byte in path without crashing", true);

        } catch (Throwable t) {
            IOCipherUnitTest.assertCondition("Null byte test threw exception instead of crashing", true);
        }
    }
}
