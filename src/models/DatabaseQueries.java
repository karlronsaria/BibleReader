/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package models;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import javafx.util.Pair;

/**
 *
 * @author Andrew
 */
public class DatabaseQueries
{
    public static int BOOKS_IN_BIBLE = 66;
    
    public interface DisplayResultAction
    {
        public String getResultString(ResultSet r, int start) throws SQLException;
    }
    
    public static class PreferencesList
    {
        private DisplayResultAction resultDisplayMethod = (ResultSet r, int start) -> getResultStringWithIndex(r, start);
        
        public DisplayResultAction getResultDisplayMethod()
        {
            return resultDisplayMethod;
        }
        
        public void disableVerseNumbers()
        {
            resultDisplayMethod = (ResultSet r, int start) -> getResultStringWithSpace(r, start);
        }
        
        public void enableVerseNumbers()
        {
            resultDisplayMethod = (ResultSet r, int start) -> getResultStringWithIndex(r, start);
        }
        
        public String getResultStringWithSpace(ResultSet r, int start) throws SQLException
        {
            String temp = "";
            
            while (!r.isClosed() && r.next())
                temp += "  " + r.getString("Verse") + "\n";
            
            return temp;
        }
        
        public String getResultStringWithIndex(ResultSet r, int start) throws SQLException
        {
            String temp = "";
            int verseIndex = start;
            
            while (!r.isClosed() && r.next())
            {
                temp += verseIndex + "  " + r.getString("Verse") + "\n";
                verseIndex++;
            }
            
            return temp;
        }
    }
    
    public static PreferencesList prefs = new PreferencesList();
    
    public static int getVersionIdFromCode(Statement s, String versionCode) throws SQLException
    {
        ResultSet r = s.executeQuery("SELECT ID FROM Versions WHERE Code = " + versionCode);
        
        if (!r.isClosed())
        {
            r.next();
            return r.getInt("ID");
        }
        
        return -1;
    }
    
    public static int getBookIdFromCode(Statement s, String bookCode) throws SQLException
    {
        ResultSet r = s.executeQuery("SELECT ID FROM Books WHERE Code = '" + bookCode + "';");
        
        if (!r.isClosed())
        {
            r.next();
            return r.getInt("ID");
        }
        
        return -1;
    }
    
    public static int getBookIdFromName(Statement s, String bookName) throws SQLException
    {
        ResultSet r = s.executeQuery("SELECT BookID FROM Book_Names WHERE NAME = '" + bookName + "';");
        
        if (!r.isClosed())
        {
            r.next();
            return r.getInt("BookID");
        }
        
        return -1;
    }
    
    public static ResultSet getVerseRangeResult(Statement s, int versionId, int bookId, int chapterNumber, int startingVerse, int endingVerse) throws SQLException
    {
        return s.executeQuery("SELECT Verse FROM " + DatabaseConnector.getBibleTableName(versionId, bookId) + " WHERE ID " + allVersesInRangeClause(chapterNumber, startingVerse, endingVerse) + ";");
    }
    
    public static ResultSet getVerseRangeResult(Statement s, int versionId, String bookName, int chapterNumber, int startingVerse, int endingVerse) throws SQLException
    {
        return getVerseRangeResult(s, versionId, getBookIdFromName(s, bookName), chapterNumber, startingVerse, endingVerse);
    }
    
    public static ResultSet getChapterResult(Statement s, int versionId, int bookId, int chapterNumber) throws SQLException
    {
        return getVerseRangeResult(s, versionId, bookId, chapterNumber, 1, 999);
    }
    
    public static ResultSet getChapterResult(Statement s, int versionId, String bookName, int chapterNumber) throws SQLException
    {
        return getChapterResult(s, versionId, getBookIdFromName(s, bookName), chapterNumber);
    }
    
    public static ResultSet getColumns(Statement s, String fields, String tableName) throws SQLException
    {
        return s.executeQuery("SELECT " + fields + " FROM " + tableName + ";");
    }
    
    public static ResultSet getItem(Statement s, String field, String tableName, int id) throws SQLException
    {
        return s.executeQuery("SELECT " + field + " FROM " + tableName + " WHERE ID = " + id + ";");
    }
    
