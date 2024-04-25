package edu.oswego.cs.gui;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    JPanel mainPanel;

    public MainFrame() {
        setTitle("The Legend of X10");



        mainPanel = new JPanel(new GridLayout(3,1));
        add(mainPanel);

        JPanel headerPanel = new JPanel(new GridBagLayout());
        JLabel header = new JLabel("Legend of X10");
        header.setFont(new Font("Serif", Font.BOLD, 50));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        headerPanel.add(header);

        JTextArea outputText = new JTextArea("ghj");
        outputText.setEditable(false);

        JPanel inputPanel = new JPanel(new BorderLayout());

        JTextField textField = new JTextField("Beep 2");
        inputPanel.add(textField, BorderLayout.CENTER);

        mainPanel.add(headerPanel);
        mainPanel.add(outputText);
        mainPanel.add(inputPanel);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        double screenWidth = screenSize.getWidth();
        double screenHeight = screenSize.getHeight();

        setSize((int) (screenWidth / 2), (int) (screenHeight / 2));
        setLocationRelativeTo(null);
        setVisible(true);
    }

}
