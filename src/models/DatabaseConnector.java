package models;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author andrewd12
 */
public class DatabaseConnector
{
    public static abstract class FileParser
    {
        private final InputStream stream;
        
        public FileParser(String packageName, String resourceName)
        {
            stream = getClass().getResourceAsStream(String.format("..\\%s\\%s", packageName, resourceName));
            stream.mark(0);
        }
        
        public FileParser()
        {
            this("files", "titles.txt");
        }
        
        public boolean hasNext() throws IOException
        {
            return stream.available() > 0;
        }
        
        public void readForCharacter(char marker) throws IOException
        {
            while (hasNext() && (char) stream.read() != marker) { }
        }
        
        public String parseToCharacter(char marker) throws IOException
        {
            StringBuilder builder = new StringBuilder();
            
            if (stream.available() > 0)
            {
                char c = (char) stream.read();
                
                while (hasNext() && c != marker)
                {
                    builder.append(c);
                    c = (char) stream.read();
                }
            }
            
            return builder.toString();
        }
        
        public int read() throws IOException
        {
            return stream.read();
        }
        
        public void reset() throws IOException
        {
            stream.reset();
        }
        
        public void mark(int m)
        {
            stream.mark(m);
        }
        
        public boolean markSupported()
        {
            return stream.markSupported();
        }
        
        public abstract void getNextPayload() throws IOException;
    }
    
    public static class TitlesTxtParser extends FileParser
    {
        public String code;
        public int chapters;
        public String firstTitle;
        public String secondTitle;
        public String thirdTitle;
        
        public TitlesTxtParser(String packageName, String resourceName)
        {
            super(packageName, resourceName);
        }
        
        public TitlesTxtParser()
        {
            super("files", "titles.txt");
        }
        
        @Override
        public void getNextPayload() throws IOException
        {
            code = parseToCharacter(':');
            chapters = Integer.parseInt(parseToCharacter(':'));
            firstTitle = parseToCharacter(':');
            secondTitle = parseToCharacter(':');
            thirdTitle = parseToCharacter('\n');
        }
    }
    
    public static class VersionsTxtParser extends FileParser
    {
        public String code;
        public String fileName;
        public String title;
        
        public VersionsTxtParser(String packageName, String resourceName)
        {
            super(packageName, resourceName);
        }
        
        public VersionsTxtParser()
        {
            super("files", "versions.txt");
        }
        
        @Override
        public void getNextPayload() throws IOException
        {
            code = parseToCharacter(':');
            fileName = parseToCharacter(':');
            title = parseToCharacter('\n');
        }
    }
    
    public static class NamesTxtParser extends FileParser
    {
        public String name;
        public int bookId;
    
        public NamesTxtParser(String packageName, String resourceName)
        {
            super(packageName, resourceName);
        }
        
        public NamesTxtParser()
        {
            super("files", "names.txt");
        }
        
        @Override
        public void getNextPayload() throws IOException
        {
            name = parseToCharacter(':');
            bookId = Integer.parseInt(parseToCharacter('\n'));
        }
    }
    
    public static class BibleJsonParser extends FileParser
    {
        static final String HEADER = "{\"resultset\": {\"row\": [";
        
        public String bookCode;
        public int bookNumber;
        public int chapterNumber;
        public int verseNumber;
        public String verse;
        
        public BibleJsonParser(String packageName, String resourceName)
        {
            super(packageName, resourceName);
        }
        
        public BibleJsonParser(String resourceName)
        {
            super("files", resourceName);
        }
        
        public boolean ready() throws IOException
        {
            readForCharacter(']');
            readForCharacter('}');
            
            return hasNext() && (char) read() == ',';
        }
        
        public void discardHeader() throws IOException
        {
            reset();
            readForCharacter('[');
        }
        
        public void readForStartOfField() throws IOException
        {
            readForCharacter('{');
        }
        
        public void readForEndOfField() throws IOException
        {
            readForCharacter('}');
        }
        
