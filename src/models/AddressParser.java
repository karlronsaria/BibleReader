package models;

// name           : \w+(\s+\w+){0,2}\.?
// installment    : (i{1,3}|\d)
// required space : \s+
// optional space : \s*
// chapter        : \d{1,3}
// verse          : \d{1,3}
// other verses   : (\s*(-|,)\s*\d{1,3})*
// title          : installment?(os)(name)
// other chapter  : (os)((title)(rs))?(chapter)
// address        : (rs)(chapter)(os)((:(os)(verse)((other verses)|-(other chapter):(os)(verse)))|(-(other chapter)))?

// (title)(address)?(;(title)?(address))*

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Andrew
 */
public class AddressParser
{
    static final HashMap<Character, Integer> SYNTAX_LEVELS = getSyntaxLevels();
    static final HashMap<Character, BiFunction<Object, Object, Object>> OPERATORS = getOperators();
    static final HashMap<String, Integer> INSTALLMENTS = Names.getInstallments();
    static final TreeSet<String> BOOK_TITLES = Names.getBookNames();
    static final int MAX_WORDS_IN_TITLE = 4;
    static final int DELIMITER_LVL = 1;
    static final int COLON_LVL = 3;
    static final int PAUSE_LVL = 4;
    static final int RANGE_OPERATOR_LVL = 5;
    static final int NEUTRAL_LVL = 6;
    static final int LEAF_LVL = 7;
    
    static final String NAME_PATTERN = "[A-Z,a-z]+((\\s+[A-Z,a-z]+){1," + (MAX_WORDS_IN_TITLE - 1) + "}|\\.?)";
    static final String INSTALLMENT_PATTERN = "(i{1,3}\\s+|\\d(st|nd|rd)?|First|Second)";
    static final String FULL_PATTERN = "(?i)^\\s*(?<installment>" + INSTALLMENT_PATTERN + ")?\\s*(?<title>" + NAME_PATTERN + ")";
    
    String input;
    int cursor;
    boolean endOfString;
    
    public static HashMap<Character, BiFunction<Object, Object, Object>> getOperators()
    {
        HashMap<Character, BiFunction<Object, Object, Object>> operators = new HashMap<>();
        
        operators.put('-', (BiFunction) (Object t, Object u) -> // Range.getNewRange((int) t, (int) u));
        {
            if (t == null || u == null)
            {
                return null;
            }
            
            if (t instanceof Address && u instanceof Address)
            {
                Range leftRange = Range.getNewPair(((Address) t).getChapter(), ((Address) t).peekRanges().getStart());
                Range rghtRange = Range.getNewPair(((Address) u).getChapter(), ((Address) u).peekRanges().getStart());
                
                return new Address("", 0, produceRangeSet(leftRange, new SpecialRangeComparator()).getAugmentedRangeSet(rghtRange));
            }
            
            return Range.getNewRange((int) t, (int) u);
        });
        operators.put(',', (BiFunction) (Object t, Object u) -> produceRangeSet(t).getAugmentedRangeSet(produceRange(u)));
        operators.put(':', (BiFunction) (Object t, Object u) -> t instanceof Integer ? new Address("", (int) t, produceRangeSet(u)) : null); // new Address("", 0, new RangeSet())
        operators.put(';', (BiFunction) (Object t, Object u) -> produceAddressList(t).getAugmentedAddressList(produceAddress(u)));
        
        return operators;
    }
    
    public static AddressList produceAddressList(Object u)
    {
        if (u == null)
        {
            return null;
        }
        
        if (u instanceof AddressList)
        {
            return (AddressList) u;
        }
        
        if (u instanceof Range)
        {
            return new AddressList((Range) u);
        }
        
        if (u instanceof Integer)
        {
            return new AddressList().getAugmentedAddressList(new Address("", (int) u));
        }
        
        return new AddressList().getAugmentedAddressList((Address) u);
    }
    
    public static Address produceAddress(Object u)
    {
        if (u == null)
        {
            return null;
        }
        
        if (u instanceof Address)
        {
            return (Address) u;
        }
        
        if (u instanceof Integer)
        {
            return new Address("", (Integer) u);
        }
        
        return new Address("", 0, produceRangeSet(u));
    }
    
    public static RangeSet produceRangeSet(Object u)
    {
        return produceRangeSet(u, new GeneralRangeComparator());
    }
    
    public static RangeSet produceRangeSet(Object u, java.util.Comparator<Range> c)
    {
        if (u == null)
        {
            return null;
        }
        
        if (u instanceof RangeSet)
        {
            return (RangeSet) u;
        }
        
        return new RangeSet(c).getAugmentedRangeSet(produceRange(u));
    }
    
