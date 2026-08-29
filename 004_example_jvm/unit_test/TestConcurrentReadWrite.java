import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileInputStream;
import info.guardianproject.iocipher.FileOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

public class TestConcurrentReadWrite {
    public static void run() {
        System.out.println("\n--- Test: Concurrent Read/Write Threading ---");
        File f = new File("/concurrent_rw.bin");
        if (f.exists()) f.delete();
        
        try {
            f.createNewFile();
            AtomicBoolean running = new AtomicBoolean(true);
            AtomicBoolean error = new AtomicBoolean(false);
            int THREAD_COUNT = 10;
            Thread[] threads = new Thread[THREAD_COUNT];

            for (int i = 0; i < THREAD_COUNT; i++) {
                final int id = i;
                threads[i] = new Thread(() -> {
                    try {
                        if (id % 2 == 0) {
                            FileOutputStream fos = new FileOutputStream(f, true); 
                            byte[] data = ("Thread " + id + "\n").getBytes();
                            for (int j = 0; j < 100 && running.get(); j++) {
                                fos.write(data);
                                Thread.sleep(1);
                            }
                            fos.close();
                        } else {
                            for (int j = 0; j < 100 && running.get(); j++) {
                                FileInputStream fis = new FileInputStream(f);
                                byte[] buffer = new byte[1024];
                                while (fis.read(buffer) != -1) {}
                                fis.close();
                                Thread.sleep(1);
                            }
                        }
                    } catch (Exception e) {
                        error.set(true);
                    }
                });
                threads[i].start();
            }

            for (Thread t : threads) {
                t.join(5000); 
                if (t.isAlive()) {
                    running.set(false);
                    t.interrupt();
                }
            }

            IOCipherUnitTest.assertCondition("Concurrent Read/Write completed without fatal errors", !error.get());

        } catch (Exception e) {
            IOCipherUnitTest.assertCondition("Concurrent Read/Write threw exception", false);
        } finally {
            f.delete();
        }
    }
}
