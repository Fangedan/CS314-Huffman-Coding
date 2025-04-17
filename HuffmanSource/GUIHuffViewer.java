import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ProgressMonitor;
import javax.swing.ProgressMonitorInputStream;

/**
 * The GUI/View for Huffman coding assignment. Clients communicate
 * with this view by attaching a model and then using the menu choices/options that
 * are part of the GUI. Thus client code that fails to call <code>setModel</code> will
 * almost certainly not work and generate null pointer problems because the view/GUI will
 * not have an associated model.
 * <P>
 * @author Owen Astrachan
 * Changes by Mike Scott
 *
 */
public class GUIHuffViewer extends JFrame implements IHuffViewer {

    private static String HUFF_SUFFIX = ".hf";
    private static String UNHUFF_SUFFIX = ".unhf";   

    private JTextArea myOutput;
    private IHuffProcessor myModel;
    private JTextField myMessage;
    private File myFile;
    private boolean myForce;
    private int myHeaderFormat;
    private Thread myFirstFileThread;
    private boolean myFirstReadingDone;

    private static JFileChooser ourChooser = 
            new JFileChooser(System.getProperties().getProperty("user.dir"));

    /** 
     * Create a new graphical user interface to perform Huffman
     * encoding operations.
     * @param title The title to display in the window of the Viewer.
     */
    public GUIHuffViewer(String title) {
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel panel = (JPanel) getContentPane();
        panel.setLayout(new BorderLayout());
        setTitle(title);
        myHeaderFormat = IHuffConstants.STORE_COUNTS;

        panel.add(makeOutput(), BorderLayout.CENTER);
        panel.add(makeMessage(), BorderLayout.SOUTH);
        makeMenus();

        pack();
        setSize(650, 400);
        setLocation(200, 100);
        setVisible(true);
    }

    /**
     * Associates this view with the given model. The GUI/View will 
     * attach itself to the model so that communication between the view
     * and the model as well as <em>vice versa</em> is supported.
     * @param model is the model for this view
     */
    public void setModel(IHuffProcessor model) {
        myModel = model;
        myModel.setViewer(this);
    }

    /**
     * Create and return a JPanel for informational messages.
     * @return The JPanel for informational messages.
     */
    protected JPanel makeMessage() {
        JPanel p = new JPanel(new BorderLayout());
        myMessage = new JTextField(30);
        p.setBorder(BorderFactory.createTitledBorder("message"));
        p.add(myMessage, BorderLayout.CENTER);
        return p;
    }

    /**
     * Create and return a JPanel for the main output of the GUI.
     * @return The JPanel for informational messages.
     */
    protected JPanel makeOutput() {
        final int NUM_ROWS = 10;
        final int NUM_COLS = 40;
        final int FONT_POINT = 18;
        JPanel p = new JPanel(new BorderLayout());
        myOutput = new JTextArea(NUM_ROWS, NUM_COLS );
        myOutput.setFont(new Font(Font.MONOSPACED, Font.BOLD, FONT_POINT));
        p.setBorder(BorderFactory.createTitledBorder("output"));
        p.add(new JScrollPane(myOutput), BorderLayout.CENTER);
        return p;

    }