    public static Range produceRange(Object u)
    {
        return u == null ? null : (u instanceof Range ? (Range) u : Range.getNewRange((int) u));
    }
    
    public static HashMap<Character, Integer> getSyntaxLevels()
    {
        HashMap<Character, Integer> syntaxLevels = new HashMap<>();
        
        syntaxLevels.put(';', DELIMITER_LVL);
        syntaxLevels.put(':', COLON_LVL);
        syntaxLevels.put('.', COLON_LVL);
        syntaxLevels.put(',', PAUSE_LVL);
        syntaxLevels.put('-', RANGE_OPERATOR_LVL);
        
        return syntaxLevels;
    }
    
    public static class Range implements Comparable
    {
        int start;
        int end;
        
        private Range(int startingVerse, int endingVerse)
        {
            start = startingVerse;
            end = endingVerse;
        }
        
        private Range(int verse)
        {
            start = verse;
            end = verse;
        }
        
        public static Range getNewPair(int l, int r)
        {
            return new Range(l, r);
        }
        
        public static Range getNewRange(int s, int e)
        {
            if (s <= e)
            {
                return new Range(s, e);
            }
            
            return null;
        }
        
        public static Range getNewRange(int v)
        {
            return new Range(v);
        }

        public int getStart()
        {
            return start;
        }

        public int getEnd()
        {
            return end;
        }

        @Override
        public int compareTo(Object o)
        {
            return start > ((Range) o).start ? 1 : (start < ((Range) o).start ? -1 : 0);
        }
    }
    
    public static class GeneralRangeComparator implements java.util.Comparator<Range>
    {
        @Override
        public int compare(Range o1, Range o2)
        {
            return o1.start > o2.start ? 1
                                       : o1.start < o2.start ? -1
                                                             :  0;
        }
    }
    
    public static class SpecialRangeComparator implements java.util.Comparator<Range>
    {
        @Override
        public int compare(Range o1, Range o2)
        {
            return o1.start > o2.start ? 1
                                       : o1.start < o2.start ? -1
                                                             : o1.end > o2.end ? 1
                                                                               : o1.end < o2.end ? -1
                                                                                                 :  0;
        }
    }
    
    public static class RangeSet extends TreeSet<Range>
    {
        public RangeSet()
        {
            super(new GeneralRangeComparator());
        }
        
        public RangeSet(java.util.Comparator<Range> c)
        {
            super(c);
        }
        
        public RangeSet getAugmentedRangeSet(Range item)
        {
            add(item);
            return this;
        }
    }
    
    public static class AddressList extends ArrayList<Address>
    {
        public AddressList getAugmentedAddressList(Address item)
        {
            add(item);
            return this;
        }
        
        public AddressList()
        {
            super();
        }
        
        public AddressList(Range r)
        {
            for (int i = r.getStart(); i <= r.getEnd(); ++i)
            {
                add(new Address("", i));
            }
        }
        
        public boolean representsWholeChapter()
        {
            return size() == 1 && get(0).representsWholeChapter();
        }
        
        public boolean representsChapterRange()
        {
            return size() == 1 && get(0).representsChapterRange();
        }
    }
    
    public static class Address
    {
        String title;
        int chapter;
        RangeSet ranges;
        
        public Address(String searchTitle, int searchChapter, RangeSet verses)
        {
            title = searchTitle;
            chapter = searchChapter;
            ranges = verses;
        }
        
        public Address(String searchTitle, int searchChapter)
        {
            title = searchTitle;
            chapter = searchChapter;
            ranges = new RangeSet();
        }
        
        public String getTitle()
        {
            return title;
        }
        
        public int getChapter()
        {
            return chapter;
        }
        
        public void setTitle(String searchTitle)
        {
            title = searchTitle;
        }
        
        public boolean representsWholeChapter()
        {
            return !hasRanges();
        }
        
        public boolean representsChapterRange()
        {
            return chapter == 0 && ranges.size() == 1;
        }
        
        public boolean representsCrossChapterRange()
        {
            return chapter == 0 && ranges.size() == 2;
        }
        
        public void addRange(Range verses)
        {
            ranges.add(verses);
        }
        
        public void addRange(int rangeStart, int rangeEnd)
        {
            ranges.add(Range.getNewRange(rangeStart, rangeEnd));
        }
        
        public void addVerse(int verse)
        {
            ranges.add(Range.getNewRange(verse, verse));
        }
        
        public boolean hasRanges()
        {
            return !ranges.isEmpty();
        }
        
