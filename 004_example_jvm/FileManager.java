/*
 *
 * IOCipher Linux Java example
 * (C) Zoff in 2024
 *
 */

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Container;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.*;
import java.awt.image.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.table.*;
import javax.swing.filechooser.FileSystemView;

import javax.imageio.ImageIO;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;

import java.net.URL;

import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileInputStream;
import info.guardianproject.iocipher.FileOutputStream;
import info.guardianproject.iocipher.IOCipherFileChannel;
import info.guardianproject.iocipher.FileFilter;
import info.guardianproject.iocipher.VirtualFileSystem;

/**
 * A basic File Manager. Requires 1.6+ for the Desktop & SwingWorker
 * classes, amongst other minor things.
 * 
 * Includes support classes FileTableModel & FileTreeCellRenderer.
 * 
 * @TODO Bugs
 *       <li>Still throws occasional AIOOBEs and NPEs, so some update on
 *       the EDT must have been missed.
 *       <li>Fix keyboard focus issues - especially when functions like
 *       rename/delete etc. are called that update nodes & file lists.
 *       <li>Needs more testing in general.
 * 
 * @TODO Functionality
 *       <li>Implement Read/Write/Execute checkboxes
 *       <li>Implement Copy
 *       <li>Extra prompt for directory delete (camickr suggestion)
 *       <li>Add File/Directory fields to FileTableModel
 *       <li>Double clicking a directory in the table, should update the tree
 *       <li>Move progress bar?
 *       <li>Add other file display modes (besides table) in CardLayout?
 *       <li>Menus + other cruft?
 *       <li>Implement history/back
 *       <li>Allow multiple selection
 *       <li>Add file search
 * 
 * @author Andrew Thompson
 * @version 2011-06-01
 * @see http://stackoverflow.com/questions/6182110
 * @license LGPL
 */
class FileManager {

    /** Title of the application */
    public static final String APP_TITLE = "VFS Manager";
    /** Used to open/edit/print files. */
    private Desktop desktop;
    /** Provides nice icons and names for files. */
    private FileSystemView fileSystemView;

    /** currently selected File. */
    private info.guardianproject.iocipher.File currentFile = null;
    /** current Directory inside the VFS. */
    private static info.guardianproject.iocipher.File current_vfs_dir = null;

    /** Main GUI container */
    private JPanel gui;

    /** File-system tree. Built Lazily */
    private JTree tree;
    private DefaultTreeModel treeModel;

    /** Directory listing */
    private JTable table;
    private JProgressBar progressBar;
    /** Table model for File[]. */
    private FileTableModel fileTableModel;
    private ListSelectionListener listSelectionListener;
    private boolean cellSizesSet = false;
    private int rowIconPadding = 6;

    /* File controls. */
    private JButton openFile;
    private JButton printFile;
    private JButton editFile;
    private JButton deleteFile;
    private JButton newFile;
    private JButton exportFiles;
    private JButton copyFile;
    /* File details. */
    private JLabel fileName;
    private JTextField path;
    private JLabel date;
    private JLabel size;
    private JCheckBox readable;
    private JCheckBox writable;
    private JCheckBox executable;
    private JRadioButton isDirectory;
    private JRadioButton isFile;

    /* GUI options/containers for new File/Directory creation. Created lazily. */
    private JPanel newFilePanel;
    private JRadioButton newTypeFile;
    private JTextField name;

    private static VirtualFileSystem vfs;
    private static String vfspath;
    private static String dbfilename = "./vfsmanager.db";
    private static String goodPassword = "super secure password 1$%_?:!";
    private static boolean loaded = false;
    private static String curdir = "/";
    private static boolean showcase_mode = false;

    public static void setUp() {
        vfs = VirtualFileSystem.get();
        vfs.setContainerPath(dbfilename);
        vfs.createNewContainer(goodPassword);
        vfs.mount(goodPassword);
        if (vfs.isMounted()) {
            System.out.println("vfs is mounted");
        } else {
            System.out.println("vfs is NOT mounted");
        }
    }

    public static void tearDown() {
        try {
            if (vfs.isMounted()) {
                System.out.println("unmounting vfs");
                vfs.unmount();
                System.out.println("vfs is unmounted");
            }
        } catch (Exception e) {
        }
    }

    public static void create_dummies()
    {
        try
        {
            info.guardianproject.iocipher.File f1 = new info.guardianproject.iocipher.File("/Images");
            f1.mkdirs();
            f1 = new info.guardianproject.iocipher.File("/Documents");
            f1.mkdirs();
            f1 = new info.guardianproject.iocipher.File("/Music");
            f1.mkdirs();
            /*
            f1 = new info.guardianproject.iocipher.File("/เอกสารสำคัญ");
            f1.mkdirs();
            */
            f1 = new info.guardianproject.iocipher.File("/listy zakupów");
            f1.mkdirs();
            f1 = new info.guardianproject.iocipher.File("/My Shopping.docx");
            f1.createNewFile();
            info.guardianproject.iocipher.RandomAccessFile fra = new info.guardianproject.iocipher.RandomAccessFile(f1, "rw");
            fra.write(new byte[45847]);
            fra.close();

            for (int i=0;i<30;i++)
            {
                f1 = new info.guardianproject.iocipher.File("/Photo_" + i + ".png");
                f1.createNewFile();
                fra = new info.guardianproject.iocipher.RandomAccessFile(f1, "rw");
                fra.write(new byte[25841 + (int) (Math.random() * 4096)]);
                fra.close();
            }
        }
        catch(Exception e)
        {
        }
    }

