/*
 * This file is part of Talc.
 * Copyright (C) 2007 Elliott Hughes <enh@jessies.org>.
 * 
 * Talc is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Talc is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jessies.talc;
/*
import e.gui.*;
import e.ptextarea.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class GuiCalc extends MainFrame {
    private Talc talc;
    private PTextArea historyArea;
    private JTextField textField;
    
    public GuiCalc() {
        super("GuiCalc");
        setSize(new Dimension(640, 480));
        
        setContentPane(makeUi());
        setJMenuBar(new GuiCalcMenuBar());
        
        talc = new Talc();
    }
    
    private JComponent makeUi() {
        historyArea = new PTextArea();
        historyArea.setEditable(false);
        
        textField = new JTextField("");
        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                evaluate(textField.getText());
            }
        });
        
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(historyArea), BorderLayout.CENTER);
        panel.add(textField, BorderLayout.SOUTH);
        return panel;
    }
    
    @Override
    public void setVisible(boolean newVisibility) {
        super.setVisible(newVisibility);
        if (newVisibility == true) {
            textField.requestFocusInWindow();
        }
    }
    
    public void evaluate(String input) {
        try {
            historyArea.append(input + "\n" + "= ");
            String output = talc.parseAndEvaluate(input);
            historyArea.append(output + "\n");
            historyArea.ensureVisibilityOfOffset(historyArea.getTextBuffer().length());
            textField.selectAll();
        } catch (Throwable th) {
            historyArea.append("error: " + StringUtilities.stackTraceFromThrowable(th));
        }
    }
    
    public void quit() {
        setVisible(false);
    }
    
    private class GuiCalcMenuBar extends JMenuBar {
        private GuiCalcMenuBar() {
            if (GuiUtilities.isMacOs() == false) {
                add(makeFileMenu());
            }
            add(makeEditMenu());
            add(makeHelpMenu());
        }
        
        private JMenu makeFileMenu() {
            JMenu menu = new JMenu("File");
            menu.add(new QuitAction());
            return menu;
        }
        
        private JMenu makeEditMenu() {
            JMenu menu = new JMenu("Edit");
            
            menu.add(new ClearHistoryAction());
            
            // FIXME: these aren't really right unless our whole UI is PTextArea.
            //menu.add(PActionFactory.makeUndoAction());
            //menu.add(PActionFactory.makeRedoAction());
            //menu.addSeparator();
            //menu.add(PActionFactory.makeCutAction());
            //menu.add(PActionFactory.makeCopyAction());
            //menu.add(PActionFactory.makePasteAction());
            
            if (GuiUtilities.isMacOs() == false) {
                //menu.add(new PreferencesAction());
            }
            return menu;
        }
        
        private JMenu makeHelpMenu() {
            HelpMenu helpMenu = new HelpMenu();
            return helpMenu.makeJMenu();
        }
        
        private class ClearHistoryAction extends AbstractAction {
            private ClearHistoryAction() {
                super("Clear History");
                putValue(ACCELERATOR_KEY, GuiUtilities.makeKeyStroke("K", false));
                GnomeStockIcon.configureAction(this);
            }
            
            public void actionPerformed(ActionEvent e) {
                historyArea.setText("");
            }
        }
        
        private class QuitAction extends AbstractAction {
            private QuitAction() {
                super("Quit");
                putValue(ACCELERATOR_KEY, GuiUtilities.makeKeyStroke("Q", false));
                GnomeStockIcon.configureAction(this);
            }
            
            public void actionPerformed(ActionEvent e) {
                quit();
            }
        }
    }
    
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                GuiUtilities.initLookAndFeel();
                new GuiCalc().setVisible(true);
            }
        });
    }
}
*/
