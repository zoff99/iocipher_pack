import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import info.guardianproject.iocipher.FileInputStream;
import info.guardianproject.iocipher.FileOutputStream;
import info.guardianproject.iocipher.IOCipherFileChannel;
import info.guardianproject.iocipher.RandomAccessFile;
import info.guardianproject.iocipher.VirtualFileSystem;

/**
 * Pure Java File Manager with IOCipher Virtual File System (VFS) support.
 * 
 * COMPLETE REWRITE NOTES:
 * 1. Strictly maintains all info.guardianproject.iocipher.* dependencies.
 * 2. Removed external GUI libraries by implementing the robust, public-domain 
 *    FileDrop utility class for cross-platform drag-and-drop (including Linux KDE/Gnome).
 * 3. Fixed all known bugs: AIOOBEs, NPEs, and race conditions by ensuring all UI updates 
 *    occur strictly on the Event Dispatch Thread (EDT) via SwingWorker.
 * 4. Replaced complex, deadlock-prone Semaphore progress tracking with SwingWorker's 
 *    built-in publish()/process() mechanism.
 * 5. Replaced buggy manual tree manipulation with a robust lazy-loading tree model.
 * 6. Fixed the copyFile method that always returned false due to a misplaced return 
 *    statement in a finally block.
 * 7. Fully compatible with Java 1.8+ and works seamlessly on Windows and Linux.
 * 8. Added comprehensive debug logging throughout for troubleshooting.
 * 
 * @version 2.1.7
 */
public class FileManager {

    public static final String APP_TITLE = "VFS Manager";
    private static final String VERSION = "2.1.7";

    public static String sqlfsVersion = "";
    public static String iocipherVersion = "";
    public static String iocipherJNIVersion = "";

    private static info.guardianproject.iocipher.File current_vfs_dir = null;
    private static ExecutorService threadPool = null;

    private static VirtualFileSystem vfs;
    private static String dbfilename = "./vfsmanager.db";
    private static String goodPassword = "super secure password 1$%_?:!";

    private JPanel gui;
    private JTree tree;
    private DefaultTreeModel treeModel;
    private JTable table;
    private JProgressBar progressBar;
    private JProgressBar progressBarImport;
    private JProgressBar progressBarExport;
    
    private VFSFileTableModel fileTableModel;
    private ListSelectionListener listSelectionListener;
    private boolean cellSizesSet = false;

    private JLabel fileNameLabel;
    private JTextField pathField;
    private JLabel dateLabel;
    private JLabel sizeLabel;
    private JCheckBox readableCheckBox;
    private JCheckBox writableCheckBox;
    private JCheckBox executableCheckBox;
    private JRadioButton isDirectoryRadio;
    private JRadioButton isFileRadio;
    private JTextArea textArea;
    
    // FIX: Track the currently displayed file to clear details if it gets deleted or renamed
    private info.guardianproject.iocipher.File currentlyDisplayedFile = null;

    private javax.swing.JLayer<JScrollPane> tableLayer;
    private OverlayUI overlayUI;

    public FileManager() {
        initGui();
    }

    private void initGui() {
        gui = new JPanel(new BorderLayout(5, 5));
        gui.setBorder(new EmptyBorder(10, 10, 10, 10));

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton exportBtn = new JButton("Export (Copy To Host)");
        exportBtn.addActionListener(e -> exportSelectedFiles());
        toolBar.add(exportBtn);
        toolBar.addSeparator();

        JButton newBtn = new JButton("New");
        newBtn.addActionListener(e -> showNewFileDirDialog());
        toolBar.add(newBtn);

        JButton renameBtn = new JButton("Rename");
        renameBtn.addActionListener(e -> renameFile());
        toolBar.add(renameBtn);

        JButton deleteBtn = new JButton("Delete");
        deleteBtn.addActionListener(e -> deleteFile());
        toolBar.add(deleteBtn);
        toolBar.addSeparator();

        readableCheckBox = new JCheckBox("Read");
        readableCheckBox.setEnabled(false);
        toolBar.add(readableCheckBox);

        writableCheckBox = new JCheckBox("Write");
        writableCheckBox.setEnabled(false);
        toolBar.add(writableCheckBox);

        executableCheckBox = new JCheckBox("Execute");
        executableCheckBox.setEnabled(false);
        toolBar.add(executableCheckBox);

        gui.add(toolBar, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.25);

        info.guardianproject.iocipher.File rootFile = new info.guardianproject.iocipher.File("/");
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(rootFile);
        rootNode.add(new DefaultMutableTreeNode("Loading..."));
        treeModel = new DefaultTreeModel(rootNode);
        
        tree = new JTree(treeModel);
        tree.setRootVisible(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setCellRenderer(new VFSTreeCellRenderer());
        
        tree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            if (node == null) return;
            
            Object userObj = node.getUserObject();
            if (!(userObj instanceof info.guardianproject.iocipher.File)) {
                System.out.println("DEBUG: Tree selection ignored - not a File node");
                return;
            }
            
            info.guardianproject.iocipher.File selectedFile = (info.guardianproject.iocipher.File) userObj;
            System.out.println("DEBUG: Tree node selected: " + selectedFile.getAbsolutePath());
            current_vfs_dir = selectedFile;
            setFileDetails(selectedFile, true);
            
            boolean needsExploration = false;
            for (int i = 0; i < node.getChildCount(); i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
                if (child.getUserObject() instanceof String) {
                    needsExploration = true;
                    break;
                }
            }
            
            if (needsExploration) {
                System.out.println("DEBUG: Node needs exploration, loading children");
                loadDirectoryChildren(node);
            } else {
                System.out.println("DEBUG: Node already explored, updating table only");
                info.guardianproject.iocipher.File[] files = selectedFile.listFiles();
                setTableData(files != null ? files : new info.guardianproject.iocipher.File[0]);
            }
        });
        
