
//
// HuffViewer.java -- Java class HuffViewer
// Project A11Test_Huffman
//
// $Id$
//
// Created by jthywiss on Nov 28, 2012.
//
// Copyright (c) 2012 The University of Texas at Austin. All rights reserved.
//

/**
 * A HuffViewer that does nothing. Intended for testing when we don't want output.
 * <p>
 * Place this in the class path before the real <code>HuffViewer</code> to stub it out.
 */
public class DoNothingHuffViewer implements IHuffViewer {

    public DoNothingHuffViewer() {
        // Do nothing
    }

    public void clear() {
        //System.out.println("HuffViewer.clear()");
    }

    public void update(String s) {
        //System.out.println("HuffViewer.update(\""+s+"\")");
    }

    public void showMessage(String s) {
        //System.out.println("HuffViewer.showMessage(\""+s+"\")");
    }

    public void showError(String s) {
        System.out.println("HuffViewer.showError(\""+s+"\")");
        System.out.println();
    }

    @Override
    public void setModel(IHuffProcessor model) {
        // TODO Auto-generated method stub
        
    }

}
