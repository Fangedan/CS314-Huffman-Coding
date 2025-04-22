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
import java.io.OutputStream;

public class Decompression implements IHuffConstants {
    private TreeNode root;
    public final static int NO_PSEUDO_ERROR_CODE = -2;

    /**
     * Decompresses the input file and creates an output file.
     * pre: none
     * @param input the input stream.
     * @param out the output stream.
     * @returns the number of bits written to the output file.
     */
    public int decompress(InputStream input, OutputStream out) throws IOException {
        BitInputStream in = new BitInputStream(input);
        int magic = in.readBits(BITS_PER_INT);
        if (magic != MAGIC_NUMBER) {
            in.close();
            return -1;
        }
        int headerFormat = in.readBits(BITS_PER_INT);
        int bitsWritten = 0;
        if (headerFormat == STORE_COUNTS) {
            createCountsTree(in);
        } else if (headerFormat == STORE_TREE) {
            createTreeTree(in);
        }
        bitsWritten = readData(in, out, root);

        return bitsWritten;
    }

    /**
     * Constructs the tree that builds the encodings for a Standard Tree Format file.
     * pre: none
     * @param in the bit input stream.
     */
    private void createTreeTree(BitInputStream in) throws IOException {
        int numBits = in.readBits(BITS_PER_INT);
        root = helper(in, new int[] {numBits}, new TreeNode(-1, 0));
    }

    /**
     * Helper method that recursively constructs the tree that builds the encodings for a Standard
     * Tree Format file.
     * pre: none
     * @param in the bit input stream.
     * @param numBits which stores the amount of bits left in the compressed file.
     * @param node the current node in the tree.
     * @return node, the current node in the tree.
     */
    private TreeNode helper(BitInputStream in, int[] numBits, TreeNode node) throws IOException {
        if (numBits[0] == 0) {
            return null;
        }
        int bit = in.readBits(1);
        numBits[0]--;
        if (bit == 1) {
            node = new TreeNode(in.readBits(BITS_PER_WORD + 1), 0);
        } else {
            node.setLeft(helper(in, numBits, new TreeNode(-1, 0)));
            node.setRight(helper(in, numBits, new TreeNode(-1, 0)));
        }
        return node;
    }

    /**
     * Creates the tree that builds the encodings for a Standard Counts Format file.
     * pre: none
     * @param in the bit input stream.
     */
    private void createCountsTree(BitInputStream in) throws IOException {
    	TreeNodePriorityQueue<TreeNode> pq = new TreeNodePriorityQueue<>();
        for (int i = 0; i < ALPH_SIZE; i++) {
            int frequency = in.readBits(BITS_PER_INT);
            if (frequency != 0) {
                pq.enqueue(new TreeNode(i, frequency));
            }
        }
        pq.enqueue(new TreeNode(PSEUDO_EOF, 1));

        root = generateTree(pq);
    }

    /**
     * Reads the data from the compressed file and uses the tree to write the decompressed 
     * output file.
     * pre: none
     * @param in the bit input stream.
     * @param output the output stream.
     * @param node the current node in the tree used to read encodings.
     * @returns the number of bits written to the output file.
     */
    private int readData(BitInputStream in, OutputStream output, TreeNode node) 
            throws IOException {
        BitOutputStream out = new BitOutputStream(output);
        int bit = in.readBits(1);
        int bitsWritten = 0;
        while (bit != -1) {
            if (node.isLeaf()) {
                out.writeBits(BITS_PER_WORD, node.getValue());
                node = root;
                bitsWritten += BITS_PER_WORD;
            }
            if (bit == 0) {
                node = node.getLeft();
            } else {
                node = node.getRight();
            }
            if (node.getValue() == PSEUDO_EOF) {
                out.close();
                return bitsWritten;
            }
            bit = in.readBits(1);
        }
        out.close();
        return NO_PSEUDO_ERROR_CODE;
    }

    /**
     * Helper method that recursively creates the tree that builds the encodings for a 
     * Standard Counts Format file.
     * pre: none
     * @param pq the priority queue that holds the nodes in the tree.
     * @returns the first node in the queue.
     */
    private TreeNode generateTree(TreeNodePriorityQueue<TreeNode> pq) {
        while (pq.size() > 1) {
            TreeNode first = pq.dequeue();
            TreeNode second = pq.dequeue();

            pq.enqueue(new TreeNode(first, -1, second));
        }

        return pq.dequeue();
    }

}