    public static String getString(Statement s, String field, String tableName, int id) throws SQLException
    {
        ResultSet r = getItem(s, field, tableName, id);
        r.next();
        return r.getString(field);
    }
    
    public static String allVersesInRangeClause(int chapter, int start, int end)
    {
        return "BETWEEN '" + DatabaseConnector.getVerseCode(chapter, start) + "' AND '" + DatabaseConnector.getVerseCode(chapter, end) + "'";
    }
    
    public static String allVersesInChapterClause(int chapter)
    {
        return allVersesInRangeClause(chapter, 1, 999);
    }
    
    public static Pair<Integer, Integer> getNextChapter(Statement s, int versionId, int bookId, int chapter) throws SQLException
    {
        ResultSet chaptersInBook = s.executeQuery("SELECT NumberChapters FROM Books WHERE ID = " + bookId + ";");
        chaptersInBook.next();
        
        if (chapter == chaptersInBook.getInt("NumberChapters"))
        {
            bookId = bookId == BOOKS_IN_BIBLE ? 1 : bookId + 1;
            chapter = 1;
        }
        else
        {
            chapter++;
        }
        
        return new Pair<>(bookId, chapter);
    }
    
    public static Pair<Integer, Integer> getPreviousChapter(Statement s, int versionId, int bookId, int chapter) throws SQLException
    {
        if (chapter == 1)
        {
            bookId = bookId == 1 ? BOOKS_IN_BIBLE : bookId - 1;
            ResultSet chaptersInBook = s.executeQuery("SELECT NumberChapters FROM Books WHERE ID = " + bookId + ";");
            chaptersInBook.next();
            chapter = chaptersInBook.getInt("NumberChapters");
        }
        else
        {
            chapter--;
        }
        
        return new Pair<>(bookId, chapter);
    }
    
    public static ResultSet getColumnNames(Statement s, String tableName) throws SQLException
    {
        return s.executeQuery("SHOW COLUMNS FROM " + tableName + ";");
    }
    
    public static ArrayList<ResultSet> queryForList(Statement s, int versionId, AddressParser.Address a) throws SQLException
    {
        ArrayList<ResultSet> list = new ArrayList<>();
        AddressParser.Range rng;
        
        while (a.hasRanges())
        {
            rng = a.popRanges();
            
            switch (a.getChapter())
            {
                case 0:
                {
                    for (int i = rng.getStart(); i <= rng.getEnd(); ++i)
                        list.add(DatabaseQueries.getChapterResult(s, versionId, a.getTitle(), a.getChapter()));
                    
                    break;
                }
                default:
                {
                    list.add(DatabaseQueries.getVerseRangeResult(s, versionId, a.getTitle(), a.getChapter(), rng.getStart(), rng.getEnd()));
                    break;
                }
            }
        }
        
        return list;
    }
    
    public static ResultSet getChapterResult(Statement s, int versionId, AddressParser.Address a) throws SQLException
    {
        return DatabaseQueries.getChapterResult(s, versionId, a.getTitle(), a.getChapter());
    }
    
    public static ResultSet getVerseRangeResult(Statement s, int versionId, AddressParser.Address a, AddressParser.Range rng) throws SQLException
    {
        return DatabaseQueries.getVerseRangeResult(s, versionId, a.getTitle(), a.getChapter(), rng.getStart(), rng.getEnd());
    }
    
    public static String getChapterString(Statement s, int versionId, int bookId, int chapter) throws SQLException
    {
        return prefs.getResultDisplayMethod().getResultString(DatabaseQueries.getChapterResult(s, versionId, bookId, chapter), 1) + "\n";
    }
    
    public static String getChapterString(Statement s, int versionId, String title, int chapter) throws SQLException
    {
        return prefs.getResultDisplayMethod().getResultString(DatabaseQueries.getChapterResult(s, versionId, title, chapter), 1) + "\n";
    }
    
    public static String getChapterRangeString(Statement s, int versionId, String title, int start, int end) throws SQLException
    {
        String temp = "";
        
        for (int i = start; i <= end; ++i)
        {
            temp += i + "\n\n";
            temp += getChapterString(s, versionId, title, i);
        }
        
        return temp;
    }
    
    public static String getVerseRangeString(Statement s, int versionId, int bookId, int chapter, int start, int end) throws SQLException
    {
        return prefs.getResultDisplayMethod().getResultString(DatabaseQueries.getVerseRangeResult(s, versionId, bookId, chapter, start, end), start) + "\n";
    }
    
