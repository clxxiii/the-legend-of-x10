package edu.oswego.cs.gui;

import edu.oswego.cs.client.Command;
import edu.oswego.cs.dungeon.Dungeon;
import edu.oswego.cs.dungeon.Entity;
import edu.oswego.cs.dungeon.Floor;
import edu.oswego.cs.dungeon.GameUser;
import edu.oswego.cs.raft.Raft;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Optional;

public class MainFrame extends JFrame {

    JPanel mainPanel;
    JTextArea outputText;
    JTextArea mapOutput;
    JTextField inputField;
    ArrayList<String> messages = new ArrayList<>();
    int lastMessageAccessed = -1;
    private Raft raft;

    public MainFrame() { }

    public void setRaft(Raft raft) {
        this.raft = raft;
    }

    public void initialize(String username, String roomNumber, Floor currentFloor) {
        setTitle("The Legend of X10");


        int borderSpacing = 10;
        int inputFieldCharacterLimit = 256;
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

        //OUTPUT

        //World text stuff - world events go here
        outputText = new JTextArea( 30, 40);
        outputText.setLineWrap(true);
        outputText.setEditable(false);

        messages.add("Welcome to the dungeon, " + username + "!");
        messages.add("Current room: " + roomNumber + "!");

        updateOutputBox();

        JPanel worldTextPanel = new JPanel();       //Just a container, really
        worldTextPanel.add(outputText);

        //Set up a scroll pane so we can, well, scroll.  Stick the world panel into it
        JScrollPane worldTextScrollPane =
                new JScrollPane(
                        worldTextPanel,
                        JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                );

        //Map goes here
        mapOutput = new JTextArea("Map Area", 30, 40);
        //NOTE: The font NEEDS to be a fixed-width font like Courier otherwise the map won't print right.
        mapOutput.setFont(new Font("Courier", Font.PLAIN, 12));

        //Smaller maps get wedged into a corner, but it looks like to reformat that we'll have to
        //adjust the toString() method itself.
        if(currentFloor != null) mapOutput.setText(currentFloor.toString());

        mapOutput.setEditable(false);

        JPanel mapPanel = new JPanel();     //Nother container
        mapPanel.add(mapOutput);

        JPanel outputPanel = new JPanel(new GridLayout(1, 2));
        outputPanel.setBorder(BorderFactory.createEmptyBorder(borderSpacing, borderSpacing, borderSpacing, borderSpacing));
        outputPanel.add(worldTextScrollPane);
        outputPanel.add(mapPanel);

        //Input panel for user commands
        JLabel inputLabel = new JLabel("Input: ");

        inputField = new JTextField();
        inputField.setFont(new Font(inputField.getFont().getName(), Font.ITALIC, inputField.getFont().getSize()));
        inputField.setDocument(new JTextFieldLimit(inputFieldCharacterLimit));

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(BorderFactory.createEmptyBorder(borderSpacing, borderSpacing, borderSpacing, borderSpacing));
        inputPanel.add(inputLabel, BorderLayout.WEST);
        inputPanel.add(inputField, BorderLayout.CENTER);

        //Add it all to the main panel
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(outputPanel, BorderLayout.CENTER);
        mainPanel.add(inputPanel, BorderLayout.SOUTH);

        //This way the exit button actually exits :D
        //TODO: Will likely need to perform an equivalent of the ".exit" command for raft
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Action listener stuff
        inputField.addActionListener(inputFieldAction);

        //Final prep
        setResizable(false);                //Looks gross maximized otherwise
        pack();                             //Packs everything together to fit whatever size the components are
        setLocationRelativeTo(null);        //Sticks the window in the middle
        setVisible(true);                   //What it says on the tin
    }


    Action inputFieldAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {

            String inputText = inputField.getText();

            if(inputText.isEmpty()) return;
            if(raft == null) {
                inputField.setText("");
                addMessage("Raft is not initialized yet.");
            }

            String[] chunked = inputText.split(" ");

            Optional<Command> optionalCommand = Command.parse(chunked[0].toLowerCase());
            //TODO: These should be in the enums for Raft, but I don't want to muck with that without Dave
            if (optionalCommand.isPresent()) {
                Command command = optionalCommand.get();
                switch (command) {
                    case CHAT:
                        if (chunked.length == 1) {
                            messages.add("Nothing to say!");
                            break;
                        }

                        raft.sendMessage(inputText);
                        break;

                    case EXIT:
                        raft.exitRaft();
                        System.exit(0);
                        break;

                    case MOVE:
                        if (chunked.length == 1) {
                            messages.add("Nowhere to go!"); //TODO: List available exits
                            break;
                        }

                        raft.sendMessage(inputText);

                        break;

                    default:
                        messages.add("Didn't understand that!");
                }
            } else {
                messages.add("Didn't understand that!");
            }

            inputField.setText("");
            updateOutputBox();

        }
    };

    /**
     * Updates the output box with the latest world messages.
     */
    public void updateOutputBox() {
        if(lastMessageAccessed == messages.size()) return;
        if(lastMessageAccessed == -1) lastMessageAccessed = 0;

        for(int i = lastMessageAccessed; i < messages.size(); i++) {
            outputText.append(messages.get(i) + "\n");
            lastMessageAccessed++;
        }
    }

    /**
     * Call this when the floor changes so we have an updated map showing in the panel.
     */
    public void updateMapOutput(Floor floor) {
        mapOutput.setText(floor.toString());
    }

    public void listRoomEnemies(GameUser user) {
        if(user.currentRoom.entities.isEmpty()) return;

        for(Entity entity: user.currentRoom.entities) {
            addMessage("Enemy in room! " + entity.name + " spawned in!");
        }
    }

    public void addMessage(String message) {
        messages.add(message);
        updateOutputBox();
    }

}
