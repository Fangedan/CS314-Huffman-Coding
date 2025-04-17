//
// A11Test_Huffman.java -- Java class A11Test_Huffman
// Project A11Test_Huffman
//
// $Id$
//
// Created by jthywiss on Nov 28, 2012.
//
// Copyright (c) 2012 The University of Texas at Austin. All rights reserved.
//

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

import edu.utexas.cs.bevotest.BevoTest;
import edu.utexas.cs.bevotest.PlaintextTestReporter;

/**
 * Test driver for C S 314 Assignment 11, "Huffman Coding and Compression"
 * <p>
 * Assumptions:
 * <ul>
 * <li>Class under test is named <code>SimpleHuffProcessor</code>.</li>
 * <li><code>SimpleHuffProcessor</code> implements <code>IHuffProcessor</code>.</li>
 * <li><code>SimpleHuffProcessor</code> can be called with no setup beyond a
 * call to <code>setViewer</code>.</li>
 * </ul>
 *
 * @author jthywiss
 */
public class A10_Huffman_Test_Student_Version {

    private static final String DIRECTORY_NAME      = "StudentTestFiles"; // change if you create a different directory
    private static final String SMALL_FILE_NAME     = "smallFile";
    private static final String TREE_TEST_FILE_NAME = "TreeTestFile.bmp";
    private static final File testInputDir          = new File(DIRECTORY_NAME);
    private static final File shortInputFile        = new File(DIRECTORY_NAME + "/" + SMALL_FILE_NAME);
    private static final long FILE_LENGTH_TOLERANCE = 1L; // long not an int, in BYTES
    private static final int BITS_PER_BYTE          = 8;

    public static void main(final String[] args) throws InterruptedException, IOException {
        huffmanTest();
    }

    @SuppressWarnings("boxing")
    private static void huffmanTest() throws InterruptedException, IOException {
        final BevoTest.Test ts = new BevoTest.Test("C S 314 Assignment 11 (Huffman) scoring test");

        final IHuffProcessor testItem = new SimpleHuffProcessor();
        testItem.setViewer(new DoNothingHuffViewer());

        final File[] testInputFiles = getFilesList();

        testSmallFileBitsSavedNegative(ts, testItem);

        testFileNotWrittenIfNoSavings(ts, testItem);

        // test all the non ".hf" files in the directory usign STANDARD COUNTS HEADER
        for (final File testInputFile : testInputFiles) {

            testPreprocessCompressReturnValue(ts, testItem, testInputFile, IHuffProcessor.STORE_COUNTS);

            testActualCompressMethod(ts, testItem, testInputFile);

            testUncompressMethodReturnValue(ts, testItem, testInputFile, IHuffProcessor.STORE_COUNTS);

            testActualCompressAndUncompressMethodsBycomparingFiles(ts, testItem, testInputFile);

        }

        // test the tree file
        runTreeFileTests(ts, testItem);


        final BevoTest.TestLog tl = new BevoTest.TestLog(ts);
        try {
            ts.run(tl);
        } finally {
            final Set<PlaintextTestReporter.ReportOption> ro = PlaintextTestReporter.ReportOption.setOf(PlaintextTestReporter.ReportOption.ONE_LINE_SHOW_STACK);
            new PlaintextTestReporter(tl).report(System.out, ro);
        }
    }

    private static void runTreeFileTests(final BevoTest.Test ts, final IHuffProcessor testItem) {

        final File treeTestFile = new File(DIRECTORY_NAME + "/" + TREE_TEST_FILE_NAME);

        testPreprocessCompressReturnValue(ts, testItem, treeTestFile, IHuffProcessor.STORE_TREE);

        testActualCompressMethod(ts, testItem, treeTestFile);

        testUncompressMethodReturnValue(ts, testItem, treeTestFile, IHuffProcessor.STORE_TREE);

        testActualCompressAndUncompressMethodsBycomparingFiles(ts, testItem, treeTestFile);

    }