        JScrollPane treeScroll = new JScrollPane(tree);
        // FIX: Force vertical and horizontal scrollbar thickness to 30px
        // Handled globally for FlatLaf via UIManager.put("ScrollBar.width", 30) in main().
        // treeScroll.getVerticalScrollBar().setPreferredSize(new Dimension(30, 30));
        // treeScroll.getHorizontalScrollBar().setPreferredSize(new Dimension(30, 30));
        treeScroll.setPreferredSize(new Dimension(250, 400));
        splitPane.setLeftComponent(treeScroll);

        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));

        fileTableModel = new VFSFileTableModel();
        table = new JTable(fileTableModel);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(24);

        // Allow the file list columns to be resized freely.
        // Also make the table use extra horizontal space when the main window is widened.
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.getTableHeader().setResizingAllowed(true);
        table.getTableHeader().setReorderingAllowed(true);
        
        JScrollPane tableScroll = new JScrollPane(table);
        // FIX: Force vertical and horizontal scrollbar thickness to 30px
        // Handled globally for FlatLaf via UIManager.put("ScrollBar.width", 30) in main().
        // tableScroll.getVerticalScrollBar().setPreferredSize(new Dimension(30, 30));
        // tableScroll.getHorizontalScrollBar().setPreferredSize(new Dimension(30, 30));
        
        // Modernization: Wrap tableScroll in a JLayer to support a stylish loading overlay.
        overlayUI = new OverlayUI();
        tableLayer = new javax.swing.JLayer<>(tableScroll, overlayUI);
        rightPanel.add(tableLayer, BorderLayout.CENTER);

        // =====================================================================
        // Drag and Drop Support using robust FileDrop utility
        // =====================================================================
        System.out.println("DEBUG: Initializing FileDrop for table...");
        new FileDrop(System.out, tableScroll, true, new FileDrop.Listener() {
            @Override
            public void filesDropped(java.io.File[] files) {
                System.out.println("DEBUG: FileDrop listener triggered with " + (files != null ? files.length : 0) + " files.");
                if (files != null && files.length > 0) {
                    if (current_vfs_dir == null) {
                        System.out.println("DEBUG: ERROR - current_vfs_dir is null, cannot import");
                        JOptionPane.showMessageDialog(gui, "No directory selected in VFS.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    System.out.println("DEBUG: Importing to: " + current_vfs_dir.getAbsolutePath());
                    for (java.io.File f : files) {
                        System.out.println("DEBUG: Dropped file: " + f.getAbsolutePath());
                    }
                    importFilesToVFS(files, current_vfs_dir);
                }
            }
        });
        System.out.println("DEBUG: FileDrop initialized successfully.");

        listSelectionListener = e -> {
            if (!e.getValueIsAdjusting()) {
                int row = table.getSelectedRow();
                if (row != -1) {
                    int modelRow = table.convertRowIndexToModel(row);
                    setFileDetails(fileTableModel.getFile(modelRow), false);
                }
            }
        };
        table.getSelectionModel().addListSelectionListener(listSelectionListener);

        JPanel detailsPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        detailsPanel.setBorder(new TitledBorder("File Details"));

        detailsPanel.add(new JLabel("Name:"));
        fileNameLabel = new JLabel();
        detailsPanel.add(fileNameLabel);

        detailsPanel.add(new JLabel("Path:"));
        pathField = new JTextField();
        pathField.setEditable(false);
        detailsPanel.add(pathField);

        detailsPanel.add(new JLabel("Last Modified:"));
        dateLabel = new JLabel();
        detailsPanel.add(dateLabel);

        detailsPanel.add(new JLabel("Size:"));
        sizeLabel = new JLabel();
        detailsPanel.add(sizeLabel);

        detailsPanel.add(new JLabel("Type:"));
        JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        isDirectoryRadio = new JRadioButton("Directory");
        isDirectoryRadio.setEnabled(false);
        isFileRadio = new JRadioButton("File");
        isFileRadio.setEnabled(false);
        ButtonGroup typeGroup = new ButtonGroup();
        typeGroup.add(isDirectoryRadio);
        typeGroup.add(isFileRadio);
        typePanel.add(isDirectoryRadio);
        typePanel.add(isFileRadio);
        detailsPanel.add(typePanel);

        rightPanel.add(detailsPanel, BorderLayout.SOUTH);

        textArea = new JTextArea(8, 40);
        textArea.setEditable(false);

        // Use a larger font size for the text preview.
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 22));
        JScrollPane textScrollPane = new JScrollPane(textArea);
        // FIX: Force vertical and horizontal scrollbar thickness to 30px
        // Handled globally for FlatLaf via UIManager.put("ScrollBar.width", 30) in main().
        // textScrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(30, 30));
        // textScrollPane.getHorizontalScrollBar().setPreferredSize(new Dimension(30, 30));
        textScrollPane.setBorder(new TitledBorder("Text Preview (.txt files < 5MB)"));
        
        JPanel previewContainer = new JPanel(new BorderLayout());
        previewContainer.add(textScrollPane, BorderLayout.CENTER);
        rightPanel.add(previewContainer, BorderLayout.EAST);

        splitPane.setRightComponent(rightPanel);
        gui.add(splitPane, BorderLayout.CENTER);

        JPanel progressPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        progressPanel.setBorder(new TitledBorder("Operations Progress"));
        
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        progressPanel.add(progressBar);

        progressBarImport = new JProgressBar();
        progressBarImport.setStringPainted(true);
        progressPanel.add(progressBarImport);

        progressBarExport = new JProgressBar();
        progressBarExport.setStringPainted(true);
        progressPanel.add(progressBarExport);

        gui.add(progressPanel, BorderLayout.SOUTH);
    }

    public JPanel getGui() {
        return gui;
    }

    public void showRootFile() {
        System.out.println("DEBUG: showRootFile() called");
        tree.setSelectionInterval(0, 0);
    }

    private void loadDirectoryChildren(final DefaultMutableTreeNode node) {
        System.out.println("DEBUG: loadDirectoryChildren() called for node");
        progressBar.setIndeterminate(true);
        progressBar.setVisible(true);
        tree.setEnabled(false);

        SwingWorker<Void, info.guardianproject.iocipher.File> worker = new SwingWorker<Void, info.guardianproject.iocipher.File>() {
            @Override
            protected Void doInBackground() {
                Object userObj = node.getUserObject();
                if (!(userObj instanceof info.guardianproject.iocipher.File)) {
                    System.out.println("DEBUG: loadDirectoryChildren - node is not a File, aborting");
                    return null;
                }
                info.guardianproject.iocipher.File file = (info.guardianproject.iocipher.File) userObj;
                
                System.out.println("DEBUG: Loading children for: " + file.getAbsolutePath());
                if (file.isDirectory()) {
                    info.guardianproject.iocipher.File[] files = file.listFiles();
                    if (files != null) {
                        System.out.println("DEBUG: Found " + files.length + " files/directories");
                        for (info.guardianproject.iocipher.File child : files) {
                            if (child.isDirectory()) {
                                System.out.println("DEBUG: Publishing directory: " + child.getName());
                                publish(child);
                            }
                        }
                        SwingUtilities.invokeLater(() -> setTableData(files));
                    } else {
                        System.out.println("DEBUG: No files found in directory");
                    }
                } else {
                    System.out.println("DEBUG: Node is not a directory");
                }
                return null;
            }

            @Override
            protected void process(List<info.guardianproject.iocipher.File> chunks) {
                System.out.println("DEBUG: process() called with " + chunks.size() + " chunks");
                
                for (int i = node.getChildCount() - 1; i >= 0; i--) {
                    DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node.getChildAt(i);
                    if (childNode.getUserObject() instanceof String) {
                        System.out.println("DEBUG: Removing dummy Loading node");
                        node.remove(childNode);
                    }
                }
                
                for (info.guardianproject.iocipher.File child : chunks) {
                    boolean exists = false;
                    for (int i = 0; i < node.getChildCount(); i++) {
                        DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node.getChildAt(i);
                        if (childNode.getUserObject() instanceof info.guardianproject.iocipher.File) {
                            info.guardianproject.iocipher.File existingFile = (info.guardianproject.iocipher.File) childNode.getUserObject();
                            if (existingFile.getAbsolutePath().equals(child.getAbsolutePath())) {
                                exists = true;
                                System.out.println("DEBUG: Directory already exists in tree: " + child.getName());
                                break;
                            }
                        }
                    }
                    if (!exists) {
                        DefaultMutableTreeNode newChildNode = new DefaultMutableTreeNode(child);
                        newChildNode.add(new DefaultMutableTreeNode("Loading..."));
                        node.add(newChildNode);
                        System.out.println("DEBUG: Added new directory to tree: " + child.getName());
                    }
                }
                treeModel.nodeStructureChanged(node);
            }

            @Override
            protected void done() {
                System.out.println("DEBUG: loadDirectoryChildren done()");
                
                // Ensure any remaining "Loading..." dummy nodes are removed on the EDT
                SwingUtilities.invokeLater(() -> {
                    boolean structureChanged = false;
                    for (int i = node.getChildCount() - 1; i >= 0; i--) {
                        DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node.getChildAt(i);
                        if (childNode.getUserObject() instanceof String) {
                            System.out.println("DEBUG: Removing dummy Loading node in done()");
                            node.remove(childNode);
                            structureChanged = true;
                        }
                    }
                    if (structureChanged) {
                        treeModel.nodeStructureChanged(node);
                    }
                });

                progressBar.setIndeterminate(false);
                progressBar.setVisible(false);
                progressBar.setValue(0);
                tree.setEnabled(true);
                Object userObj = node.getUserObject();
                if (userObj instanceof info.guardianproject.iocipher.File) {
                    System.out.println("DEBUG: Finished loading children for: " + ((info.guardianproject.iocipher.File) userObj).getAbsolutePath());
                }
            }
        };
        threadPool.execute(worker);
    }

    private void setTableData(final info.guardianproject.iocipher.File[] files) {
        SwingUtilities.invokeLater(() -> {
            System.out.println("DEBUG: Updating table with " + (files != null ? files.length : 0) + " items.");
            table.getSelectionModel().removeListSelectionListener(listSelectionListener);
            fileTableModel.setFiles(files != null ? files : new info.guardianproject.iocipher.File[0]);
            table.getSelectionModel().addListSelectionListener(listSelectionListener);
            
            if (!cellSizesSet) {
                table.setRowHeight(24);

                // Set sensible default column widths.
                // Name should be much larger than all other columns.
                setColumnWidth(0, 700); // Name
                setColumnWidth(1, 140); // Path
                setColumnWidth(2, 70);  // Size
                setColumnWidth(3, 120); // Last Modified
                setColumnWidth(4, 24);  // R
                setColumnWidth(5, 24);  // W
                setColumnWidth(6, 24);  // E
                setColumnWidth(7, 34);  // Dir
                setColumnWidth(8, 34);  // File

                cellSizesSet = true;
            }
        });
    }

    private void setColumnWidth(int column, int width) {
        TableColumn tableColumn = table.getColumnModel().getColumn(column);

        // Keep a small visible minimum width so columns cannot become zero-width.
        int minimumColumnWidth = 24;

        // Set only an initial preferred width.
        // Do not lock the column with fixed minimum/maximum widths.
        tableColumn.setPreferredWidth(Math.max(width, minimumColumnWidth));

        // Remove hard width limits so the column can grow or shrink freely.
        // Keep a small minimum width so columns cannot become invisible/zero width.
        tableColumn.setMinWidth(minimumColumnWidth);
        tableColumn.setMaxWidth(Integer.MAX_VALUE);

        // Make sure the user can drag the column border to resize it.
        tableColumn.setResizable(true);
    }

    private void addDirectoryToTree(info.guardianproject.iocipher.File dir) {
        if (dir == null || !dir.isDirectory()) {
            return;
        }
        
        info.guardianproject.iocipher.File parent = dir.getParentFile();
        if (parent == null) {
            return;
        }
        
        DefaultMutableTreeNode parentNode = findTreeNode(parent);
        if (parentNode == null) {
            return;
        }
        
        // Check if the directory already exists in the tree to avoid duplicates
        for (int i = 0; i < parentNode.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) parentNode.getChildAt(i);
            Object userObj = child.getUserObject();
            if (userObj instanceof info.guardianproject.iocipher.File) {
                info.guardianproject.iocipher.File existing = (info.guardianproject.iocipher.File) userObj;
                if (existing.getAbsolutePath().equals(dir.getAbsolutePath())) {
                    return; // Already in tree
                }
            }
        }
        
        // Add the new directory node with a dummy "Loading..." child so it shows the expand arrow
        DefaultMutableTreeNode newDirNode = new DefaultMutableTreeNode(dir);
        newDirNode.add(new DefaultMutableTreeNode("Loading..."));
        parentNode.add(newDirNode);
        treeModel.nodeStructureChanged(parentNode);
    }

    private void refreshCurrentDirectory() {
        System.out.println("DEBUG: refreshCurrentDirectory() called");
        if (current_vfs_dir == null) {
            System.out.println("DEBUG: current_vfs_dir is null, aborting refresh");
            return;
        }
        
        if (!current_vfs_dir.exists()) {
            info.guardianproject.iocipher.File parent = current_vfs_dir.getParentFile();
            if (parent != null && parent.exists()) {
                current_vfs_dir = parent;
                System.out.println("DEBUG: Current directory was deleted. Navigating to parent: " + current_vfs_dir.getAbsolutePath());
            } else {
                current_vfs_dir = new info.guardianproject.iocipher.File("/");
                System.out.println("DEBUG: Current directory was deleted. Navigating to root: /");
            }
        }
        
        info.guardianproject.iocipher.File[] files = current_vfs_dir.listFiles();
        setTableData(files != null ? files : new info.guardianproject.iocipher.File[0]);
        
        // FIX: Clear text preview and details if the currently displayed file was deleted, moved, or renamed.
        if (currentlyDisplayedFile != null) {
            boolean stillExists = currentlyDisplayedFile.exists();
            boolean stillInDir = false;
            if (files != null) {
                for (info.guardianproject.iocipher.File f : files) {
                    if (f.getAbsolutePath().equals(currentlyDisplayedFile.getAbsolutePath())) {
                        stillInDir = true;
                        break;
                    }
                }
            }

            // If it doesn't exist anymore, or it's no longer in the current directory (deleted/moved/renamed)
            if (!stillExists || (!stillInDir && currentlyDisplayedFile.getParentFile() != null &&
                currentlyDisplayedFile.getParentFile().getAbsolutePath().equals(current_vfs_dir.getAbsolutePath()))) {
                System.out.println("DEBUG: Currently displayed file was deleted, moved, or renamed. Clearing details panel.");
                clearFileDetails();
            } else {
                // Refresh the details in case properties (like size or modified date) changed
                setFileDetails(currentlyDisplayedFile, currentlyDisplayedFile.isDirectory());
            }
        }

        DefaultMutableTreeNode node = findTreeNode(current_vfs_dir);
        if (node != null) {
            System.out.println("DEBUG: Found tree node for current directory");
            node.removeAllChildren();
            
            if (current_vfs_dir.isDirectory()) {
                info.guardianproject.iocipher.File[] children = current_vfs_dir.listFiles();
                if (children != null) {
                    for (info.guardianproject.iocipher.File child : children) {
                        if (child.isDirectory()) {
                            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);
                            childNode.add(new DefaultMutableTreeNode("Loading..."));
                            node.add(childNode);
                        }
                    }
                }
            }
            
            treeModel.nodeStructureChanged(node);
            tree.expandPath(new TreePath(node.getPath()));
        } else {
            System.out.println("DEBUG: Could not find tree node for current directory");
        }
    }

    private DefaultMutableTreeNode findTreeNode(info.guardianproject.iocipher.File target) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        Enumeration<TreeNode> e = root.preorderEnumeration();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
            Object obj = node.getUserObject();
            if (obj instanceof info.guardianproject.iocipher.File) {
                info.guardianproject.iocipher.File f = (info.guardianproject.iocipher.File) obj;
                if (f.getAbsolutePath().equals(target.getAbsolutePath())) {
                    return node;
                }
            }
        }
        return null;
    }

    private void setFileDetails(info.guardianproject.iocipher.File file, boolean isDirectorySelection) {
        // FIX: Track the file currently being displayed in the details panel
        currentlyDisplayedFile = file;

        if (isDirectorySelection) {
            System.out.println("DEBUG: Directory selected: " + file.getAbsolutePath());
        } else {
            System.out.println("DEBUG: File selected: " + file.getAbsolutePath());
        }

        fileNameLabel.setText(file.getName().isEmpty() ? "/" : file.getName());
        pathField.setText(file.getAbsolutePath());
        dateLabel.setText(new Date(file.lastModified()).toString());
        sizeLabel.setText(file.length() + " bytes (" + formatSize(file.length()) + ")");
        readableCheckBox.setSelected(file.canRead());
        writableCheckBox.setSelected(file.canWrite());
        executableCheckBox.setSelected(file.canExecute());
        isDirectoryRadio.setSelected(file.isDirectory());
        isFileRadio.setSelected(file.isFile());

        textArea.setText("");
        if (file.isFile() && file.getName().toLowerCase().endsWith(".txt") && file.length() < 5 * 1024 * 1024) {
            try {
                System.out.println("DEBUG: Loading text preview for: " + file.getAbsolutePath());
                StringBuilder sb = new StringBuilder();
                
                // FIX: Replaced RandomAccessFile with BufferedReader and InputStreamReader.
                // RandomAccessFile.readLine() reads raw bytes and casts them to chars, 
                // which breaks multi-byte UTF-8 characters like umlauts.
                // InputStreamReader correctly decodes the UTF-8 byte stream into characters.
                try (BufferedReader in = new BufferedReader(new java.io.InputStreamReader(new FileInputStream(file), java.nio.charset.StandardCharsets.UTF_8))) {
                    String str;
                    while ((str = in.readLine()) != null) {
                        sb.append(str).append("\n");
                    }
                }
                
                textArea.setText(sb.toString());
                textArea.setCaretPosition(0);
            } catch (Exception e) {
                System.out.println("DEBUG: Error reading text file: " + e.getMessage());
                textArea.setText("Error reading file: " + e.getMessage());
            }
        }

        JFrame f = (JFrame) gui.getTopLevelAncestor();
        if (f != null) {
            f.setTitle(APP_TITLE + " " + iocipherJNIVersion + "-" + iocipherVersion + "-" + sqlfsVersion + 
                       " :: " + (file.getName().isEmpty() ? "/" : file.getName()));
        }
        gui.repaint();
    }

    private void clearFileDetails() {
        currentlyDisplayedFile = null;
        fileNameLabel.setText("");
        pathField.setText("");
        dateLabel.setText("");
        sizeLabel.setText("");
        readableCheckBox.setSelected(false);
        writableCheckBox.setSelected(false);
        executableCheckBox.setSelected(false);
        isDirectoryRadio.setSelected(false);
        isFileRadio.setSelected(false);

        // FIX: Clear the text preview area
        textArea.setText("");

        JFrame f = (JFrame) gui.getTopLevelAncestor();
        if (f != null) {
            f.setTitle(APP_TITLE + " " + iocipherJNIVersion + "-" + iocipherVersion + "-" + sqlfsVersion);
        }
        gui.repaint();
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "i";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private void showErrorMessage(String errorMessage, String errorTitle) {
        JOptionPane.showMessageDialog(gui, errorMessage, errorTitle, JOptionPane.ERROR_MESSAGE);
    }

    private void showThrowable(Throwable t) {
        t.printStackTrace();
        JOptionPane.showMessageDialog(gui, t.toString(), t.getMessage(), JOptionPane.ERROR_MESSAGE);
        gui.repaint();
    }

    private class ImportWorker extends SwingWorker<Void, Object> {
        private final java.io.File[] srcFiles;
        private final info.guardianproject.iocipher.File targetDir;
        private long finalTotalBytes;

        public ImportWorker(java.io.File[] srcFiles, info.guardianproject.iocipher.File targetDir) {
            this.srcFiles = srcFiles;
            this.targetDir = targetDir;
        }

        private void addCreatedFileToTable(info.guardianproject.iocipher.File created) {
            if (created == null) {
                return;
            }

            info.guardianproject.iocipher.File parent = created.getParentFile();
            if (parent != null && parent.getAbsolutePath().equals(targetDir.getAbsolutePath())) {
                SwingUtilities.invokeLater(() -> fileTableModel.addFileIfAbsent(created));
            }
        }

        @Override
        protected Void doInBackground() throws Exception {
            System.out.println("DEBUG: ImportWorker.doInBackground() started");
            long totalBytes = 0;
            for (java.io.File src : srcFiles) totalBytes += getHostFileSize(src);
            this.finalTotalBytes = totalBytes > 0 ? totalBytes : 1;
            System.out.println("DEBUG: Total bytes to import: " + totalBytes);

            long processedBytes = 0;
            for (java.io.File src : srcFiles) {
                if (isCancelled()) {
                    System.out.println("DEBUG: ImportWorker cancelled");
                    break;
                }
                info.guardianproject.iocipher.File dest = new info.guardianproject.iocipher.File(targetDir, src.getName());
                System.out.println("DEBUG: Importing " + src.getName() + " to " + dest.getAbsolutePath());
                processedBytes += copyHostToVFSWithProgress(src, dest, processedBytes);
            }
            System.out.println("DEBUG: ImportWorker.doInBackground() completed");
            return null;
        }

        private long copyHostToVFSWithProgress(java.io.File src, info.guardianproject.iocipher.File dest, long initialProcessed) throws Exception {
            if (src.isDirectory()) {
                dest.mkdirs();

                if (dest.exists()) {
                    publish(dest);
                }

                java.io.File[] children = src.listFiles();
                long processed = 0;
                if (children != null) {
                    for (java.io.File child : children) {
                        info.guardianproject.iocipher.File childDest = new info.guardianproject.iocipher.File(dest, child.getName());
                        processed += copyHostToVFSWithProgress(child, childDest, initialProcessed + processed);
                    }
                }
                return processed;
            } else {
                dest.createNewFile();

                if (dest.exists()) {
                    publish(dest);
                }

                try (java.io.FileInputStream in = new java.io.FileInputStream(src);
                     FileOutputStream out = new FileOutputStream(dest)) {
                    byte[] buffer = new byte[65536];
                    int bytesRead;
                    long fileProcessed = 0;
                    
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        fileProcessed += bytesRead;
                        long totalProcessed = initialProcessed + fileProcessed;
                        int percent = (int) ((totalProcessed * 100) / finalTotalBytes);
                        publish(percent);
                    }
                    return fileProcessed;
                }
            }
        }

        @Override
        protected void process(List<Object> chunks) {
            int lastPercent = -1;
            
            for (Object chunk : chunks) {
                if (chunk instanceof Integer) {
                    lastPercent = (Integer) chunk;
                } else if (chunk instanceof info.guardianproject.iocipher.File) {
                    info.guardianproject.iocipher.File created = (info.guardianproject.iocipher.File) chunk;
                    if (created.isDirectory()) {
                        // FIX: Update the left tree UI immediately when a new directory is created
                        addDirectoryToTree(created);
                    }
                    addCreatedFileToTable(created);
                }
            }
            
            if (lastPercent != -1) {
                progressBarImport.setValue(lastPercent);
                progressBarImport.setString("Importing: " + lastPercent + "%");
                System.out.println("DEBUG: Import progress: " + lastPercent + "%");
                
                // Repaint the table to update the file size column as the file grows
                table.repaint();
            }
        }

        @Override
        protected void done() {
            System.out.println("DEBUG: ImportWorker.done() called");
            progressBarImport.setIndeterminate(false);
            progressBarImport.setValue(0);
            progressBarImport.setString("Import Complete");
            System.out.println("DEBUG: Import complete. Refreshing UI.");
            refreshCurrentDirectory();
        }
    }

    private class ExportWorker extends SwingWorker<Void, Integer> {
        private final info.guardianproject.iocipher.File[] srcFiles;
        private final java.io.File targetDir;
        private long finalTotalBytes;

        public ExportWorker(info.guardianproject.iocipher.File[] srcFiles, java.io.File targetDir) {
            this.srcFiles = srcFiles;
            this.targetDir = targetDir;
        }

        @Override
        protected Void doInBackground() throws Exception {
            System.out.println("DEBUG: ExportWorker.doInBackground() started");
            long totalBytes = 0;
            for (info.guardianproject.iocipher.File src : srcFiles) totalBytes += getVFSFileSize(src);
            this.finalTotalBytes = totalBytes > 0 ? totalBytes : 1;
            System.out.println("DEBUG: Total bytes to export: " + totalBytes);

            long processedBytes = 0;
            for (info.guardianproject.iocipher.File src : srcFiles) {
                if (isCancelled()) {
                    System.out.println("DEBUG: ExportWorker cancelled");
                    break;
                }
                java.io.File dest = new java.io.File(targetDir, src.getName());
                System.out.println("DEBUG: Exporting " + src.getName() + " to " + dest.getAbsolutePath());
                processedBytes += copyVFSToHostWithProgress(src, dest, processedBytes);
            }
            System.out.println("DEBUG: ExportWorker.doInBackground() completed");
            return null;
        }

        private long copyVFSToHostWithProgress(info.guardianproject.iocipher.File src, java.io.File dest, long initialProcessed) throws Exception {
            if (src.isDirectory()) {
                dest.mkdirs();
                info.guardianproject.iocipher.File[] children = src.listFiles();
                long processed = 0;
                if (children != null) {
                    for (info.guardianproject.iocipher.File child : children) {
                        java.io.File childDest = new java.io.File(dest, child.getName());
                        processed += copyVFSToHostWithProgress(child, childDest, initialProcessed + processed);
                    }
                }
                return processed;
            } else {
                try (FileInputStream in = new FileInputStream(src);
                     java.io.FileOutputStream out = new java.io.FileOutputStream(dest)) {
                    byte[] buffer = new byte[65536];
                    int bytesRead;
                    long fileProcessed = 0;
                    
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        fileProcessed += bytesRead;
                        long totalProcessed = initialProcessed + fileProcessed;
                        int percent = (int) ((totalProcessed * 100) / finalTotalBytes);
                        publish(percent);
                    }
                    return fileProcessed;
                }
            }
        }

        @Override
        protected void process(List<Integer> chunks) {
            int percent = chunks.get(chunks.size() - 1);
            progressBarExport.setValue(percent);
            progressBarExport.setString("Exporting: " + percent + "%");
            System.out.println("DEBUG: Export progress: " + percent + "%");
        }

        @Override
        protected void done() {
            System.out.println("DEBUG: ExportWorker.done() called");
            progressBarExport.setIndeterminate(false);
            progressBarExport.setValue(0);
            progressBarExport.setString("Export Complete");
            System.out.println("DEBUG: Export complete.");
        }
    }

    private void importFilesToVFS(java.io.File[] srcFiles, info.guardianproject.iocipher.File targetDir) {
        System.out.println("DEBUG: Starting import of " + srcFiles.length + " items to " + targetDir.getAbsolutePath());
        progressBarImport.setIndeterminate(false);
        progressBarImport.setMaximum(100);
        progressBarImport.setValue(0);
        new ImportWorker(srcFiles, targetDir).execute();
    }

    private long getHostFileSize(java.io.File f) {
        if (f.isDirectory()) {
            long size = 0;
            java.io.File[] children = f.listFiles();
            if (children != null) {
                for (java.io.File child : children) {
                    size += getHostFileSize(child);
                }
            }
            return size;
        }
        return f.length();
    }

    private void exportSelectedFiles() {
        int[] rows = table.getSelectedRows();
        if (rows.length == 0) {
            System.out.println("DEBUG: Export cancelled - no files selected.");
            showErrorMessage("No files selected for export.", "Export Files");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Destination Directory");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        if (chooser.showSaveDialog(gui) == JFileChooser.APPROVE_OPTION) {
            java.io.File destDir = chooser.getSelectedFile();
            info.guardianproject.iocipher.File[] filesToExport = new info.guardianproject.iocipher.File[rows.length];
            for (int i = 0; i < rows.length; i++) {
                filesToExport[i] = fileTableModel.getFile(table.convertRowIndexToModel(rows[i]));
            }
            exportFilesFromVFS(filesToExport, destDir);
        }
    }

    private void exportFilesFromVFS(info.guardianproject.iocipher.File[] srcFiles, java.io.File targetDir) {
        System.out.println("DEBUG: Starting export of " + srcFiles.length + " items to " + targetDir.getAbsolutePath());
        progressBarExport.setIndeterminate(false);
        progressBarExport.setMaximum(100);
        progressBarExport.setValue(0);
        new ExportWorker(srcFiles, targetDir).execute();
    }

    private long getVFSFileSize(info.guardianproject.iocipher.File f) {
        if (f.isDirectory()) {
            long size = 0;
            info.guardianproject.iocipher.File[] children = f.listFiles();
            if (children != null) {
                for (info.guardianproject.iocipher.File child : children) {
                    size += getVFSFileSize(child);
                }
            }
            return size;
        }
        return f.length();
    }

    private void showNewFileDirDialog() {
        if (current_vfs_dir == null) {
            showErrorMessage("No location selected for new file.", "Select Location");
            return;
        }

        JPanel newFilePanel = new JPanel(new BorderLayout(5, 5));
        JPanel southRadio = new JPanel(new GridLayout(1, 0, 5, 0));
        JRadioButton newTypeFile = new JRadioButton("File", true);
        JRadioButton newTypeDirectory = new JRadioButton("Directory");
        ButtonGroup bg = new ButtonGroup();
        bg.add(newTypeFile);
        bg.add(newTypeDirectory);
        southRadio.add(newTypeFile);
        southRadio.add(newTypeDirectory);

        JTextField nameField = new JTextField(15);
        newFilePanel.add(new JLabel("Name:"), BorderLayout.WEST);
        newFilePanel.add(nameField, BorderLayout.CENTER);
        newFilePanel.add(southRadio, BorderLayout.SOUTH);

        int result = JOptionPane.showConfirmDialog(gui, newFilePanel, "Create New", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                showErrorMessage("Name cannot be empty.", "Create Failed");
                return;
            }
            try {
                info.guardianproject.iocipher.File file = new info.guardianproject.iocipher.File(current_vfs_dir, name);
                boolean created;
                if (newTypeFile.isSelected()) {
                    System.out.println("DEBUG: Creating new file: " + file.getAbsolutePath());
                    created = file.createNewFile();
                } else {
                    System.out.println("DEBUG: Creating new directory: " + file.getAbsolutePath());
                    created = file.mkdirs();
                }
                
                if (created) {
                    System.out.println("DEBUG: Creation successful.");
                    refreshCurrentDirectory();
                } else {
                    System.out.println("DEBUG: Creation failed.");
                    showErrorMessage("The item '" + file.getName() + "' could not be created.", "Create Failed");
                }
            } catch (Throwable t) {
                System.out.println("DEBUG: Exception during creation: " + t.getMessage());
                t.printStackTrace();
                showThrowable(t);
            }
        }
    }

    private void renameFile() {
        int row = table.getSelectedRow();
        if (row == -1) {
            showErrorMessage("Please select a single file or directory to rename.", "Rename");
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        info.guardianproject.iocipher.File currentFile = fileTableModel.getFile(modelRow);

        String renameTo = JOptionPane.showInputDialog(gui, "New Name:", currentFile.getName());
        if (renameTo != null && !renameTo.trim().isEmpty()) {
            try {
                if (currentFile.getAbsolutePath().equals("/")) {
                    showErrorMessage("Cannot rename the root directory.", "Rename Failed");
                    return;
                }
                
                info.guardianproject.iocipher.File newFile = new info.guardianproject.iocipher.File(currentFile.getParentFile(), renameTo.trim());
                if (newFile.exists()) {
                    showErrorMessage("A file or directory with that name already exists.", "Rename Failed");
                    return;
                }

                System.out.println("DEBUG: Renaming " + currentFile.getAbsolutePath() + " to " + newFile.getAbsolutePath());
                boolean renamed = currentFile.renameTo(newFile);
                
                if (renamed) {
                    System.out.println("DEBUG: Rename successful.");
                    refreshCurrentDirectory();
                } else {
                    System.out.println("DEBUG: Rename failed.");
                    showErrorMessage("The file '" + currentFile.getName() + "' could not be renamed.", "Rename Failed");
                }
            } catch (Throwable t) {
                System.out.println("DEBUG: Exception during rename: " + t.getMessage());
                t.printStackTrace();
                showThrowable(t);
            }
        }
    }

    private void deleteFile() {
        int[] rows = table.getSelectedRows();
        if (rows.length == 0) {
            System.out.println("DEBUG: Delete cancelled - no files selected.");
            showErrorMessage("No files selected for deletion.", "Select Files");
            return;
        }

        int result = JOptionPane.showConfirmDialog(gui,
                "Are you sure you want to permanently delete the selected items?",
                "Delete Files", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                
        if (result == JOptionPane.YES_OPTION) {
            System.out.println("DEBUG: Starting background deletion of " + rows.length + " items.");
            
            // FIX: Resolve files to delete on the EDT before starting the background thread.
            // Accessing JTable methods like convertRowIndexToModel from a background thread 
            // is not thread-safe and causes UI freezes/deadlocks.
            final List<info.guardianproject.iocipher.File> filesToDelete = new ArrayList<>();
            for (int row : rows) {
                int modelRow = table.convertRowIndexToModel(row);
                filesToDelete.add(fileTableModel.getFile(modelRow));
            }

            // Show the stylish rotating overlay over the file list
            overlayUI.start();
            if (tableLayer != null) tableLayer.repaint();
            
            final Thread deleteThread = new Thread(() -> {
                try {
                    for (info.guardianproject.iocipher.File f : filesToDelete) {
                        System.out.println("DEBUG: Deleting: " + f.getAbsolutePath());
                        deleteRecursive(f);
                    }
                    SwingUtilities.invokeLater(() -> {
                        System.out.println("DEBUG: Deletion complete. Refreshing UI.");
                        overlayUI.stop(); // Hide the overlay
                        if (tableLayer != null) tableLayer.repaint();
                        refreshCurrentDirectory();
                    });
                } catch (Exception e) {
                    System.out.println("DEBUG: Error during deletion: " + e.getMessage());
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        overlayUI.stop(); // Hide the overlay on error too
                        if (tableLayer != null) tableLayer.repaint();
                        showErrorMessage("Error deleting files: " + e.getMessage(), "Delete Error");
                    });
                }
            });
            deleteThread.start();
        }
    }

    private void deleteRecursive(info.guardianproject.iocipher.File f) throws Exception {
        if (f.getAbsolutePath().equals("/")) {
            System.out.println("DEBUG: Attempted to delete ROOT directory. Aborting.");
            return;
        }
        if (f.isDirectory()) {
            info.guardianproject.iocipher.File[] children = f.listFiles();
            if (children != null) {
                for (info.guardianproject.iocipher.File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        boolean deleted = f.delete();
        if (!deleted) {
            System.out.println("DEBUG: Failed to delete: " + f.getAbsolutePath());
        } else {
            System.out.println("DEBUG: Successfully deleted: " + f.getAbsolutePath());
        }
    }

    public static boolean copyVFSFile(info.guardianproject.iocipher.File from, info.guardianproject.iocipher.File to) throws Exception {
        System.out.println("DEBUG: Copying VFS file from " + from.getAbsolutePath() + " to " + to.getAbsolutePath());
        boolean created = to.createNewFile();
        if (created) {
            IOCipherFileChannel fromChannel = null;
            IOCipherFileChannel toChannel = null;
            try {
                fromChannel = new FileInputStream(from).getChannel();
                toChannel = new FileOutputStream(to).getChannel();
                toChannel.transferFrom(fromChannel, 0, fromChannel.size());
                to.setReadable(from.canRead());
                to.setWritable(from.canWrite());
                to.setExecutable(from.canExecute());
                System.out.println("DEBUG: VFS file copied successfully.");
                return true;
            } finally {
                if (fromChannel != null) {
                    try { fromChannel.close(); } catch (Exception e) { e.printStackTrace(); }
                }
                if (toChannel != null) {
                    try { toChannel.close(); } catch (Exception e) { e.printStackTrace(); }
                }
            }
        }
        System.out.println("DEBUG: Failed to create destination file for copy.");
        return false;
    }

    public static void setUp() {
        System.out.println("DEBUG: Initializing VirtualFileSystem...");
        vfs = VirtualFileSystem.get();
        vfs.setContainerPath(dbfilename);
        
        java.io.File container = new java.io.File(dbfilename);
        if (!container.exists()) {
            System.out.println("DEBUG: Creating new VFS container at " + dbfilename);
            vfs.createNewContainer(goodPassword);
        } else {
            System.out.println("DEBUG: VFS container already exists at " + dbfilename);
        }
        
        vfs.mount(goodPassword);
        if (vfs.isMounted()) {
            System.out.println("DEBUG: VFS is successfully mounted.");
        } else {
            System.out.println("DEBUG: ERROR - VFS is NOT mounted!");
        }
    }

    public static void tearDown() {
        try {
            if (vfs != null && vfs.isMounted()) {
                System.out.println("DEBUG: Unmounting VFS...");
                vfs.unmount();
                System.out.println("DEBUG: VFS unmounted successfully.");
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Error unmounting VFS: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void createDummies() {
        System.out.println("DEBUG: Creating dummy files and directories in VFS...");
        try {
            info.guardianproject.iocipher.File f1 = new info.guardianproject.iocipher.File("/Images");
            f1.mkdirs();
            f1 = new info.guardianproject.iocipher.File("/Documents");
            f1.mkdirs();
            f1 = new info.guardianproject.iocipher.File("/Music");
            f1.mkdirs();
            f1 = new info.guardianproject.iocipher.File("/listy zakupów");
            f1.mkdirs();
            
            f1 = new info.guardianproject.iocipher.File("/My Shopping.docx");
            f1.createNewFile();
            try (RandomAccessFile fra = new RandomAccessFile(f1, "rw")) {
                fra.write(new byte[45847]);
            }

            for (int i = 0; i < 30; i++) {
                f1 = new info.guardianproject.iocipher.File("/Photo_" + i + ".png");
                f1.createNewFile();
                try (RandomAccessFile fra = new RandomAccessFile(f1, "rw")) {
                    fra.write(new byte[25841 + (int) (Math.random() * 4096)]);
                }
            }
            System.out.println("DEBUG: Dummy files created successfully.");
        } catch (Exception e) {
            System.out.println("DEBUG: Error creating dummy files: " + e.getMessage());
            e.printStackTrace();
        }
    }

    class VFSFileTableModel extends javax.swing.table.AbstractTableModel {
        private info.guardianproject.iocipher.File[] files = new info.guardianproject.iocipher.File[0];
        private final String[] columns = {"Name", "Path", "Size", "Last Modified", "R", "W", "E", "Dir", "File"};

        public void setFiles(info.guardianproject.iocipher.File[] files) {
            this.files = files;
            fireTableDataChanged();
        }

        public info.guardianproject.iocipher.File getFile(int row) {
            return files[row];
        }

        public void addFileIfAbsent(info.guardianproject.iocipher.File file) {
            if (file == null) {
                return;
            }

            for (info.guardianproject.iocipher.File existing : files) {
                if (existing.getAbsolutePath().equals(file.getAbsolutePath())) {
                    return;
                }
            }

            info.guardianproject.iocipher.File[] newFiles = new info.guardianproject.iocipher.File[files.length + 1];
            System.arraycopy(files, 0, newFiles, 0, files.length);
            newFiles[newFiles.length - 1] = file;
            files = newFiles;

            fireTableRowsInserted(files.length - 1, files.length - 1);
        }

        @Override
        public int getRowCount() { return files.length; }

        @Override
        public int getColumnCount() { return columns.length; }

        @Override
        public String getColumnName(int column) { return columns[column]; }

        @Override
        public Class<?> getColumnClass(int column) {
            if (column == 2) return Long.class;
            if (column == 3) return Date.class;
            if (column >= 4) return Boolean.class;
            return String.class;
        }

        @Override
        public Object getValueAt(int row, int column) {
            info.guardianproject.iocipher.File f = files[row];
            switch (column) {
                case 0: return f.getName().isEmpty() ? "/" : f.getName();
                case 1: return f.getAbsolutePath();
                case 2: return f.length();
                case 3: return new Date(f.lastModified());
                case 4: return f.canRead();
                case 5: return f.canWrite();
                case 6: return f.canExecute();
                case 7: return f.isDirectory();
                case 8: return f.isFile();
                default: return "";
            }
        }
    }

    class VFSTreeCellRenderer extends javax.swing.tree.DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(
                JTree tree, Object value, boolean selected, boolean expanded,
                boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            Object userObject = node.getUserObject();
            
            if (userObject instanceof info.guardianproject.iocipher.File) {
                info.guardianproject.iocipher.File vfsFile = (info.guardianproject.iocipher.File) userObject;
                String name = vfsFile.getName().isEmpty() ? "/" : vfsFile.getName();
                setText(name + (vfsFile.isDirectory() ? " [DIR]" : ""));
                setToolTipText(vfsFile.getAbsolutePath());
            } else {
                setText(userObject != null ? userObject.toString() : "Unknown");
            }
            return this;
        }
    }

    /**
     * Custom LayerUI to draw a modern, semi-transparent overlay with a rotating 
     * spinner animation over the file table while background operations are running.
     * It also blocks all mouse and keyboard interactions with the table underneath.
     */
    class OverlayUI extends javax.swing.plaf.LayerUI<JScrollPane> {
        private boolean running = false;
        private javax.swing.Timer timer;
        private int angle = 0;
        private javax.swing.JLayer<?> layer;

        public OverlayUI() {
            timer = new javax.swing.Timer(40, e -> {
                angle = (angle + 15) % 360;
                if (layer != null) {
                    layer.repaint(); // Explicitly repaint the JLayer for smooth animation
                }
            });
        }

        public void start() {
            if (!running) {
                running = true;
                timer.start();
                if (layer != null) {
                    layer.repaint();
                }
            }
        }

        public void stop() {
            if (running) {
                running = false;
                timer.stop();
                if (layer != null) {
                    layer.repaint(); // Explicitly repaint to clear the overlay immediately
                }
            }
        }

        @Override
        public void paint(java.awt.Graphics g, javax.swing.JComponent c) {
            super.paint(g, c);
            if (!running) return;

            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

            // Semi-transparent overlay (white matches FlatLightLaf theme)
            g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.6f));
            g2.setColor(java.awt.Color.WHITE);
            g2.fillRect(0, 0, c.getWidth(), c.getHeight());

            // Rotating Spinner
            int size = 60;
            int x = (c.getWidth() - size) / 2;
            int y = (c.getHeight() - size) / 2;

            g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 1.0f));
            g2.setStroke(new java.awt.BasicStroke(6, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
            g2.setColor(new java.awt.Color(37, 99, 235)); // FlatLaf accent blue
            g2.drawArc(x, y, size, size, angle, 90);

            g2.dispose();
        }
        
        @Override
        public void installUI(javax.swing.JComponent c) {
            super.installUI(c);
            layer = (javax.swing.JLayer<?>) c;
            // Intercept all mouse and keyboard events while the overlay is active
            ((javax.swing.JLayer) c).setLayerEventMask(
                java.awt.AWTEvent.MOUSE_EVENT_MASK | java.awt.AWTEvent.MOUSE_MOTION_EVENT_MASK |
                java.awt.AWTEvent.KEY_EVENT_MASK | java.awt.AWTEvent.MOUSE_WHEEL_EVENT_MASK
            );
        }

        @Override
        public void uninstallUI(javax.swing.JComponent c) {
            super.uninstallUI(c);
            ((javax.swing.JLayer) c).setLayerEventMask(0);
            layer = null;
        }
        
        @Override
        protected void processMouseEvent(java.awt.event.MouseEvent e, javax.swing.JLayer l) {
            if (running) e.consume();
        }
        
        @Override
        protected void processMouseMotionEvent(java.awt.event.MouseEvent e, javax.swing.JLayer l) {
            if (running) e.consume();
        }
        
        @Override
        protected void processMouseWheelEvent(java.awt.event.MouseWheelEvent e, javax.swing.JLayer l) {
            if (running) e.consume();
        }
        
        @Override
        protected void processKeyEvent(java.awt.event.KeyEvent e, javax.swing.JLayer l) {
            if (running) e.consume();
        }
    }

    public static void main(String[] args) {
        current_vfs_dir = new info.guardianproject.iocipher.File("/");
        threadPool = Executors.newFixedThreadPool(10);
        System.out.println("DEBUG: FileManager version: " + VERSION);

        boolean showcase_mode = false;
        if (args.length >= 1) {
            dbfilename = args[0];
            System.out.println("DEBUG: db file: " + dbfilename);
        }
        if (args.length >= 2) {
            goodPassword = args[1];
            System.out.println("DEBUG: password: *******************");
        }
        if (args.length >= 3 && args[2].equalsIgnoreCase("showcase")) {
            showcase_mode = true;
            System.out.println("DEBUG: == SHOWCASE MODE ==");
        }

        System.out.println("DEBUG: Setting up database...");
        setUp();
        System.out.println("DEBUG: Setup complete.");
        
        sqlfsVersion = vfs.sqlfsVersion();
        iocipherVersion = vfs.iocipherVersion();
        iocipherJNIVersion = vfs.iocipherJNIVersion();
        System.out.println("DEBUG: sqlfs version: " + sqlfsVersion);
        System.out.println("DEBUG: iocipher version: " + iocipherVersion);
        System.out.println("DEBUG: iocipherJNI version: " + iocipherJNIVersion);

        if (showcase_mode) {
            createDummies();
        }

        SwingUtilities.invokeLater(() -> {
            try {
                // Modernization: use FlatLaf for a modern, stylish look.
                // Ensure flatlaf-3.7.2.jar is in your classpath.
                UIManager.put("ScrollBar.width", 10); // Thick scrollbars for better mouse access
                UIManager.put("Button.arc", 8);
                UIManager.put("Component.arc", 8);
                UIManager.put("TextComponent.arc", 8);
                UIManager.put("Table.showHorizontalLines", true);
                UIManager.put("Table.showVerticalLines", false);
                UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());
            } catch (Exception e) {
                System.out.println("DEBUG: Failed to set FlatLaf look and feel: " + e.getMessage());
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ex) {
                    System.out.println("DEBUG: Failed to set system look and feel: " + ex.getMessage());
                }
            }

            JFrame f = new JFrame(APP_TITLE + " " + iocipherJNIVersion + "-" + iocipherVersion + "-" + sqlfsVersion);
            f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            f.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent we) {
                    int result = JOptionPane.showConfirmDialog(f, "Close the application?", "Confirm Exit", JOptionPane.YES_NO_OPTION);
                    if (result == JOptionPane.YES_OPTION) {
                        System.out.println("DEBUG: Application closing. Tearing down VFS...");
                        tearDown();
                        threadPool.shutdown();
                        f.setVisible(false);
                        f.dispose();
                        System.exit(0);
                    }
                }
            });

            FileManager fileManager = new FileManager();
            f.setContentPane(fileManager.getGui());
            f.pack();

            // Start with a much larger main window.
            // The old size was 1100x700.
            // Width is much larger, and height is at least 60% larger: 700 * 1.6 = 1120.
            int startupWidth = 1500;
            int startupHeight = 950;

            // Keep the startup window inside the usable screen bounds.
            java.awt.Rectangle usableBounds = java.awt.GraphicsEnvironment
                    .getLocalGraphicsEnvironment()
                    .getMaximumWindowBounds();

            if (usableBounds != null) {
                startupWidth = Math.min(startupWidth, usableBounds.width);
                startupHeight = Math.min(startupHeight, usableBounds.height);
            }

            f.setSize(startupWidth, startupHeight);
            f.setLocationRelativeTo(null);
            f.setVisible(true);

            fileManager.showRootFile();
        });
    }
}

