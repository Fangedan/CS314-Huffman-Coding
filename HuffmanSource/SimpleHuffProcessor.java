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
import java.util.HashMap;
import java.util.PriorityQueue;

public class SimpleHuffProcessor implements IHuffProcessor {

    private IHuffViewer myViewer;
    private TreeNode huffmanTreeRoot;
    private int myBitsSaved;

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
    public int preprocessCompress(InputStream in, int headerFormat) throws IOException {
        BitInputStream bitSc = new BitInputStream(in);
        HashMap<Integer, Integer> freqMap = new HashMap<>();

        // Count frequency of each byte
        int totalBeforeComp = 0;
        int current = bitSc.read();
        while (current != -1) {
            freqMap.put(current, freqMap.getOrDefault(current, 0) + 1);
            current = bitSc.read();
            totalBeforeComp += IHuffConstants.BITS_PER_WORD;
        }
        bitSc.close();

        // Add PSEUDO_EOF with frequency 1
        freqMap.put(IHuffConstants.PSEUDO_EOF, 1);

        TreeNodePriorityQueue pq = new TreeNodePriorityQueue();

        // Create leaf nodes and add to priority queue
        for (int key : freqMap.keySet()) {
            pq.offer(new TreeNode(key, freqMap.get(key)));
        }

        // Build Huffman tree
        while (pq.size() > 1) {
            TreeNode left = pq.poll();
            TreeNode right = pq.poll();
            pq.offer(new TreeNode(left, -1, right));
        }

        TreeNode root = pq.poll(); // Huffman tree root
        huffmanTreeRoot = root;

        int totalBits = 0;

        if (headerFormat == IHuffConstants.STORE_COUNTS) {
            // MAGIC + FORMAT + 256 * frequencies
            totalBits = IHuffConstants.BITS_PER_INT * (2 + IHuffConstants.ALPH_SIZE);
        } else if (headerFormat == IHuffConstants.STORE_TREE) {
            // MAGIC + FORMAT + treeSize + encoded tree
            int treeBits = countTreeBits(root);
            totalBits = IHuffConstants.BITS_PER_INT * 3 + treeBits;
        }

        myBitsSaved = totalBeforeComp - totalBits;
        return myBitsSaved;
        //return totalBits;
    }

    // Helper method to count bits to encode tree in STORE_TREE format
    private int countTreeBits(TreeNode node) {
        if (node.getLeft() == null && node.getRight() == null) {
            return 10; // 1 bit for leaf + 9 bits for value
        } else {
            return 1 + countTreeBits(node.getLeft()) + countTreeBits(node.getRight());
        }
    }

    // Method to get the root if needed later
    public TreeNode getHuffmanTreeRoot() {
        return huffmanTreeRoot;
    }

    // Helper method to show messages in viewer
    private void showString(String message) {
        if (myViewer != null) {
            myViewer.update(message);
        }
    }

    // Helper method to show message in setViewer
    public void setViewer(IHuffViewer viewer) {
        myViewer = viewer;
    }

    /**
    * Builds a map of 8-bit int values to Huffman coding strings.
    * @param root The root of the Huffman tree.
    * @return A map from each int (chunk) to its Huffman coding.
    */
    private Map<Integer, String> buildHuffmanCodingMap(TreeNode root) {
        Map<Integer, String> map = new HashMap<>();
        buildCodingHelper(root, "", map);
        return map;
    }