        public Range peekRanges()
        {
            return ranges.first();
        }
        
        public Range popRanges()
        {
            return ranges.pollFirst();
        }
    }
    
    public class Syntax
    {
        private Node root;
        
        class Node
        {
            Object token;
            int priority;
            Node left;
            Node rght;
            
            Node(Object newToken, int newPriority, Node newLeft, Node newRght)
            {
                token = newToken;
                priority = newPriority;
                left = newLeft;
                rght = newRght;
            }
            
            Node(Object newToken, int newPriority)
            {
                token = newToken;
                priority = newPriority;
                left = null;
                rght = null;
            }
        }
        
        public Syntax()
        {
            root = new Node(0, 0, null, new Node(null, LEAF_LVL, null, null));
        }
        
        public String getInOrderTraversal()
        {
            return getInOrderTraversal(root.rght);
        }
        
        private String getInOrderTraversal(Node n)
        {
            String temp = "";

            if (n != null && n.priority < LEAF_LVL)
            {
                temp += getInOrderTraversal(n.left);
                temp += n.token;
                temp += getInOrderTraversal(n.rght);
            }
            
            return temp;
        }
        
        public boolean add(Object newToken)
        {
            int priority = newToken instanceof Integer ? NEUTRAL_LVL : SYNTAX_LEVELS.getOrDefault((Character) newToken, -1);
               
            if (priority == -1)
            {
                return false;
            }
            
            Node next = new Node(newToken, priority, null, new Node(0, LEAF_LVL, null, null));
            
            if (root.rght == null)
            {
                root.rght = next;
                return true;
            }
            
            Node cursor = root;
            
            while (next.priority > cursor.rght.priority)
            {
                cursor = cursor.rght;
            }
            
            if (next.priority == cursor.rght.priority)
            {
                Node temp = cursor.rght.rght;
                
                if (temp == null)
                {
                    return false;
                }
                
                if (next.priority == COLON_LVL)
                {
                    if (temp.priority != RANGE_OPERATOR_LVL)
                    {
                        return false;
                    }
                    
                    cursor.rght.rght = temp.left;
                    next.left = temp.rght;
                    temp.left = cursor.rght;
                    temp.rght = next;
                    cursor.rght = temp;
                    
                    return true;
                }
                
                if (next.priority > PAUSE_LVL)
                {
                    return false;
                }
            }
            
            next.left = cursor.rght;
            cursor.rght = next;
            
            return true;
        }
        
        public AddressList getAddressList()
        {
            if (root.rght == null)
            {
                return new AddressList();
            }
            
            return produceAddressList(interpret(root.rght));
        }
        
        public Address getAddress()
        {
            if (root.rght == null)
            {
                return new Address("", 1, new RangeSet().getAugmentedRangeSet(Range.getNewRange(1)));
            }
            
            return produceAddress(interpret(root.rght));
        }
        
        private Object interpret(Node parent)
        {
            if (parent.priority <= RANGE_OPERATOR_LVL)
            {
                return OPERATORS.get((Character) parent.token).apply(interpret(parent.left), interpret(parent.rght));
            }
            
            return parent.token;
        }
    }
    
    public AddressParser(String newAddress)
    {
        input = newAddress;
        cursor = 0;
        endOfString = false;
    }
    
    public Syntax getSyntax()
    {
        Syntax tree = new Syntax();
        resetCursor();
        
        while (!endOfString)
        {
            if (!tree.add(parseToken()))
            {
                resetCursor();
                return tree;
            }
        }
        
        resetCursor();
        return tree;
    }
    
    // [!] Do not use.
    public AddressList compileExclusivelyVerseSyntaxToAddressList()
    {
        Syntax tree = new Syntax();
        
        resetCursor();
        
        while (!endOfString)
        {
            if (!tree.add(parseToken()))
            {
                resetCursor();
                return new AddressList();
            }
        }
        
        resetCursor();
        return tree.getAddressList();
    }
    
    public AddressList compileToAddressList()
    {
        if (input == null || input.length() == 0)
            return new AddressList();
        
        String temp = input;
        String[] split = input.split(";");
        
        Matcher m;
        
        int installment;
        String title = "";
        
        AddressList list = new AddressList();
        Address address;
        
        for (String addressStr : split)
        {
            resetCursor();
            m = Pattern.compile(FULL_PATTERN).matcher(addressStr);
            
            if (m.find())
            {
                String installmentStr = m.group("installment");
                installment = installmentStr == null ? 0 : getInstallmentFromString(installmentStr);
                title = (installment > 0 ? String.format("%d", installment) : "") + m.group("title").toLowerCase();
                
                if (!BOOK_TITLES.contains(title))
                    return new AddressList();
                
                moveCursorTo(m.end());
            }
            
            if (endOfString && split.length == 1)
                return new AddressList().getAugmentedAddressList(new Address(title, 1));
            
            input = addressStr;
            address = compileToAddress();
            resetCursor();
            
            if (address == null)
            {
                input = temp;
                return new AddressList();
            }
            
            address.setTitle(title);
            list.add(address);
        }
        
        input = temp;
        return list;
    }
    