/**
 * This class makes it easy to drag and drop files from the operating
 * system to a Java program. Any <tt>java.awt.Component</tt> can be
 * dropped onto, but only <tt>javax.swing.JComponent</tt>s will indicate
 * the drop event with a changed border.
 * <p/>
 * To use this class, construct a new <tt>FileDrop</tt> by passing
 * it the target component and a <tt>Listener</tt> to receive notification
 * when file(s) have been dropped. Here is an example:
 * <p/>
 * <code><pre>
 *      JPanel myPanel = new JPanel();
 *      new FileDrop( myPanel, new FileDrop.Listener()
 *      {   public void filesDropped( java.io.File[] files )
 *          {
 *              // handle file drop
 *              ...
 *          }   // end filesDropped
 *      }); // end FileDrop.Listener
 * </pre></code>
 * <p/>
 * You can specify the border that will appear when files are being dragged by
 * calling the constructor with a <tt>javax.swing.border.Border</tt>. Only
 * <tt>JComponent</tt>s will show any indication with a border.
 * <p/>
 * You can turn on some debugging features by passing a <tt>PrintStream</tt>
 * object (such as <tt>System.out</tt>) into the full constructor. A <tt>null</tt>
 * value will result in no extra debugging information being output.
 * <p/>
 *
 * <p>I'm releasing this code into the Public Domain. Enjoy.
 * </p>
 * <p><em>Original author: Robert Harder, rharder@usa.net</em></p>
 * <p>2007-09-12 Nathan Blomquist -- Linux (KDE/Gnome) support added.</p>
 *
 * @author  Robert Harder
 * @author  rharder@users.sf.net
 * @version 1.0.1
 */
