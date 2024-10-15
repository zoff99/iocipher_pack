import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.Border;
import javax.swing.filechooser.FileSystemView;

import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileFilter;
import info.guardianproject.iocipher.VirtualFileSystem;

/**
This code uses a JList in two forms (layout orientation vertical & horizontal wrap) to
display a File[].  The renderer displays the file icon obtained from FileSystemView.
*/
class FileList
{
    private static VirtualFileSystem vfs;
    private static String path;
    final static String dbfilename = "./vfsmanager.db";
    final static String goodPassword = "super secure password 1$%_?:!";
    private static boolean loaded = false;
    private static String curdir = "/";

    public static void setUp()
    {
        path = dbfilename;
        java.io.File db = new java.io.File(path);
        if (db.exists())
        {
            db.delete();
        }
        vfs = VirtualFileSystem.get();

        vfs.setContainerPath(path);
        vfs.createNewContainer(goodPassword);
        vfs.mount(goodPassword);
        if (vfs.isMounted()) {
            System.out.println("vfs is mounted");
        } else {
            System.out.println("vfs is NOT mounted");
        }
    }

    public static void tearDown()
    {
        try
        {
            if (vfs.isMounted())
            {
                System.out.println("unmounting vfs");
                vfs.unmount();
                System.out.println("vfs is unmounted");
            }
        }
        catch(Exception e)
        {
        }
    }

    public Component getGui(File[] all, boolean detail)
    {
        JList fileList = new JList(all);
        fileList.setCellRenderer(new FileRenderer(detail));
        return new JScrollPane(fileList);
    }

    public static void create_dummies()
    {
        // ----------------------
        File f2 = new File("/dir1/dir2");
        f2.mkdirs();

        try
        {
            File fd = new File("/dummy.txt");
            info.guardianproject.iocipher.FileOutputStream out = new info.guardianproject.iocipher.FileOutputStream(fd);
            out.write(new byte[100]);
            out.close();
        }
        catch(Exception e)
        {
        }
        // ----------------------
    }

    public static void main(String[] args)
    {
        System.out.println("setup db ...");
        setUp();
        System.out.println("setup db ... OK");
        loaded = true;

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                System.out.println("sqlfs version: " + vfs.sqlfsVersion());

                create_dummies();

                File f = new File(curdir);
                FileList fl = new FileList();
                Component c1 = fl.getGui(f.listFiles(new TextFileFilter()), false);
                Component c2 = fl.getGui(f.listFiles(), true);



                JFrame frame = new JFrame("VFS Manager");
                JPanel gui = new JPanel(new BorderLayout());
                gui.add(c1, BorderLayout.WEST);
                gui.add(c2, BorderLayout.CENTER);
                c2.setPreferredSize(new Dimension(375,100));
                gui.setBorder(new EmptyBorder(3,3,3,3));

                frame.setContentPane(gui);
                frame.pack();
                frame.setLocationByPlatform(true);
                frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                WindowListener listener = new WindowAdapter()
                {

                    @Override
                    public void windowClosing(WindowEvent we)
                    {
                        int result = JOptionPane.showConfirmDialog(frame, "Close the application");
                        if (result==JOptionPane.OK_OPTION)
                        {
                            tearDown();
                            frame.setVisible(false);
                            frame.dispose();
                        }
                    }
                };
                frame.addWindowListener(listener);
                frame.setVisible(true);
            }
        });
    }
}