        public void readForStartOfValue() throws IOException
        {
            readForCharacter(':');
            readForCharacter('[');
        }
        
        public boolean checkNext() throws IOException
        {
            return (char) read() != ']';
        }
        
        @Override
        public void getNextPayload() throws IOException
        {
            bookCode = parseToCharacter(',');
            read();
            bookNumber = Integer.parseInt(parseToCharacter(','));
            read();
            chapterNumber = Integer.parseInt(parseToCharacter(','));
            read();
            verseNumber = Integer.parseInt(parseToCharacter(','));
            
            readForCharacter('"');
            
            verse = parseToCharacter('"');
        }
    }
    
    private static DatabaseConnector main;
    public static boolean ready = false;
    
    public static final String DRIVER_NAME = "org.h2.Driver";
    public static final String DATABASE_PATH = "jdbc:h2:~/test";
    public static final String TEST_QUERY = "SELECT Versions.ID, Books.ID, Book_Names.Name FROM Versions, Books, Book_Names LIMIT 1;";
//    public static final String TEST_QUERY = "SELECT Versions.ID, Books.ID FROM Versions, Books LIMIT 1;";
    
    private Connection my_connection;
    
    public static DatabaseConnector request() throws ClassNotFoundException, SQLException
    {
        instantiateMainConnector();
        
        if (!ready)
            unlockMainConnector();
        
        if (!ready)
            return null;
        
        return main;
    }
    
    private DatabaseConnector() throws ClassNotFoundException, SQLException
    {
        Class.forName(DRIVER_NAME);
        my_connection = DriverManager.getConnection(DATABASE_PATH);
    }
    
    private DatabaseConnector(String driverName, String dbPath) throws ClassNotFoundException, SQLException
    {
        Class.forName(driverName);
        my_connection = DriverManager.getConnection(dbPath);
    }
    
    private static void instantiateMainConnector()
    {
        try
        {
            if (main == null)
                main = new DatabaseConnector();
        }
        catch (ClassNotFoundException | SQLException ex)
        {
            Logger.getLogger(DatabaseConnector.class.getName()).log(Level.SEVERE, null, ex);
            destroyAndLockDownMainConnector();
        }
    }
    
    private static void unlockMainConnector()
    {
        try
        {
            ready = main.queryDatabase();
        }
        catch (SQLException ex)
        {
            Logger.getLogger(DatabaseConnector.class.getName()).log(Level.SEVERE, null, ex);
            ready = false;
        }
    }
    
