import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileOutputStream;

public class TestOpenFileDeleteParent {
    public static void run() {
        System.out.println("\n--- Test: Open File Handle + Delete Parent ---");
        File dir = new File("/orphan_dir");
        File f = new File(dir, "orphan.txt");
        
        try {
            dir.mkdirs();
            f.createNewFile();
            
            FileOutputStream fos = new FileOutputStream(f);
            
            // Delete the file and directory while stream is OPEN
            f.delete();
            dir.delete(); 
            
            // Try to write to the orphaned stream
            try {
                fos.write("Hello orphan!".getBytes());
                fos.flush();
            } catch (Exception e) {
                // Expected: IOException
            }
            
            IOCipherUnitTest.assertCondition("JVM survived writing to deleted file", true);
            fos.close();
            
        } catch (Throwable t) {
            IOCipherUnitTest.assertCondition("Orphan file test threw exception instead of crashing", true);
        } finally {
            new File("/orphan_dir/orphan.txt").delete();
            new File("/orphan_dir").delete();
        }
    }
}
