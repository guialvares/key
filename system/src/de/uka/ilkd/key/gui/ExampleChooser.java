// This file is part of KeY - Integrated Deductive Software Design
// Copyright (C) 2001-2011 Universitaet Karlsruhe, Germany
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General Public License. 
// See LICENSE.TXT for details.
//
//

package de.uka.ilkd.key.gui;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public final class ExampleChooser extends JDialog {
    
    /**
     * 
     */
    private static final long serialVersionUID = -4405666868752394532L;
    private static final String KEY_FILE_NAME = "project.key";
    private static final String README_NAME = "README.txt";
    
    private static ExampleChooser instance;
    
    private final JList exampleList;  
    private final JTextArea descriptionText;    
    private final JButton loadButton;
    private final JButton cancelButton;
    
    private boolean success = false;
    
    
    //-------------------------------------------------------------------------
    //constructors
    //-------------------------------------------------------------------------

    private ExampleChooser(File examplesDir) {
	super(MainWindow.getInstance(), "Load Example", true);
	assert examplesDir != null;
	assert examplesDir.isDirectory();
	
	//create list panel
	final JPanel listPanel = new JPanel();
	listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.X_AXIS));
	getContentPane().add(listPanel);
	
	//create example list
	final DefaultListModel model = new DefaultListModel();
	File[] examples = examplesDir.listFiles();
        Arrays.sort(examples, new Comparator<File> () {
            public int compare(File f1, File f2) {
                return f1.getName().compareToIgnoreCase(f2.getName());
            }
        });
	for(File example : examples) {
	    if(example.isDirectory()) {
		final File keyfile = new File(example, KEY_FILE_NAME);
		if(keyfile.isFile()) {
		    model.addElement(example);
		}
	    }
	}
	exampleList = new JList();
	exampleList.setModel(model);
	exampleList.addListSelectionListener(
		new ListSelectionListener() {
		    public void valueChanged(ListSelectionEvent e) {
			updateDescription();
		    }
		});	
	exampleList.addMouseListener(new MouseAdapter() {
	    public void mouseClicked(MouseEvent e){
		if(e.getClickCount() == 2){
		    loadButton.doClick();
		}
	    }
	});	
	final JScrollPane exampleScrollPane = new JScrollPane(exampleList);
	exampleScrollPane.setBorder(new TitledBorder("Examples"));
	final Dimension exampleListDim = new Dimension(500, 400);	
	exampleScrollPane.setPreferredSize(exampleListDim);
	exampleScrollPane.setMinimumSize(exampleListDim);
	listPanel.add(exampleScrollPane);
	
	//create description label
	descriptionText = new JTextArea();
	descriptionText.setEditable(false);
	descriptionText.setLineWrap(true);
	descriptionText.setWrapStyleWord(true);
	final JScrollPane descriptionScrollPane 
		= new JScrollPane(descriptionText);
	descriptionScrollPane.setBorder(new TitledBorder("Description"));
	final Dimension descriptionLabelDim = new Dimension(500, 400);	
	descriptionScrollPane.setPreferredSize(descriptionLabelDim);
	descriptionScrollPane.setMinimumSize(descriptionLabelDim);	
	listPanel.add(descriptionScrollPane);
	
	//create button panel
	final JPanel buttonPanel = new JPanel();
	buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 5));
	final Dimension buttonDim = new Dimension(140, 27);
        buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 
                                                 (int)buttonDim.getHeight() 
                                                     + 10));
	getContentPane().add(buttonPanel);	
	
	//create "load" button
	loadButton = new JButton("Load Example");
	loadButton.setPreferredSize(buttonDim);
	loadButton.setMinimumSize(buttonDim);
	loadButton.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) { 
		success = true;
		setVisible(false);
	    }
	});
	buttonPanel.add(loadButton);
	getRootPane().setDefaultButton(loadButton);

	//create "cancel" button
	cancelButton = new JButton("Cancel");
	cancelButton.setPreferredSize(buttonDim);
	cancelButton.setMinimumSize(buttonDim);
	cancelButton.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		success = false;
		setVisible(false);
	    }
	});
	buttonPanel.add(cancelButton);
        ActionListener escapeListener = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if(event.getActionCommand().equals("ESC")) {
                    cancelButton.doClick();
                }
            }
        };
        cancelButton.registerKeyboardAction(
                            escapeListener,
                            "ESC",
                            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                            JComponent.WHEN_IN_FOCUSED_WINDOW);	

        //select first example, or disable load button
	if(model.size() == 0) {
	    loadButton.setEnabled(false);
	} else {
	    exampleList.setSelectedIndex(0);
	}
	
	//show
        getContentPane().setLayout(new BoxLayout(getContentPane(), 
                                                 BoxLayout.Y_AXIS));	
	pack();
	setLocation(20, 20);
    }	
    
    
    
    //-------------------------------------------------------------------------
    //internal methods
    //-------------------------------------------------------------------------    
    
    private static File lookForExamples() {
        final ClassLoader loader = ExampleChooser.class.getClassLoader();
        String path = loader.getResource(".").getPath();
        if(path.startsWith("file:")) {
            path = path.substring("file:".length());
        }
        int i = path.lastIndexOf("/");
        while(i > 0) {
            path = path.substring(0, i);
            final String resultPath = path + "/examples/heap";
            final File result = new File(resultPath);
            if(result.isDirectory()) {
        	return result;
            }
            i = path.lastIndexOf("/");
        }
    	return null;
    }
    
    
    private void updateDescription() {
	final File selectedExample = (File) exampleList.getSelectedValue();
	final File readme = new File(selectedExample, README_NAME);
	if(readme.isFile()) {
	    try {
		final BufferedReader br 
			= new BufferedReader(new FileReader(readme));
		final StringBuilder sb = new StringBuilder();
	        final String ls = System.getProperty("line.separator");
	        String line;
	        while((line = br.readLine()) != null) {
	            sb.append(line);
	            sb.append(ls);
	        }
	        descriptionText.setText(sb.toString());
	        descriptionText.getCaret().setDot(0);
	    } catch(IOException e) {
		descriptionText.setText("Reading description from "
			                 + "README file failed.");
	    }
	} else {
	    descriptionText.setText("No description available.");
	}
    }
    
    
    //-------------------------------------------------------------------------
    //public interface
    //-------------------------------------------------------------------------
    
    /**
     * Shows the dialog, using the passed examples directory. If null is passed,
     * tries to find examples directory on its own.
     */
    public static File showInstance(String examplesDirString) {
	//get examples directory
	final File examplesDir;
	if(examplesDirString == null) {
	    examplesDir = lookForExamples();
	} else {
	    examplesDir = new File(examplesDirString);
	}
	if(examplesDir == null || !examplesDir.isDirectory()) {
	    return null;
	}	    
	
	//show dialog
	if(instance == null) {
	    instance = new ExampleChooser(examplesDir);
	}
	instance.setVisible(true);
	
	//return result
	final File result = instance.success 
        		    ? new File((File)instance.exampleList
        			                     .getSelectedValue(), 
        			       KEY_FILE_NAME) 
        	            : null;	
	return result;
    }
}