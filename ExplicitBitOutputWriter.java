import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;

public class ExplicitBitOutputWriter {

    public static void main(String[] args) {


        // get the file
        JFileChooser chooser = new JFileChooser(".");
        int retval = chooser.showOpenDialog(null);
        File f = null;
        chooser.requestFocusInWindow();
        if (retval == JFileChooser.APPROVE_OPTION)
            f = chooser.getSelectedFile();
        // first line is assumed to be the topic!
        // load the phrases


        try{
            BitInputStream bitsIn = new BitInputStream(f);
            int bit = bitsIn.readBits(1);
            int count = 1;
            while(bit != -1) {
                System.out.print(bit);
                if(count % 8 == 0 && count % 32 != 0)
                    System.out.print(" ");
                else if(count % 32 == 0)
                    System.out.println();
                count++;
                bit = bitsIn.readBits(1);

            }
            bitsIn.close();
        }
        catch(IOException e) {
            System.out.println("problem reading: " + e);
        }
    }
}