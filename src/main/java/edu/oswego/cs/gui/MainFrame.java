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
        JPanel worldTextPanel = new JPanel();
        JTextArea outputText = new JTextArea("Output Text", 30, 40);
        outputText.setLineWrap(true);
        outputText.setEditable(false);
        worldTextPanel.add(outputText);

        //Setup a scroll pane so we can, well, scroll.
        JScrollPane worldTextScrollPane =
                new JScrollPane(worldTextPanel,
                                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                );

        //Map goes here
        JPanel mapPanel = new JPanel();
        JTextArea mapOutput = new JTextArea("Map Area", 30, 40);
        mapOutput.setEditable(false);
        mapPanel.add(mapOutput);

        outputPanel.setBorder(BorderFactory.createEmptyBorder(borderSpacing, borderSpacing, borderSpacing, borderSpacing));
        outputPanel.add(worldTextScrollPane);
        outputPanel.add(mapPanel);

        //Input panel for user commands
        JPanel inputPanel = new JPanel(new BorderLayout());

        JTextField textField = new JTextField("Input");
        textField.addActionListener(inputFieldAction);
        textField.setFont(new Font(textField.getFont().getName(), Font.ITALIC, textField.getFont().getSize()));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(borderSpacing, borderSpacing, borderSpacing, borderSpacing));
        inputPanel.add(textField, BorderLayout.CENTER);

        //Add it all to the main panel
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(outputPanel, BorderLayout.CENTER);
        mainPanel.add(inputPanel, BorderLayout.SOUTH);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

//        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
//        double screenWidth = screenSize.getWidth();
//        double screenHeight = screenSize.getHeight();

        //setSize((int) (screenWidth / 2), (int) (screenHeight / 2));

        setResizable(false);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    Action inputFieldAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            System.out.println("Huzzah!");
        }
    };

}