    public static String getVerseRangeString(Statement s, int versionId, String title, int chapter, int start, int end) throws SQLException
    {
        return prefs.getResultDisplayMethod().getResultString(DatabaseQueries.getVerseRangeResult(s, versionId, title, chapter, start, end), start) + "\n";
    }
    
    public static int getNumberOfChapters(Statement s, int bookId) throws SQLException
    {
        ResultSet r = s.executeQuery("SELECT NumberChapters FROM Books WHERE ID = " + bookId + ";");
        
        if (!r.isClosed() && r.next())
            return r.getInt("NumberChapters");
        
        return -1;
    }
    
    public static String getPage(Statement s, int versionId, int bookId, int chapter) throws SQLException
    {
        String page = "";
        ResultSet r = getItem(s, "FirstTitle", "Books", bookId);
        
        if (r.isClosed() || !r.next())
            return "";
            
        page += r.getString("FirstTitle") + " " + chapter + "\n\n" + getChapterString(s, versionId, bookId, chapter);
        
        return page;
    }
    
    public static String getPage(Statement s, int versionId, int bookId, int startingChapter, int endingChapter) throws SQLException
    {
        String page = "";
        
        for (int i = startingChapter; i <= endingChapter; ++i)
            page += getPage(s, versionId, bookId, i);
        
        return page;
    }
    
    public static String queryForString(Statement s, int versionId, AddressParser.Address a) throws SQLException
    {
        String str = "";
        AddressParser.Range first;
        
        if (!a.hasRanges())
        {
            str += getChapterString(s, versionId, a.getTitle(), a.getChapter());
        }
        else
        {
            switch (a.getChapter())
            {
                case 0:
                {
                    first = a.popRanges();
                    
                    if (a.hasRanges())
                    {
                        AddressParser.Range secnd = a.popRanges();
                        str += first.getStart() + "\n\n";
                        
                        if (first.getStart() == secnd.getStart())
                        {
                            // Gen 2:3-2:13
                            str += getVerseRangeString(s, versionId, a.getTitle(), first.getStart(), first.getEnd(), secnd.getEnd());
                        }
                        else
                        {
                            // Gen 2:3-4:13
                            str += getVerseRangeString(s, versionId, a.getTitle(), first.getStart(), first.getEnd(), 999);
                            str += getChapterRangeString(s, versionId, a.getTitle(), first.getStart() + 1, secnd.getStart() - 1);
                            str += secnd.getStart() + "\n\n";
                            str += getVerseRangeString(s, versionId, a.getTitle(), secnd.getStart(), 1, secnd.getEnd());
                        }
                    }
                    else
                    {
                        // Gen 2-4
                        str += getChapterRangeString(s, versionId, a.getTitle(), first.getStart(), first.getEnd());
                    }
                    
                    break;
                }
                default:
                {
                    while (a.hasRanges())
                    {
                        first = a.popRanges();
                        str += getVerseRangeString(s, versionId, a.getTitle(), a.getChapter(), first.getStart(), first.getEnd());
                    }
                    
                    break;
                }
            }
        }
        
        return str;
    }
    
    public static String query(Statement s, int versionId, AddressParser.AddressList l) throws SQLException
    {
        String str = "";
        int prevBookId = 0;
        int bookId;
        int prevChapter = -1;
        int chapter;
        ResultSet r;
        
        for (AddressParser.Address a: l)
        {
            bookId = getBookIdFromName(s, a.getTitle());
            chapter = a.getChapter();
            
            if (bookId == -1)
                return "";
            
            if (bookId == prevBookId)
            {
                if (chapter != prevChapter && chapter != 0)
                {
                    str += chapter + "\n\n";
                    prevChapter = chapter;
                }
            }
            else
            {
                r = getItem(s, "FirstTitle", "Books", bookId);
                
                if (r.isClosed() || !r.next())
                    return "";
                
                str += r.getString("FirstTitle") + " ";
                prevBookId = bookId;
                
                if (chapter != 0)
                {
                    str += chapter + "\n\n";
                    prevChapter = chapter;
                }
            }
            
            str += queryForString(s, versionId, a);
        }
        
        return str;
    }
}












