/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package frames;

import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.HashMap;

/**
 *
 * @author andrewd12
 */
public class MainFrame extends javax.swing.JFrame {

    static final String IDLE_TERMINAL_MESSAGE = "Terminal (Ctl + T)";
    
    TableStruct tables;
    
    java.util.ArrayList<panels.Reader> panelsList = new java.util.ArrayList<>();
    
    java.awt.event.KeyListener commonKeyListener = new java.awt.event.KeyListener() {
        @Override
        public void keyTyped(KeyEvent ke) {}
        
        @Override
        public void keyPressed(KeyEvent ke) {
            pressKey(ke);
        }
        
        @Override
        public void keyReleased(KeyEvent ke) {}
    };
    
    java.awt.event.KeyListener greenMeansGo = new java.awt.event.KeyListener() {
        @Override
        public void keyTyped(KeyEvent ke) {}
        
        @Override
        public void keyPressed(KeyEvent ke) {
            switch(ke.getKeyCode())
            {
                case KeyEvent.VK_ENTER : insertNewTab();
                break;
            }
        }
        
        @Override
        public void keyReleased(KeyEvent ke) {}
    };
    
    java.awt.event.KeyListener redMeansItsTimeToStop = new java.awt.event.KeyListener() {
        @Override
        public void keyTyped(KeyEvent ke) {}
        
        @Override
        public void keyPressed(KeyEvent ke) {
            switch(ke.getKeyCode())
            {
                case KeyEvent.VK_ENTER : removeCurrentTab();
                break;
            }
        }
        
        @Override
        public void keyReleased(KeyEvent ke) {}
    };
    
    /**
     * Creates new form MainFrame
     */
    public MainFrame()
    {
        initComponents();
        tables = new TableStruct();
        jTextField3.setText(IDLE_TERMINAL_MESSAGE);
        insertNewTab(0);
        
        pack();
        
        this.addKeyListener(commonKeyListener);
        jTextField3.addKeyListener(commonKeyListener);
        
        jTextField3.addFocusListener(new java.awt.event.FocusListener() {
            @Override
            public void focusGained(FocusEvent fe) {
                jTextField3.setForeground(new Color(255, 255, 255));
                jTextField3.setBackground(new Color(0, 0, 0));
                jTextField3.setText("");
            }
            
            @Override
            public void focusLost(FocusEvent fe) {
                jTextField3.setForeground(new Color(102, 102, 102));
                jTextField3.setBackground(new Color(255, 255, 255));
                if (jTextField3.getText().length() == 0)
                    jTextField3.setText(IDLE_TERMINAL_MESSAGE);
            }
        });
        
        jTabbedPane1.addKeyListener(commonKeyListener);
        jTabbedPane1.addMouseListener(new java.awt.event.MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                javax.swing.JTabbedPane source = (javax.swing.JTabbedPane) e.getSource();
                if (source.getSelectedIndex() == source.getTabCount() - 1)
                {
                    insertNewTab(source.getTabCount() - 1);
                    source.setSelectedIndex(source.getTabCount() - 2);
                }
            }
            
            @Override
            public void mousePressed(MouseEvent e) {}
            
            @Override
            public void mouseReleased(MouseEvent e) {}
            
            @Override
            public void mouseEntered(MouseEvent e) {}
            
            @Override
            public void mouseExited(MouseEvent e) {}
        });
        
        jTabbedPane1.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_LEFT)
                {
                    e.consume();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {}
        });
        jTabbedPane1.addTab(" +  ", new javax.swing.JPanel());
        jTabbedPane1.setFocusable(false);
        
