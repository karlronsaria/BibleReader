package models;


import java.math.BigInteger;
import java.util.Random;

/**
 *  The {@code RabinKarp} class finds the first occurrence of a pattern string
 *  in a text string.
 *  <p>
 *  This implementation uses the Rabin-Karp algorithm.
 *  <p>
 *  For additional documentation,
 *  see <a href="http://algs4.cs.princeton.edu/53substring">Section 5.3</a> of
 *  <i>Algorithms, 4th Edition</i> by Robert Sedgewick and Kevin Wayne.
 */
public class StringSearch {
    private String pat;      // the pattern  // needed only for Las Vegas
    private long patHash;    // pattern hash value
    private int m;           // pattern length
    private long q;          // a large prime, small enough to avoid long overflow
    private int R;           // radix
    private long RM;         // R^(M-1) % Q
    private boolean caseSensitive;

    /**
     * Preprocesses the pattern string.
     *
     * @param pattern the pattern string
     * @param R the alphabet size
     */
    public StringSearch(char[] pattern, int R) {
        this.pat = String.valueOf(pattern);
        this.R = R;        
        throw new UnsupportedOperationException("Operation not supported yet");
    }

    /**
     * Preprocesses the pattern string.
     *
     * @param pat the pattern string
     */
    public StringSearch(String pat, boolean caseSensitive) {
        this.pat = caseSensitive ? pat : pat.toLowerCase();      // save pattern (needed only for Las Vegas)
        R = 256;
        m = pat.length();
        q = longRandomPrime();

        // precompute R^(m-1) % q for use in removing leading digit
        RM = 1;
        for (int i = 1; i <= m-1; i++)
            RM = (R * RM) % q;
        patHash = hash(this.pat, m);
    }

    // Compute hash for key[0..m-1]. 
    private long hash(String key, int m) { 
        long h = 0; 
        for (int j = 0; j < m; j++) 
            h = (R * h + key.charAt(j)) % q;
        return h;
    }

    // Compute hash for key[0..m-1]. 
    private long hash(String key, int start, int m) { 
        long h = 0;
        for (int j = start; j < m; j++) 
            h = (R * h + key.charAt(j)) % q;
        return h;
    }

    // Las Vegas version: does pat[] match txt[i..i-m+1] ?
    private boolean check(String txt, int i) {
        for (int j = 0; j < m; j++) 
            if (pat.charAt(j) != txt.charAt(i + j)) 
                return false; 
        return true;
    }

    // Monte Carlo version: always return true
    // private boolean check(int i) {
    //    return true;
    //}
 
    /**
     * Returns the index of the first occurrence of the pattern string
     * in the text string.
     *
     * @param  txt the text string
     * @return the index of the first occurrence of the pattern string
     *         in the text string; n if no such match
     */
    public int search(String txt) {
        int n = txt.length();
        if (n < m) return n;
        long txtHash = hash(txt, m); 

        // check for match at offset 0
        if ((patHash == txtHash) && check(txt, 0))
            return 0;

        // check for hash match; if hash match, check for exact match
        for (int i = m; i < n; i++) {
            // Remove leading digit, add trailing digit, check for match. 
            txtHash = (txtHash + q - RM*txt.charAt(i-m) % q) % q; 
            txtHash = (txtHash*R + txt.charAt(i)) % q; 

            // match
            int offset = i - m + 1;
            if ((patHash == txtHash) && check(txt, offset))
                return offset;
        }

        // no match
        return n;
    }
    
//    public int search(String txt, int start) {
//        
//        // The length of the text
//        int n = txt.length(); 
//        
//        // if the length of the string excedes the amount of characters left
//        // to check for: length of the text minus the starting position
//        if (n - start < m) return n;
//        long txtHash = hash(txt, m); 
//
//        // check for match at offset 0
//        if ((patHash == txtHash) && check(txt, 0))
//            return 0;
//
//        // check for hash match; if hash match, check for exact match
//        for (int i = m; i < n; i++) {
//            // Remove leading digit, add trailing digit, check for match. 
//            txtHash = (txtHash + q - RM*txt.charAt(i-m) % q) % q; 
//            txtHash = (txtHash*R + txt.charAt(i)) % q; 
//
//            // match
//            int offset = i - m + 1;
//            if ((patHash == txtHash) && check(txt, offset))
//                return offset;
//        }
//
//        // no match
//        return n;
//    }
    
    public int search(String txt, int start) {
        int n = txt.length();
        if (n - start < m) return n;
        long txtHash = hash(txt, start, m);

        // check for match at offset start
        if ((patHash == txtHash) && check(txt, start))
            return start;

        // check for hash match; if hash match, check for exact match
        for (int i = start + m; i < n; i++) {
            // Remove leading digit, add trailing digit, check for match. 
            txtHash = (txtHash + q - RM*txt.charAt(i-m) % q) % q; 
            txtHash = (txtHash*R + txt.charAt(i)) % q;
            
            // match
            int offset = i - m + 1;
            if ((patHash == txtHash) && check(txt, offset))
                return offset;
        }

        // no match
        return n;
    }
    
    public Object[] instances(String txt)
    {
        if (!caseSensitive)
            txt = txt.toLowerCase();
        
        java.util.ArrayList indices = new java.util.ArrayList();
        
        int index = search(txt);
        int prev = 0;
        
        while (index < txt.length())
        {
            indices.add(prev + index);
            txt = txt.substring(index + m);
            
            prev += index + m;
            index = search(txt);
        }
        
        return indices.toArray();
    }
    
//    public Object[] instances(String txt)
//    {
//        java.util.ArrayList indices = new java.util.ArrayList();
//        
//        int index = search(txt, 0);
//        
//        while (index < txt.length())
//        {
//            indices.add(index);
//            index = search(txt, index + m);
//        }
//        
//        return indices.toArray();
//    }


    // a random 31-bit prime
    private static long longRandomPrime() {
        BigInteger prime = BigInteger.probablePrime(31, new Random());
        return prime.longValue();
    }

    /** 
     * Takes a pattern string and an input string as command-line arguments;
     * searches for the pattern string in the text string; and prints
     * the first occurrence of the pattern string in the text string.
     *
     * @param args the command-line arguments
     */
//    public static void main(String[] args) {
//        
//        args = new String[2];
//        
//        args[0] = " a";
//        args[1] = "It's all I have to bring today, this and my heart beside, this and my heart and all the fields, and all the meadows wide. Be sure you count, should I forget, someone the sum could tell, this and my heart and all the bees, which in the clover dwell.";
//        
//        String pat = args[0];
//        String txt = args[1];
//
//        StringSearch searcher = new StringSearch(pat);
//        // int offset = searcher.search(txt);
//
//        // print results
//        System.out.println("text:    " + txt);
//
//        // from brute force search method 1
//        for (Object what : searcher.instances(txt))
//        {
//            System.out.print("pattern: ");
//            for (int i = 0; i < (int) what; i++)
//                System.out.print(" ");
//            System.out.println(pat);
//        }
//    }
}