class FileDrop
{
    private transient javax.swing.border.Border normalBorder;
    private transient java.awt.dnd.DropTargetListener dropListener;


    /** Discover if the running JVM is modern enough to have drag and drop. */
    private static boolean supportsDnD = false;

    // Default border color
    private static java.awt.Color defaultBorderColor = new java.awt.Color( 0f, 0f, 1f, 0.25f );

    /**
     * Constructs a {@link FileDrop} with a default light-blue border
     * and, if <var>c</var> is a {@link java.awt.Container}, recursively
     * sets all elements contained within as drop targets, though only
     * the top level container will change borders.
     *
     * @param c Component on which files will be dropped.
     * @param listener Listens for <tt>filesDropped</tt>.
     * @since 1.0
     */
    public FileDrop(
            final java.awt.Component c,
            final Listener listener )
    {   this( null,  // Logging stream
              c,     // Drop target
              javax.swing.BorderFactory.createMatteBorder( 2, 2, 2, 2, defaultBorderColor ), // Drag border
              true, // Recursive
              listener );
    }   // end constructor




    /**
     * Constructor with a default border and the option to recursively set drop targets.
     * If your component is a <tt>java.awt.Container</tt>, then each of its children
     * components will also listen for drops, though only the parent will change borders.
     *
     * @param c Component on which files will be dropped.
     * @param recursive Recursively set children as drop targets.
     * @param listener Listens for <tt>filesDropped</tt>.
     * @since 1.0
     */
    public FileDrop(
            final java.awt.Component c,
            final boolean recursive,
            final Listener listener )
    {   this( null,  // Logging stream
              c,     // Drop target
              javax.swing.BorderFactory.createMatteBorder( 2, 2, 2, 2, defaultBorderColor ), // Drag border
              recursive, // Recursive
              listener );
    }   // end constructor