    public Address compileToAddress()
    {
        Syntax tree = new Syntax();
        
        while (ignoreWhitespace())
        {
            if (!tree.add(parseToken()))
            {
                return null;
            }
        }
        
        resetCursor();
        return tree.getAddress();
    }
    
    public Address compileToAddress(String str, int pos)
    {
        input = str;
        moveCursorTo(pos);
        
        return compileToAddress();
    }
    
    public static boolean checkBookTitle(int installment, String title)
    {
        return BOOK_TITLES.contains((String) (installment == 0 ? String.format("%s", title) : String.format("%d%s", installment, title)).toLowerCase());
    }
    
    public Object parseToken()
    {
        StringBuilder word = new StringBuilder("");
        char temp = input.charAt(cursor);
        
        while (Character.isDigit(temp))
        {
            word.append(temp);
            
            if (!moveCursor()) break;
            temp = input.charAt(cursor);
        }
        
        if (word.length() > 0)
        {
            return Integer.parseInt(word.toString());
        }
        
        moveCursor();
        
        return temp;
    }
    
    // Used primarily for testing.
    public static void consume(AddressList addresses)
    {
        for (Address a : addresses)
        {
            System.out.println("Title: " + a.getTitle());
            System.out.println("  Chapter: " + a.getChapter());
            System.out.println();
            
            while (a.hasRanges())
            {
                Range r = a.popRanges();
                
                System.out.println("    " + r.getStart() + " - " + r.getEnd());
            }
            
            System.out.println();
        }
    }
    
    private boolean moveCursor()
    {
        endOfString = ++cursor >= input.length();
        return !endOfString;
    }
    
    private boolean moveCursorTo(int pos)
    {
        cursor = pos;
        endOfString = cursor >= input.length();
        return !endOfString;
    }
    
    private boolean ignoreWhitespace()
    {
        while (!endOfString && Character.isWhitespace(input.charAt(cursor)))
        {
            moveCursor();
        }
        
        return !endOfString;
    }
    
    private void resetCursor()
    {
        cursor = 0;
        endOfString = false;
    }
    
    int getInstallmentFromRomanNumeral()
    {
        int installment = 0;
        
        while (Character.toLowerCase(input.charAt(cursor)) == 'i')
        {
            installment = installment + 1;
            if (!moveCursor()) break;
        }
        
        return installment;
    }
    
    int getInstallmentFromDigit()
    {
        int temp = cursor;
        moveCursor();
        return Integer.parseInt(input.charAt(temp) + "");
    }
    
    static int getInstallmentFromRomanNumeral(String str)
    {
        int installment = 0;
        
        int i = 0;
        
        while (Character.toLowerCase(str.charAt(i)) == 'i')
        {
            installment = installment + 1;
            
            i = i + 1;
        }
        
        return installment;
    }
    
    static int getInstallmentFromDigit(String str)
    {
        return Integer.parseInt(str);
    }
    
    static int getInstallmentFromString(String str)
    {
//        if (Character.isAlphabetic(str.charAt(0)))
//        {
//            return getInstallmentFromRomanNumeral(str);
//        }
//        
//        return getInstallmentFromDigit(str);
        
        return INSTALLMENTS.getOrDefault(str.toLowerCase().trim(), 0);
    }
    
    void discardSpace()
    {
        while (Character.isWhitespace(input.charAt(cursor)))
        {
            if (!moveCursor()) break;
        }
    }
    
    String getWord()
    {
        StringBuilder wordBuilder = new StringBuilder();
        
        char tempChar = input.charAt(cursor);
        
        while (Character.isAlphabetic(tempChar))
        {
            wordBuilder.append(tempChar);
            
            if (!moveCursor()) break;
            tempChar = input.charAt(cursor);
        }
        
        return wordBuilder.toString();
    }
    