    public void listSubdirectories(java.io.File directory, String current_work_dir) {
        if (directory.isDirectory()) {
            try
            {
                java.io.File[] subdirectories = directory.listFiles(java.io.File::isDirectory);
                if (subdirectories != null) {
                    for (java.io.File subdirectory : subdirectories) {
                        System.out.println("D: " + current_work_dir + "/" + subdirectory.getName());
                        info.guardianproject.iocipher.File create_dir =
                            new info.guardianproject.iocipher.File(current_work_dir, subdirectory.getName());
                        create_dir.mkdirs();

                        if (subdirectory.isDirectory()) {
                            java.io.File[] files_in_dir = subdirectory.listFiles(java.io.File::isFile);
                            for (java.io.File file_in_dir : files_in_dir) {
                                import_file(file_in_dir, create_dir);
                            }
                        }

                        listSubdirectories(subdirectory, current_work_dir + java.io.File.separator + subdirectory.getName());
                    }
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    public void check_and_create_dir_structure(java.io.File file)
    {
        System.out.println("check_and_create_dir_structure: " + file.getAbsolutePath() + " " + file.getName());
        info.guardianproject.iocipher.File create_dir =
            new info.guardianproject.iocipher.File(current_vfs_dir.getAbsolutePath(), file.getName());
        create_dir.mkdirs();

        try
        {
            if (file.isDirectory()) {
                java.io.File[] files_in_dir = file.listFiles(java.io.File::isFile);
                for (java.io.File file_in_dir : files_in_dir) {
                    import_file(file_in_dir, create_dir);
                }
            }

            listSubdirectories(file, current_vfs_dir.getAbsolutePath() + java.io.File.separator + file.getName());
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public Container getGui() {
        if (gui == null) {
            gui = new JPanel(new BorderLayout(3, 3));
            gui.setBorder(new EmptyBorder(5, 5, 5, 5));

            fileSystemView = FileSystemView.getFileSystemView();
            desktop = Desktop.getDesktop();

            JPanel detailView = new JPanel(new BorderLayout(3, 3));

            new FileDrop(detailView, new FileDrop.Listener() {
                public void filesDropped(java.io.File[] files) {
                    // System.out.println("dropped file(s): ");
                    info.guardianproject.iocipher.File target_vfs_dir = current_vfs_dir;
                    if (current_vfs_dir == null) {
                        System.out.println("Error. no current directory inside VFS selected");
                        return;
                    }

                    // System.out.println("current dir: " + target_vfs_dir);
                    for (java.io.File file : files) {
                        // first check if there are directories and make all the needed dirs
                        if (file.isDirectory()) {
                            check_and_create_dir_structure(file);
                        }
                    }
                    update_files_and_dirs();

                    for (java.io.File file : files) {
                        if (file.isDirectory()) {
                            System.out.println("dropped dir: " + file.getAbsolutePath());
                            // TODO: import also full directories with subdirs? somehow
                        } else {
                            System.out.println("dropped file: " + file.getAbsolutePath());
                            import_file(file, target_vfs_dir);
                        }
                    }
                }
            });

            table = new JTable();
            table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            table.setAutoCreateRowSorter(true);
            table.setShowVerticalLines(false);

            listSelectionListener = new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent lse) {
                    int row = table.getSelectionModel().getLeadSelectionIndex();
                    setFileDetails(((FileTableModel) table.getModel()).getFile(row), false);
                }
            };
            table.getSelectionModel().addListSelectionListener(listSelectionListener);
            JScrollPane tableScroll = new JScrollPane(table);
            Dimension d = tableScroll.getPreferredSize();
            tableScroll.setPreferredSize(new Dimension((int) d.getWidth() * 3, (int) ((float)d.getHeight() * 1.7f)));
            detailView.add(tableScroll, BorderLayout.CENTER);

            // the File tree
            DefaultMutableTreeNode root = new DefaultMutableTreeNode();
            treeModel = new DefaultTreeModel(root);

            TreeSelectionListener treeSelectionListener = new TreeSelectionListener() {
                public void valueChanged(TreeSelectionEvent tse) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) tse.getPath().getLastPathComponent();
                    showChildren(node, true);
                    setFileDetails((File) node.getUserObject(), true);
                }
            };

            // show the files in root "/" dir.
            File fileSystemRoot = new File("/");
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(fileSystemRoot);
            root.add(node);
            //
            File[] files = fileSystemRoot.listFiles();
            // System.out.println("listfiles:001");
            for (File file : files) {
                if (file.isDirectory()) {
                    node.add(new DefaultMutableTreeNode(file));
                }
            }

            tree = new JTree(treeModel);
            tree.setRootVisible(false);
            tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
            tree.addTreeSelectionListener(treeSelectionListener);
            tree.setCellRenderer(new FileTreeCellRenderer());
            tree.expandRow(0);
            JScrollPane treeScroll = new JScrollPane(tree);

            // as per trashgod tip
            tree.setVisibleRowCount(15);

            Dimension preferredSize = treeScroll.getPreferredSize();
            Dimension widePreferred = new Dimension(
                    240,
                    (int)preferredSize.getHeight());
            treeScroll.setPreferredSize(widePreferred);

            // details for a File
            JPanel fileMainDetails = new JPanel(new BorderLayout(4, 2));
            fileMainDetails.setBorder(new EmptyBorder(0, 6, 0, 6));

            JPanel fileDetailsLabels = new JPanel(new GridLayout(0, 1, 2, 2));
            fileMainDetails.add(fileDetailsLabels, BorderLayout.WEST);

            JPanel fileDetailsValues = new JPanel(new GridLayout(0, 1, 2, 2));
            fileMainDetails.add(fileDetailsValues, BorderLayout.CENTER);

            fileDetailsLabels.add(new JLabel("File", JLabel.TRAILING));
            fileName = new JLabel();
            fileDetailsValues.add(fileName);
            fileDetailsLabels.add(new JLabel("Path/name", JLabel.TRAILING));
            path = new JTextField(5);
            path.setEditable(false);
            fileDetailsValues.add(path);
            fileDetailsLabels.add(new JLabel("Last Modified", JLabel.TRAILING));
            date = new JLabel();
            fileDetailsValues.add(date);
            fileDetailsLabels.add(new JLabel("File size", JLabel.TRAILING));
            size = new JLabel();
            fileDetailsValues.add(size);
            fileDetailsLabels.add(new JLabel("Type", JLabel.TRAILING));

            JPanel flags = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 0));
            isDirectory = new JRadioButton("Directory");
            isDirectory.setEnabled(false);
            flags.add(isDirectory);

            isFile = new JRadioButton("File");
            isFile.setEnabled(false);
            flags.add(isFile);
            fileDetailsValues.add(flags);

            int count = fileDetailsLabels.getComponentCount();
            for (int ii = 0; ii < count; ii++) {
                fileDetailsLabels.getComponent(ii).setEnabled(false);
            }

            JToolBar toolBar = new JToolBar();
            // mnemonics stop working in a floated toolbar
            toolBar.setFloatable(false);

            /*
             * openFile = new JButton("Open");
             * openFile.setMnemonic('o');
             * 
             * openFile.addActionListener(new ActionListener(){
             * public void actionPerformed(ActionEvent ae) {
             * try {
             * // TODO: open VFS file somehow ...
             * System.out.println("TODO: open VFS file somehow ...");
             * // desktop.open(currentFile);
             * } catch(Throwable t) {
             * showThrowable(t);
             * }
             * gui.repaint();
             * }
             * });
             * toolBar.add(openFile);
             */

            /*
             * editFile = new JButton("Edit");
             * editFile.setMnemonic('e');
             * editFile.addActionListener(new ActionListener(){
             * public void actionPerformed(ActionEvent ae) {
             * try {
             * desktop.edit(currentFile);
             * } catch(Throwable t) {
             * showThrowable(t);
             * }
             * }
             * });
             * toolBar.add(editFile);
             */

            /*
             * printFile = new JButton("Print");
             * printFile.setMnemonic('p');
             * printFile.addActionListener(new ActionListener(){
             * public void actionPerformed(ActionEvent ae) {
             * try {
             * desktop.print(currentFile);
             * } catch(Throwable t) {
             * showThrowable(t);
             * }
             * }
             * });
             * toolBar.add(printFile);
             */

            // Check the actions are supported on this platform!
            // openFile.setEnabled(desktop.isSupported(Desktop.Action.OPEN));
            // editFile.setEnabled(desktop.isSupported(Desktop.Action.EDIT));
            // printFile.setEnabled(desktop.isSupported(Desktop.Action.PRINT));

            toolBar.addSeparator();

            exportFiles = new JButton("Export");
            exportFiles.setMnemonic('e');
            exportFiles.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    exportFiles();
                }
            });
            toolBar.add(exportFiles);

            toolBar.addSeparator();

            newFile = new JButton("New");
            newFile.setMnemonic('n');
            newFile.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    newFile();
                }
            });
            toolBar.add(newFile);

            /*
             * copyFile = new JButton("Copy");
             * copyFile.setMnemonic('c');
             * copyFile.addActionListener(new ActionListener(){
             * public void actionPerformed(ActionEvent ae) {
             * showErrorMessage("'Copy' not implemented.", "Not implemented.");
             * }
             * });
             * toolBar.add(copyFile);
             */

            JButton renameFile = new JButton("Rename");
            renameFile.setMnemonic('r');
            renameFile.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    renameFile();
                }
            });
            toolBar.add(renameFile);

            deleteFile = new JButton("Delete");
            deleteFile.setMnemonic('d');
            deleteFile.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    deleteFile();
                }
            });
            toolBar.add(deleteFile);

            toolBar.addSeparator();

            readable = new JCheckBox("Read  ");
            readable.setMnemonic('a');
            // readable.setEnabled(false);
            toolBar.add(readable);

            writable = new JCheckBox("Write  ");
            writable.setMnemonic('w');
            // writable.setEnabled(false);
            toolBar.add(writable);

            executable = new JCheckBox("Execute");
            executable.setMnemonic('x');
            // executable.setEnabled(false);
            executable.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    // TODO: actually change the bit of the file
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        // System.out.println("Execute bit is enabled");
                    } else {
                        // System.out.println("Execute bit disabled");
                    }
                }
            });

            toolBar.add(executable);

            JPanel fileView = new JPanel(new BorderLayout(3, 3));

            fileView.add(toolBar, BorderLayout.NORTH);
            fileView.add(fileMainDetails, BorderLayout.CENTER);

            detailView.add(fileView, BorderLayout.SOUTH);

            JSplitPane splitPane = new JSplitPane(
                    JSplitPane.HORIZONTAL_SPLIT,
                    treeScroll,
                    detailView);
            gui.add(splitPane, BorderLayout.CENTER);

            JPanel simpleOutput = new JPanel(new BorderLayout(3, 3));
            progressBar = new JProgressBar();
            progressBar.setPreferredSize(new Dimension(400, 20));
            simpleOutput.add(progressBar, BorderLayout.EAST);
            progressBar.setVisible(false);

            gui.add(simpleOutput, BorderLayout.SOUTH);

        }
        return gui;
    }

    public void showRootFile() {
        // ensure the main files are displayed
        tree.setSelectionInterval(0, 0);
    }

    private synchronized TreePath findTreePath(java.io.File find)
    {
        TreePath t = null;
        int max_tries = 5;
        int tries = 0;
        while(t == null) {
            t = findTreePath_real(find);
            tries++;
            if (tries > max_tries) {
                break;
            }
        }
        return t;
    }

    private synchronized TreePath findTreePath_real(java.io.File find)
    {
        try
        {
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
            java.util.List<TreePath> paths = new ArrayList<>();
            @SuppressWarnings("unchecked")
            java.util.Enumeration<TreeNode> e = (java.util.Enumeration<TreeNode>)root.preorderEnumeration();
            while (e.hasMoreElements()) {
                try
                {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode)e.nextElement();
                    // System.out.println("findTreePath_real:node=" + node);
                    java.io.File nodeFile = (File) node.getUserObject();
                    if (nodeFile.getAbsolutePath().compareTo(find.getAbsolutePath()) == 0)
                    {
                        TreePath treePath = new TreePath(node.getPath());
                        return treePath;
                    }
                }
                catch(Exception e2)
                {
                    // e2.printStackTrace();
                }
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
            System.out.println("findTreePath ======= EXCEPTION ===\n\n");
        }
        // not found!
        System.out.println("findTreePath ======= DONE ERROR ==\n\n");
        return null;
    }

    private void renameFile() {
        if (currentFile == null) {
            showErrorMessage("No file selected to rename.", "Select File");
            return;
        }

        String renameTo = JOptionPane.showInputDialog(gui, "New Name");
        if (renameTo != null) {
            try {
                boolean directory = currentFile.isDirectory();
                TreePath parentPath = findTreePath(currentFile.getParentFile());
                DefaultMutableTreeNode parentNode = null;
                try {
                    parentNode = (DefaultMutableTreeNode) parentPath.getLastPathComponent();
                } catch (Exception e) {
                }

                if (currentFile.getAbsolutePath().compareTo("/") == 0)
                {
                    System.out.println("trying to rename ROOT directory");
                    return;
                }

                boolean renamed = currentFile.renameTo(new File(
                        currentFile.getParentFile(), renameTo));
                if (renamed) {
                    if (directory) {
                        // rename the node..

                        // delete the current node..
                        TreePath currentPath = findTreePath(currentFile);
                        // System.out.println(currentPath);

                        DefaultMutableTreeNode currentNode = null;
                        try {
                            currentNode = (DefaultMutableTreeNode) parentPath.getLastPathComponent();
                        } catch (Exception e) {
                        }

                        treeModel.removeNodeFromParent(currentNode);

                        // add a new node..
                        showChildren(parentNode, true);
                    } else {
                        parentPath = findTreePath(current_vfs_dir);
                        parentNode = (DefaultMutableTreeNode) parentPath.getLastPathComponent();
                        showChildren(parentNode, true);
                    }
                } else {
                    String msg = "The file '" +
                            currentFile +
                            "' could not be renamed.";
                    showErrorMessage(msg, "Rename Failed");
                }
            } catch (Throwable t) {
                showThrowable(t);
            }
        }
        currentFile = null;
        setFileDetails(new File(""), false);
        gui.repaint();
    }

    private void exportFiles() {
        if (currentFile == null) {
            showErrorMessage("No file selected for export.", "Export Files");
            return;
        }

        int result = JOptionPane.showConfirmDialog(
                gui,
                "Are you sure you want to export the selected Files?",
                "Export Files",
                JOptionPane.ERROR_MESSAGE);

                if (result == JOptionPane.OK_OPTION) {
                    try {
                        // System.out.println("current File: " + currentFile);
                        // System.out.println("current Directory: " + current_vfs_dir);
                        TreePath parentPath = findTreePath(current_vfs_dir);
                        // System.out.println("parentPath: " + parentPath);
        
                        DefaultMutableTreeNode parentNode = null;
                        try {
                            parentNode = (DefaultMutableTreeNode) parentPath.getLastPathComponent();
                        } catch (Exception e) {
                        }
        
                        // System.out.println("parentNode: " + parentNode);
        
                        boolean directory = currentFile.isDirectory();

                        FileTableModel model = (FileTableModel) table.getModel();
                        if (table.getRowCount() > 0) {
                            if (table.getSelectedRowCount() > 0) {
                                int selectedRow[] = table.getSelectedRows();
                                for (int i : selectedRow) {
                                    info.guardianproject.iocipher.File f_iter = model.getFile(i);
                                    if (!f_iter.isDirectory())
                                    {
                                        // System.out.println("selected " + i + " " + f_iter.getAbsolutePath());
                                        System.out.println("exporting " + f_iter.getAbsolutePath());
                                        export_file(f_iter, new java.io.File("./"));
                                    }
                                }
                            }
                        }

                    } catch (Throwable t) {
                        showThrowable(t);
                    }
                }
    }

    private void deleteFile() {
        if (currentFile == null) {
            showErrorMessage("No file selected for deletion.", "Select File");
            return;
        }

        int result = JOptionPane.showConfirmDialog(
                gui,
                "Are you sure you want to delete the selected Files?",
                "Delete Files",
                JOptionPane.ERROR_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            try {
                // System.out.println("currentFile: " + currentFile);
                TreePath parentPath = findTreePath(current_vfs_dir);
                // System.out.println("parentPath: " + parentPath);

                DefaultMutableTreeNode parentNode = null;
                try {
                    parentNode = (DefaultMutableTreeNode) parentPath.getLastPathComponent();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // System.out.println("parentNode: " + parentNode);

                boolean directory = currentFile.isDirectory();






                // delete files first
                FileTableModel model = (FileTableModel) table.getModel();
                if (table.getRowCount() > 0) {
                    if (table.getSelectedRowCount() > 0) {
                        int selectedRow[] = table.getSelectedRows();
                        for (int i : selectedRow) {
                            info.guardianproject.iocipher.File f_iter = model.getFile(i);
                            if (!f_iter.isDirectory())
                            {
                                // System.out.println("selected " + i + " " + f_iter.getAbsolutePath());
                                // System.out.println("deleting " + f_iter.getAbsolutePath());

                                if (f_iter.getAbsolutePath().compareTo("/") == 0)
                                {
                                    System.out.println("trying to delete ROOT directory");
                                    continue;
                                }
                                boolean deleted = f_iter.delete();
                                if (deleted) {
                                    if (directory) {
                                        // delete the node..
                                        TreePath currentPath = findTreePath(f_iter);
                                        // System.out.println(currentPath);
                                        DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) currentPath
                                                .getLastPathComponent();
                
                                        treeModel.removeNodeFromParent(currentNode);
                                        showChildren(parentNode, true);
                                    } else {
                                        parentPath = findTreePath(current_vfs_dir);
                                        parentNode = (DefaultMutableTreeNode) parentPath.getLastPathComponent();
                                        showChildren(parentNode, true);
                                    }
                                } else {
                                    System.out.println("The file could not be deleted.");
                                }
                
                            }
                        }
                    }
                }




                // delete directories after all files have been deleted
                if (table.getRowCount() > 0) {
                    if (table.getSelectedRowCount() > 0) {
                        int selectedRow[] = table.getSelectedRows();
                        for (int i : selectedRow) {
                            info.guardianproject.iocipher.File f_iter = model.getFile(i);
                            if (f_iter.isDirectory())
                            {
                                // System.out.println("selected " + i + " " + f_iter.getAbsolutePath());
                                // System.out.println("deleting " + f_iter.getAbsolutePath());

                                boolean deleted = f_iter.delete();
                                if (deleted) {
                                    if (directory) {
                                        // delete the node..
                                        TreePath currentPath = findTreePath(f_iter);
                                        // System.out.println(currentPath);
                                        DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) currentPath
                                                .getLastPathComponent();
                
                                        treeModel.removeNodeFromParent(currentNode);
                                        showChildren(parentNode, true);
                                    } else {
                                        parentPath = findTreePath(current_vfs_dir);
                                        parentNode = (DefaultMutableTreeNode) parentPath.getLastPathComponent();
                                        showChildren(parentNode, true);
                                    }
                                } else {
                                    System.out.println("The file could not be deleted.");
                                }
                
                            }
                        }
                    }
                }



            } catch (Throwable t) {
                t.printStackTrace();
                // showThrowable(t);
            }
        }
        currentFile = null;
        setFileDetails(new File(""), false);
        gui.repaint();
    }

    private void newFile() {
        if (current_vfs_dir == null) {
            showErrorMessage("No location selected for new file.", "Select Location");
            return;
        }

        if (newFilePanel == null) {
            newFilePanel = new JPanel(new BorderLayout(3, 3));

            JPanel southRadio = new JPanel(new GridLayout(1, 0, 2, 2));
            newTypeFile = new JRadioButton("File", true);
            JRadioButton newTypeDirectory = new JRadioButton("Directory");
            ButtonGroup bg = new ButtonGroup();
            bg.add(newTypeFile);
            bg.add(newTypeDirectory);
            southRadio.add(newTypeFile);
            southRadio.add(newTypeDirectory);

            name = new JTextField(15);

            newFilePanel.add(new JLabel("Name"), BorderLayout.WEST);
            newFilePanel.add(name);
            newFilePanel.add(southRadio, BorderLayout.SOUTH);
        }

        int result = JOptionPane.showConfirmDialog(
                gui,
                newFilePanel,
                "Create File",
                JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                boolean created;
                File parentFile = current_vfs_dir;
                File file = new File(parentFile, name.getText());
                if (newTypeFile.isSelected()) {
                    created = file.createNewFile();
                } else {
                    created = file.mkdir();
                }
                if (created) {

                    TreePath parentPath = findTreePath(parentFile);
                    DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) parentPath.getLastPathComponent();

                    if (file.isDirectory()) {
                        // add the new node..
                        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(file);

                        /*
                        TreePath currentPath = findTreePath(currentFile);
                        DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) currentPath
                                .getLastPathComponent();
                        */

                        treeModel.insertNodeInto(newNode, parentNode, parentNode.getChildCount());
                        // System.out.println("insertNodeInto:005");
                        showChildren(parentNode, true);
                    } else {
                        parentPath = findTreePath(current_vfs_dir);
                        parentNode = (DefaultMutableTreeNode) parentPath.getLastPathComponent();
                        showChildren(parentNode, true);
                    }
                } else {
                    String msg = "The file '" +
                            file +
                            "' could not be created.";
                    showErrorMessage(msg, "Create Failed");
                }
            } catch (Throwable t) {
                t.printStackTrace();
                // showThrowable(t);
            }
        }
        gui.repaint();
    }

    private void showErrorMessage(String errorMessage, String errorTitle) {
        JOptionPane.showMessageDialog(
                gui,
                errorMessage,
                errorTitle,
                JOptionPane.ERROR_MESSAGE);
    }

    private void showThrowable(Throwable t) {
        t.printStackTrace();
        JOptionPane.showMessageDialog(
                gui,
                t.toString(),
                t.getMessage(),
                JOptionPane.ERROR_MESSAGE);
        gui.repaint();
    }

    /** Update the table on the EDT */
    private void setTableData(final File[] files) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (fileTableModel == null) {
                    fileTableModel = new FileTableModel();
                    table.setModel(fileTableModel);
                }
                table.getSelectionModel().removeListSelectionListener(listSelectionListener);
                fileTableModel.setFiles(files);
                table.getSelectionModel().addListSelectionListener(listSelectionListener);
                if (!cellSizesSet) {
                    // Icon icon = fileSystemView.getSystemIcon(files[0]);

                    // size adjustment to better account for icons
                    // table.setRowHeight( icon.getIconHeight()+rowIconPadding );
                    // TODO: 30 is just a random value. calculate this better somehow
                    table.setRowHeight(30 + rowIconPadding);

                    setColumnWidth(0, 0); // icon -> unused with VFS, or does anybody know how to get system icons without a real system file?

                    setColumnWidth(1, 350); // File(name)
                    table.getColumnModel().getColumn(1).setMaxWidth(5000);

                    setColumnWidth(2, 8); // Path/name
                    table.getColumnModel().getColumn(2).setMaxWidth(1500);

                    setColumnWidth(3, 160); // size
                    table.getColumnModel().getColumn(3).setMaxWidth(450);

                    setColumnWidth(4, 200); // last modified
                    table.getColumnModel().getColumn(4).setMaxWidth(450);

                    setColumnWidth(5, -1); // R
                    setColumnWidth(6, -1); // W
                    setColumnWidth(7, -1); // E
                    setColumnWidth(8, -1); // D
                    setColumnWidth(9, -1); // File or Directory

                    cellSizesSet = true;
                }
            }
        });
    }

    private void setColumnWidth(int column, int width) {
        TableColumn tableColumn = table.getColumnModel().getColumn(column);
        if (width < 0) {
            // use the preferred width of the header..
            JLabel label = new JLabel((String) tableColumn.getHeaderValue());
            Dimension preferred = label.getPreferredSize();
            // altered 10->14 as per camickr comment.
            width = (int) preferred.getWidth() + 14;
        }
        tableColumn.setPreferredWidth(width);
        tableColumn.setMaxWidth(width);
        tableColumn.setMinWidth(width);
    }

    private synchronized void update_files_and_dirs() {
        try {
            TreePath parentPath = findTreePath(current_vfs_dir);
            DefaultMutableTreeNode parentNode = null;
            try {
                parentNode = (DefaultMutableTreeNode) parentPath.getLastPathComponent();
            } catch (Exception e) {
                e.printStackTrace();
            }
            showChildren(parentNode, true);
            gui.repaint();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void import_file(java.io.File src_file, info.guardianproject.iocipher.File dst_dir) {
        tree.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(false);
        final int chunk_size = (int)(8192 * 100); // should be multiple of 8192 !
        long f_len_mbytes = src_file.length() / chunk_size;
        if ((f_len_mbytes < 1) || (f_len_mbytes > 2000000)) {
            // System.out.println("setIndeterminate");
            progressBar.setIndeterminate(true);
        } else {
            // System.out.println("max=" + f_len_mbytes);
            progressBar.setValue(0);
            progressBar.setMaximum((int) f_len_mbytes);
        }

        SwingWorker<Void, Long> worker = new SwingWorker<Void, Long>() {

            boolean first_paint = false;

            @Override
            public Void doInBackground() {
                try
                {
                    first_paint = false;
                    info.guardianproject.iocipher.File f = new info.guardianproject.iocipher.File(dst_dir + java.io.File.separator + src_file.getName());
                    info.guardianproject.iocipher.FileOutputStream out = new info.guardianproject.iocipher.FileOutputStream(f);

                    java.io.FileInputStream in = new java.io.FileInputStream(src_file);
                    final byte[] buf = new byte[chunk_size];
                    // System.out.println("dst=" + f.getAbsolutePath());

                    for (long i = 0; i < f_len_mbytes; i++) {
                        in.read(buf);
                        out.write(buf);
                        // System.out.println("Value in thread : " + i);
                        publish(i);
                    }

                    long last_bytes = 0;
                    long bluk_bytes = f_len_mbytes * chunk_size;
                    if ((bluk_bytes > 0) || (f_len_mbytes == 0)) {
                        last_bytes = src_file.length() - bluk_bytes;
                    }
                    if (last_bytes > 0) {
                        // System.out.println("last_bytes : " + last_bytes);
                        final byte[] buf_last = new byte[(int)last_bytes];
                        in.read(buf_last);
                        out.write(buf_last);
                    }

                    in.close();
                    out.close();
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void process(List<Long> chunks) {
                Long val = chunks.get(chunks.size() - 1);
                String progress = String.valueOf(val);
                // System.out.println("progress : " + progress);
                progressBar.setValue(Math.toIntExact(val));
                if (!first_paint) {
                    first_paint = true;

                    try {
                        TreePath parentPath = findTreePath(current_vfs_dir);
                        DefaultMutableTreeNode parentNode = null;
                        try {
                            parentNode = (DefaultMutableTreeNode) parentPath.getLastPathComponent();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        showChildren(parentNode, false);
                        gui.repaint();
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            protected void done() {
                progressBar.setIndeterminate(false);
                progressBar.setVisible(false);
                tree.setEnabled(true);

                try {
                    TreePath parentPath = findTreePath(current_vfs_dir);
                    DefaultMutableTreeNode parentNode = null;
                    try {
                        parentNode = (DefaultMutableTreeNode) parentPath.getLastPathComponent();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    showChildren(parentNode, true);
                    gui.repaint();
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    private void export_file(info.guardianproject.iocipher.File src_file, java.io.File dst_dir) {
        tree.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(false);
        final int chunk_size = (int)(8192 * 100); // should be multiple of 8192 !
        long f_len_mbytes = src_file.length() / chunk_size;
        if ((f_len_mbytes < 1) || (f_len_mbytes > 2000000)) {
            // System.out.println("setIndeterminate");
            progressBar.setIndeterminate(true);
        } else {
            // System.out.println("max=" + f_len_mbytes);
            progressBar.setValue(0);
            progressBar.setMaximum((int) f_len_mbytes);
        }

        SwingWorker<Void, Long> worker = new SwingWorker<Void, Long>() {
            @Override
            public Void doInBackground() {
                try
                {
                    java.io.File f = new java.io.File(dst_dir + java.io.File.separator + src_file.getName());
                    java.io.FileOutputStream out = new java.io.FileOutputStream(f);

                    info.guardianproject.iocipher.FileInputStream in = new info.guardianproject.iocipher.FileInputStream(src_file);
                    final byte[] buf = new byte[chunk_size];
                    // System.out.println("dst=" + f.getAbsolutePath());

                    for (long i = 0; i < f_len_mbytes; i++) {
                        in.read(buf);
                        out.write(buf);
                        // System.out.println("Value in thread : " + i);
                        publish(i);
                    }

                    long last_bytes = 0;
                    long bluk_bytes = f_len_mbytes * chunk_size;
                    if ((bluk_bytes > 0) || (f_len_mbytes == 0)) {
                        last_bytes = src_file.length() - bluk_bytes;
                    }
                    if (last_bytes > 0) {
                        // System.out.println("last_bytes : " + last_bytes);
                        final byte[] buf_last = new byte[(int)last_bytes];
                        in.read(buf_last);
                        out.write(buf_last);
                    }

                    in.close();
                    out.close();
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void process(List<Long> chunks) {
                Long val = chunks.get(chunks.size() - 1);
                // String progress = String.valueOf(val);
                // System.out.println("progress : " + progress);
                progressBar.setValue(Math.toIntExact(val));
            }

            @Override
            protected void done() {
                progressBar.setIndeterminate(false);
                progressBar.setVisible(false);
                tree.setEnabled(true);

                try {
                    TreePath parentPath = findTreePath(current_vfs_dir);
                    DefaultMutableTreeNode parentNode = null;
                    try {
                        parentNode = (DefaultMutableTreeNode) parentPath.getLastPathComponent();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    showChildren(parentNode, true);
                    gui.repaint();
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    /**
     * Add the files that are contained within the directory of this node.
     * Thanks to Hovercraft Full Of Eels.
     */
    private void showChildren(final DefaultMutableTreeNode node, boolean lock) {
        if (lock)
        {
            tree.setEnabled(false);
            progressBar.setVisible(true);
            progressBar.setIndeterminate(true);
        }

        SwingWorker<Void, File> worker = new SwingWorker<Void, File>() {
            @Override
            public Void doInBackground() {
                File file = (File) node.getUserObject();
                if (file.isDirectory()) {
                    File[] files = file.listFiles(); // !!
                    // System.out.println("listfiles:002");
                    if (node.isLeaf()) {
                        for (File child : files) {
                            if (child.isDirectory()) {
                                // System.out.println("listfiles:003");
                                publish(child);
                            }
                        }
                    }
                    setTableData(files);
                }
                return null;
            }

            @Override
            protected void process(List<File> chunks) {
                for (java.io.File child : chunks) {
                    node.add(new DefaultMutableTreeNode(child));
                    // System.out.println("node.add:003:" + child.getAbsolutePath());
                }
            }

            @Override
            protected void done() {
                if (lock)
                {
                    progressBar.setIndeterminate(false);
                    progressBar.setVisible(false);
                    tree.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    /** Update the File details view with the details of this File. */
    private void setFileDetails(File file, boolean select_dir) {
        if (select_dir) {
            current_vfs_dir = file;
            currentFile = null;
            // System.out.println("current_vfs_dir=" + current_vfs_dir.getAbsolutePath());
            return;
        } else {
            currentFile = file;
            // System.out.println("currentFile=" + currentFile.getAbsolutePath());
        }
        // Icon icon = fileSystemView.getSystemIcon(file);
        // fileName.setIcon(icon);
        fileName.setText(fileSystemView.getSystemDisplayName(file));
        path.setText(file.getPath());
        date.setText(new Date(file.lastModified()).toString());
        size.setText(file.length() + " bytes");
        readable.setSelected(file.canRead());
        writable.setSelected(file.canWrite());
        executable.setSelected(file.canExecute());
        isDirectory.setSelected(file.isDirectory());

        isFile.setSelected(file.isFile());

        JFrame f = (JFrame) gui.getTopLevelAncestor();
        if (f != null) {
            f.setTitle(
                    APP_TITLE +
                            " :: " +
                            fileSystemView.getSystemDisplayName(file));
        }

        gui.repaint();
    }

    public static boolean copyFile(File from, File to) throws Exception {

        boolean created = to.createNewFile();

        if (created) {
            IOCipherFileChannel fromChannel = null;
            IOCipherFileChannel toChannel = null;
            try {
                fromChannel = new FileInputStream(from).getChannel();
                toChannel = new FileOutputStream(to).getChannel();

                toChannel.transferFrom(fromChannel, 0, fromChannel.size());

                // set the flags of the to the same as the from
                to.setReadable(from.canRead());
                to.setWritable(from.canWrite());
                to.setExecutable(from.canExecute());
            } finally {
                if (fromChannel != null) {
                    fromChannel.close();
                }
                if (toChannel != null) {
                    toChannel.close();
                }
                return false;
            }
        }
        return created;
    }

    public static void main(String[] args) {

        // select ROOT dir at start
        current_vfs_dir = new info.guardianproject.iocipher.File("/");

        // System.out.println("number of args: " + args.length);
        showcase_mode = false;
        if (args.length == 1) {
            System.out.println("db file: " + args[0]);
            dbfilename = args[0];
        } else if (args.length == 2) {
            System.out.println("db file : " + args[0]);
            dbfilename = args[0];
            System.out.println("password: *******************");
            goodPassword = args[1];
        } else if (args.length == 3) {
            System.out.println("db file : " + args[0]);
            dbfilename = args[0];
            System.out.println("password: *******************");
            goodPassword = args[1];
            if (args[1].compareTo("showcase") != 0) {
                // HINT: showcase mode. so lets create some files and directories
                showcase_mode = true;
                System.out.println("== SHOWCASE MODE ==");
            }
        }
        System.out.println("setup db ...");
        setUp();
        System.out.println("setup db ... OK");
        System.out.println("sqlfs version: " + vfs.sqlfsVersion());
        if (showcase_mode)
        {
            create_dummies();
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    // Significantly improves the look of the output in
                    // terms of the file names returned by FileSystemView!
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception weTried) {
                }
                JFrame f = new JFrame(APP_TITLE);
                f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                WindowListener listener = new WindowAdapter() {

                    @Override
                    public void windowClosing(WindowEvent we) {
                        int result = JOptionPane.showConfirmDialog(f, "Close the application");
                        if (result == JOptionPane.OK_OPTION) {
                            tearDown();
                            f.setVisible(false);
                            f.dispose();
                        }
                    }
                };
                f.addWindowListener(listener);

                FileManager fileManager = new FileManager();
                f.setContentPane(fileManager.getGui());
                f.pack();
                f.setLocationByPlatform(true);
                f.setMinimumSize(f.getSize());
                f.setVisible(true);

                fileManager.showRootFile();
            }
        });
    }
}

/** A TableModel to hold File[]. */
class FileTableModel extends AbstractTableModel {

    private File[] files;
    private FileSystemView fileSystemView = FileSystemView.getFileSystemView();
    private String[] columns = {
            "Icon",
            "File",
            "Path/name",
            "Size",
            "Last Modified",
            "R",
            "W",
            "E",
            "D",
            "F",
    };

    FileTableModel() {
        this(new File[0]);
    }

    FileTableModel(File[] files) {
        this.files = files;
    }

    public Object getValueAt(int row, int column) {
        File file = files[row];
        switch (column) {
            case 0:
                // return fileSystemView.getSystemIcon(file);
                return null;
            case 1:
                return fileSystemView.getSystemDisplayName(file);
            case 2:
                return file.getPath();
            case 3:
                return file.length();
            case 4:
                return file.lastModified();
            case 5:
                return file.canRead();
            case 6:
                return file.canWrite();
            case 7:
                return file.canExecute();
            case 8:
                return file.isDirectory();
            case 9:
                return file.isFile();
            default:
                System.err.println("Logic Error");
        }
        return "";
    }

    public int getColumnCount() {
        return columns.length;
    }

    public Class<?> getColumnClass(int column) {
        switch (column) {
            case 0:
                return ImageIcon.class;
            case 3:
                return Long.class;
            case 4:
                return Date.class;
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
                return Boolean.class;
        }
        return String.class;
    }

    public String getColumnName(int column) {
        return columns[column];
    }

    public int getRowCount() {
        return files.length;
    }

    public File getFile(int row) {
        return files[row];
    }

    public void setFiles(File[] files) {
        this.files = files;
        fireTableDataChanged();
    }
}

/** A TreeCellRenderer for a File. */
class FileTreeCellRenderer extends DefaultTreeCellRenderer {

    private FileSystemView fileSystemView;

    private JLabel label;

    FileTreeCellRenderer() {
        label = new JLabel();
        label.setOpaque(true);
        fileSystemView = FileSystemView.getFileSystemView();
    }

    @Override
    public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus) {

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        java.io.File file = (File) node.getUserObject();
        // label.setIcon(fileSystemView.getSystemIcon(file));
        if (file.getAbsolutePath().compareTo("/") == 0)
        {
            label.setText(fileSystemView.getSystemDisplayName(file) + " (ROOT node)");
        }
        else
        {
            label.setText(fileSystemView.getSystemDisplayName(file));
        }
        label.setToolTipText(file.getPath());

        if (selected) {
            label.setBackground(backgroundSelectionColor);
            label.setForeground(textSelectionColor);
        } else {
            label.setBackground(backgroundNonSelectionColor);
            label.setForeground(textNonSelectionColor);
        }

        return label;
    }
}