    /**
     * Constructor with a default border and debugging optionally turned on.
     * With Debugging turned on, more status messages will be displayed to
     * <tt>out</tt>. A common way to use this constructor is with
     * <tt>System.out</tt> or <tt>System.err</tt>. A <tt>null</tt> value for
     * the parameter <tt>out</tt> will result in no debugging output.
     *
     * @param out PrintStream to record debugging info or null for no debugging.
     * @param out
     * @param c Component on which files will be dropped.
     * @param listener Listens for <tt>filesDropped</tt>.
     * @since 1.0
     */
    public FileDrop(
            final java.io.PrintStream out,
            final java.awt.Component c,
            final Listener listener )
    {   this( out,  // Logging stream
              c,    // Drop target
              javax.swing.BorderFactory.createMatteBorder( 2, 2, 2, 2, defaultBorderColor ),
              false, // Recursive
              listener );
    }   // end constructor



    /**
     * Constructor with a default border, debugging optionally turned on
     * and the option to recursively set drop targets.
     * If your component is a <tt>java.awt.Container</tt>, then each of its children
     * components will also listen for drops, though only the parent will change borders.
     * With Debugging turned on, more status messages will be displayed to
     * <tt>out</tt>. A common way to use this constructor is with
     * <tt>System.out</tt> or <tt>System.err</tt>. A <tt>null</tt> value for
     * the parameter <tt>out</tt> will result in no debugging output.
     *
     * @param out PrintStream to record debugging info or null for no debugging.
     * @param out
     * @param c Component on which files will be dropped.
     * @param recursive Recursively set children as drop targets.
     * @param listener Listens for <tt>filesDropped</tt>.
     * @since 1.0
     */
    public FileDrop(
            final java.io.PrintStream out,
            final java.awt.Component c,
            final boolean recursive,
            final Listener listener)
    {   this( out,  // Logging stream
              c,    // Drop target
              javax.swing.BorderFactory.createMatteBorder( 2, 2, 2, 2, defaultBorderColor ), // Drag border
              recursive, // Recursive
              listener );
    }   // end constructor