    /**
     * Read a file. Choice is via a pop up window. 
     * @return The file opened.
     */
    protected File doRead() {
        // Get the user choice for the file.
        int retval = ourChooser.showOpenDialog(null);
        if (retval != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        showMessage("reading/initializing");
        myFile = ourChooser.getSelectedFile();
        
        // Create an input stream for the file and a progress monitor.
        final ProgressMonitorInputStream pmis 
            = getMonitorableStream(getFastByteReader(myFile), "counting/reading bits ...");
        final ProgressMonitor progress = pmis.getProgressMonitor();
        try {
            myFirstFileThread = new Thread() {
                public void run() {
                    try {
                        myFirstReadingDone = false;
                        int saved = myModel.preprocessCompress(pmis, myHeaderFormat);
                        showMessage("saved: "+ saved +" bits");
                        myFirstReadingDone = true;
                    } catch (IOException e) {
                        showError("reading exception\n " + e);
                        //e.printStackTrace();
                    }
                    if (progress.isCanceled()) {
                        showError("reading cancelled");
                    }
                }
            };
            myFirstFileThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        File ret = myFile;
        myFile = null;
        return ret;
    }

    /**
     * Make and return the menu options for the OPTIONS part of the GUI.
     * @return The OPTIONS menu for the GUI.
     */
    protected JMenu makeOptionsMenu() {
        JMenu menu = new JMenu("Options");
        JCheckBoxMenuItem force = new JCheckBoxMenuItem(new AbstractAction(
                "Force Compression") {
            
            public void actionPerformed(ActionEvent ev) {
                myForce = !myForce;
            }
            
        });
        menu.add(force);
        return menu;
    }

    
    
    protected JMenu makeHeaderMenu() {


        JMenu headerMenu = new JMenu("Header Format");
        ButtonGroup group = new ButtonGroup();

        // create the radio button for standard count format
        JRadioButtonMenuItem countHeaderButton 
        = new JRadioButtonMenuItem();
        countHeaderButton.setSelected(true);
        countHeaderButton.setAction(new AbstractAction("Use Count Format Header(SCF)") {
            public void actionPerformed(ActionEvent ev) {
                myHeaderFormat = IHuffConstants.STORE_COUNTS;
            }
        });
        group.add(countHeaderButton);
        headerMenu.add(countHeaderButton);

        // create the radio button for standard tree format
        JRadioButtonMenuItem treeHeaderButton 
        = new JRadioButtonMenuItem();
        treeHeaderButton.setSelected(false);
        treeHeaderButton.setAction(new AbstractAction("Use Tree Format Header(STF)") {
            public void actionPerformed(ActionEvent ev) {
                myHeaderFormat = IHuffConstants.STORE_TREE;
            }
        });

        group.add(treeHeaderButton);
        headerMenu.add(treeHeaderButton);
        return headerMenu;
    }

    protected JMenu makeFileMenu() {
        JMenu fileMenu = new JMenu("File");

        fileMenu.add(new AbstractAction("Open/Count - Precompress only. Does not create output file.") {
            public void actionPerformed(ActionEvent ev) {
                doRead();
            }
        });

        fileMenu.add(new AbstractAction("Compress - Results in call to preprocessCompress and"
                + " and then compress.") {
            public void actionPerformed(ActionEvent ev) {
                doSave();
            }
        });

        fileMenu.add(new AbstractAction("Uncompress") {
            public void actionPerformed(ActionEvent ev) {
                doDecode();
            }
        });

        fileMenu.add(new AbstractAction("Quit") {
            public void actionPerformed(ActionEvent ev) {
                System.exit(0);
            }
        });
        return fileMenu;
    }

    protected void makeMenus() {
        JMenuBar bar = new JMenuBar();
        bar.add(makeFileMenu());
        bar.add(makeOptionsMenu());
        bar.add(makeHeaderMenu());
        setJMenuBar(bar);
    }

    private void doDecode() {
        File file = null;
        showMessage("uncompressing");
        try {
            int retval = ourChooser.showOpenDialog(null);
            if (retval != JFileChooser.APPROVE_OPTION) {
                return; // GACK
            }
            file = ourChooser.getSelectedFile();
            String name = file.getName();
            String uname = name;
            if (name.endsWith(HUFF_SUFFIX)) {
                uname = name.substring(0,name.length() - HUFF_SUFFIX.length()) + UNHUFF_SUFFIX;
            }
            else {
                uname = name + UNHUFF_SUFFIX;
            }
            String newName = JOptionPane.showInputDialog(this,
                    "Name of uncompressed file", uname);
            if (newName == null) {
                return;
            }
            String path = file.getCanonicalPath();

            int pos = path.lastIndexOf(name);
            newName = path.substring(0, pos) + newName;
            final File newFile = new File(newName);
            ProgressMonitorInputStream temp = null;
            temp = getMonitorableStream(getFastByteReader(file),"uncompressing bits ...");
            final ProgressMonitorInputStream stream = temp;

            final ProgressMonitor progress = stream.getProgressMonitor();
            final OutputStream out = new FileOutputStream(newFile);
            Thread fileReaderThread = new Thread() {
                public void run() {
                    try {
                        myModel.uncompress(stream, out);
                    } catch (IOException e) {

                        cleanUp(newFile);
                        showError("could not uncompress\n "+e);
                        //e.printStackTrace();
                    }
                    if (progress.isCanceled()) {
                        cleanUp(newFile);
                        showError("reading cancelled");
                    }
                }
            };
            fileReaderThread.start();
        } catch (FileNotFoundException e) {
            showError("could not open " + file.getName());
            e.printStackTrace();
        } catch (IOException e) {
            showError("IOException, uncompression halted from viewer");
            e.printStackTrace();
        }
    }

    private void doSave() {
        myFile = doRead();
        if (myFile == null){
            return;
        }

        String name = myFile.getName();
        showMessage("compressing " + name);
        String newName = JOptionPane.showInputDialog(this,
                "Name of compressed file", name + HUFF_SUFFIX);
        if (newName == null) {
            return;
        }
        String path = null;
        try {
            path = myFile.getCanonicalPath();
        } catch (IOException e) {
            showError("trouble with file canonicalizing");
            return;
        }
        int pos = path.lastIndexOf(name);
        newName = path.substring(0, pos) + newName;
        final File outputFile = new File(newName);
        try {
            final FileOutputStream out = new FileOutputStream(outputFile);
            ProgressMonitorInputStream  temp = null;
            temp = getMonitorableStream(getFastByteReader(myFile), "compressing bits...");
  
            final ProgressMonitorInputStream pmis = temp; 
            final ProgressMonitor progress = pmis.getProgressMonitor();
            Thread fileWriterThread = new Thread() {
                public void run() {
                    try {
                        while (! myFirstReadingDone){
                            try {
                                sleep(100);
                            } catch (InterruptedException e) {
                                // what to do?
                                showError("Trouble in Thread " + e);
                            }
                        }
                        myModel.compress(pmis, out, myForce);
                    } catch (IOException e) {
                        showError("compression exception\n " + e);
                        cleanUp(outputFile);
                        //e.printStackTrace();
                    }
                    if (progress.isCanceled()) {
                        showError("compression cancelled");
                        cleanUp(outputFile);
                    }
                }
            };
            fileWriterThread.start();
        } catch (FileNotFoundException e) {
            showError("could not open " + outputFile.getName());
            e.printStackTrace();
        }
        myFile = null;
    }

    private void cleanUp(File f) {
        if (!f.delete()) {
            showError("trouble deleting " + f.getName());
        } else {
            // do something here?
        }
    }

    private ProgressMonitorInputStream getMonitorableStream(InputStream stream, String message) {


        final ProgressMonitorInputStream pmis = new ProgressMonitorInputStream(
                this, message, stream);

        ProgressMonitor progress = pmis.getProgressMonitor();
        progress.setMillisToDecideToPopup(1);
        progress.setMillisToPopup(1);

        return pmis;
    }



    private ProgressMonitorInputStream getMonitorableStream(File file,
            String message) {
        try {
            FileInputStream stream = new FileInputStream(file);
            if (stream == null){
                System.out.println("null on "+file.getCanonicalPath());
            }
            final ProgressMonitorInputStream pmis = new ProgressMonitorInputStream(
                    this, message, stream);

            ProgressMonitor progress = pmis.getProgressMonitor();
            progress.setMillisToDecideToPopup(1);
            progress.setMillisToPopup(1);

            return pmis;
        } catch (IOException e) {
            showError("could not open " + file.getName());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Clear the text area, e.g., for a new message.
     */
    public void clear(){
        showMessage("");
        myOutput.setText("");
    }

    /**
     * To be called by model/client code to display strings in the View. Displays string
     * on a single line. Call multiple times show several strings.
     * @param s is string to be displayed
     */
    public void update(String s) {
        myOutput.append(s+"\n");
    }

    /**
     * Display a text message in the view (e.g., in the small text area
     * at the bottom of the GUI), thus a modeless message the user can ignore.
     * @param s is the message displayed
     */
    public void showMessage(String s) {
        myMessage.setText(s);
    }

    /**
     * Show a modal-dialog indicating an error; the user must dismiss the
     * displayed dialog.
     * @param s is the error-message displayed
     */
    public void showError(String s) {
        JOptionPane.showMessageDialog(this, s, "Huff info",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private ByteArrayInputStream getFastByteReader(File f){
        ByteBuffer buffer = null;
        try {
            FileChannel channel = new FileInputStream(f).getChannel();
            buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            byte[] barray = new byte[buffer.limit()];

            if (barray.length != channel.size()){               
                showError(String.format("Reading %s error: lengths differ %d %ld\n",f.getName(),barray.length,channel.size()));
            }
            buffer.get(barray);
            return new ByteArrayInputStream(barray);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
