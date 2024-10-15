import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileFilter;

class TextFileFilter implements FileFilter
{
    public boolean accept(File file)
    {
        if (file.isDirectory())
        {
            return true;
        }
        else
        {
            return false;
        }
    }
}