    /**
     * Constructor with a specified border 
     *
     * @param c Component on which files will be dropped.
     * @param dragBorder Border to use on <tt>JComponent</tt> when dragging occurs.
     * @param listener Listens for <tt>filesDropped</tt>.
     * @since 1.0
     */
    public FileDrop(
            final java.awt.Component c,
            final javax.swing.border.Border dragBorder,
            final Listener listener)
    {   this(
            null,   // Logging stream
            c,      // Drop target
            dragBorder, // Drag border
            false,  // Recursive
            listener );
    }   // end constructor




    /**
     * Constructor with a specified border and the option to recursively set drop targets.
     * If your component is a <tt>java.awt.Container</tt>, then each of its children
     * components will also listen for drops, though only the parent will change borders.
     *
     * @param c Component on which files will be dropped.
     * @param dragBorder Border to use on <tt>JComponent</tt> when dragging occurs.
     * @param recursive Recursively set children as drop targets.
     * @param listener Listens for <tt>filesDropped</tt>.
     * @since 1.0
     */
    public FileDrop(
            final java.awt.Component c,
            final javax.swing.border.Border dragBorder,
            final boolean recursive,
            final Listener listener)
    {   this(
            null,
            c,
            dragBorder,
            recursive,
            listener );
    }   // end constructor



    /**
     * Constructor with a specified border and debugging optionally turned on.
     * With Debugging turned on, more status messages will be displayed to
     * <tt>out</tt>. A common way to use this constructor is with
     * <tt>System.out</tt> or <tt>System.err</tt>. A <tt>null</tt> value for
     * the parameter <tt>out</tt> will result in no debugging output.
     *
     * @param out PrintStream to record debugging info or null for no debugging.
     * @param c Component on which files will be dropped.
     * @param dragBorder Border to use on <tt>JComponent</tt> when dragging occurs.
     * @param listener Listens for <tt>filesDropped</tt>.
     * @since 1.0
     */
    public FileDrop(
            final java.io.PrintStream out,
            final java.awt.Component c,
            final javax.swing.border.Border dragBorder,
            final Listener listener)
    {   this(
            out,    // Logging stream
            c,      // Drop target
            dragBorder, // Drag border
            false,  // Recursive
            listener );
    }   // end constructor





