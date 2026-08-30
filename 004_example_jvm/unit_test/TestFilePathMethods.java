import info.guardianproject.iocipher.File;
import java.io.IOException;

public class TestFilePathMethods {
    public static void run() {
        System.out.println("\n--- Test: File Path Methods ---");
        try {
            File dir = new File("/test_paths/dir");
            dir.mkdirs();
            File f = new File("/test_paths/dir/file.txt");
            f.createNewFile();

            IOCipherUnitTest.assertCondition("getName is file.txt", "file.txt".equals(f.getName()));
            IOCipherUnitTest.assertCondition("getPath is correct", "/test_paths/dir/file.txt".equals(f.getPath()));
            IOCipherUnitTest.assertCondition("getParent is /test_paths/dir", "/test_paths/dir".equals(f.getParent()));
            
            File parentFile = f.getParentFile();
            IOCipherUnitTest.assertCondition("getParentFile is not null", parentFile != null);
            IOCipherUnitTest.assertCondition("getParentFile matches parent path", "/test_paths/dir".equals(parentFile.getPath()));

            IOCipherUnitTest.assertCondition("Absolute path returns true", f.isAbsolute());
            File relative = new File("relative.txt");
            IOCipherUnitTest.assertCondition("Relative path returns false", !relative.isAbsolute());

            // Test getCanonicalPath on a standard, existing path
            // We verify the file exists first, then prove that getCanonicalPath() still fails.
            IOCipherUnitTest.assertCondition("File exists before getCanonicalPath check", f.exists());
            
            boolean canonicalThrew = false;
            try {
                f.getCanonicalPath();
            } catch (IOException e) {
                canonicalThrew = true;
            }
            IOCipherUnitTest.assertCondition("getCanonicalPath throws IOException even for existing paths (VFS limitation)", canonicalThrew);

            // IOCipher Quirk: Paths with ".." and "." also fail
            File weirdPath = new File("/test_paths/dir/../dir/./file.txt");
            boolean weirdThrew = false;
            try {
                weirdPath.getCanonicalPath();
            } catch (IOException e) {
                weirdThrew = true;
            }
            IOCipherUnitTest.assertCondition("getCanonicalPath throws IOException for paths with '..' (VFS limitation)", weirdThrew);
            
            // equals and hashCode
            File f2 = new File("/test_paths/dir/file.txt");
            IOCipherUnitTest.assertCondition("equals works for same path", f.equals(f2));
            IOCipherUnitTest.assertCondition("hashCode matches for same path", f.hashCode() == f2.hashCode());

            // compareTo
            File a = new File("/a.txt");
            File b = new File("/b.txt");
            IOCipherUnitTest.assertCondition("compareTo sorts correctly", a.compareTo(b) < 0);

            f.delete();
            dir.delete();
            new File("/test_paths").delete();
        } catch (Exception e) {
            IOCipherUnitTest.assertCondition("File Path Methods test failed with exception", false);
            e.printStackTrace();
        }
    }
}