    String getTitle()
    {
        String phrase = getWord();
        
        if (BOOK_TITLES.contains(phrase))
            return phrase;
        
        for (int words = 1; words <= 2; ++words)
        {
            if (endOfString)
                return "";
            
            if (Character.isWhitespace(input.charAt(cursor)))
            {
                phrase += " ";
                
                if (!moveCursor())
                    return "";
            }
            
            discardSpace();
            
            if (endOfString)
                return "";
            
            phrase += getWord();
            
            if (BOOK_TITLES.contains(phrase))
                return phrase;
        }
        
        return "";
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        
        
//// name           : \w+(\s+\w+){0,2}\.?
//// installment    : (i{1,3}|\d)
//// required space : \s+
//// optional space : \s*
//// chapter        : \d{1,3}
//// verse          : \d{1,3}
//// other verses   : (\s*(-|,)\s*\d{1,3})*
//// title          : installment?(os)(name)
//// other chapter  : (os)((title)(rs))?(chapter)
//// address        : (rs)(chapter)(os)((:(os)(verse)((other verses)|-(other chapter):(os)(verse)))|(-(other chapter)))?
//
//// (title)(address)?(;(title)?(address))*
//
////        String nameP = "\\w+((\\s+\\w+){1,2}|\\.?)";
////        String installmentP = "(i{1,3}|\\d)";
////        String numberP = "\\d{1,3}";
////        String verses = "\\s*(-|,)\\s*(?<verse>\\d)";
////        
////        String addressesStr = "Ruth;Job;Song of Songs;I Pe.; iiJo.";
////        
////        String[] addresses = addressesStr.split(";");
////        
////        String what = "(?i)^\\s*(?<installment>" + installmentP + ")?\\s*(?<title>" + nameP + ")";
////        
////        for (String address : addresses)
////        {
////            Matcher m = Pattern.compileExclusivelyVerseSyntax(what).matcher(address);
////            
////            while (m.find())
////            {
////                String installment = m.group("installment");
////                
////                if (installment != null)
////                {
////                    System.out.print(installment + " ");
////                }
////                
////                System.out.println(m.group("title"));
////            }
////        }
//
//
//
//
////        String str = "First Samuel 1:14-15;2-3;1:3,4,17-19,21,22-25;5:20-6:1";
////
////        AddressParser what = new AddressParser(str);
////        
////        AddressParser.consume(what.compileToAddressList());
//
//
//        StringBuilder s = new StringBuilder();
//        
//        try (FileReader what = new java.io.FileReader("C:\\Users\\Andrew\\Documents\\NetBeansProjects\\PatternMatch\\src\\files\\t_asv.json"))
////        try (FileReader what = new java.io.FileReader("/files/titles.txt"))
////        try (FileReader what = new FileReader(new File(new Names().getClass().getResource("C:\\Users\\Andrew\\Documents\\NetBeansProjects\\PatternMatch\\src\\files\\titles.txt").toString())))
//        {
//            int i;
//            
//            while (what.ready())
//            {
//                i = what.read();
//                s.append((char) i);
//            }
//            
//            what.close();
//        }
//        catch (FileNotFoundException ex)
//        {
//            Logger.getLogger(Names.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        catch (IOException ex)
//        {
//            Logger.getLogger(Names.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        
//        System.out.println(s.length());
//        
////        AddressParser.Syntax tree = what.getSyntax();
////        System.out.println(str);
////        System.out.println(tree.getInOrderTraversal());
////        
////        AddressParser.consume(what.compileExclusivelyVerseSyntax());
//
//
//
//
////        String temp = "(?<areaCode>\\d{3}) ";
////        
////        Pattern p = Pattern.compileExclusivelyVerseSyntax(temp);
////        
////        Matcher m = p.matcher("333 444 565 ");
////        
////        while (m.find())
////        {
////            System.out.println(m.group("areaCode"));
////        }
//        
////        System.out.println(Pattern.compileExclusivelyVerseSyntax("^\\d<what>\\d$").matcher("55").group("what"));
//        
////        String address = "II Dickenson 1:2-3:2";
////        
////        AddressParser parser = new AddressParser(address);
//        
//        // III John 2:2 - 3:12
//        // III John
//        //              -
//        //           :     :
//        //          2 2   3 12
//        
////        System.out.println(new AddressParser("III Dickenson").getInstallmentFromRomanNumeral());
////        System.out.println(new AddressParser("3 Dickenson").getInstallmentFromDigit());
////        System.out.println(new AddressParser("3Dickenson").getInstallmentFromDigit());
////        System.out.println(new AddressParser("Leaves of Grass").getTitle());
////        System.out.println(new AddressParser("Leaves of ").getTitle());
////        System.out.println(new AddressParser("Leaves of").getTitle());
    }
}
