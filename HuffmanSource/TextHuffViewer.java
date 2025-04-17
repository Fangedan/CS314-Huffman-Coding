import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Scanner;

/**
 * A text based version of the IHuffViewer. Used as an alternative to the
 * GUIHuffViewer. All functionality from the GUI version of the viewer is
 * replicated here.
 * 
 * @author Mike Scott
 */
public class TextHuffViewer implements IHuffViewer {

    private IHuffProcessor myModel;
    private File myInputFile;
    private boolean myForce;
    private int myHeaderFormat;
    private final Scanner KEY;

    private static String HUFF_SUFFIX = ".hf";
    private static String UNHUFF_SUFFIX = ".unhf";

    /**
     * Create a text based interface Huffman viewer. After this constructor is
     * called the setModel method must be called and then the start method must
     * be called to allow a user to interact with the viewer.
     */
    public TextHuffViewer() {
        myHeaderFormat = IHuffConstants.STORE_COUNTS;
        KEY = new Scanner(System.in);
    }

    /**
     * Associates this view with the given model. The GUI (or other View) will
     * attach itself to the model so that communication between the view and the
     * model as well as <em>vice versa</em> is supported.
     * 
     * @param model is the model for this view
     */
    public void setModel(IHuffProcessor model) {
        myModel = model;
        myModel.setViewer(this);
    }

    /**
     * To be called by model/client code to display strings in the View.
     * Displays string on a single line. Call multiple times show several
     * strings.
     * 
     * @param s is string to be displayed
     */
    public void update(String s) {
        System.out.println(s);
    }

    /**
     * Display a text message in the view. Messages are informational in
     * purpose.
     * 
     * @param s is the message displayed
     */
    public void showMessage(String s) {
        System.out.println("MESSAGE");
        System.out.println(s);
        System.out.println();
    }

    /**
     * Show a message indicating an error. The user must respond with a return
     * before proceeding.
     * 
     * @param s is the error-message displayed
     */
    public void showError(String s) {
        System.out.println("ERROR");
        System.out.println(s);
        System.out.println();
        System.out.println("Presss enter to continue: ");
        KEY.nextLine();
        System.out.println();
    }

    // The menu main choices for the user.
    private static enum MenuChoices {
        UNUSED, FILE_OP, FLIP_COMPRESSION_STATUS, FLIP_HEADER_CHOICE;
    }

    /**
     * Start processing Huffman operation requests. Do so until the user stops.
     */
    public void start() {
        int choice = 1;
        while (keepGoing(choice)) {
            System.out.println("STATUS");
            System.out.print("Current Header format for compressing: ");
            if (this.myHeaderFormat == IHuffConstants.STORE_COUNTS) {
                System.out.println("Standard COUNT Format");
            } else {
                System.out.println("Standard TREE Format");
            }
            System.out.println("Current force compression status: " + this.myForce);
            System.out.println();
            System.out.println("OPTIONS");
            System.out.println("1. File operations. ");
            System.out.println("2. Flip Force Compression status.");
            System.out.println("3. Flip Header format.");
            System.out.println("4. Quit (or any value other than 1, 2, or 3)");
            System.out.println();
            choice = processChoice();
        }
        System.out.println("Ending program.");
    }

    /*
     * Return true if the choice is options 1 (file operation), 2 (flip force
     * status), or 3 (flip header choice). False for anything else.
     */
    private boolean keepGoing(int choice) {
        return MenuChoices.FILE_OP.ordinal() <= choice
                && choice <= MenuChoices.FLIP_HEADER_CHOICE.ordinal();
    }

    /*
     * Get the next choice from the user and process it.
     */
    private int processChoice() {
        System.out.print("Enter menu option: ");
        String input = KEY.nextLine();
        System.out.println();
        if (input.length() == 0) {
            input = "4";
        }
        int firstCharValue = input.charAt(0) - '0';
        // If it is 1, 2, or 3 and process choice
        if (keepGoing(firstCharValue)) {
            if (firstCharValue == MenuChoices.FILE_OP.ordinal()) {
                processFileRequest();
            } else if (firstCharValue == MenuChoices.FLIP_COMPRESSION_STATUS.ordinal()) {
                myForce = !myForce;
            } else {
                // must be a 3, flip the header format
                myHeaderFormat =
                        (myHeaderFormat == IHuffConstants.STORE_COUNTS) ? IHuffConstants.STORE_TREE
                                : IHuffConstants.STORE_COUNTS;

            }
        }
        return firstCharValue;
    }

