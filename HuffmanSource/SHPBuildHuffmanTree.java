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

import java.util.PriorityQueue;

public class SHPBuildHuffmanTree implements SimpleHuffProcessor {

    // Nested static class representing a Node in the Huffman tree
    public static class Node implements Comparable<Node> {
        int byteValue;
        int frequency;
        Node left;
        Node right;

        public Node(int byteValue, int frequency) {
            this.byteValue = byteValue;
            this.frequency = frequency;
            this.left = null;
            this.right = null;
        }

        @Override
        public int compareTo(Node other) {
            return Integer.compare(this.frequency, other.frequency);
        }
    }

    private PriorityQueue<Node> priorityQueue;

    public HuffmanTree() {
        this.priorityQueue = new PriorityQueue<>();
    }

    // Method to build the Huffman tree from byte values and their frequencies
    public void buildHuffmanTree(int[] byteValues, int[] frequencies) {
        for (int i = 0; i < byteValues.length; i++) {
            Node node = new Node(byteValues[i], frequencies[i]);
            priorityQueue.offer(node);
        }

        while (priorityQueue.size() > 1) {
            Node left = priorityQueue.poll();
            Node right = priorityQueue.poll();

            Node parent = new Node(-1, left.frequency + right.frequency);
            parent.left = left;
            parent.right = right;

            priorityQueue.offer(parent);
        }
    }
}