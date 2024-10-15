import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.Border;
import info.guardianproject.iocipher.File;
import javax.swing.filechooser.FileSystemView;

class FileRenderer extends DefaultListCellRenderer
{
    private boolean detail;
    private Border detailBorder = new EmptyBorder(3,3,3,3);

    FileRenderer(boolean detail)
    {
        this.detail = detail;
    }

    @Override
    public Component getListCellRendererComponent(
        JList list,
        Object value,
        int index,
        boolean isSelected,
        boolean cellHasFocus)
    {
        Component c = super.getListCellRendererComponent(list,value,index,isSelected,cellHasFocus);
        JLabel l = (JLabel)c;
        java.io.File f = new java.io.File(((info.guardianproject.iocipher.File)value).getPath());
        l.setText(f.getName());
        l.setIcon(FileSystemView.getFileSystemView().getSystemIcon(f));
        if (detail)
        {
            l.setBorder(detailBorder);
        }

        return l;
    }
}
