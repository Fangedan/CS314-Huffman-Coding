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
    private int[] charFreq;
    private HashMap<Integer, String> codeMap;
    private int headerType;
    private TreeNode treeRoot;
    private int uncompressedLength;
    private int totalNodes;
    
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
        this.headerType = headerInfo;
        this.charFreq = new int[ALPH_SIZE + 1];
        int initialLen = countCharacters(in);
        this.treeRoot = createTree();
        this.codeMap = new HashMap<>();
        this.totalNodes = createEncoding(treeRoot, "");
        int dataLen = countDataLen();
        int headerLen = 0;
        if (headerType == STORE_COUNTS) {
            headerLen = ALPH_SIZE * BITS_PER_INT;
        } else if (headerType == STORE_TREE) {
            headerLen = BITS_PER_INT + totalNodes + codeMap.size() * (BITS_PER_WORD + 1);
        }
        this.uncompressedLength = initialLen
            - (dataLen + 2 * BITS_PER_INT + headerLen);       
        return uncompressedLength;
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
        if (uncompressedLength < 0 && !force) {
            return uncompressedLength;
        }
        BitOutputStream output = new BitOutputStream(out);
        int count = 0;
        output.writeBits(BITS_PER_INT, MAGIC_NUMBER);
        output.writeBits(BITS_PER_INT, headerType);
        count += BITS_PER_INT * 2;
        if (headerType == STORE_COUNTS) {
            for (int i = 0; i < charFreq.length - 1; i++) {
                output.writeBits(BITS_PER_INT, charFreq[i]);
                count += BITS_PER_INT;
            }
        } else if (headerType == STORE_TREE) {
            int treeSize = totalNodes + codeMap.size() * (BITS_PER_WORD + 1);
            output.writeBits(BITS_PER_INT, treeSize);
            count += BITS_PER_INT;
            count += writeTree(treeRoot, output);
        }
        count += writeData(input, output);
        output.close();
        input.close();
        return count;
    }

    /**
     * Recursively encodes the Huffman tree structure to the output stream.
     * @param node the current node in the Huffman tree being processed
     * @param output the BitOutputStream where the tree structure is being written
     * @return total number of bits written to the output stream
     */
    private int writeTree(TreeNode node, BitOutputStream output) {
        if (node.isLeaf()) {
            output.writeBits(1, 1);
            output.writeBits(BITS_PER_WORD + 1, node.getValue());
            return BITS_PER_WORD + 2;
        } else {
            output.writeBits(1, 0);
            int leftBits  = writeTree(node.getLeft(), output);
            int rightBits = writeTree(node.getRight(), output);
            return leftBits + rightBits + 1;
        }
    }

    /**
     * Reads raw data from the input stream, encodes it using Huffman codes,
     * and writes the resulting bits to the output stream.
     * @param in the input stream to be compressed
     * @param output the BitOutputStream receiving the encoded bit data
     * @return total number of bits written to the compressed file
     * @throws IOException if an error occurs during input or output operations
     */
    private int writeData(InputStream in, BitOutputStream output) throws IOException {
        BitInputStream input = new BitInputStream(in);
        int count = 0;
        for (int inbits = input.readBits(BITS_PER_WORD); inbits != -1; inbits = input.readBits(BITS_PER_WORD)) {
            String code = codeMap.get(inbits);
            printCode(code, output);
            count += code.length();
        }
        String pseudoCode = codeMap.get(PSEUDO_EOF);
        printCode(pseudoCode, output);
        count += pseudoCode.length();
        input.close();
        return count;
    }

    /**
     * Converts a binary string into bits and writes them to the output stream.
     * @param code Huffman code string made of '0's and '1's
     * @param output stream where the individual bits are written
     */
    private void printCode(String code, BitOutputStream output) {
        for (int i = 0, len = code.length(); i < len; ++i) {
            output.writeBits(1, code.charAt(i) - '0');
        }
    }
    /**
     * Determines the bit length of the compressed file content
     * by summing each character’s frequency times its Huffman code length.
     * @return total number of bits required for encoding all input characters
     */
    private int countDataLen() {
        int sum = 0;
        for (int i = 0, n = charFreq.length; i < n; i++) {
            int freq = charFreq[i];
            if (freq == 0){
                continue;
            }
            sum += codeMap.get(i).length() * freq;
        }
        return sum;
    }

    /**
     * Fills codeMap with the value of each node and the path it took to get to that node
     * @param treeNode represents the current node we are considering
     * @param path represents the path it took to get to the current node
     * @return the total number of nodes
     */
    private int createEncoding(TreeNode node, String path) {
        if (node.isLeaf()) {
            codeMap.put(node.getValue(), path);
            return 1;
        }
        int subtotal = 1;
        String leftPath = path + "0";
        String rightPath= path + "1";
        subtotal += createEncoding(node.getLeft(), leftPath);
        subtotal += createEncoding(node.getRight(), rightPath);
        return subtotal;
    }

    /**
     * Creates a tree to store all of the character frequencies.
     * @return TreeNode that stores all of the character frequencies
     */
    private TreeNode createTree() {
        TreeNodePriorityQueue<TreeNode> pq = new TreeNodePriorityQueue<>();
        int n = charFreq.length;
        for (int i = 0; i < n; i++) {
            int f = charFreq[i];
            if (f == 0){
                continue;
            }
            pq.enqueue(new TreeNode(i, f));
        }
        return generateTree(pq);
    }

    /**
     * Builds a Huffman tree from a priority queue of TreeNodes.
     * Combines the two nodes with the lowest frequencies until only one tree remains.
     * @param pq priority queue of TreeNodes, ordered by frequency
     * @return the root of the final Huffman tree
     */
    private TreeNode generateTree(TreeNodePriorityQueue<TreeNode> pq) {
        while (pq.size() > 1) {
            TreeNode first = pq.dequeue(); 
            TreeNode second = pq.dequeue();  
            pq.enqueue(new TreeNode(first, -1, second));
        }
        return pq.dequeue();
    }

    /**
     * Tallies the number of characters in the input stream and updates
     * the frequency table accordingly.
     * @param in the stream from which characters are read
     * @return number of bits read from the input stream
     * @throws IOException if any I/O operation fails
     */
    private int countCharacters(InputStream in) throws IOException {
        BitInputStream bits = new BitInputStream(in);
        int totalBits = 0;
        for (int value = bits.readBits(BITS_PER_WORD); value != -1; value = bits.readBits(BITS_PER_WORD)) {
            totalBits += BITS_PER_WORD;
            charFreq[value]++;
        }
        charFreq[charFreq.length - 1]++;
        bits.close();
        return totalBits;
    }
}