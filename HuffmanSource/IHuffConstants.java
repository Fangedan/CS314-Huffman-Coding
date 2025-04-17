

/**
 * Global constants used in Huff/Unhuff. Clients needing these
 * values should implement this interface or access the
 * values directly, e.g., as <code>IHuffConstants.BITS_PER_WORD</code>. However,
 * implementing the interface is preferred in which case
 * the values can be accessed simply as <code>BITS_PER_WORD</code>, for example.
 * <P>
 * @author Owen Astrachan, minor changes Mike Scott
 * @date November 2009
 */
public interface IHuffConstants {
    /**
     * The standard number of bits per chunk/word when huffing.
     */
    public static final int BITS_PER_WORD = 8;
    
    /**
     * The size of the alphabet given the number of bits per chunk, this
     * should be 2^BITS_PER_WORD.
     */
    public static final int ALPH_SIZE = (1 << BITS_PER_WORD);
    
    /**
     * The standard number of bits needed to represent/store
     * an int, this is 32 in Java and nearly all other languages.
     */
    public static final int BITS_PER_INT = 32;
    
    /**
     * The value of the PSEUDO_EOF character. This is one-more
     * than the maximum value of a legal BITS_PER_WORD-bit character.
     */
    
    public static final int PSEUDO_EOF = ALPH_SIZE;
    
    /**
     * Isolate the magic numbers in one place. Files compressed with
     * a HuffProcessor must start with this value.
     */
    public static final int MAGIC_NUMBER = 0xface8200;
    
    /**
     * A value in files compressed with a HuffProcessor indicating
     * the code values are stored in Standard Count Format.
     * <tt>ALPHA_SIZE</tt> ints will follow this constant with the count for each value.
     */
    public static final int STORE_COUNTS = MAGIC_NUMBER | 1;
    
    /**
     * A value in files compressed with a HuffProcessor indicating
     * the code values are stored in Standard Tree Format.
     */   
    public static final int STORE_TREE = MAGIC_NUMBER | 2;
    
    /**
     * A value in files compressed with a HuffProcessor indicating
     * the code values are stored in a custom format. Something
     * besides Standard Count Format or Standard Tree Format.
     */      
    public static final int STORE_CUSTOM = MAGIC_NUMBER | 4;
}
