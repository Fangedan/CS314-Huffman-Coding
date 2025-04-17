
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitorInputStream;
import javax.swing.UIManager;

/**
 * Compare two files to see if they are the same bit for bit.
 * @author Owen Astrachan
 *
 */
public class Diff {
 
    protected static JFileChooser ourChooser = new JFileChooser(".");
    
    /**
     * Show a popup with the diff output.
     * @param message The message to show,
     */
    public static void showMessage(String message) {
        JOptionPane.showMessageDialog(null, message,"Diff Output",
                JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Check for the give files for differences.
     * @param files The files to check. We expect files.length = 2.
     * @param diffStats Stores the difference statistics.
     */
    public static void doDiffer(File[] files, DiffStats diffStats) {
        final int BITS_PER_BYTE = 8;
        try {
            ProgressMonitorInputStream stream1 = 
                new ProgressMonitorInputStream(null, "reading " + files[0].getName(),
                        new FileInputStream(files[0]));
            ProgressMonitorInputStream stream2 = 
                new ProgressMonitorInputStream(null, "reading " + files[1].getName(),
                        new FileInputStream(files[1]));
            BitInputStream b1 = new BitInputStream(stream1);
            BitInputStream b2 = new BitInputStream(stream2);
            diffStats.file1Size = files[0].length();
            diffStats.file2Size = files[1].length();
            int x = b1.readBits(BITS_PER_BYTE);
            int y = b2.readBits(BITS_PER_BYTE);
            int bytesRead = 1;
            while (x != -1 && y != -1) {
                if (x != y) {
                    diffStats.totalDifferences++;
                    if (diffStats.firstDiff == -1)
                        diffStats.firstDiff = bytesRead;
                }
                x = b1.readBits(BITS_PER_BYTE);
                y = b2.readBits(BITS_PER_BYTE);
                bytesRead++;
            }
            bytesRead--; // last read didn't work
            b1.close();
            b2.close();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,"trouble reading","Diff Error",
                    JOptionPane.ERROR_MESSAGE);
        }
      
    }
    
    /**
     * Check two files for differences. 
     * @param args None expected.
     */
    public static void main(String[] args){
        setLookAndFeel();
        ourChooser.setMultiSelectionEnabled(true);
        ourChooser.setDialogTitle("Diff: choose two files");
        int retval = ourChooser.showOpenDialog(null);
        if (retval == JFileChooser.APPROVE_OPTION){
                File[] files = ourChooser.getSelectedFiles();
                if (files.length != 2){
                    JOptionPane.showMessageDialog(null,"Choose Two Files", 
                            "Diff Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    DiffStats ds = new DiffStats();
                    doDiffer(files, ds); 
                    System.out.println("Results of comparing files: ");
                    System.out.println(ds);
                }
        }           
    }

    private static void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch(Exception e) {
            System.out.println("Unable to set look at feel to local settings. " +
                    "Continuing with default Java look and feel.");
        }
    }
    
    // Used to store differences between files, if any.
    private static class DiffStats {
        private long file1Size;
        private long file2Size;
        private int firstDiff;
        private int totalDifferences;
        
        private DiffStats() {
            firstDiff = -1;
        }
        
        public String toString() {
            String result =  "file 1 size in bytes: " + file1Size + "\n"
                   + "file 2 size in bytes: " + file2Size + "\n";
            if (firstDiff == -1) {
                result += "All bytes in files the same\n";
            } else {
                result += "number of bytes different: " + totalDifferences + "\n";
                result += "first difference occured after " + firstDiff + " bytes.\n";
            }
            return result;
        }
    }
}
