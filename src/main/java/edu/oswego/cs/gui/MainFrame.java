package edu.oswego.cs.gui;

import edu.oswego.cs.dungeon.Dungeon;
import edu.oswego.cs.dungeon.Entity;
import edu.oswego.cs.dungeon.Floor;
import edu.oswego.cs.dungeon.GameUser;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

public class MainFrame extends JFrame {

    JPanel mainPanel;
    JTextArea outputText;
    JTextArea mapOutput;
    JTextField inputField;
    ArrayList<String> messages = new ArrayList<>();
    int lastMessageAccessed = -1;

    Dungeon dungeon;
    Floor currentFloor;
    GameUser user;

    public MainFrame(Dungeon dungeon, Floor currentFloor, GameUser user) {
        this.dungeon = dungeon;
        this.currentFloor = currentFloor;
        this.user = user;
        initialize();
    }

    public MainFrame() {
        initialize();
    }

    public void initialize() {
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

        messages.add("Welcome to the dungeon, " + user.username + "!");
        messages.add("Current room: " + user.getRoomNumber());

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
        if(currentFloor != null) mapOutput.setText(currentFloor.toString());

        //TODO: This is dummy data.  Use floor.toString() when debug info is cleared out
        //Smaller maps get wedged into a corner, but it looks like to reformat that we'll have to
        //adjust the toString() method itself.
        String mapText =
                "     ┌───┐     ┌───┐\n" +
                "     │008│     │004│\n" +
                "     └─ ─┘     └─ ─┘\n" +
                "┌───┐┌─ ─┐┌───┐┌─ ─┐\n" +
                "│009  003  000  001│\n" +
                "└───┘└─ ─┘└─ ─┘└─ ─┘\n" +
                "     ┌─ ─┐┌─ ─┐┌─ ─┐\n" +
                "     │007  002  005│\n" +
                "     └───┘└─ ─┘└───┘\n" +
                "          ┌─ ─┐     \n" +
                "          │006│     \n" +
                "          └───┘";

        String bigMapText =
                        "                                             ┌───┐┌───┐┌───┐                    \n" +
                        "                                             │097  074  096│                    \n" +
                        "                                             └───┘└─ ─┘└─ ─┘                    \n" +
                        "                                   ┌───┐          ┌─ ─┐┌─ ─┐┌───┐               \n" +
                        "                                   │077│          │056││075││098│               \n" +
                        "                                   └─ ─┘          └─ ─┘└─ ─┘└─ ─┘               \n" +
                        "                              ┌───┐┌─ ─┐┌───┐     ┌─ ─┐┌─ ─┐┌─ ─┐┌───┐          \n" +
                        "                              │079││058  078│     │044  057  076  099│          \n" +
                        "                              └─ ─┘└─ ─┘└───┘     └─ ─┘└─ ─┘└───┘└───┘          \n" +
                        "               ┌───┐          ┌─ ─┐┌─ ─┐┌───┐┌───┐┌─ ─┐┌─ ─┐     ┌───┐          \n" +
                        "               │091│          │059  046  036││030  035  045│     │082│          \n" +
                        "               └─ ─┘          └─ ─┘└─ ─┘└─ ─┘└─ ─┘└───┘└─ ─┘     └─ ─┘          \n" +
                        "          ┌───┐┌─ ─┐┌───┐┌───┐┌─ ─┐┌─ ─┐┌─ ─┐┌─ ─┐     ┌─ ─┐┌───┐┌─ ─┐          \n" +
                        "          │093  070  053  069││080││037  031  024│     │062  048  061│          \n" +
                        "          └─ ─┘└─ ─┘└─ ─┘└─ ─┘└─ ─┘└─ ─┘└─ ─┘└─ ─┘     └─ ─┘└─ ─┘└─ ─┘          \n" +
                        "          ┌─ ─┐┌─ ─┐┌─ ─┐┌─ ─┐┌─ ─┐┌─ ─┐┌─ ─┐┌─ ─┐┌───┐┌─ ─┐┌─ ─┐┌─ ─┐┌───┐┌───┐\n" +
                        "          │072  092││041││081  060  047││025  017││026  032  038  049  063  083│\n" +
                        "          └─ ─┘└───┘└─ ─┘└───┘└─ ─┘└─ ─┘└─ ─┘└─ ─┘└─ ─┘└─ ─┘└─ ─┘└─ ─┘└─ ─┘└───┘\n" +
                        "┌───┐┌───┐┌─ ─┐┌───┐┌─ ─┐┌───┐┌─ ─┐┌─ ─┐┌─ ─┐┌─ ─┐┌─ ─┐┌─ ─┐┌─ ─┐┌─ ─┐┌─ ─┐     \n" +
                        "│095  073  055  043  034  029  022  015  019││010││018  039  050  064  084│     \n" +
                        "└───┘└───┘└─ ─┘└─ ─┘└─ ─┘└───┘└───┘└─ ─┘└─ ─┘└─ ─┘└─ ─┘└───┘└─ ─┘└─ ─┘└───┘     \n" +
                        "          ┌─ ─┐┌─ ─┐┌─ ─┐          ┌─ ─┐┌─ ─┐┌─ ─┐┌─ ─┐┌───┐┌─ ─┐┌─ ─┐          \n" +
                        "          │071  054  042│          │008  012  004  011││087  065││085│          \n" +
                        "          └─ ─┘└───┘└─ ─┘          └─ ─┘└─ ─┘└─ ─┘└───┘└───┘└─ ─┘└───┘          \n" +
                        "          ┌─ ─┐     ┌─ ─┐┌───┐┌───┐┌─ ─┐┌─ ─┐┌─ ─┐          ┌─ ─┐               \n" +
                        "          │094│     │023  016  009  003  000  001│          │086│               \n" +
                        "          └───┘     └─ ─┘└─ ─┘└─ ─┘└─ ─┘└─ ─┘└─ ─┘          └───┘               \n" +
                        "                    ┌─ ─┐┌─ ─┐┌─ ─┐┌─ ─┐┌─ ─┐┌─ ─┐                              \n" +
                        "                    │028  021  014  007  002  005│                              \n" +
                        "                    └───┘└─ ─┘└─ ─┘└───┘└─ ─┘└───┘                              \n" +
                        "          ┌───┐┌───┐┌───┐┌─ ─┐┌─ ─┐     ┌─ ─┐                                   \n" +
                        "          │088  066││051││027  020│     │006│                                   \n" +
                        "          └───┘└─ ─┘└─ ─┘└─ ─┘└───┘     └─ ─┘                                   \n" +
                        "          ┌───┐┌─ ─┐┌─ ─┐┌─ ─┐          ┌─ ─┐                                   \n" +
                        "          │068  052  040  033│          │013│                                   \n" +
                        "          └─ ─┘└─ ─┘└───┘└───┘          └───┘                                   \n" +
                        "          ┌─ ─┐┌─ ─┐                                                            \n" +
                        "          │090││067│                                                            \n" +
                        "          └───┘└─ ─┘                                                            \n" +
                        "               ┌─ ─┐                                                            \n" +
                        "               │089│                                                            \n" +
                        "               └───┘  ";

        //mapOutput.setText(mapText);
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

        //I guess this sort of thing isn't recommended but might change my mind later
//        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
//        double screenWidth = screenSize.getWidth();
//        double screenHeight = screenSize.getHeight();
//        setSize((int) (screenWidth / 2), (int) (screenHeight / 2));

        //Action listener stuff
        inputField.addActionListener(inputFieldAction);

        updateRoomEnemies();

        //Final prep
        setResizable(false);                //Looks gross maximized otherwise
        pack();                             //Packs everything together to fit whatever size the components are
        setLocationRelativeTo(null);        //Sticks the window in the middle
        setVisible(true);                   //What it says on the tin
    }

