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

import java.util.*;

public class TreeNodePriorityQueue {
    private static class NodeWrapper {
        TreeNode node;
        int order;

        NodeWrapper(TreeNode node, int order) {
            this.node = node;
            this.order = order;
        }
    }

    private List<NodeWrapper> heap;
    private int insertionCounter;

    public TreeNodePriorityQueue() {
        heap = new ArrayList<>();
        insertionCounter = 0;
    }

    public void offer(TreeNode node) {
        NodeWrapper wrapped = new NodeWrapper(node, insertionCounter++);
        heap.add(wrapped);
        siftUp(heap.size() - 1);
    }

    public TreeNode poll() {
        if (heap.isEmpty()) return null;
        NodeWrapper result = heap.get(0);
        NodeWrapper last = heap.remove(heap.size() - 1);
        if (!heap.isEmpty()) {
            heap.set(0, last);
            siftDown(0);
        }
        return result.node;
    }

    public int size() {
        return heap.size();
    }

    private void siftUp(int idx) {
        while (idx > 0) {
            int parent = (idx - 1) / 2;
            if (compare(heap.get(idx), heap.get(parent)) < 0) {
                swap(idx, parent);
                idx = parent;
            } else break;
        }
    }

    private void siftDown(int idx) {
        int size = heap.size();
        while (true) {
            int left = 2 * idx + 1;
            int right = 2 * idx + 2;
            int smallest = idx;

            if (left < size && compare(heap.get(left), heap.get(smallest)) < 0) {
                smallest = left;
            }
            if (right < size && compare(heap.get(right), heap.get(smallest)) < 0) {
                smallest = right;
            }

            if (smallest != idx) {
                swap(idx, smallest);
                idx = smallest;
            } else break;
        }
    }

    private int compare(NodeWrapper a, NodeWrapper b) {
        int freqDiff = a.node.getFrequency() - b.node.getFrequency();
        if (freqDiff != 0) return freqDiff;
        return a.order - b.order; // fair tiebreaking
    }

    private void swap(int i, int j) {
        NodeWrapper temp = heap.get(i);
        heap.set(i, heap.get(j));
        heap.set(j, temp);
    }
}