//        addNewTabButton();
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        jTextField3 = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jTextField3.setForeground(new java.awt.Color(102, 102, 102));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTabbedPane1)
                    .addComponent(jTextField3, javax.swing.GroupLayout.DEFAULT_SIZE, 361, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(29, 29, 29)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 397, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    
//    private void addNewTabButton()
//    {
//        panels.SmallButton newTabButton = new panels.SmallButton("");
//        newTabButton.addActionListener(e -> {
//            insertNewTab(jTabbedPane1.getTabCount() - 1);
//            jTabbedPane1.setSelectedIndex(jTabbedPane1.getTabCount() - MINIMUM_NUMBER_TABS);
//            jTabbedPane1.getSelectedComponent().requestFocus();
//        });
//        newTabButton.addFocusListenerToButton(new java.awt.event.FocusListener() {
//            @Override
//            public void focusGained(FocusEvent e)
//            {
//                jTabbedPane1.setSelectedIndex(jTabbedPane1.getTabCount() - MINIMUM_NUMBER_TABS);
//                jTabbedPane1.getSelectedComponent().requestFocus();
//            }
//            
//            @Override
//            public void focusLost(FocusEvent e) {}
//        });
//        jTabbedPane1.addTab("", new javax.swing.JPanel());
//        jTabbedPane1.setTabComponentAt(jTabbedPane1.getTabCount() - 1, newTabButton);
//    }
    
    private panels.Reader getCurrentlySelectedTab()
    {
        return (panels.Reader) jTabbedPane1.getSelectedComponent();
    }
    
    public interface Responder
    {
        public abstract void respond(KeyEvent e);
    }
    
    public class TableStruct
    {
        public final HashMap<Integer, Responder> CONTROL_DOWN_KEY_MAP = newControlDownKeyMap();
        public final HashMap<Integer, Responder> SHIFT_DOWN_KEY_MAP = newShiftDownKeyMap();
        public final HashMap<Integer, Responder> ALT_DOWN_KEY_MAP = newAltDownKeyMap();
        public final HashMap<Integer, Responder> NEUTRAL_KEY_MAP = newNeutralKeyMap();
        
        public HashMap<Integer, Responder> newControlDownKeyMap()
        {
            HashMap<Integer, Responder> map = new HashMap<>();
            
            map.put(KeyEvent.VK_PAGE_UP, e -> {e.consume(); goUp();});
            map.put(KeyEvent.VK_PAGE_DOWN, e -> {e.consume(); goDown();});
            map.put(KeyEvent.VK_N, e -> insertNewTab());
            map.put(KeyEvent.VK_W, e -> removeCurrentTab());
            map.put(KeyEvent.VK_F, e -> getCurrentlySelectedTab().getSearchBar().requestFocus());
            map.put(KeyEvent.VK_T, e -> jTextField3.requestFocus());
            map.put(KeyEvent.VK_HOME, e -> getCurrentlySelectedTab().scrollToTop());
            map.put(KeyEvent.VK_END, e -> getCurrentlySelectedTab().scrollToBottom());
            
            return map;
        }
        
        public HashMap<Integer, Responder> newShiftDownKeyMap()
        {
            HashMap<Integer, Responder> map = new HashMap<>();
            
            map.put(KeyEvent.VK_F3, e -> {
                panels.Reader panel = getCurrentlySelectedTab();
                panel.getTextPane().requestFocus();
                panel.searchData().moveCaretToPreviousSearchResult();
            });
            
            return map;
        }
        
        public HashMap<Integer, Responder> newAltDownKeyMap()
        {
            HashMap<Integer, Responder> map = new HashMap<>();
            
            map.put(KeyEvent.VK_RIGHT, e -> getCurrentlySelectedTab().goForward());
            map.put(KeyEvent.VK_LEFT, e -> getCurrentlySelectedTab().goBack());
            
            return map;
        }
        
        public HashMap<Integer, Responder> newNeutralKeyMap()
        {
            HashMap<Integer, Responder> map = new HashMap<>();
            
            map.put(KeyEvent.VK_PAGE_UP, e -> getCurrentlySelectedTab().scrollUp());
            map.put(KeyEvent.VK_PAGE_DOWN, e -> getCurrentlySelectedTab().scrollDown());
            map.put(KeyEvent.VK_F3, e -> {
                panels.Reader panel = getCurrentlySelectedTab();
                panel.getTextPane().requestFocus();
                panel.searchData().moveCaretToNextSearchResult();
            });

            return map;
        }
    }
    
    private void pressKey(KeyEvent ke)
    {
        if (ke.isControlDown())
        {
            tables.CONTROL_DOWN_KEY_MAP.getOrDefault(ke.getKeyCode(), e -> {}).respond(ke);
        }
        else if (ke.isShiftDown())
        {
            tables.SHIFT_DOWN_KEY_MAP.getOrDefault(ke.getKeyCode(), e -> {}).respond(ke);
        }
        else if (ke.isAltDown())
        {
            tables.ALT_DOWN_KEY_MAP.getOrDefault(ke.getKeyCode(), e -> {}).respond(ke);
        }
        else
        {
            tables.NEUTRAL_KEY_MAP.getOrDefault(ke.getKeyCode(), e -> {}).respond(ke);
        }
    }
    
    private void goUp(int offset)
    {
        int currentIndex = jTabbedPane1.getSelectedIndex();
        jTabbedPane1.setSelectedIndex(currentIndex == 0 ? jTabbedPane1.getTabCount() - offset : currentIndex - 1);
    }
    
    private void goDown(int offset)
    {
        int currentIndex = jTabbedPane1.getSelectedIndex();
        jTabbedPane1.setSelectedIndex(currentIndex == jTabbedPane1.getTabCount() - offset ? 0 : currentIndex + 1);
    }
    
    private void goUp()
    {
        goUp(MINIMUM_NUMBER_TABS);
    }
    
    private void goDown()
    {
        goDown(MINIMUM_NUMBER_TABS);
    }
    
    static final int MINIMUM_NUMBER_TABS = 2;
    
    private void removeCurrentTab()
    {    
        int currentIndex = jTabbedPane1.getSelectedIndex();
        
        jTabbedPane1.removeTabAt(currentIndex);
        panelsList.remove(currentIndex);
        
        if (jTabbedPane1.getTabCount() == MINIMUM_NUMBER_TABS - 1)
        {
            insertNewTab(0);
        }
        
        if (jTabbedPane1.getSelectedIndex() == jTabbedPane1.getTabCount() - 1)
        {
            jTabbedPane1.setSelectedIndex(jTabbedPane1.getSelectedIndex() - 1);
        }
    }
    
    private void insertNewTab()
    {
        insertNewTab(jTabbedPane1.getTabCount() - 1);
    }
    
    private void setTabName(javax.swing.JTabbedPane j, String address)
    {
        panels.Reader r = getCurrentlySelectedTab();
        r.setName(String.format("[%s] %s", r.getVersionMenu().getSelectedItem(), address));
    }
    
    private String newTabName(panels.Reader r, String address)
    {
        return String.format("[%s] %s", r.getVersionMenu().getSelectedItem(), address);
    }
    
    private void insertNewTab(int index)
    {
        panels.Reader what = new panels.Reader();
        panels.TabBar customTabComponent = new panels.TabBar(newTabName(what, what.getCurrentAddress()), "");
        
        customTabComponent.addTabCloseKeyListener(redMeansItsTimeToStop);
        customTabComponent.addTabCloseActionListener(e -> removeCurrentTab());
        
        what.addCurrentAddressListener((currentAdddress) -> {
            panels.Reader r = getCurrentlySelectedTab();
            ((panels.TabBar) jTabbedPane1.getTabComponentAt(jTabbedPane1.getSelectedIndex())).setLabelText(newTabName(r, currentAdddress));
        });
        
        what.addKeyListener(commonKeyListener);
        
        for (java.awt.Component thing : what.getThings())
        {
            thing.addKeyListener(commonKeyListener);
        }
        
        what.getSearchBar().addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}
            
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.isControlDown())
                {
                    switch (e.getKeyCode())
                    {
                        case KeyEvent.VK_ENTER :
                            
                            String text = what.getSearchBar().getText();
                            insertNewTab(jTabbedPane1.getSelectedIndex() + 1);
                            goDown();
                            panels.Reader panel = getCurrentlySelectedTab();
                            panel.getSearchBar().setText(text);
                            panel.updateSearchResults();
                            break;
                            
                        default: break;
                    }
                }
            }
            
            @Override
            public void keyReleased(KeyEvent e) {}
        });
        panelsList.add(index, what);
        
        jTabbedPane1.insertTab("", null, what, "New Tab", index);
        jTabbedPane1.setTabComponentAt(index, customTabComponent);
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextField jTextField3;
    // End of variables declaration//GEN-END:variables
}
