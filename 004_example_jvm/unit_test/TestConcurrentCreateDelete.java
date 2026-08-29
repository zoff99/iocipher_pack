import info.guardianproject.iocipher.File;
import java.util.concurrent.atomic.AtomicBoolean;

public class TestConcurrentCreateDelete {
    public static void run() {
        System.out.println("\n--- Test: Concurrent Create/Delete Threading ---");
        String filePath = "/flicker_file.txt";
        AtomicBoolean running = new AtomicBoolean(true);
        AtomicBoolean crash = new AtomicBoolean(false);

        Thread creator = new Thread(() -> {
            try {
                while (running.get()) {
                    File f = new File(filePath);
                    if (f.exists()) f.delete(); else f.createNewFile();
                    Thread.sleep(5);
                }
            } catch (Exception e) {}
        });

        Thread accessor = new Thread(() -> {
            try {
                // Target the parent directory for listFiles(), not the file itself
                File parentDir = new File("/");
                
                while (running.get()) {
                    File f = new File(filePath);
                    try {
                        f.exists(); 
                        f.length(); 
                        
                        // Call listFiles on the DIRECTORY. 
                        // This safely tests SQLite's concurrent directory iteration.
                        parentDir.listFiles(); 
                    } catch (Exception e) {}
                    Thread.sleep(2);
                }
            } catch (Exception e) { crash.set(true); }
        });

        creator.start();
        accessor.start();

        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        running.set(false);
        try { creator.join(1000); accessor.join(1000); } catch (InterruptedException e) {}

        IOCipherUnitTest.assertCondition("Concurrent Create/Delete survived without JNI crash", !crash.get());
        new File(filePath).delete();
    }
}
