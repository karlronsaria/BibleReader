package models;

// Following program is a Java implementation
// of Rabin Karp Algorithm given in the CLRS book
public class Algorithms
{
    public static Object searchRightLeaning(Comparable[] sortedSet, Comparable searchWord)
    {
        int mid;
        int bot = 0;
        int top = sortedSet.length - 1;
        
        while (top > bot)
        {
            mid = (top + bot) / 2;
            
            if (searchWord.compareTo(sortedSet[mid]) == 0)
            {
                return mid;
            }
            else if (searchWord.compareTo(sortedSet[mid]) < 0)
            {
                top = mid;
            }
            else
            {
                bot = mid + 1;
            }
        }
        
        return bot - 1;
    }
    
    public static Comparable searchLeftLeaning(Comparable[] sortedSet, Comparable searchWord)
    {
        int mid;
        int bot = 0;
        int top = sortedSet.length - 1;
        
        while (top > bot)
        {
            mid = (top + bot + 1) / 2;
            
            if (searchWord.compareTo(sortedSet[mid]) == 0)
            {
                return mid;
            }
            else if (searchWord.compareTo(sortedSet[mid]) < 0)
            {
                top = mid - 1;
            }
            else
            {
                bot = mid;
            }
        }
        
        return top + 1;
    }
    
    // D is the number of characters in input alphabet
    public final static int D = 256;
    
    /* pat -> pattern
        txt -> text
        q -> A prime number
    */
    public static Comparable[] search(String pat, String txt, boolean caseSensitive)
    {
        if (txt.length() == 0 || pat.length() == 0)
            return new Comparable[0];
        
        java.util.ArrayList<Comparable> what = new java.util.ArrayList<>();
        
        int M = pat.length();
        int N = txt.length();
        int i, j;
        int p = 0; // hash value for pattern
        int t = 0; // hash value for txt
        int h = 1;
        int q = 101;
        
        // The value of h would be "pow(d, M-1)%q"
        for (i = 0; i < M-1; i++)
            h = (h*D)%q;
        
        // Calculate the hash value of pattern and first
        // window of text
        for (i = 0; i < M; i++)
        {
            p = (D*p + (caseSensitive ? pat.charAt(i) : Character.toLowerCase(pat.charAt(i))))%q;
            t = (D*t + (caseSensitive ? txt.charAt(i) : Character.toLowerCase(txt.charAt(i))))%q;
        }
        
        // Slide the pattern over text one by one
        for (i = 0; i <= N - M; i++)
        {
            // Check the hash values of current window of text
            // and pattern. If the hash values match then only
            // check for characters on by one
            if (p == t)
            {
                /* Check for characters one by one */
                for (j = 0; j < M; j++)
                {
                    if (caseSensitive)
                    {
                        if (txt.charAt(i+j) != pat.charAt(j))
                            break;
                    }
                    else
                    {
                        if (Character.toLowerCase(txt.charAt(i+j)) != Character.toLowerCase(pat.charAt(j)))
                            break;
                    }
                }
                
                // if p == t and pat[0...M-1] = txt[i, i+1, ...i+M-1]
                if (j == M)
                    what.add(i);
            }
            
            // Calculate hash value for next window of text: Remove
            // leading digit, add trailing digit
            if (i < N-M)
            {
                if (caseSensitive)
                    t = (D*(t - txt.charAt(i)*h) + txt.charAt(i+M))%q;
                else
                    t = (D*(t - Character.toLowerCase(txt.charAt(i))*h) + Character.toLowerCase(txt.charAt(i+M)))%q;
                
                // We might get negative value of t, converting it
                // to positive
                if (t < 0)
                    t = (t + q);
            }
        }
        
        Comparable[] temp = new Comparable[what.size()];
        
        for (i = 0; i < temp.length; ++i)
            temp[i] = what.get(i);
        
        return temp;
    }
}
 
// This code is contributed by nuclode
