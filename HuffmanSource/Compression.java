package HuffmanSource;

/*  Student information for assignment:
 *
 *  On our honor, Vishal Vijayakumar and Andrew Lin,
 *  this programming assignment is our own work
 *  and we have not provided this code to any other student.
 *
 *  Number of slip days used:
 *
 *  Student 1: Vishal Vijayakumar
 *  UTEID: vv8945
 *  email address: vishal.vijayakumar@utexas.edu
 *
 *  Student 2: Andrew Lin
 *  UTEID: al58444
 *  email address: alin257274@utexas.edu
 *
 *  Grader name: Casey
 *  Section number: 50760
 */

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.io.OutputStream;

public class Compression implements IHuffConstants {
    private int[] charCounts;
    private HashMap<Integer, String> encoding;
    private int headerFormat;
    private TreeNode compressTree;
    private int precompressVal;
    private int totalNumNodes;
    
    /**
     * Preprocess data so that compression is possible ---
     * count characters/create tree/store state so that
     * a subsequent call to compress will work. The InputStream
     * is <em>not</em> a BitInputStream, so wrap it int one as needed.
     * @param in is the stream which could be subsequently compressed
     * @param headerFormat a constant from IHuffProcessor that determines what kind of
     * header to use, standard count format, standard tree format, or
     * possibly some format added in the future.
     * @return number of bits saved by compression or some other measure
     * Note, to determine the number of
     * bits saved, the number of bits written includes
     * ALL bits that will be written including the
     * magic number, the header format number, the header to
     * reproduce the tree, AND the actual data.
     * @throws IOException if an error occurs while reading from the input file.
     */
    public int preCompress(InputStream in, int headerInfo) throws IOException {
        charCounts = new int[ALPH_SIZE + 1];
        headerFormat = headerInfo; 
        // Count characters in the inputed file, and fills
        // charCounts array of frequencies
        int initialLen = countCharacters(in);

        // Creates tree based on frequencies
        compressTree = createTree();

        // Creates encodings for all the characters in
        // the inputted file to 
        encoding = new HashMap<>();
        totalNumNodes = createEncoding(compressTree, "");
        
        int actualDataLen = countActualDataLen();
        int headerLen = 0;
        if (headerFormat == STORE_COUNTS) {
            // the number of bits in this header is the total number of characters times the
            // number of bits it takes to represent each frequency
            headerLen = ALPH_SIZE * BITS_PER_INT;
        } else if (headerFormat == STORE_TREE) {
            // The number of bits representing the size of tree + the number of total nodes + 
            // the number of leaves * (bits per word + 1)
            headerLen = BITS_PER_INT + totalNumNodes + encoding.size() * (BITS_PER_WORD + 1);
        }

        // the number of bits saved is calculated as
        // the number of bits used in the initial file - the bits it takes to 
        // write all of the data in the new file - the length of the magic number
        // - the length of the header format number - the length of the header
        precompressVal = initialLen - (actualDataLen + BITS_PER_INT * 2 + headerLen);

        return precompressVal;
    }

    /**
	 * Compresses input to output, where the same InputStream has
     * previously been pre-processed via <code>preprocessCompress</code>
     * storing state used by this call.
     * <br> pre: <code>preprocessCompress</code> must be called before this method
     * @param in is the stream being compressed (NOT a BitInputStream)
     * @param out is bound to a file/stream to which bits are written
     * for the compressed file (not a BitOutputStream)
     * @param force if this is true create the output file even if it is larger than the input file.
     * If this is false do not create the output file if it is larger than the input file.
     * @return the number of bits written.
     * @throws IOException if an error occurs while reading from the input file or
     * writing to the output file.
     */
    public int compress(InputStream input, OutputStream out, boolean force) throws IOException {
        if (precompressVal < 0 && !force) {
            // This means that we are actually writing more bits when
            // "compressing"
            return precompressVal;
        }

        BitOutputStream output = new BitOutputStream(out);
        output.writeBits(BITS_PER_INT, MAGIC_NUMBER);
        output.writeBits(BITS_PER_INT, headerFormat);
        int count = BITS_PER_INT * 2;

        // Prints the headers based on how it should be formatted
        if (headerFormat == STORE_COUNTS) {
            for (int i = 0; i < charCounts.length - 1; i++) {
                output.writeBits(BITS_PER_INT, charCounts[i]);
                count += BITS_PER_INT;
            }
        } else if (headerFormat == STORE_TREE) {
            count += BITS_PER_INT;
            // writes how many of the following bits are part of the encoded tree
            output.writeBits(BITS_PER_INT, totalNumNodes + encoding.size() * (BITS_PER_WORD + 1));
            count += writeTree(compressTree, output);
        }

        count += writeData(input, output);
        output.close();
        input.close();

        return count;
    }

