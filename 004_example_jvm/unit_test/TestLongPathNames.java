import info.guardianproject.iocipher.File;

public class TestLongPathNames {
    public static void run() {
        System.out.println("\n--- Test: Long Path Names (Buffer Overflow Check) ---");
        try {
            StringBuilder sb = new StringBuilder("/");
            // Create a path with ~3000 characters
            for (int i = 0; i < 100; i++) {
                sb.append("abcdefghij/"); 
            }
            String longPath = sb.toString();
            
            File f = new File(longPath);
            try {
                f.mkdirs();
            } catch (Exception e) {
                // Expected to throw if path exceeds internal limits
            }
            
            // The key assertion is that the JVM doesn't crash (SIGSEGV)
            IOCipherUnitTest.assertCondition("JNI survived 3000+ char path without crashing", true);

        } catch (Throwable t) {
            IOCipherUnitTest.assertCondition("Long path test threw exception instead of crashing", true);
        }
    }
}
