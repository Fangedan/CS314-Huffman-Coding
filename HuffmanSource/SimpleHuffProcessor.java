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
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class SimpleHuffProcessor implements IHuffProcessor {

    private IHuffViewer myViewer;
    private TreeNode huffmanTreeRoot;
    private int myBitsSaved;
    private int myHeaderFormat;
    private Map<Integer,Integer> myFreqMap;

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
        Map<Integer,Integer> freqMap = new HashMap<>();
        myHeaderFormat = headerFormat;
        int totalBeforeComp = 0;
        int curr;
        while ((curr = bitSc.read()) != -1) {
            freqMap.put(curr, freqMap.getOrDefault(curr, 0) + 1);
            totalBeforeComp += IHuffConstants.BITS_PER_WORD;
        }
        bitSc.close();
        freqMap.put(IHuffConstants.PSEUDO_EOF, 1);
        huffmanTreeRoot = buildTreeFromCounts(freqMap);
        myFreqMap = freqMap;
        int headerBits = 0;
        if (headerFormat == IHuffConstants.STORE_COUNTS) {
            headerBits = IHuffConstants.BITS_PER_INT * (2 + IHuffConstants.ALPH_SIZE);
            showString("Standard Count Format:");
        } else {
            headerBits = IHuffConstants.BITS_PER_INT * 3 + countTreeBits(huffmanTreeRoot);
            showString("Standard Tree Format:");
        }
        Map<Integer,String> codeMap = buildHuffmanCodingMap(huffmanTreeRoot);
        int bodyBits = 0;
        for (Map.Entry<Integer,Integer> entry : freqMap.entrySet()) {
            bodyBits += entry.getValue() * codeMap.get(entry.getKey()).length();
        } 
        int totalBits = headerBits + bodyBits;
        myBitsSaved = totalBeforeComp - totalBits;
        return myBitsSaved;
    }

    // Building huffman tree from storecounts helper variable
    private TreeNode buildTreeFromCounts(Map<Integer,Integer> freqMap) {
        TreeNodePriorityQueue pq = new TreeNodePriorityQueue();
        for (int i = 0; i < IHuffConstants.ALPH_SIZE; i++) {
            int f = freqMap.getOrDefault(i, 0);
            if (f > 0) {
                pq.offer(new TreeNode(i, f));
            }
        }
        pq.offer(new TreeNode(IHuffConstants.PSEUDO_EOF,
        		freqMap.getOrDefault(IHuffConstants.PSEUDO_EOF, 1)));
        while (pq.size() > 1) {
            TreeNode left  = pq.poll();
            TreeNode right = pq.poll();
            pq.offer(new TreeNode(left, -1, right));
        }
        return pq.poll();
    }

    // Helper method to count bits to encode tree in STORE_TREE format
    private int countTreeBits(TreeNode node) {
        if (node.getLeft() == null && node.getRight() == null) {
            return 10;
        } else {
            return 1 + countTreeBits(node.getLeft()) + countTreeBits(node.getRight());
        }
    }

    // Returns root
    public TreeNode getHuffmanTreeRoot() {
        return huffmanTreeRoot;
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
        if (node != null) {
            if (node.isLeaf()) {
                map.put(node.getValue(), path);
            } else {
                buildCodingHelper(node.getLeft(),  path + "0", map);
                buildCodingHelper(node.getRight(), path + "1", map);
            }
        }
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
        if (huffmanTreeRoot == null) {
            throw new IllegalStateException("Must call preprocessCompress before compress");
        }
        if (myBitsSaved < 0 && !force) {
            myViewer.update("Compression would enlarge file; skipping.");
            return 0;
        } 
        Map<Integer,String> codeMap = buildHuffmanCodingMap(huffmanTreeRoot);
        BitInputStream bitIn = new BitInputStream(in);
        BitOutputStream bitOut = new BitOutputStream(out);
        int bitsWritten = 0;
        bitOut.writeBits(IHuffConstants.BITS_PER_INT,
                         IHuffConstants.MAGIC_NUMBER);
        bitOut.writeBits(IHuffConstants.BITS_PER_INT, myHeaderFormat);
        bitsWritten += 2 * IHuffConstants.BITS_PER_INT;
        if (myHeaderFormat == IHuffConstants.STORE_COUNTS) {
            for (int v = 0; v < IHuffConstants.ALPH_SIZE; v++) {
                int freq = myFreqMap.getOrDefault(v, 0);
                bitOut.writeBits(IHuffConstants.BITS_PER_INT, freq);
                bitsWritten += IHuffConstants.BITS_PER_INT;
            }
            int eofFreq = myFreqMap.get(IHuffConstants.PSEUDO_EOF);
            bitOut.writeBits(IHuffConstants.BITS_PER_INT, eofFreq);
            bitsWritten += IHuffConstants.BITS_PER_INT;
        } else { 
            bitsWritten += writeHeaderTree(huffmanTreeRoot, bitOut);
        }
        int b;
        while ((b = bitIn.read()) != -1) {
            String code = codeMap.get(b);
            for (char c : code.toCharArray()) {
                bitOut.writeBits(1, c - '0');
                bitsWritten++;
            }
        }
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
     * Internal node: write a single 0 bit.
     * Leaf node:write a 1 bit, then (BITS_PER_WORD+1) bits of the value.
     * Returns how many bits were written.
     */
    private int writeHeaderTree(TreeNode node, BitOutputStream out) throws IOException {
        if (node.isLeaf()) {
            out.writeBits(1, 1);
            out.writeBits(IHuffConstants.BITS_PER_WORD + 1, node.getValue());
            return 1 + (IHuffConstants.BITS_PER_WORD + 1);
        } else {
            out.writeBits(1, 0);
            int leftBits  = writeHeaderTree(node.getLeft(), out);
            int rightBits = writeHeaderTree(node.getRight(), out);
            return 1 + leftBits + rightBits;
        }
    }

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
        BitInputStream  bitIn  = new BitInputStream(in);
        BitOutputStream bitOut = new BitOutputStream(out);
        int magic = bitIn.readBits(IHuffConstants.BITS_PER_INT);
        if (magic != IHuffConstants.MAGIC_NUMBER) {
            throw new IOException("Input is not a valid .hf file");
        }
        int format = bitIn.readBits(IHuffConstants.BITS_PER_INT);
        TreeNode root = null;
        if (format == IHuffConstants.STORE_COUNTS) {
            int[] freqs = new int[IHuffConstants.ALPH_SIZE + 1];
            for (int i = 0; i < IHuffConstants.ALPH_SIZE; i++) {
                freqs[i] = bitIn.readBits(IHuffConstants.BITS_PER_INT);
            }
            freqs[IHuffConstants.PSEUDO_EOF] = bitIn.readBits(IHuffConstants.BITS_PER_INT);
            Map<Integer,Integer> freqMap = new HashMap<>();
            for (int i = 0; i < freqs.length; i++) {
                if (freqs[i] > 0) {
                    freqMap.put(i, freqs[i]);
                }
            }
            root = buildTreeFromCounts(freqMap);
        } else if (format == IHuffConstants.STORE_TREE) {
            int[] size = new int[1];
            size[0] = bitIn.readBits(BITS_PER_INT);
            TreeNode newNode = new TreeNode(-1,0);
            System.out.println(size[0]);
            root = readTree(bitIn, size, newNode);
        } else {
            throw new IOException("Unknown header format: " + format);
        }
        int bitsWritten = 0;
        TreeNode node = root;
        boolean done = false;
        while (!done) {
            int bit = bitIn.readBits(1);
            if (bit == -1) {
                throw new IOException("Unexpected end of input");
            }
            if (node.isLeaf()) {
                int value = node.getValue();
                if (value == IHuffConstants.PSEUDO_EOF) {
                    done = true;
                } else {
                    bitOut.writeBits(IHuffConstants.BITS_PER_WORD, value);
                    bitsWritten += IHuffConstants.BITS_PER_WORD;
                    node = root; 
                }
            }
            if (bit == 0) {
                node = node.getLeft();
            } else {
                node = node.getRight();
            }
        }
        bitIn.close();
        bitOut.close();
        return bitsWritten;
    }

    /** 
     * Reconstructs a tree from a STORE_TREE header. 
     * 0‑bit = internal, 1‑bit + (BITS_PER_WORD+1) bits = leaf value.
     */
    private TreeNode readTree(BitInputStream in, int[] size, TreeNode newNode) throws IOException {
        int bit = in.readBits(1);
        if (bit == -1) {
            throw new IOException("Invalid tree header");
        }
        if (size[0] == 0){
            return null;
        }
        size[0]--;
        if (bit == 1) {
            int value = in.readBits(IHuffConstants.BITS_PER_WORD + 1);
            newNode = new TreeNode(value, 0);
        } else {
            newNode.setLeft(readTree(in, size, new TreeNode(-1,0)));
            newNode.setRight(readTree(in, size, new TreeNode(-1,0)));
        }
        return newNode;
    }

    // viewer method
    public void setViewer(IHuffViewer viewer) {
        myViewer = viewer;
    }

    // show string viewer method
    private void showString(String s){
        if (myViewer != null) {
            myViewer.update(s);
        }
    }
}