    private static File[] getFilesList() {
        final File[] testInputFiles = testInputDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(final File dir, final String name) {
                return !name.endsWith(".hf") && !name.toLowerCase().contains("tree");
            }
        });
        return testInputFiles;
    }


    private static void testSmallFileBitsSavedNegative(final BevoTest.Test ts, final IHuffProcessor testItem) {
        new BevoTest.TestReturns<Object, Boolean>(ts, Object.class, "preprocessCompress return < 0 for a small file", true, 1000L) {
            @Override
            public void executeTest() throws IOException {
                starting(testItem);
                final int bitsSaved = testPreprocessReturnValueOnly(shortInputFile, testItem, IHuffConstants.STORE_COUNTS);
                returned(bitsSaved < 0);
            }
        };
    }


    private static void testFileNotWrittenIfNoSavings(final BevoTest.Test ts, final IHuffProcessor testItem) {
        new BevoTest.TestReturns<Object, Boolean>(ts, Object.class, "compress does not write if output would be larger (when forced=false)", true, 1000L) {
            @Override
            public void executeTest() throws IOException {
                starting(testItem);
                // Run:
                final File compressOut = testActualCompressResult(shortInputFile, testItem, false, IHuffProcessor.STORE_COUNTS);
                returned(!compressOut.exists() || compressOut.length() == 0);
                // Tear down:
                compressOut.delete();
            }
        };
    }

    // This test calls the student's preprocessCompress method and compares the value
    // returned by that method to the actual difference between the original file and
    // the expected output file. (the .hf file).
    // This test uses the header based on the headerType parameter.
    // A difference of 1 byte is allowed.
    private static void testPreprocessCompressReturnValue(final BevoTest.Test ts, final IHuffProcessor testItem, final File testInputFile, final int headerType) {
        new BevoTest.TestReturns<Object, Boolean>(ts, Object.class, "Check return value from preprocessCompress for " + testInputFile.getName(), true, 30000L) {
            @Override
            public void executeTest() throws IOException {
                // Set up:
                final File expectedOutFile = new File(testInputFile.getPath() + ".hf");
                starting(testItem);
                // Run:
                final long actualBytesSaved = testPreprocessReturnValueOnly(testInputFile, testItem, headerType) / BITS_PER_BYTE;
                final long expectedBytesSaved = testInputFile.length() - expectedOutFile.length();

                System.out.println("Comparing expected BYTES saved for " + testInputFile.getName() + " and value returned by preprocessCompress method.");
                System.out.println("actual: " + actualBytesSaved);
                System.out.println("expected: " + expectedBytesSaved);
                System.out.println();

                returned(Math.abs(actualBytesSaved - expectedBytesSaved) <= FILE_LENGTH_TOLERANCE);
                // Tear down: NONE
            }
        };
    }

    // This test uses the student solution to compress the provided file and
    // compares the file produced by the student's solution to the provided (expected) .hf file.
    // This test uses the header based on the headerType parameter.
    private static void testUncompressMethodReturnValue(final BevoTest.Test ts, final IHuffProcessor testItem, final File testInputFile, final int headerType) {

        new BevoTest.TestReturns<Object, Boolean>(ts, Object.class, "Compare actual compressed file for " + testInputFile.getName() + " and expected compressed file.", true, 30000L) {
            @Override
            public void executeTest() throws IOException {
                // Set up:
                final File expectedOutFile = new File(testInputFile.getPath() + ".hf");
                starting(testItem);
                // Run:
                final File compressOut = testActualCompressResult(testInputFile, testItem, true, headerType);
                returned(compareFiles(compressOut, expectedOutFile));
                // Tear down:
                System.out.println("COMPRESSED FILE NAME: " + compressOut.getPath());
                // compressOut.delete();
            }
        };
    }

    // This test uses the student solution to uncompress the provided .hf file and
    // compares the value returned by the student's uncompress method to see
    // if it is the same as the size of the original file.
    // A difference of 1 byte is allowed.
    private static void testActualCompressMethod(final BevoTest.Test ts, final IHuffProcessor testItem, final File testInputFile) {

        new BevoTest.TestReturns<Object, Boolean>(ts, Object.class, "int value returned by uncompress method for " + testInputFile.getName() + ".hf", true, 30000L) {
            @Override
            public void executeTest() throws IOException {
                // Set up:
                final File expectedOutFile = new File(testInputFile.getPath() + ".hf");
                starting(testItem);
                // Run:
                final int BITS_WRITTEN_VALUE_FROM_UNCOMPRESS = testUncompressIntReturnedByMethod(expectedOutFile, testItem);
                returned(compareFileLengthsToBitsWritten(testInputFile.length(), BITS_WRITTEN_VALUE_FROM_UNCOMPRESS, FILE_LENGTH_TOLERANCE));


                System.out.println("Comparing size of expected " + testInputFile.getName() + " and value returned by uncompress method.");
                System.out.println("return value from uncompress method: " + BITS_WRITTEN_VALUE_FROM_UNCOMPRESS);
                System.out.println("actual length of " + testInputFile.getName() + " : " + testInputFile.length() * BITS_PER_BYTE);
                System.out.println();
                // Tear down: NONE
            }
        };
    }


    // This test uses the student solution to uncompress the provided .hf file and
    //  compares the file produced by the student's solution to the original, expected file.
    private static void testActualCompressAndUncompressMethodsBycomparingFiles(final BevoTest.Test ts, final IHuffProcessor testItem, final File testInputFile) {


        new BevoTest.TestReturns<Object, Boolean>(ts, Object.class, "Compress & uncompress Actual File Written compared to expected" + testInputFile.getName(), true, 30000L) {
            @Override
            public void executeTest() throws IOException {
                // Set up:
                final File expectedOutFile = new File(testInputFile.getPath() + ".hf");
                starting(testItem);
                // Run:
                final File uncompressOut = testUncompressActualFile(expectedOutFile, testItem);
                returned(compareFiles(testInputFile, uncompressOut));
                // Tear down:
                uncompressOut.delete();
            }
        };
    }



    private static int testPreprocessReturnValueOnly(final File inFile, final IHuffProcessor huffProcessor, final int headerType) throws IOException {
        final InputStream in = new BufferedInputStream(new FileInputStream(inFile));
        final int bitsSaved = huffProcessor.preprocessCompress(in, headerType);
        in.close();
        return bitsSaved;
    }

    private static File testActualCompressResult(final File inFile, final IHuffProcessor huffProcessor, final boolean force, int headerType) throws IOException {
        InputStream in = new BufferedInputStream(new FileInputStream(inFile));
        /* final int bitsSaved = */ huffProcessor.preprocessCompress(in, headerType);
        in.close();
        in = new BufferedInputStream(new FileInputStream(inFile));
        final File outFile = File.createTempFile("A11Test-comp-act-" + inFile.getName() + "-", null);
        // outFile.deleteOnExit();
        final OutputStream out = new BufferedOutputStream(new FileOutputStream(outFile));
        final int bitsWritten = huffProcessor.compress(in, out, force);
        System.out.println("result for: " + inFile);
        System.out.println(outFile.length() + " " + (bitsWritten + 7)/ 8 + " bits written according to compress method: " + bitsWritten);
        in.close();
        out.close();
        // check BYTES written matches number of bits (converted to bits) written as reported by the compress method.
        assert outFile.length() == (bitsWritten + 7) / BITS_PER_BYTE : "compress: output file length doesn't correspond to compress method's \"bits written\" return value";
        return outFile;
    }

    private static int testUncompressIntReturnedByMethod(final File inFile, final IHuffProcessor huffProcessor) throws IOException {
        final InputStream in = new BufferedInputStream(new FileInputStream(inFile));
        final File outFile = File.createTempFile("A11Test-uncp-" + inFile.getName() + "-", null);
        outFile.deleteOnExit();
        final OutputStream out = new BufferedOutputStream(new FileOutputStream(outFile));
        final int bitsWritten = huffProcessor.uncompress(in, out);
        in.close();
        out.close();
        outFile.delete();
        return bitsWritten;
    }

    private static File testUncompressActualFile(final File inFile, final IHuffProcessor huffProcessor) throws IOException {
        final InputStream in = new BufferedInputStream(new FileInputStream(inFile));
        final File outFile = File.createTempFile("A11Test-uncp-" + inFile.getName() + "-", null);
        outFile.deleteOnExit();
        final OutputStream out = new BufferedOutputStream(new FileOutputStream(outFile));
        final int bitsWritten = huffProcessor.uncompress(in, out);
        in.close();
        out.close();
        // System.out.println(outFile.length() + " " + (bitsWritten + 7) / 8);
        assert outFile.length() == (bitsWritten + 7) / 8 : "uncompress: output file length doesn't correspond to uncompress method's \"bits written\" return value";
        return outFile;
    }

    private static boolean compareFiles(final File file1, final File file2) throws IOException {
        final int BUF_SIZE = 16384;

        final InputStream in1 = new BufferedInputStream(new FileInputStream(file1));
        final InputStream in2 = new BufferedInputStream(new FileInputStream(file2));
        final byte[] buf1 = new byte[BUF_SIZE];
        final byte[] buf2 = new byte[BUF_SIZE];

        try {
            // DataInputStream.readFully trick from:
            // http://stackoverflow.com/questions/4245863#answer-4245881
            final DataInputStream d2 = new DataInputStream(in2);
            for (int len; (len = in1.read(buf1)) > 0;) {
                d2.readFully(buf2, 0, len);
                for (int i = 0; i < len; i++) {
                    if (buf1[i] != buf2[i]) {
                        return false;
                    }
                }
            }
            return d2.read() < 0;
        } catch (final EOFException e) {
            return false;
        } finally {
            in1.close();
            in2.close();
        }
    }

    private static boolean lengthsWithin(final File f1, final File f2, final long tolerence) {
        return Math.abs(f1.length() - f2.length()) <= tolerence;
    }

    private static boolean compareFileLengthsToBitsWritten(final long EXPECTED_LENGTH_IN_BYTES,
            final long BITS_WRITTEN_VALUE_FROM_UNCOMPRESS, final long TOLERANCE) {
        return Math.abs(EXPECTED_LENGTH_IN_BYTES * BITS_PER_BYTE - BITS_WRITTEN_VALUE_FROM_UNCOMPRESS) / BITS_PER_BYTE <= TOLERANCE;
    }
}