    /**
    * Recursive helper method to build Huffman codes.
    * @param node Current TreeNode in the traversal.
    * @param path The binary string built so far (e.g. "010").
    * @param map The map that will store int-to-code mappings.
    */
    private void buildCodingHelper(TreeNode node, String path, Map<Integer, String> map) {
        if (node == null) return;
        if (node.isLeaf()) {
            map.put(node.getValue(), path);
            return;
        }
        buildCodingHelper(node.getLeft(), path + "0", map);
        buildCodingHelper(node.getRight(), path + "1", map);
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
    public int compress(InputStream in, OutputStream out, boolean force)
            throws IOException {
        // precondition
        if (huffmanTreeRoot == null) {
            throw new IllegalStateException(
                "Must call preprocessCompress(...) before compress(...)");
        }
        if (myBitsSaved < 0 && !force) {
            myViewer.update("Compression would enlarge file; skipping (use force=true to override).");
            return 0;
        }
        // build code map
        Map<Integer,String> codeMap = buildHuffmanCodingMap(huffmanTreeRoot);

        BitInputStream  bitIn  = new BitInputStream(in);
        BitOutputStream bitOut = new BitOutputStream(out);
        int bitsWritten = 0;

        // 1) MAGIC and FORMAT
        bitOut.writeBits(IHuffConstants.BITS_PER_INT,
                         IHuffConstants.MAGIC_NUMBER);
        bitOut.writeBits(IHuffConstants.BITS_PER_INT, myHeaderFormat);
        bitsWritten += 2 * IHuffConstants.BITS_PER_INT;

        // 3) HEADER
        if (myHeaderFormat == IHuffConstants.STORE_COUNTS) {
            // write 256 frequencies (one per value 0–255), then PSEUDO_EOF count
            for (int v = 0; v < IHuffConstants.ALPH_SIZE; v++) {
                int freq = myFreqMap.getOrDefault(v, 0);
                bitOut.writeBits(IHuffConstants.BITS_PER_INT, freq);
                bitsWritten += IHuffConstants.BITS_PER_INT;
            }
            // PSEUDO_EOF
            int eofFreq = myFreqMap.get(IHuffConstants.PSEUDO_EOF);
            bitOut.writeBits(IHuffConstants.BITS_PER_INT, eofFreq);
            bitsWritten += IHuffConstants.BITS_PER_INT;

        } else { // STORE_TREE
            bitsWritten += writeHeaderTree(huffmanTreeRoot, bitOut);
        }

        // 4) BODY: for each 8‑bit chunk, emit one bit at a time
        int b;
        while ((b = bitIn.read()) != -1) {
            String code = codeMap.get(b);
            for (char c : code.toCharArray()) {
                bitOut.writeBits(1, c - '0');
                bitsWritten++;
            }
        }

        // 5) PSEUDO_EOF
        String eofCode = codeMap.get(IHuffConstants.PSEUDO_EOF);
        for (char c : eofCode.toCharArray()) {
            bitOut.writeBits(1, c - '0');
            bitsWritten++;
        }

        bitIn.close();
        bitOut.close();
        return bitsWritten;
    }

    /**
     * Helper: writes the shape+leaves of the Huffman tree in pre‐order.
     * Internal node → write a single 0 bit.
     * Leaf node     → write a 1 bit, then (BITS_PER_WORD+1) bits of the value.
     * Returns how many bits were written.
     */
    private int writeHeaderTree(TreeNode node, BitOutputStream out) throws IOException {
        if (node.isLeaf()) {
            // leaf marker
            out.writeBits(1, 1);
            // leaf value
            out.writeBits(IHuffConstants.BITS_PER_WORD + 1, node.getValue());
            return 1 + (IHuffConstants.BITS_PER_WORD + 1);
        } else {
            // internal marker
            out.writeBits(1, 0);
            // recurse left/right
            int leftBits  = writeHeaderTree(node.getLeft(), out);
            int rightBits = writeHeaderTree(node.getRight(), out);
            return 1 + leftBits + rightBits;
        }
    }

        // String code = codeMap.get(value);
        // for (int i = 0; i < code.length(); i++) {
        //     int bit = code.charAt(i) - '0'; // converts '0' or '1' to int 0 or 1
        //     out.writeBits(1, bit);
        // }

        //return 0;

    /**
     * Uncompress a previously compressed stream in, writing the
     * uncompressed bits/data to out.
     * @param in is the previously compressed data (not a BitInputStream)
     * @param out is the uncompressed file/stream
     * @return the number of bits written to the uncompressed file/stream
     * @throws IOException if an error occurs while reading from the input file or
     * writing to the output file.
     */
    public int uncompress(InputStream in, OutputStream out) throws IOException {
	        throw new IOException("uncompress not implemented");
	        //return 0;
    }

    public void setViewer(IHuffViewer viewer) {
        myViewer = viewer;
    }

    private void showString(String s){
        if (myViewer != null) {
            myViewer.update(s);
        }
    }
}