    private static void destroyAndLockDownMainConnector()
    {
        try
        {
            if (main.my_connection != null)
            {
                main.my_connection.close();
                main.my_connection = null;
            }
        }
        catch (SQLException ex)
        {
            Logger.getLogger(DatabaseConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        main = null;
        ready = false;
    }
    
    private boolean queryDatabase() throws SQLException
    {
        ResultSet temp = my_connection.createStatement().executeQuery(TEST_QUERY);
        return !temp.isClosed() && temp.next();
    }
    
    public Connection getConnection()
    {
        return my_connection;
    }
    
    public Statement createStatement() throws SQLException
    {
        return my_connection.createStatement();
    }
    
    public void closeConnection() throws SQLException
    {
        my_connection.close();
    }
    
    public static final String CREATE_BOOKS =
            
            "DROP TABLE Books IF EXISTS;"
            + "CREATE TABLE IF NOT EXISTS Books"
            + "("
            + "    ID int IDENTITY(1,1) PRIMARY KEY,"
            + "    Code varchar(4),"
            + "    NumberChapters int,"
            + "    FirstTitle varchar(50),"
            + "    SecondTitle varchar(50),"
            + "    ThirdTitle varchar(50)"
            + ");";
    
    public static final String CREATE_VERSIONS = 
            
            "DROP TABLE Versions IF EXISTS;"
            + "CREATE TABLE IF NOT EXISTS Versions"
            + "("
            + "    ID int IDENTITY(1,1) PRIMARY KEY,"
            + "    Code varchar(5),"
            + "    FileName varchar(10),"
            + "    Name varchar(50)"
            + ");";
    
    public static final String CREATE_BOOK_NAMES = 
            
            "DROP TABLE Book_Names IF EXISTS;"
            + "CREATE TABLE IF NOT EXISTS Book_Names"
            + "("
            + "    Name varchar(25) PRIMARY KEY,"
            + "    BookID int,"
            + "    FOREIGN KEY (BookID) REFERENCES Books(ID)"
            + ");";
    
    public static final String CREATE_BOOK =
            
            "DROP TABLE %s IF EXISTS;"
            + "CREATE TABLE IF NOT EXISTS %s"
            + "("
            + "    ID varchar(8) PRIMARY KEY,"
            + "    Verse varchar(max),"
            + "    Heading varchar(128),"
            + "    Indentation int,"
            + "    Spacing int"
            + ");";
    
    public static final String INSERT_BOOK =
            
            "INSERT INTO Books (Code, NumberChapters, FirstTitle, SecondTitle, ThirdTitle) "
            + "VALUES ('%s', %d, %s, %s, %s);";
    
    public static final String INSERT_VERSION =
            
            "INSERT INTO Versions (Code, FileName, Name) "
            + "VALUES ('%s', '%s', '%s');";
    
    public static final String INSERT_BOOK_NAME =
            
            "INSERT INTO Book_Names (Name, BookID) "
            + "VALUES ('%s', '%d');";
    
    public static final String INSERT_VERSE =
            
            "INSERT INTO %s (ID, Verse, Heading, Indentation, Spacing) "
            + "VALUES ('%s', '%s', %s, %d, %d);";
    
    public static final String GET_VERSIONS =
            
            "SELECT ID, FileName FROM Versions;";
    
    public static String getBibleTableName(int versionId, int bookId)
    {
        return String.format("Version%03d_Book%03d", versionId, bookId);
    }
    
    public static String getEntry(String str)
    {
        return str == null || str.matches("^\\s*$") ? "NULL" : String.format("'%s'", str.replace("'", "''"));
    }
    
    public static String getVerseCode(int chapterNumber, int verseNumber)
    {
        return String.format("%03d.%03d", chapterNumber, verseNumber);
    }
    
    private static void propogateBooks(Statement s) throws IOException, SQLException
    {
        TitlesTxtParser parser = new TitlesTxtParser();
        
        while (parser.hasNext())
        {
            parser.getNextPayload();
            s.execute
            (
                String.format
                (
                    INSERT_BOOK,
                    parser.code,
                    parser.chapters,
                    getEntry(parser.firstTitle),
                    getEntry(parser.secondTitle),
                    getEntry(parser.thirdTitle)
                )
            );
        }
    }
    
    private static void propogateVersions(Statement s) throws IOException, SQLException
    {
        VersionsTxtParser parser = new VersionsTxtParser();
        
        while (parser.hasNext())
        {
            parser.getNextPayload();
            s.execute
            (
                String.format
                (
                    INSERT_VERSION,
                    parser.code,
                    parser.fileName,
                    parser.title
                )
            );
        }
    }
    
    private static void propogateBookNames(Statement s) throws IOException, SQLException
    {
        NamesTxtParser parser = new NamesTxtParser();
        
        while (parser.hasNext())
        {
            parser.getNextPayload();
            s.execute
            (
                String.format
                (
                    INSERT_BOOK_NAME,
                    parser.name,
                    parser.bookId
                )
            );
        }
    }
    
    public static void tester()
    {
        try {
            
            request();
            Statement s = main.createStatement();
            ResultSet versionResults = s.executeQuery(GET_VERSIONS);
            
            while (!versionResults.isClosed() && versionResults.next())
            {
                int versionId = versionResults.getInt("ID");
                String fileName = versionResults.getString("FileName");

                System.out.println();
                System.out.println(String.format("%03d  %s", versionId, fileName));
                System.out.println();
            }
            
        } catch (SQLException | ClassNotFoundException ex) {
            Logger.getLogger(DatabaseConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static boolean propogateDatabase()
    {
        try
        {
            instantiateMainConnector();
            
            if (main == null)
                return false;
            
            Statement s = main.createStatement();
            Statement executor = main.createStatement();
            
            ResultSet tempSet = main
                .getConnection()
                .getMetaData()
                .getTables(null, null, "BOOKS", null);
            
            if (tempSet.next())
                return true;
            
            s.execute(CREATE_BOOKS);
            s.execute(CREATE_VERSIONS);
            s.execute(CREATE_BOOK_NAMES);
            
            propogateBooks(s);
            propogateVersions(s);
            propogateBookNames(s);
            
            try (ResultSet versionResults = s.executeQuery(GET_VERSIONS))
            {
                BibleJsonParser parser;
                String tableName = "";
                String fileName;
                int versionId;
                int bookId;
                
                while (!versionResults.isClosed() && versionResults.next())
                {
                    bookId = 0;
                    versionId = versionResults.getInt("ID");
                    fileName = versionResults.getString("FileName");
                    
                    System.out.println();
                    System.out.println(String.format("%03d  %s", versionId, fileName));
                    System.out.println();
                    
                    parser = new BibleJsonParser(fileName);
                    
                    parser.discardHeader();
                    parser.readForStartOfField();
                    
                    while (parser.hasNext())
                    {
                        parser.readForStartOfValue();
                        parser.getNextPayload();
                        
                        if (parser.bookNumber > bookId)
                        {
                            bookId = parser.bookNumber;
                            tableName = getBibleTableName(versionId, bookId);
                            executor.execute(String.format(CREATE_BOOK, tableName, tableName));
                            
                            // TEST
                            System.out.println("Book: " + bookId);
                        }
                        
                        executor.execute
                        (
                            String.format
                            (
                                INSERT_VERSE,
                                tableName,
                                getVerseCode(parser.chapterNumber, parser.verseNumber),
                                parser.verse.replace("'", "''"),
                                null,
                                0,
                                0
                            )
                        );
                        
                        parser.readForEndOfField();
                        
                        if (!parser.checkNext())
                            break;
                    }
                }
            }
        }
        catch (IOException | SQLException ex)
        {
            Logger.getLogger(DatabaseConnector.class.getName()).log(Level.SEVERE, null, ex);
            destroyAndLockDownMainConnector();
            return false;
        }
        
        unlockMainConnector();
        return true;
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
//        DatabaseConnector.tester();
        DatabaseConnector.propogateDatabase();
        
//        try
//        {
//            DatabaseConnector temp = DatabaseConnector.request();
//            
////            ResultSet r = temp.createStatement().executeQuery("SELECT * FROM BOOKS");
////            
////            while (!r.isClosed() && r.next())
////            {
////                System.out.println(r.getInt("ID") + "   " + r.getString("FirstTitle"));
////            }
//
//            ResultSet r;
//            
//            System.out.println(DatabaseQueries.getString(temp.createStatement(), "FirstTitle", "Books", 14));
//            System.out.println();
//            
//            r = DatabaseQueries.getChapterResult(temp.createStatement(), 1, 14, 9);
//            
//            while (!r.isClosed() && r.next())
//            {
//                System.out.println(r.getString("verse"));
//            }
//            
//            System.out.println();
//            
//            r = temp.createStatement().executeQuery("SHOW COLUMNS FROM Books;");
//            
//            while (!r.isClosed() && r.next())
//            {
//                System.out.println(r.getString(1));
//            }
//            
//            temp.closeConnection();
//        }
//        catch (ClassNotFoundException | SQLException ex)
//        {
//            Logger.getLogger(DatabaseConnector.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }
}