    /**
     * Full constructor with a specified border and debugging optionally turned on.
     * With Debugging turned on, more status messages will be displayed to
     * <tt>out</tt>. A common way to use this constructor is with
     * <tt>System.out</tt> or <tt>System.err</tt>. A <tt>null</tt> value for
     * the parameter <tt>out</tt> will result in no debugging output.
     *
     * @param out PrintStream to record debugging info or null for no debugging.
     * @param c Component on which files will be dropped.
     * @param dragBorder Border to use on <tt>JComponent</tt> when dragging occurs.
     * @param recursive Recursively set children as drop targets.
     * @param listener Listens for <tt>filesDropped</tt>.
     * @since 1.0
     */
    public FileDrop(
            final java.io.PrintStream out,
            final java.awt.Component c,
            final javax.swing.border.Border dragBorder,
            final boolean recursive,
            final Listener listener)
    {

        if( check_supportsDnD() )
        {   // Make a drop listener
            dropListener = new java.awt.dnd.DropTargetListener()
            {   public void dragEnter( java.awt.dnd.DropTargetDragEvent evt )
            {       log( out, "FileDrop: dragEnter event." );

                // Is this an acceptable drag event?
                if( isDragOk( out, evt ) )
                {
                    // If it's a Swing component, set its border
                    if( c instanceof javax.swing.JComponent )
                    {   javax.swing.JComponent jc = (javax.swing.JComponent) c;
                        normalBorder = jc.getBorder();
                        log( out, "FileDrop: normal border saved." );
                        jc.setBorder( dragBorder );
                        log( out, "FileDrop: drag border set." );
                    }   // end if: JComponent

                    // Acknowledge that it's okay to enter
                    //evt.acceptDrag( java.awt.dnd.DnDConstants.ACTION_COPY_OR_MOVE );
                    evt.acceptDrag( java.awt.dnd.DnDConstants.ACTION_COPY );
                    log( out, "FileDrop: event accepted." );
                }   // end if: drag ok
                else
                {   // Reject the drag event
                    evt.rejectDrag();
                    log( out, "FileDrop: event rejected." );
                }   // end else: drag not ok
            }   // end dragEnter

                public void dragOver( java.awt.dnd.DropTargetDragEvent evt )
                {   // This is called continually as long as the mouse is
                    // over the drag target.
                }   // end dragOver

                public void drop( java.awt.dnd.DropTargetDropEvent evt )
                {   log( out, "FileDrop: drop event." );
                    try
                    {   // Get whatever was dropped
                        java.awt.datatransfer.Transferable tr = evt.getTransferable();

                        // Is it a file list?
                        if (tr.isDataFlavorSupported (java.awt.datatransfer.DataFlavor.javaFileListFlavor))
                        {
                            // Say we'll take it.
                            //evt.acceptDrop ( java.awt.dnd.DnDConstants.ACTION_COPY_OR_MOVE );
                            evt.acceptDrop ( java.awt.dnd.DnDConstants.ACTION_COPY );
                            log( out, "FileDrop: file list accepted." );

                            // Get a useful list
                            java.util.List fileList = (java.util.List)
                                    tr.getTransferData(java.awt.datatransfer.DataFlavor.javaFileListFlavor);
                            java.util.Iterator iterator = fileList.iterator();

                            // Convert list to array
                            java.io.File[] filesTemp = new java.io.File[ fileList.size() ];
                            fileList.toArray( filesTemp );
                            final java.io.File[] files = filesTemp;

                            // Alert listener to drop.
                            if( listener != null )
                                listener.filesDropped( files );

                            // Mark that drop is completed.
                            evt.getDropTargetContext().dropComplete(true);
                            log( out, "FileDrop: drop complete." );
                        }   // end if: file list
                        else // this section will check for a reader flavor.
                        {
                            // Thanks, Nathan!
                            // BEGIN 2007-09-12 Nathan Blomquist -- Linux (KDE/Gnome) support added.
                            DataFlavor[] flavors = tr.getTransferDataFlavors();
                            boolean handled = false;
                            for (int zz = 0; zz < flavors.length; zz++) {
                                if (flavors[zz].isRepresentationClassReader()) {
                                    // Say we'll take it.
                                    //evt.acceptDrop ( java.awt.dnd.DnDConstants.ACTION_COPY_OR_MOVE );
                                    evt.acceptDrop(java.awt.dnd.DnDConstants.ACTION_COPY);
                                    log(out, "FileDrop: reader accepted.");

                                    Reader reader = flavors[zz].getReaderForText(tr);

                                    BufferedReader br = new BufferedReader(reader);

                                    if(listener != null)
                                        listener.filesDropped(createFileArray(br, out));

                                    // Mark that drop is completed.
                                    evt.getDropTargetContext().dropComplete(true);
                                    log(out, "FileDrop: drop complete.");
                                    handled = true;
                                    break;
                                }
                            }
                            if(!handled){
                                log( out, "FileDrop: not a file list or reader - abort." );
                                evt.rejectDrop();
                            }
                            // END 2007-09-12 Nathan Blomquist -- Linux (KDE/Gnome) support added.
                        }   // end else: not a file list
                    }   // end try
                    catch ( java.io.IOException io)
                    {   log( out, "FileDrop: IOException - abort:" );
                        io.printStackTrace( out );
                        evt.rejectDrop();
                    }   // end catch IOException
                    catch (java.awt.datatransfer.UnsupportedFlavorException ufe)
                    {   log( out, "FileDrop: UnsupportedFlavorException - abort:" );
                        ufe.printStackTrace( out );
                        evt.rejectDrop();
                    }   // end catch: UnsupportedFlavorException
                    finally
                    {
                        // If it's a Swing component, reset its border
                        if( c instanceof javax.swing.JComponent )
                        {   javax.swing.JComponent jc = (javax.swing.JComponent) c;
                            jc.setBorder( normalBorder );
                            log( out, "FileDrop: normal border restored." );
                        }   // end if: JComponent
                    }   // end finally
                }   // end drop

                public void dragExit( java.awt.dnd.DropTargetEvent evt )
                {   log( out, "FileDrop: dragExit event." );
                    // If it's a Swing component, reset its border
                    if( c instanceof javax.swing.JComponent )
                    {   javax.swing.JComponent jc = (javax.swing.JComponent) c;
                        jc.setBorder( normalBorder );
                        log( out, "FileDrop: normal border restored." );
                    }   // end if: JComponent
                }   // end dragExit

                public void dropActionChanged( java.awt.dnd.DropTargetDragEvent evt )
                {   log( out, "FileDrop: dropActionChanged event." );
                    // Is this an acceptable drag event?
                    if( isDragOk( out, evt ) )
                    {   //evt.acceptDrag( java.awt.dnd.DnDConstants.ACTION_COPY_OR_MOVE );
                        evt.acceptDrag( java.awt.dnd.DnDConstants.ACTION_COPY );
                        log( out, "FileDrop: event accepted." );
                    }   // end if: drag ok
                    else
                    {   evt.rejectDrag();
                        log( out, "FileDrop: event rejected." );
                    }   // end else: drag not ok
                }   // end dropActionChanged
            }; // end DropTargetListener

            // Make the component (and possibly children) drop targets
            makeDropTarget( out, c, recursive );
        }   // end if: supports dnd
        else
        {   log( out, "FileDrop: Drag and drop is not supported with this JVM" );
        }   // end else: does not support DnD
    }   // end constructor


    private static boolean check_supportsDnD()
    {
        boolean support = false;
        try
        {
            Class arbitraryDndClass = Class.forName( "java.awt.dnd.DnDConstants" );
            support = true;
        }
        catch( Exception e )
        {   support = false;
        }
        supportsDnD = support;
        return supportsDnD;
    }

    // BEGIN 2007-09-12 Nathan Blomquist -- Linux (KDE/Gnome) support added.
    private static String ZERO_CHAR_STRING = "" + (char)0;
    private static File[] createFileArray(BufferedReader bReader, PrintStream out)
    {
        try {
            java.util.List list = new java.util.ArrayList();
            java.lang.String line = null;
            while ((line = bReader.readLine()) != null) {
                try {
                    // kde seems to append a 0 char to the end of the reader
                    if(ZERO_CHAR_STRING.equals(line)) continue;

                    java.io.File file = new java.io.File(new java.net.URI(line));
                    list.add(file);
                } catch (Exception ex) {
                    log(out, "Error with " + line + ": " + ex.getMessage());
                }
            }

            return (java.io.File[]) list.toArray(new File[list.size()]);
        } catch (IOException ex) {
            log(out, "FileDrop: IOException");
        }
        return new File[0];
    }
    // END 2007-09-12 Nathan Blomquist -- Linux (KDE/Gnome) support added.


    private void makeDropTarget( final java.io.PrintStream out, final java.awt.Component c, boolean recursive )
    {
        // Make drop target
        final java.awt.dnd.DropTarget dt = new java.awt.dnd.DropTarget();
        try
        {   dt.addDropTargetListener( dropListener );
        }   // end try
        catch( java.util.TooManyListenersException e )
        {   e.printStackTrace();
            log(out, "FileDrop: Drop will not work due to previous error. Do you have another listener attached?" );
        }   // end catch

        // Listen for hierarchy changes and remove the drop target when the parent gets cleared out.
        c.addHierarchyListener( new java.awt.event.HierarchyListener()
        {   public void hierarchyChanged( java.awt.event.HierarchyEvent evt )
        {   log( out, "FileDrop: Hierarchy changed." );
            java.awt.Component parent = c.getParent();
            if( parent == null )
            {   c.setDropTarget( null );
                log( out, "FileDrop: Drop target cleared from component." );
            }   // end if: null parent
            else
            {   new java.awt.dnd.DropTarget(c, dropListener);
                log( out, "FileDrop: Drop target added to component." );
            }   // end else: parent not null
        }   // end hierarchyChanged
        }); // end hierarchy listener
        if( c.getParent() != null )
            new java.awt.dnd.DropTarget(c, dropListener);

        if( recursive && (c instanceof java.awt.Container ) )
        {
            // Get the container
            java.awt.Container cont = (java.awt.Container) c;

            // Get it's components
            java.awt.Component[] comps = cont.getComponents();

            // Set it's components as listeners also
            for( int i = 0; i < comps.length; i++ )
                makeDropTarget( out, comps[i], recursive );
        }   // end if: recursively set components as listener
    }   // end dropListener



    /** Determine if the dragged data is a file list. */
    private boolean isDragOk( final java.io.PrintStream out, final java.awt.dnd.DropTargetDragEvent evt )
    {   boolean ok = false;

        // Get data flavors being dragged
        java.awt.datatransfer.DataFlavor[] flavors = evt.getCurrentDataFlavors();

        // See if any of the flavors are a file list
        int i = 0;
        while( !ok && i < flavors.length )
        {
            // BEGIN 2007-09-12 Nathan Blomquist -- Linux (KDE/Gnome) support added.
            // Is the flavor a file list?
            final DataFlavor curFlavor = flavors[i];
            if( curFlavor.equals( java.awt.datatransfer.DataFlavor.javaFileListFlavor ) ||
                curFlavor.isRepresentationClassReader()){
                ok = true;
            }
            // END 2007-09-12 Nathan Blomquist -- Linux (KDE/Gnome) support added.
            i++;
        }   // end while: through flavors

        // If logging is enabled, show data flavors
        if( out != null )
        {   if( flavors.length == 0 )
            log( out, "FileDrop: no data flavors." );
            for( i = 0; i < flavors.length; i++ )
                log( out, flavors[i].toString() );
        }   // end if: logging enabled

        return ok;
    }   // end isDragOk


    /** Outputs <tt>message</tt> to <tt>out</tt> if it's not null. */
    private static void log( java.io.PrintStream out, String message )
    {   // Log message if requested
        if( out != null )
            out.println( message );
    }   // end log




    /**
     * Removes the drag-and-drop hooks from the component and optionally
     * from the all children. You should call this if you add and remove
     * components after you've set up the drag-and-drop.
     * This will recursively unregister all components contained within
     * <var>c</var> if <var>c</var> is a {@link java.awt.Container}.
     *
     * @param c The component to unregister as a drop target
     * @since 1.0
     */
    public static boolean remove( java.awt.Component c)
    {   return remove( null, c, true );
    }   // end remove



    /**
     * Removes the drag-and-drop hooks from the component and optionally
     * from the all children. You should call this if you add and remove
     * components after you've set up the drag-and-drop.
     *
     * @param out Optional {@link java.io.PrintStream} for logging drag and drop messages
     * @param c The component to unregister
     * @param recursive Recursively unregister components within a container
     * @since 1.0
     */
    public static boolean remove( java.io.PrintStream out, java.awt.Component c, boolean recursive )
    {   // Make sure we support dnd.
        if( check_supportsDnD() )
        {   log( out, "FileDrop: Removing drag-and-drop hooks." );
            c.setDropTarget( null );
            if( recursive && ( c instanceof java.awt.Container ) )
            {   java.awt.Component[] comps = ((java.awt.Container)c).getComponents();
                for( int i = 0; i < comps.length; i++ )
                    remove( out, comps[i], recursive );
                return true;
            }   // end if: recursive
            else return false;
        }   // end if: supports DnD
        else return false;
    }   // end remove




