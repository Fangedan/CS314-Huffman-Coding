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

public class Decompressor implements IHuffConstants {
    private TreeNode huffRoot;
    public final static int NO_PSEUDO_ERROR_CODE = -2;

    /**
     * Reads a Huffman-compressed file, validates its format, rebuilds the tree,
     * and decompresses the encoded data into the output stream.
     * @param input the compressed input stream
     * @param output the stream to write the decompressed data to
     * @return number of bits written to output, or -1 for invalid input
     * @throws IOException if there’s an error in reading or writing streams
     */
    public int decompress(InputStream input, OutputStream output) throws IOException {
        BitInputStream bitIn = new BitInputStream(input);
        int num = bitIn.readBits(BITS_PER_INT);
        if (num != MAGIC_NUMBER) {
            bitIn.close();
            return -1;
        }
        int formatType = bitIn.readBits(BITS_PER_INT);
        int totalBitsWritten = 0;
        if (formatType == STORE_COUNTS) {
            buildTreeFromCounts(bitIn);
        } else if (formatType == STORE_TREE) {
            buildTreeFromStructure(bitIn);
        }
        totalBitsWritten = decodeCompressedData(bitIn, output, huffRoot);
        return totalBitsWritten;
    }

    /**
     * Reconstructs the encoding tree from a compressed file's tree-based header format.
     * Begins by reading the total number of bits used to represent the tree.
     * @param in bit stream containing the serialized tree structure
     * @throws IOException if reading the stream fails
     */
    private void buildTreeFromStructure(BitInputStream bitIn) throws IOException {
        int treeBitCount = bitIn.readBits(BITS_PER_INT);
        huffRoot = rebuildTree(bitIn, new int[] {treeBitCount}, new TreeNode(-1, 0));
    }

    /**
     * Builds the Huffman tree from a tree-encoded bit stream using recursion.
     * Leaf nodes are marked by a 1 followed by a value, and internal nodes by a 0,
     * followed by their left and right subtrees.
     *
     * @param bitIn the bit input stream with the encoded tree
     * @param bitsRemaining a mutable integer array tracking remaining bits to read
     * @param currentNode the node currently being initialized or expanded
     * @return the constructed node corresponding to this portion of the tree
     * @throws IOException if reading from the bit stream fails
     */
    private TreeNode rebuildTree(BitInputStream bitIn, int[] bitsRemaining, TreeNode currentNode) throws IOException {
        if (bitsRemaining[0] == 0) {
            return null;
        }
        int nextBit = bitIn.readBits(1);
        bitsRemaining[0]--;
        if (nextBit == 1) {
            currentNode = new TreeNode(bitIn.readBits(BITS_PER_WORD + 1), 0);
        } else {
            currentNode.setLeft(rebuildTree(bitIn, bitsRemaining, new TreeNode(-1, 0)));
            currentNode.setRight(rebuildTree(bitIn, bitsRemaining, new TreeNode(-1, 0)));
        }
        return currentNode;
    }

    /**
     * Constructs a Huffman tree based on character frequencies provided in the input stream.
     * The frequencies are stored in a priority queue and used to generate the encoding tree.
     *
     * @param bitIn bit input stream with character count data
     * @throws IOException if reading from the input stream fails
     */
    private void buildTreeFromCounts(BitInputStream bitIn) throws IOException {
        TreeNodePriorityQueue<TreeNode> nodeQueue = new TreeNodePriorityQueue<>();
        for (int i = 0; i < ALPH_SIZE; i++) {
            int charFreq = bitIn.readBits(BITS_PER_INT);
            if (charFreq != 0) {
                nodeQueue.enqueue(new TreeNode(i, charFreq));
            }
        }
        nodeQueue.enqueue(new TreeNode(PSEUDO_EOF, 1));
        huffRoot = constructTree(nodeQueue);
    }

    /**
     * Uses the Huffman tree to decode a stream of compressed bits into raw characters,
     * writing the result to the output stream. Ends decoding when PSEUDO_EOF is reached.
     *
     * @param bitIn the bit input stream of the compressed data
     * @param bitOut the output stream to write decoded characters
     * @param currentNode the node used for decoding traversal (typically the root)
     * @return total number of bits written to output, or an error code if PSEUDO_EOF is missing
     * @throws IOException if reading from input or writing to output fails
     */
    private int decodeCompressedData(BitInputStream in, OutputStream out, TreeNode currentNode) 
            throws IOException {
        BitOutputStream bitOut = new BitOutputStream(out);
        int nextBit = in.readBits(1);
        int totalBitsWritten = 0;

        while (nextBit != -1) {
            if (currentNode.isLeaf()) {
                bitOut.writeBits(BITS_PER_WORD, currentNode.getValue());
                currentNode = huffRoot;
                totalBitsWritten += BITS_PER_WORD;
            }
            if (nextBit == 0) {
                currentNode = currentNode.getLeft();
            } else {
                currentNode = currentNode.getRight();
            }
            if (currentNode.getValue() == PSEUDO_EOF) {
                bitOut.close();
                return totalBitsWritten;
            }
            nextBit = in.readBits(1);
        }
        bitOut.close();
        return NO_PSEUDO_ERROR_CODE;
    }

    /**
     * Generates the Huffman tree used for encoding by merging nodes from the priority queue.
     * Continues merging the two smallest nodes until the tree is complete.
     *
     * @param nodeQueue the queue containing TreeNodes ordered by frequency
     * @return the root node of the resulting Huffman tree
     */
    private TreeNode constructTree(TreeNodePriorityQueue<TreeNode> nodeQueue) {
        while (nodeQueue.size() > 1) {
            TreeNode left = nodeQueue.dequeue();
            TreeNode right = nodeQueue.dequeue();
            TreeNode merged = new TreeNode(left, -1, right);
            nodeQueue.enqueue(merged);
        }
        return nodeQueue.dequeue();
    }
}