    /*
     * Process a file request. These are the important operations for the
     * program.
     */
    private void processFileRequest() {
        // We assume the user shall enter a valid int for the menu choice.
        // Future work will be to make this more robust.
        System.out.println("FILE OPERATION");
        System.out.println("1. Preprocess a file to determine if compression is possible.");
        System.out.println("2. Compress a file. Results in a call to preprocessing as well.");
        System.out.println("3. Uncompress a file.");
        System.out.println();
        System.out.print("Enter file operation option: ");
        int choice = KEY.nextInt();
        KEY.nextLine();
        System.out.println();
        if (choice == 1) {
            setMyFile("preprocess only"); // Gross! An array for choices?
            preprocess();
        } else if (choice == 2) {
            setMyFile("compress");
            compress();
        } else {
            setMyFile("decompress");
            uncompress();
        }
    }

    /*
     * Process a file to determine the amount of bits saved if it were to be
     * compressed.
     */
    private void preprocess() {
        showMessage("Begining preprocessing of " + myInputFile + ".");
        ByteArrayInputStream inputStream = getFastByteReader(myInputFile);
        try {
            int saved = myModel.preprocessCompress(inputStream, myHeaderFormat);
            showMessage("save(d): " + saved + " bits");
        } catch (IOException e) {
            showError("Error / Exception while reading file for preprocessing.");
            e.printStackTrace();
        }
    }

    // Compress a file based on user input.
    private void compress() {
        preprocess();
        String newName = myInputFile.getName() + HUFF_SUFFIX;
        newName = setNewFileName("compressed", newName);
        if (newName == null) {
            showError("Trouble with file setting path of output file. Exiting compress.");
        } else {
            try {
                FileOutputStream out = new FileOutputStream(new File(newName));
                ByteArrayInputStream inputStream = getFastByteReader(myInputFile);
                myModel.compress(inputStream, out, myForce);
            } catch (IOException e) {
                showError("Problem while trying to compress file.");
                e.printStackTrace();
            }
        }
        myInputFile = null; // must enter new file after this.
    }

    /*
     * Set the name of the output file. This can be for
     * a compressed or uncompressed file. The parameter
     * type specifies if the output file is for compression
     * or uncompression. Allow the user to alter the name
     * of the output file if they wish.
     */
    private String setNewFileName(String type, String newName) {
        System.out.println("Proposed name for " + type + " file: " + newName);
        System.out.print("Enter c to change " + type + " file name. "
                + "Anything else to keep proposed name: ");
        String response = KEY.nextLine();
        System.out.println();
        if (response.length() == 1 && response.charAt(0) == 'c') {
            System.out.print("Enter name for " + type + " file: ");
            newName = KEY.nextLine();
            System.out.println();
        }
        newName = setFullPath(newName);
        if (newName == null) {
            showError("Trouble with file setting path of output file.");
        }
        return newName;
    }

    /*
     * Set the newName to have the full path of the original file.
     * In other words, the compressed file will be placed in the same
     * directory as the file we are compressing.
     * Return the resulting path or null if a an IOException occurs
     * while determining the canonical path of the input file.
     */
    private String setFullPath(String newName) {
        String result = null;
        try {
            String path = myInputFile.getCanonicalPath();
            int pos = path.lastIndexOf(myInputFile.getName());
            result = path.substring(0, pos) + newName;
        } catch (IOException e) {
            showError("Problem in trying to obtain Canoical Path of file " + newName);
            e.printStackTrace();
        }
        return result;
    }

    /*
     * Uncompress a file. Ask the user for the input file
     * and the name for the uncompressed output file.
     * Then uncompress the file using the current processor.
     */
    private void uncompress() {
        showMessage("Uncompressing file based on user choice.");
        String name = myInputFile.getName();
        String uncompressedName = name;
        if (name.endsWith(HUFF_SUFFIX)) {
            uncompressedName =
                    name.substring(0, name.length() - HUFF_SUFFIX.length()) + UNHUFF_SUFFIX;
        } else {
            uncompressedName = name + UNHUFF_SUFFIX;
        }
        uncompressedName = setNewFileName("compressed", uncompressedName);
        try {
            FileOutputStream out = new FileOutputStream(new File(uncompressedName));
            ByteArrayInputStream inputStream = getFastByteReader(myInputFile);
            myModel.uncompress(inputStream, out);
        } catch (IOException e) {
            showError("Problem while trying to uncompress file.");
            e.printStackTrace();
        }
    }

    // Set myFile based on user input.
    private void setMyFile(String prompt) {
        System.out.print("Enter name (full or relative path) of file to " + prompt + ": ");
        String path = KEY.nextLine();
        System.out.println();
        myInputFile = new File(path);
    }

    // Get a ByteArrayInputStream to read from the given file.
    private ByteArrayInputStream getFastByteReader(File f) {
        try {
            FileChannel channel = new FileInputStream(f).getChannel();
            ByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            byte[] barray = new byte[buffer.limit()];
            if (barray.length != channel.size()) {
                showError(String.format("Reading %s error: lengths differ %d %ld\n", f.getName(),
                        barray.length, channel.size()));
            }
            buffer.get(barray);
            return new ByteArrayInputStream(barray);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
