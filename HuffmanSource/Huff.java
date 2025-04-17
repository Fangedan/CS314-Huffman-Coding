import javax.swing.UIManager;

/**
 * Main/launch program for Huff assignment. A better
 * comment than this is warranted.
 */
public class Huff {

    /*
     * Create an interface to interact with a
     *  Huffman processor.
     */
    public static void main(String[] args){
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch(Exception e) {
            System.out.println("Unable to set look at feel to local settings. " +
                    "Continuing with default Java look and feel.");
        }
        // To use a Graphical User Interface to perform Huffman operations.
        // Comment out the following line and uncomment the line after to use a TextHuffViewer.
        IHuffViewer sv = new GUIHuffViewer("Huffman Compression");
        // IHuffViewer sv = new TextHuffViewer();
        IHuffProcessor proc = new SimpleHuffProcessor();
        sv.setModel(proc);    
        if (sv instanceof TextHuffViewer) {
            ((TextHuffViewer) sv).start();
        }
    }
}
