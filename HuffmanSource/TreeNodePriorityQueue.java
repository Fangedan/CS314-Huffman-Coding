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

import java.util.ArrayList;

public class TreeNodePriorityQueue<E extends Comparable<? super E>> {
    private ArrayList<E> con;

    /**
     * Creates an empty PriorityQueue314 object.
     * pre: none
     */
    public TreeNodePriorityQueue() {
        con = new ArrayList<>();
    }

    /**
     * Adds data to its place in the queue sorted in ascending order.
     * pre: data != null
     * @param data the data to add to the queue.
     */
    public void enqueue(E data) {
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
        int index = findIndex(data);
        con.add(index, data);
    }

    /**
     * Helper for enqueue, finds the index that enqueue must add data to.
     * pre: data != null
     * @param data the data to add to the queue.
     * @return the index that data belongs in to maintain sorted ascending order.
     */
    private int findIndex(E data) {
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
        for (int i = 0; i < con.size(); i++) {
            if (con.get(i).compareTo(data) <= 0) {
                return i;
            }
        }
        return con.size();
    }

    /**
     * pre: none
     * @return the first element in the queue.
     */
    public E dequeue() {
        if (isEmpty()) {
            return null;
        }
        return con.remove(con.size() - 1);
    }

    /**
     * Accesses first element in the queue.
     * pre: none
     * @return the first element in the queue.
     */
    public E front() {
        if (isEmpty()) {
            return null;
        }
        return con.get(con.size() - 1);
    }

    /**
     * pre: none
     * @return whether the queue is empty or not.
     */
    public boolean isEmpty() {
        return con.size() == 0;
    }

    /**
     * pre: none
     * @return the size of the queue.
     */
    public int size() {
        return con.size();
    }

    /**
     * pre: none
     * @return a String representation of the queue, in ascending order surrounded by square 
     * brackets and separated by commas and spaces.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = con.size() - 1; i > 0; i--) {
            sb.append(con.get(i));
            sb.append(", ");
        }
        if (con.size() > 0) {
            sb.append(con.get(0));
        }
        sb.append("]");

        return sb.toString();
    }
}