    //TODO: Will be modified to handle world events
    Action inputFieldAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            Object source = actionEvent.getSource();

            if(source == inputField) {
                String inputText = inputField.getText();
                System.out.println(inputText);

                if(inputText.isEmpty()) return;

                //TODO: I think Dave has code in his stuff to break down user actions.
                messages.add("\nCommand attempted: " + inputText + "\n");
                String[] chunked = inputText.split(" ");

                //TODO: These should be in the enums for Raft, but I don't want to muck with that without Dave
                switch(chunked[0].toUpperCase()) {
                    case(".CHAT"):
                        if(chunked.length == 1) {
                            messages.add("Nothing to say!");
                            break;
                        }

                        String chatMessage = inputText.substring(chunked[0].length() + 1);

                        messages.add("You: " + chatMessage);

                        break;
                    case(".MOVE"):
                        if(chunked.length == 1) {
                            messages.add("Nowhere to go!"); //TODO: List exits
                            break;
                        }

                        char direction = chunked[1].charAt(0);
                        boolean success = user.changeRooms(direction);

                        if(success) {
                            messages.add("Moved " + chunked[1] + ". Current room: " + user.getRoomNumber());
                        } else {
                            messages.add("No room that way!");
                        }

                        updateRoomEnemies();
                        break;
                    default:
                        messages.add("Didn't understand that!");
                }

                inputField.setText("");
                updateOutputBox();
            }
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
    public void updateMapOutput() {
        mapOutput.setText(currentFloor.toString());
    }

    public void updateRoomEnemies() {
        if(user.currentRoom.entities.isEmpty()) return;

        for(Entity entity: user.currentRoom.entities) {
            messages.add("Enemy in room! " + entity.name + " spawned in!");
        }
    }

}