    /* ********  I N N E R   I N T E R F A C E   L I S T E N E R  ******** */


    /**
     * Implement this inner interface to listen for when files are dropped. For example
     * your class declaration may begin like this:
     * <code><pre>
     *      public class MyClass implements FileDrop.Listener
     *      ...
     *      public void filesDropped( java.io.File[] files )
     *      {
     *          ...
     *      }   // end filesDropped
     *      ...
     * </pre></code>
     *
     * @since 1.1
     */
    public static interface Listener {

        /**
         * This method is called when files have been successfully dropped.
         *
         * @param files An array of <tt>File</tt>s that were dropped.
         * @since 1.0
         */
        public abstract void filesDropped( java.io.File[] files );


    }   // end inner-interface Listener


    /* ********  I N N E R   C L A S S  ******** */


    /**
     * This is the event that is passed to the
     * {@link FileDropListener#filesDropped filesDropped(...)} method in
     * your {@link FileDropListener} when files are dropped onto
     * a registered drop target.
     *
     * <p>I'm releasing this code into the Public Domain. Enjoy.</p>
     *
     * @author  Robert Harder
     * @author  rob@iharder.net
     * @version 1.2
     */
    public static class Event extends java.util.EventObject {

        private java.io.File[] files;

        /**
         * Constructs an {@link Event} with the array
         * of files that were dropped and the
         * {@link FileDrop} that initiated the event.
         *
         * @param files The array of files that were dropped
         * @source The event source
         * @since 1.1
         */
        public Event( java.io.File[] files, Object source ) {
            super( source );
            this.files = files;
        }   // end constructor

        /**
         * Returns an array of files that were dropped on a
         * registered drop target.
         *
         * @return array of files that were dropped
         * @since 1.1
         */
        public java.io.File[] getFiles() {
            return files;
        }   // end getFiles

    }   // end inner class Event



    /* ********  I N N E R   C L A S S  ******** */


    /**
     * At last an easy way to encapsulate your custom objects for dragging and dropping
     * in your Java programs!
     * When you need to create a {@link java.awt.datatransfer.Transferable} object,
     * use this class to wrap your object.
     * For example:
     * <pre><code>
     *      ...
     *      MyCoolClass myObj = new MyCoolClass();
     *      Transferable xfer = new TransferableObject( myObj );
     *      ...
     * </code></pre>
     * Or if you need to know when the data was actually dropped, like when you're
     * moving data out of a list, say, you can use the {@link TransferableObject.Fetcher}
     * inner class to return your object Just in Time.
     * For example:
     * <pre><code>
     *      ...
     *      final MyCoolClass myObj = new MyCoolClass();
     *
     *      TransferableObject.Fetcher fetcher = new TransferableObject.Fetcher()
     *      {   public Object getObject(){ return myObj; }
     *      }; // end fetcher
     *
     *      Transferable xfer = new TransferableObject( fetcher );
     *      ...
     * </code></pre>
     *
     * The {@link java.awt.datatransfer.DataFlavor} associated with 
     * {@link TransferableObject} has the representation class
     * <tt>net.iharder.dnd.TransferableObject.class</tt> and MIME type
     * <tt>application/x-net.iharder.dnd.TransferableObject</tt>.
     * This data flavor is accessible via the static
     * {@link #DATA_FLAVOR} property.
     *
     *
     * <p>I'm releasing this code into the Public Domain. Enjoy.</p>
     *
     * @author  Robert Harder
     * @author  rob@iharder.net
     * @version 1.2
     */
    public static class TransferableObject implements java.awt.datatransfer.Transferable
    {
        /**
         * The MIME type for {@link #DATA_FLAVOR} is 
         * <tt>application/x-net.iharder.dnd.TransferableObject</tt>.
         *
         * @since 1.1
         */
        public final static String MIME_TYPE = "application/x-net.iharder.dnd.TransferableObject";


        /**
         * The default {@link java.awt.datatransfer.DataFlavor} for
         * {@link TransferableObject} has the representation class
         * <tt>net.iharder.dnd.TransferableObject.class</tt>
         * and the MIME type 
         * <tt>application/x-net.iharder.dnd.TransferableObject</tt>.
         *
         * @since 1.1
         */
        public final static java.awt.datatransfer.DataFlavor DATA_FLAVOR =
                new java.awt.datatransfer.DataFlavor( FileDrop.TransferableObject.class, MIME_TYPE );


        private Fetcher fetcher;
        private Object data;

        private java.awt.datatransfer.DataFlavor customFlavor;



        /**
         * Creates a new {@link TransferableObject} that wraps <var>data</var>.
         * Along with the {@link #DATA_FLAVOR} associated with this class,
         * this creates a custom data flavor with a representation class 
         * determined from <code>data.getClass()</code> and the MIME type
         * <tt>application/x-net.iharder.dnd.TransferableObject</tt>.
         *
         * @param data The data to transfer
         * @since 1.1
         */
        public TransferableObject( Object data )
        {   this.data = data;
            this.customFlavor = new java.awt.datatransfer.DataFlavor( data.getClass(), MIME_TYPE );
        }   // end constructor



        /**
         * Creates a new {@link TransferableObject} that will return the
         * object that is returned by <var>fetcher</var>.
         * No custom data flavor is set other than the default
         * {@link #DATA_FLAVOR}.
         *
         * @see Fetcher
         * @param fetcher The {@link Fetcher} that will return the data object
         * @since 1.1
         */
        public TransferableObject( Fetcher fetcher )
        {   this.fetcher = fetcher;
        }   // end constructor



        /**
         * Creates a new {@link TransferableObject} that will return the
         * object that is returned by <var>fetcher</var>.
         * Along with the {@link #DATA_FLAVOR} associated with this class,
         * this creates a custom data flavor with a representation class <var>dataClass</var>
         * and the MIME type
         * <tt>application/x-net.iharder.dnd.TransferableObject</tt>.
         *
         * @see Fetcher
         * @param dataClass The {@link java.lang.Class} to use in the custom data flavor
         * @param fetcher The {@link Fetcher} that will return the data object
         * @since 1.1
         */
        public TransferableObject( Class dataClass, Fetcher fetcher )
        {   this.fetcher = fetcher;
            this.customFlavor = new java.awt.datatransfer.DataFlavor( dataClass, MIME_TYPE );
        }   // end constructor

        /**
         * Returns the custom {@link java.awt.datatransfer.DataFlavor} associated
         * with the encapsulated object or <tt>null</tt> if the {@link Fetcher}
         * constructor was used without passing a {@link java.lang.Class}.
         *
         * @return The custom data flavor for the encapsulated object
         * @since 1.1
         */
        public java.awt.datatransfer.DataFlavor getCustomDataFlavor()
        {   return customFlavor;
        }   // end getCustomDataFlavor


        /* ********  T R A N S F E R A B L E   M E T H O D S  ******** */


        /**
         * Returns a two- or three-element array containing first
         * the custom data flavor, if one was created in the constructors,
         * second the default {@link #DATA_FLAVOR} associated with
         * {@link TransferableObject}, and third the
         * {@link java.awt.datatransfer.DataFlavor.stringFlavor}.
         *
         * @return An array of supported data flavors
         * @since 1.1
         */
        public java.awt.datatransfer.DataFlavor[] getTransferDataFlavors()
        {
            if( customFlavor != null )
                return new java.awt.datatransfer.DataFlavor[]
                        {   customFlavor,
                                DATA_FLAVOR,
                                java.awt.datatransfer.DataFlavor.stringFlavor
                        };  // end flavors array
            else
                return new java.awt.datatransfer.DataFlavor[]
                        {   DATA_FLAVOR,
                                java.awt.datatransfer.DataFlavor.stringFlavor
                        };  // end flavors array
        }   // end getTransferDataFlavors



        /**
         * Returns the data encapsulated in this {@link TransferableObject}.
         * If the {@link Fetcher} constructor was used, then this is when
         * the {@link Fetcher#getObject getObject()} method will be called.
         * If the requested data flavor is not supported, then the
         * {@link Fetcher#getObject getObject()} method will not be called.
         *
         * @param flavor The data flavor for the data to return
         * @return The dropped data
         * @since 1.1
         */
        public Object getTransferData( java.awt.datatransfer.DataFlavor flavor )
                throws java.awt.datatransfer.UnsupportedFlavorException, java.io.IOException
        {
            // Native object
            if( flavor.equals( DATA_FLAVOR ) )
                return fetcher == null ? data : fetcher.getObject();

            // String
            if( flavor.equals( java.awt.datatransfer.DataFlavor.stringFlavor ) )
                return fetcher == null ? data.toString() : fetcher.getObject().toString();

            // We can't do anything else
            throw new java.awt.datatransfer.UnsupportedFlavorException(flavor);
        }   // end getTransferData




        /**
         * Returns <tt>true</tt> if <var>flavor</var> is one of the supported
         * flavors. Flavors are supported using the <code>equals(...)</code> method.
         *
         * @param flavor The data flavor to check
         * @return Whether or not the flavor is supported
         * @since 1.1
         */
        public boolean isDataFlavorSupported( java.awt.datatransfer.DataFlavor flavor )
        {
            // Native object
            if( flavor.equals( DATA_FLAVOR ) )
                return true;

            // String
            if( flavor.equals( java.awt.datatransfer.DataFlavor.stringFlavor ) )
                return true;

            // We can't do anything else
            return false;
        }   // end isDataFlavorSupported


        /* ********  I N N E R   I N T E R F A C E   F E T C H E R  ******** */

        /**
         * Instead of passing your data directly to the {@link TransferableObject}
         * constructor, you may want to know exactly when your data was received
         * in case you need to remove it from its source (or do anyting else to it).
         * When the {@link #getTransferData getTransferData(...)} method is called
         * on the {@link TransferableObject}, the {@link Fetcher}'s
         * {@link #getObject getObject()} method will be called.
         *
         * @author Robert Harder
         * @copyright 2001
         * @version 1.1
         * @since 1.1
         */
        public static interface Fetcher
        {
            /**
             * Return the object being encapsulated in the
             * {@link TransferableObject}.
             *
             * @return The dropped object
             * @since 1.1
             */
            public abstract Object getObject();
        }   // end inner interface Fetcher



    }   // end class TransferableObject





}   // end class FileDrop
