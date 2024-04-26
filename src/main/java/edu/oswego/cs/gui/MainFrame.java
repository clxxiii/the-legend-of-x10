package edu.oswego.cs.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class MainFrame extends JFrame {

    JPanel mainPanel;

    public MainFrame() {
        setTitle("The Legend of X10");


        int borderSpacing = 10;
        String titleFontFamily = "Serif";

        //The big cheese
        mainPanel = new JPanel(new BorderLayout());
        add(mainPanel);

        //Panel with header text
        JPanel headerPanel = new JPanel(new GridBagLayout());
        JLabel header = new JLabel("~ The Legend of X10 ~");
        header.setFont(new Font(titleFontFamily, Font.BOLD, 50));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(borderSpacing, borderSpacing, borderSpacing, borderSpacing));
        headerPanel.add(header);

        //Panel with output stuff - world events and the map
        JPanel outputPanel = new JPanel(new GridLayout(1, 2));

        //World text stuff - world events go here
        //TODO: Consider different font stuff.  Maybe color or something.
        JPanel worldTextPanel = new JPanel();       //Just a container, really
        JTextArea outputText = new JTextArea("Output Text", 30, 40);
        outputText.setLineWrap(true);
        outputText.setEditable(false);
        worldTextPanel.add(outputText);

        //Set up a scroll pane so we can, well, scroll.  Stick the world panel into it
        JScrollPane worldTextScrollPane =
                new JScrollPane(worldTextPanel,
                                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                );

        //Map goes here
        JPanel mapPanel = new JPanel();     //Nother container
        JTextArea mapOutput = new JTextArea("Map Area", 30, 40);
        mapOutput.setEditable(false);
        mapPanel.add(mapOutput);

        outputPanel.setBorder(BorderFactory.createEmptyBorder(borderSpacing, borderSpacing, borderSpacing, borderSpacing));
        outputPanel.add(worldTextScrollPane);
        outputPanel.add(mapPanel);

        //Input panel for user commands
        JPanel inputPanel = new JPanel(new BorderLayout());

        JTextField textField = new JTextField("Input");
        textField.setFont(new Font(textField.getFont().getName(), Font.ITALIC, textField.getFont().getSize()));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(borderSpacing, borderSpacing, borderSpacing, borderSpacing));
        inputPanel.add(textField, BorderLayout.CENTER);

        //Add it all to the main panel
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(outputPanel, BorderLayout.CENTER);
        mainPanel.add(inputPanel, BorderLayout.SOUTH);

        //This way the exit button actually exits :D
        //TODO: Will likely need to perform an equivalent of the ".exit" command for raft
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //I guess this sort of thing isn't recommended but might change my mind later
//        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
//        double screenWidth = screenSize.getWidth();
//        double screenHeight = screenSize.getHeight();
//        setSize((int) (screenWidth / 2), (int) (screenHeight / 2));

        //Action listener stuff
        textField.addActionListener(inputFieldAction);

        //Final prep
        setResizable(false);                //Looks gross maximized otherwise
        pack();                             //Packs everything together to fit whatever size the components are
        setLocationRelativeTo(null);        //Sticks the window in the middle
        setVisible(true);                   //What it says on the tin
    }

    Action inputFieldAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            System.out.println("Huzzah!");
        }
    };

}
