/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package panels;

import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.CaretEvent;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.Style;
import models.AddressParser;
//import javax.swing.text.StyledDocument;

/**
 *
 * @author andrewd12
 */
public class Reader extends javax.swing.JPanel
{
    static final String IDLE_SEARCH_BAR_MESSAGE = "Find (Ctl + F)";
    
    enum ContentTypes
    {
        SEARCH_RESULT,
        WHOLE_CHAPTER
    }
    
    public static class PreferenceStruct
    {
        public int defaultStartingVersionId = 1;
        public String defaultStartingAddress = "Genesis 1";
        boolean caseSensitive = false;
        public DefaultHighlightPainter highlight = new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 255, 0));
        public DefaultHighlightPainter selector = new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 180, 0));
    }
    
    public class ContentDataStruct
    {
        boolean typedLast = false;
        
        public int currentBookId = 1;
        public int currentStartingChapter = 1;
        public int currentEndingChapter = 1;
        
        public int prevBookId;
        public int prevChapter;
        
        public int nextBookId;
        public int nextChapter;
        
        private String currentAddress = prefs.defaultStartingAddress;
        ContentTypes currentContentType = ContentTypes.WHOLE_CHAPTER;
        
        public void setCurrentAddress(String newAddress)
        {
            currentAddress = newAddress;
        }
        
        public String getCurrentAddress()
        {
            return currentAddress;
        }
        
        public int getVersionId()
        {
            return versionMenu.getSelectedIndex() + 1;
        }
        
        public void setVersionId(int versionId)
        {
            versionMenu.setSelectedIndex(versionId - 1);
        }
        
        public void setPage()
        {
            try
            {
                Statement s = models.DatabaseConnector.request().createStatement();
                
                textPane.getHighlighter().removeAllHighlights();
                
                javafx.util.Pair<Integer, Integer> bookAndChapter = models.DatabaseQueries.getNextChapter(s, getVersionId(), currentBookId, currentEndingChapter);
                nextBookId = bookAndChapter.getKey();
                nextChapter = bookAndChapter.getValue();
                nextPageButton.setToolTipText(getAddressFromPage(nextBookId, nextChapter));
                
                bookAndChapter = models.DatabaseQueries.getPreviousChapter(s, getVersionId(), currentBookId, currentStartingChapter);
                prevBookId = bookAndChapter.getKey();
                prevChapter = bookAndChapter.getValue();
                prevPageButton.setToolTipText(getAddressFromPage(prevBookId, prevChapter));
                
                textPane.setText(models.DatabaseQueries.getPage(s, getVersionId(), currentBookId, currentStartingChapter, currentEndingChapter));
                textPane.setCaretPosition(0);
                changeCurrentAddress(getAddressFromPage(currentBookId, currentStartingChapter, currentEndingChapter));
            }
            catch (SQLException | ClassNotFoundException ex)
            {
                Logger.getLogger(Reader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        public String getAddressFromPage(int bookId, int chapter)
        {
            return getAddressFromPage(bookId, chapter, chapter);
        }
        
        public String getAddressFromPage(int bookId, int start, int end)
        {
            try
            {
                Statement s = models.DatabaseConnector.request().createStatement();
                ResultSet r = s.executeQuery("SELECT FirstTitle FROM Books WHERE ID = " + bookId + ";");
                r.next();
                return String.format("%s %s", r.getString("FirstTitle"), start == end ? start : String.format("%d-%d", start, end));
            }
            catch (SQLException | ClassNotFoundException ex)
            {
                Logger.getLogger(Reader.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            return prefs.defaultStartingAddress;
        }
        
        public void goToNextChapter()
        {
            if (currentContentType == ContentTypes.WHOLE_CHAPTER)
            {
                currentBookId = nextBookId;
                currentStartingChapter = currentEndingChapter = nextChapter;
                setPage();
            }
        }
        
        public void goToPreviousChapter()
        {
            if (currentContentType == ContentTypes.WHOLE_CHAPTER)
            {
                currentBookId = prevBookId;
                currentStartingChapter = currentEndingChapter = prevChapter;
                setPage();
            }
        }
    }
    
    public class SearchDataStruct
    {
        final static int HISTORY_MAX = 500;
        
        String phrase = "";
        Comparable[] instances = null;
        java.util.ArrayList highlights = new java.util.ArrayList<>();
        
        int[] versions = new int[HISTORY_MAX];
        String[] history = new String[HISTORY_MAX];
        private int pos = 0;
        private int max = 0;
        
        public SearchDataStruct(ContentDataStruct c)
        {
            history[pos] = c.currentAddress;
            versions[pos] = prefs.defaultStartingVersionId;
        }
        
        public void dropInstances()
        {
            instances = null;
        }
        
        public boolean matchesCurrentHistoryItem(String searchPhrase)
        {
            return history[pos].equals(searchPhrase);
        }
        
        public void updateHistory(String searchPhrase)
        {
            // The input is expected to never be null.
            if (searchPhrase.length() > 0 && !search.matchesCurrentHistoryItem(searchPhrase))
                search.newHistoryItem(searchPhrase);
        }
        
        public void setPhrase(String searchPhrase)
        {
            phrase = searchPhrase;
        }
        
        public void goBack()
        {
            if (pos > 0)
            {
                String temp = searchBar.getText();
                
                if (!matchesCurrentHistoryItem(temp))
                    newHistoryItem(temp);
                
                versions[pos] = content.getVersionId();
                pos = pos - 1;
            }
        }
        
        public void goForward()
        {
            if (pos < max)
                pos = pos + 1;
        }
        
        private void record(String searchItem)
        {
            history[pos] = searchItem;
            versions[pos] = content.getVersionId();
        }
        
        public void newHistoryItem(String currentSearchItem)
        {
            if (pos < HISTORY_MAX - 1)
            {
                max = ++pos;
                record(currentSearchItem);
            }
        }
        
        public void updateContent()
        {
            searchBar.setText(history[pos]);
            content.setVersionId(versions[pos]);
            changeSearch(history[pos]);
        }
        
        public void phraseSearch()
        {
            phraseSearch(phrase);
        }
        
        public void phraseSearch(String searchPhrase)
        {
            phrase = searchPhrase;
            instances = models.Algorithms.search(phrase, textPane.getText(), prefs.caseSensitive);
            
            if (instances.length > 0)
            {
                textPane.setCaretPosition((int) instances[0]);
                highlights.add(highlightSelectedResult((int) instances[0], phrase.length()));
            }
            
            for (int i = 0; i < instances.length; ++i)
            {
                highlights.add(highlightResult((int) instances[i], phrase.length(), prefs.highlight));
            }
        }
        
        public void moveCaretToNextSearchResult()
        {
            if (instances != null && instances.length > 0)
            {
                int nextIndex;
                
                if (textPane.getCaretPosition() >= (int) instances[instances.length - 1])
                {
                    nextIndex = 0;
                }
                else
                {
                    nextIndex = (int) models.Algorithms.searchRightLeaning(instances, textPane.getCaretPosition());
                    nextIndex = nextIndex + 1;
                }
                
                textPane.setCaretPosition((int) instances[nextIndex]);
            }
        }
        
        public void moveCaretToPreviousSearchResult()
        {
            if (instances != null && instances.length > 0)
            {
                int nextIndex;
                
                if (textPane.getCaretPosition() <= (int) instances[0])
                {
                    nextIndex = instances.length - 1;
                }
                else
                {
                    nextIndex = (int) models.Algorithms.searchLeftLeaning(instances, textPane.getCaretPosition());
                    nextIndex = nextIndex - 1;
                }
                
                textPane.setCaretPosition((int) instances[nextIndex]);
            }
        }
    }
    
    static PreferenceStruct prefs = new PreferenceStruct();
    ContentDataStruct content = new ContentDataStruct();
    SearchDataStruct search = new SearchDataStruct(content);
    
    public SearchDataStruct searchData()
    {
        return search;
    }
    
    public ContentDataStruct contentData()
    {
        return content;
    }
    
    public void goBack()
    {
        searchBar.requestFocus();
        search.goBack();
        search.updateContent();
    }
    
    public void goForward()
    {
        searchBar.requestFocus();
        search.goForward();
        search.updateContent();
    }
    
    public interface CurrentAddressListener
    {
        public abstract void respond(String currentAddress);
    }
    
    private final java.util.ArrayList<CurrentAddressListener> currentAddressListeners = new java.util.ArrayList<>();
    
    public void addCurrentAddressListener(CurrentAddressListener l)
    {
        currentAddressListeners.add(l);
    }
    
    private void changeCurrentAddress(String newAddress)
    {
        content.setCurrentAddress(newAddress);
        currentAddressListeners.forEach(l -> l.respond(newAddress));
    }
    
    public String getCurrentAddress()
    {
        return content.currentAddress;
    }
    
    private void constructVersionsDropdown()
    {
        try
        {
            Statement s = models.DatabaseConnector.request().createStatement();
            ResultSet r = s.executeQuery("SELECT Code FROM Versions ORDER BY ID");
            
            while (!r.isClosed() && r.next())
                versionMenu.addItem(r.getString("Code").toUpperCase());
            
            versionMenu.addItemListener(e -> {
                changeCurrentAddress(content.currentAddress);
                changeSearch(content.currentAddress);
            });
            
            content.setVersionId(prefs.defaultStartingVersionId);
        }
        catch (SQLException | ClassNotFoundException ex)
        {
            Logger.getLogger(Reader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Creates new form Reader
     */
    public Reader()
    {
        initComponents();
        
        nextPageButton.setFocusable(false);
        prevPageButton.setFocusable(false);
        
        textPane.setEditable(false);
        textPane.addCaretListener
        (
            (CaretEvent ce) ->
            {
                if (search.instances != null)
                {
                    javax.swing.JTextPane source = (javax.swing.JTextPane) ce.getSource();
                    
                    search.highlights.forEach(source.getHighlighter()::removeHighlight);
                    search.highlights.clear();
                    
                    int pos = source.getCaretPosition();
                    
                    for (Object instance : search.instances)
                    {
                        if (pos >= (int) instance && pos < (int) instance + search.phrase.length())
                            search.highlights.add(highlightSelectedResult((int) instance, search.phrase.length()));
                        else
                            search.highlights.add(highlightResult((int) instance, search.phrase.length(), prefs.highlight));
                    }
                }
            }
        );
        
        textPane.addFocusListener
        (
            new java.awt.event.FocusListener()
            {
                @Override
                public void focusGained(FocusEvent fe) {
                    ((javax.swing.JTextPane) fe.getSource()).getCaret().setVisible(true);
                }
                
                @Override
                public void focusLost(FocusEvent fe) {
                    ((javax.swing.JTextPane) fe.getSource()).getCaret().setVisible(false);
                }
            }
        );
        
        searchBar.setText(IDLE_SEARCH_BAR_MESSAGE);
        searchBar.addFocusListener
        (
            new java.awt.event.FocusListener()
            {
                @Override
                public void focusGained(FocusEvent fe)
                {
                    javax.swing.JTextField source = (javax.swing.JTextField) fe.getSource();
                    source.setForeground(Color.BLACK);
                    
                    if (!content.typedLast)
                    {
                        source.setText("");
                    }
                    else
                    {
                        source.setSelectionStart(0);
                        source.setSelectionEnd(source.getText().length());
                    }
                }
                
                @Override
                public void focusLost(FocusEvent fe)
                {
                    javax.swing.JTextField source = (javax.swing.JTextField) fe.getSource();
                    source.setForeground(new Color(102, 102, 102));
                    
                    if (source.getText().length() == 0)
                    {
                        source.setText(IDLE_SEARCH_BAR_MESSAGE);
                        content.typedLast = false;
                    }
                }
            }
        );
        
        searchBar.addKeyListener
        (
            new java.awt.event.KeyListener()
            {
                @Override
                public void keyTyped(KeyEvent ke)
                {
                    content.typedLast = true;
                }
                
                @Override
                public void keyPressed(KeyEvent ke) {}
                
                @Override
                public void keyReleased(KeyEvent ke) {}
            }
        );
        
        textPane.setFont(new java.awt.Font("Helvetica", 0, 14));
        nextPageButton.addKeyListener
        (
            new java.awt.event.KeyListener()
            {
                @Override
                public void keyTyped(KeyEvent ke) {}

                @Override
                public void keyPressed(KeyEvent ke)
                {
                    switch (ke.getKeyCode())
                    {
                        case KeyEvent.VK_ENTER : content.goToNextChapter();
                        break;
                        default : break;
                    }
                }

                @Override
                public void keyReleased(KeyEvent ke) {}
            }
        );
        
        nextPageButton.addActionListener(e -> content.goToNextChapter());
        prevPageButton.addKeyListener
        (
            new java.awt.event.KeyListener()
            {
                @Override
                public void keyTyped(KeyEvent ke) {}

                @Override
                public void keyPressed(KeyEvent ke)
                {
                    switch (ke.getKeyCode())
                    {
                        case KeyEvent.VK_ENTER : content.goToPreviousChapter();
                        break;
                        default : break;
                    }
                }

                @Override
                public void keyReleased(KeyEvent ke) {}
            }
        );
        
        constructVersionsDropdown();
        prevPageButton.addActionListener(e -> content.goToPreviousChapter());
        changeCurrentAddress(prefs.defaultStartingAddress);
        content.setPage();
    }
    
    private Object highlightResult(int pos, int len, javax.swing.text.Highlighter.HighlightPainter h)
    {
        Object temp = null;
        
        try
        {
            temp = textPane.getHighlighter().addHighlight(pos, pos + len, h);
        }
        catch (javax.swing.text.BadLocationException ex)
        {
            java.util.logging.Logger.getLogger(Reader.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        
        return temp;
    }
    
    private Object highlightSelectedResult(int pos, int len)
    {
        return highlightResult(pos, len, prefs.selector);
    }
    
    public void updateSearchResults()
    {
        search.dropInstances();
        search.updateHistory(searchBar.getText());
        performSearch(searchBar.getText());
    }
    
    private void changeSearch(String searchPhrase)
    {
        search.dropInstances();
        performSearch(searchPhrase);
    }
    
    private void performSearch(String searchPhrase)
    {
        models.AddressParser parser = new models.AddressParser(searchPhrase);
        models.AddressParser.AddressList list = parser.compileToAddressList();
        
        if (!list.isEmpty())
        {
            try
            {
                Statement s = models.DatabaseConnector.request().createStatement();
                
                if (list.representsWholeChapter())
                {
                    content.currentContentType = ContentTypes.WHOLE_CHAPTER;
                    content.currentBookId = models.DatabaseQueries.getBookIdFromName(s, list.get(0).getTitle());
                    content.currentStartingChapter = content.currentEndingChapter = list.get(0).getChapter();
                    content.setPage();
                }
                else if (list.representsChapterRange())
                {
                    content.currentContentType = ContentTypes.WHOLE_CHAPTER;
                    content.currentBookId = models.DatabaseQueries.getBookIdFromName(s, list.get(0).getTitle());
                    int temp = models.DatabaseQueries.getNumberOfChapters(s, content.currentBookId);
                    AddressParser.Range rng = list.get(0).popRanges();
                    content.currentEndingChapter = temp < rng.getEnd() ? temp : rng.getEnd();
                    content.currentStartingChapter = rng.getStart();
                    content.setPage();
                }
                else
                {
                    content.currentContentType = ContentTypes.SEARCH_RESULT;
                    String newText = models.DatabaseQueries.query(s, content.getVersionId(), list);
                    textPane.setText(newText);
                    textPane.moveCaretPosition(0);
                    changeCurrentAddress(searchPhrase);
                    nextPageButton.setToolTipText("");
                    prevPageButton.setToolTipText("");
                }
            }
            catch (SQLException | ClassNotFoundException ex)
            {
                Logger.getLogger(Reader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        else
        {
            search.setPhrase(searchPhrase);
        }
        
        search.phraseSearch();
    }
    
    public void scrollUp()
    {
        javax.swing.JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
        scrollBar.setValue(scrollBar.getValue() - textPane.getHeight());
    }
    
    public void scrollDown()
    {
        javax.swing.JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
        scrollBar.setValue(scrollBar.getValue() + textPane.getHeight());
    }

    public void setText(String newText)
    {
        textPane.setText(newText);
    }
    
    public javax.swing.JTextField getSearchBar()
    {
        return searchBar;
    }
    
    public javax.swing.JTextPane getTextPane()
    {
        return textPane;
    }
    
    public javax.swing.JComboBox<String> getVersionMenu()
    {
        return versionMenu;
    }
    
    public static final int NUMBER_COMPONENTS = 9;
    
    public java.awt.Component[] getThings()
    {
        java.awt.Component[] temp = new java.awt.Component[NUMBER_COMPONENTS];
        
        temp[0] = topPositionButton;
        temp[1] = midPositionButton;
        temp[2] = botPositionButton;
        temp[3] = nextPageButton;
        temp[4] = prevPageButton;
        temp[5] = versionMenu;
        temp[6] = scrollPane;
        temp[7] = textPane;
        temp[8] = searchBar;
        
        return temp;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        menuPanel = new javax.swing.JPanel();
        versionMenu = new javax.swing.JComboBox<>();
        searchBar = new javax.swing.JTextField();
        prevPageButton = new javax.swing.JButton();
        nextPageButton = new javax.swing.JButton();
        sidePanel = new javax.swing.JPanel();
        positioningPanel = new javax.swing.JPanel();
        botPositionButton = new javax.swing.JButton();
        midPositionButton = new javax.swing.JButton();
        topPositionButton = new javax.swing.JButton();
        scrollPane = new javax.swing.JScrollPane();
        textPane = new javax.swing.JTextPane();

        setMinimumSize(new java.awt.Dimension(100, 200));

        menuPanel.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

        searchBar.setForeground(new java.awt.Color(102, 102, 102));
        searchBar.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                searchBarKeyPressed(evt);
            }
        });

        javax.swing.GroupLayout menuPanelLayout = new javax.swing.GroupLayout(menuPanel);
        menuPanel.setLayout(menuPanelLayout);
        menuPanelLayout.setHorizontalGroup(
            menuPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(menuPanelLayout.createSequentialGroup()
                .addComponent(searchBar, javax.swing.GroupLayout.DEFAULT_SIZE, 223, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(versionMenu, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        menuPanelLayout.setVerticalGroup(
            menuPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(menuPanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(menuPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(versionMenu, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(searchBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, 0))
        );

        prevPageButton.setText(".");

        nextPageButton.setText(".");

        botPositionButton.setText("Bot");
        botPositionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                botPositionButtonActionPerformed(evt);
            }
        });
        botPositionButton.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                botPositionButtonKeyPressed(evt);
            }
        });

        midPositionButton.setText("Mid");
        midPositionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                midPositionButtonActionPerformed(evt);
            }
        });
        midPositionButton.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                midPositionButtonKeyPressed(evt);
            }
        });

        topPositionButton.setText("Top");
        topPositionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                topPositionButtonActionPerformed(evt);
            }
        });
        topPositionButton.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                topPositionButtonKeyPressed(evt);
            }
        });

        javax.swing.GroupLayout positioningPanelLayout = new javax.swing.GroupLayout(positioningPanel);
        positioningPanel.setLayout(positioningPanelLayout);
        positioningPanelLayout.setHorizontalGroup(
            positioningPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(positioningPanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(positioningPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(topPositionButton, javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(botPositionButton, javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(midPositionButton))
                .addGap(0, 0, 0))
        );

        positioningPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {botPositionButton, midPositionButton, topPositionButton});

        positioningPanelLayout.setVerticalGroup(
            positioningPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(positioningPanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(positioningPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.CENTER, positioningPanelLayout.createSequentialGroup()
                        .addComponent(topPositionButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 337, Short.MAX_VALUE)
                        .addComponent(botPositionButton))
                    .addComponent(midPositionButton, javax.swing.GroupLayout.Alignment.CENTER))
                .addGap(0, 0, 0))
        );

        javax.swing.GroupLayout sidePanelLayout = new javax.swing.GroupLayout(sidePanel);
        sidePanel.setLayout(sidePanelLayout);
        sidePanelLayout.setHorizontalGroup(
            sidePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sidePanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(positioningPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0))
        );
        sidePanelLayout.setVerticalGroup(
            sidePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sidePanelLayout.createSequentialGroup()
                .addGap(29, 29, 29)
                .addComponent(positioningPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(0, 0, 0))
        );

        scrollPane.setViewportView(textPane);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(prevPageButton, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(menuPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(scrollPane))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(nextPageButton, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sidePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sidePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(menuPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(nextPageButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(prevPageButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(scrollPane))))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    public void scrollToTop()
    {
        scrollPane.getVerticalScrollBar().setValue(0);
    }
    
    public void scrollToBottom()
    {
        javax.swing.JScrollBar vertical = scrollPane.getVerticalScrollBar();
        vertical.setValue(vertical.getMaximum());
    }
    
    public void scrollToMiddle()
    {
        scrollToBottom();
        scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getValue() / 2);
    }

    private void searchBarKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_searchBarKeyPressed
        if (!evt.isControlDown())
        {
            switch(evt.getKeyCode())
            {
                case KeyEvent.VK_ENTER : updateSearchResults();
                break;
            }
        }
    }//GEN-LAST:event_searchBarKeyPressed

    private void topPositionButtonKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_topPositionButtonKeyPressed
        switch (evt.getKeyCode())
        {
            case KeyEvent.VK_ENTER : scrollToTop();
            break;
        }
    }//GEN-LAST:event_topPositionButtonKeyPressed

    private void topPositionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_topPositionButtonActionPerformed
        scrollToTop();
    }//GEN-LAST:event_topPositionButtonActionPerformed

    private void midPositionButtonKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_midPositionButtonKeyPressed
        switch (evt.getKeyCode())
        {
            case KeyEvent.VK_ENTER : scrollToMiddle();
            break;
        }
    }//GEN-LAST:event_midPositionButtonKeyPressed

    private void midPositionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_midPositionButtonActionPerformed
        scrollToMiddle();
    }//GEN-LAST:event_midPositionButtonActionPerformed

    private void botPositionButtonKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_botPositionButtonKeyPressed
        switch (evt.getKeyCode())
        {
            case KeyEvent.VK_ENTER : scrollToBottom();
            break;
        }
    }//GEN-LAST:event_botPositionButtonKeyPressed

    private void botPositionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_botPositionButtonActionPerformed
        scrollToBottom();
    }//GEN-LAST:event_botPositionButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton botPositionButton;
    private javax.swing.JPanel menuPanel;
    private javax.swing.JButton midPositionButton;
    private javax.swing.JButton nextPageButton;
    private javax.swing.JPanel positioningPanel;
    private javax.swing.JButton prevPageButton;
    private javax.swing.JScrollPane scrollPane;
    private javax.swing.JTextField searchBar;
    private javax.swing.JPanel sidePanel;
    private javax.swing.JTextPane textPane;
    private javax.swing.JButton topPositionButton;
    private javax.swing.JComboBox<String> versionMenu;
    // End of variables declaration//GEN-END:variables
}