    /**
     * Writes stored tree to output file
     * @param node represents the current node we are writing to the file
     * @param output is bound to a file/stream to which bits are written
     * for the compressed file, it is a BitOutputStream
     * @return the number of bits written
     */
    private int writeTree(TreeNode node, BitOutputStream output) {
        if (node.isLeaf()) {
            output.writeBits(1, 1);
            output.writeBits(BITS_PER_WORD + 1, node.getValue());
            return BITS_PER_WORD + 2;
        }
        output.writeBits(1, 0);
        return 1 + writeTree(node.getLeft(), output) + writeTree(node.getRight(), output);
    }

    /**
     * Encodes the data from in to output
     * @param in is the stream which could be subsequently compressed
     * @param output is bound to a file/stream to which bits are written
     * for the compressed file, it is a BitOutputStream
     * @return the number of bits written
     * @throws IOException if an error occurs while reading from the input file or
     * writing to the output file.
     */
    private int writeData(InputStream in, BitOutputStream output) throws IOException {
        BitInputStream input = new BitInputStream(in);
        int count = 0;
        int inbits = input.readBits(BITS_PER_WORD);
        
        while (inbits != -1) {
            String code = encoding.get(inbits);
            printCode(code, output);
            count += code.length();
            inbits = input.readBits(BITS_PER_WORD);
        }
        String pseudoCode = encoding.get(PSEUDO_EOF);
        printCode(pseudoCode, output);
        count += pseudoCode.length();
        input.close();
        return count;
    }

    /**
     * Prints code to output
     * @param code represents the String we are printing to output
     * @param output is bound to a file/stream to which bits are written
     * for the compressed file, it is a BitOutputStream
     */
    private void printCode(String code, BitOutputStream output) {
        for (int i = 0; i < code.length(); i++) {
            output.writeBits(1, Integer.valueOf(code.substring(i, i + 1)));
        }
    }

    /**
     * Calculates the number of characters used in the original file
     * @return the number of characters read
     */
    private int countActualDataLen() {
        int counter = 0;
        for (int i = 0; i < charCounts.length; i++) {
            if (charCounts[i] != 0) {
                counter += encoding.get(i).length() * charCounts[i];
            }
        }

        return counter;
    }

    /**
     * Fills encoding with the value of each node and the path it took to get to that node
     * @param node represents the current node we are considering
     * @param path represents the path it took to get to the current node
     * @return the total number of nodes
     */
    private int createEncoding(TreeNode node, String path) {
        if (node.isLeaf()) {
            encoding.put(node.getValue(), path);
            return 1;
        }
        int num = createEncoding(node.getLeft(), path + "0");
        num++;
        num += createEncoding(node.getRight(), path + "1");
        return num;
    }

    /**
     * Creates a tree to store all of the character frequencies
     * @return TreeNode that stores all of the character frequencies
     */
    private TreeNode createTree() {
    	TreeNodePriorityQueue<TreeNode> pq = new TreeNodePriorityQueue<>();
        // Adds all characters with frequencies (TreeNodes) to PriorityQueue
        for (int i = 0 ; i < charCounts.length; i++) {
            if (charCounts[i] != 0) {
                pq.enqueue(new TreeNode(i, charCounts[i]));
            }
        }

        return generateTree(pq);
    }

    /**
     * Generates a tree from the passed PriorityQueue with TreeNodes of characters with
     * higher frequencies typically close to the root in terms of height
     * @param pq represents a PriorityQueue with nodes of characters and frequencies
     * @return tree of TreeNodes
     */
    private TreeNode generateTree(TreeNodePriorityQueue<TreeNode> pq) {
        // We shall keep merging the first and second element of the 
        // PriorityQueue until there exists one element, as that means we are done
        while (pq.size() > 1) {
            TreeNode first = pq.dequeue();
            TreeNode second = pq.dequeue();

            pq.enqueue(new TreeNode(first, -1, second));
        }

        return pq.dequeue();
    }

    /**
     * Counts the number of characters in the input file
     * @param in is the stream being compressed (NOT a BitInputStream)
     * @return the number of characters read
     * @throws IOException if an error occurs while reading from the input file or
     * writing to the output file.
     */
    private int countCharacters(InputStream in) throws IOException {
        BitInputStream bits = new BitInputStream(in);
        int count = 0;
        int inbits = bits.readBits(BITS_PER_WORD);
        
        while (inbits != -1) {
            count += BITS_PER_WORD;
            charCounts[inbits]++;
            inbits =  bits.readBits(BITS_PER_WORD);
        }
        
        // this represents the PSEUDO_EOF character
        charCounts[charCounts.length - 1]++;
        bits.close();
        return count;
    }
}