import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Random;

public class GameFrame extends JFrame {
    private final int EASY = 1;
    private final int MEDIUM = 2;
    private final int HARD = 3;
    private  int difficulty = EASY; // default value
    private int size;
    private int numMines;
    private boolean firstMove; // Used to prevent 'stepping' on mine in first click

    private JMenuBar menuBar;
    private JTextField user, timeScore;
    private JLayeredPane gamePane;
    private JButton[][] buttons;
    private JLabel[][] labels;
    private Timer timer;
    Connection conn;
    
    public GameFrame(Connection connection){
        // Setting basics of frame
        super("Minesweeper");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.conn = connection;
        // Setting layout
        GridBagLayout gb = new GridBagLayout();
        this.setLayout(gb);
        // Setting and adding menu bar to frame
        JMenuItem newGame = new JMenuItem("New Game");
        newGame.addActionListener(e -> {this.remove(gamePane); timer.stop(); startGame(difficulty);});
        JMenuItem changeDiff = new JMenuItem("Change difficulty");
        changeDiff.addActionListener(e -> {
            String[] opts = {"Easy", "Medium", "Hard"};
            int diff = JOptionPane.showOptionDialog(this, "Choose difficulty:", "Change difficulty", JOptionPane.OK_OPTION, JOptionPane.PLAIN_MESSAGE, null, opts, opts[0]);
            if (diff != -1) {
                difficulty = diff+1;
                this.remove(gamePane);
                timer.stop();
                this.setVisible(false); // Transition looks nicer with this
                startGame(difficulty);
            }
        });
        JMenuItem viewRes = new JMenuItem("View results");
        viewRes.addActionListener(e -> ResultsFrame.getInstance(conn, user.getText()).setVisible(true));
        JMenuItem logOut = new JMenuItem("Log out");
        logOut.addActionListener(e -> { logIn(); this.remove(gamePane); timer.stop(); startGame(difficulty); });
        JMenu optsMenu = new JMenu("Options");
        optsMenu.add(newGame);
        optsMenu.add(changeDiff);
        optsMenu.add(viewRes);
        optsMenu.add(logOut);
        menuBar = new JMenuBar();
        menuBar.add(optsMenu);
        this.addComponent(menuBar,0,0,2,1, GridBagConstraints.HORIZONTAL, 0,0, new Insets(0,0,0,0), GridBagConstraints.PAGE_START, 1.0, 0.0);
        // Initialising and adding username and score components
        user = new JTextField();
        user.setEditable(false);
        user.setBorder(BorderFactory.createTitledBorder((BorderFactory.createLineBorder(Color.BLUE)), "Player name"));
        user.setHorizontalAlignment(SwingConstants.CENTER);
        this.addComponent(user,0,1,1,1,GridBagConstraints.HORIZONTAL,0,0,new Insets(0,10,0,0),GridBagConstraints.FIRST_LINE_START,0.8,0.0);
        timeScore = new JTextField();
        timeScore.setEditable(false);
        timeScore.setBorder(BorderFactory.createTitledBorder((BorderFactory.createLineBorder(Color.RED)), "Time"));
        timeScore.setHorizontalAlignment(SwingConstants.CENTER);
        this.addComponent(timeScore,1,1,1,1,GridBagConstraints.NONE,40,0,new Insets(0,0,0,10),GridBagConstraints.FIRST_LINE_END,0.2,0.0);

        logIn();

        startGame(difficulty);
    }

    // Method to create account or login an existing user
    private void logIn() {
        boolean notLoggedIn = true;
        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();
        password.setEchoChar('*');
        JCheckBox toggle = new JCheckBox("(show password)");
        toggle.addActionListener(e -> {
            if (password.getEchoChar() == (char) 0)
                password.setEchoChar('*');
            else
                password.setEchoChar((char) 0);
        });
        Object[] input = {"Username: ", username, "Password: ", password, toggle};
        Object[] opts = {"Login", "Create account", "Play as guest"};
        do {
            int res = JOptionPane.showOptionDialog(null, input, "Login", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, opts, null);
            switch (res) {
                // Clicked on 'X'
                case -1:
                    System.exit(0);
                // Clicked on 'Login'
                case 0: {
                    String playerName = username.getText();
                    String playerPassword = String.valueOf(password.getPassword());
                    // Check if user entered anything in username and password text fields
                    if (playerName.equals("") || playerPassword.equals(""))
                        JOptionPane.showMessageDialog(null, "You have to type in username and password");
                   // Check if username and password exists in database
                    else if (!checkIfAccExists(playerName, playerPassword))
                        JOptionPane.showMessageDialog(null, "Wrong username or password");
                    // If everything is ok
                    else {
                        user.setText(playerName);
                        notLoggedIn = false;
                    }
                    break;
                }
                // Clicked on 'Create account'
                case 1: {
                    String playerName = username.getText();
                    String playerPassword = String.valueOf(password.getPassword());
                    // Check if user entered anything in username and password text fields
                    if (playerName.equals("") || playerPassword.equals(""))
                        JOptionPane.showMessageDialog(null, "You have to type in username and password");
                    // Check if that username already exists in database
                    else if (checkIfAccExists(playerName))
                        JOptionPane.showMessageDialog(null, "This username already exists");
                    // If everything is ok
                    else {
                        addNewUser(playerName, playerPassword);
                        user.setText(playerName);
                        notLoggedIn = false;
                    }
                    break;
                }
                case 2: {
                    user.setText("Guest");
                    notLoggedIn = false;
                    break;
                }
            }
        } while (notLoggedIn);
    }

    // Simple method to add new account (username and password) to database
    private void addNewUser(String playerName, String playerPassword) {
        try {
            PreparedStatement statement = conn.prepareStatement("INSERT INTO users (username, password) VALUES (?,?)");
            statement.setString(1, playerName);
            statement.setString(2, playerPassword);
            statement.execute();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Oooops, something went wrong...");
        }
    }

    // Method to check if account with given username and password exists in database (used in 'login' option)
    private boolean checkIfAccExists(String playerName, String playerPassword) {
        try {
            Statement statement = conn.createStatement();
            ResultSet set = statement.executeQuery("SELECT username, password FROM users");
            while (set.next())
                if (playerName.equals(set.getString("username")) && playerPassword.equals(set.getString("password")))
                    return true;
            return false;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Oooops, something went wrong...");
            return false;
        }
    }

    // Overloaded method to check if username exists in database (used in 'create account' option)
    // (password isn't important in this case, because two different usernames are allowed to have same password)
    private boolean checkIfAccExists(String playerName) {
        try {
            Statement statement = conn.createStatement();
            ResultSet set = statement.executeQuery("SELECT username, password FROM users");
            while (set.next())
                if (playerName.equals(set.getString("username")))
                    return true;
            return false;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Oooops, something went wrong...");
            return false;
        }
    }

    // Method that contains all logic of the game
    private void startGame(int difficulty) {
        // Setting size of the game panel and number of mines, creating game panel and adding it to a game frame
        switch (difficulty) {
            case EASY: size = 9; numMines = 10; break;
            case MEDIUM: size = 16; numMines = 40; break;
            case HARD: size = 22; numMines = 89; break;
        }
        gamePane = new JLayeredPane();
        gamePane.setPreferredSize(new Dimension(30*size,30*size));
        this.addComponent(gamePane, 0,2,2,1,GridBagConstraints.BOTH,0,0, new Insets(10,10,10,10),GridBagConstraints.CENTER,1.0,1.0);

        // Setting size and position of game frame (has to be here, because every new game started can be with different size of game panel)
        this.pack();
        this.setResizable(false);
        int screanX = (Toolkit.getDefaultToolkit().getScreenSize().width - this.getWidth())/2;
        int screanY = (Toolkit.getDefaultToolkit().getScreenSize().height - this.getHeight())/2;
        this.setLocation(screanX, screanY);
        firstMove = true;
        // Adding 2D arrays to game panel
        fillButtonAndLabelArrays();
        for (int i = 0; i < size; i++){
            for (int j =0; j < size; j++) {
                gamePane.add(buttons[i][j],1,0);
                gamePane.add(labels[i][j], 0,0);
            }
        }
        timeScore.setText("0");
        this.setVisible(true); // Setting game frame visible
        // Initialising timer that will start after first left click and will stop when game is over or won
        timer = new Timer(1000, new ActionListener() {
            int count = 0;
            @Override
            public void actionPerformed(ActionEvent e) {
                count++;
                timeScore.setText(String.valueOf(count));
            }
        });

        // Adding mouse listener to every button
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                int row = i;
                int column = j;
                buttons[row][column].addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        JButton bt = (JButton)e.getSource();
                        // Prevent clicking when all buttons are disabled (after game is over or won)
                        if (!bt.isEnabled())
                            return;

                        if(SwingUtilities.isLeftMouseButton(e)) {
                            timer.start();
                            // Remove clicked button from game panel and buttons array
                            gamePane.remove(buttons[row][column]);
                            repaint();
                            buttons[row][column] = null;
                            // After first button is clicked, place mines (but not under that button)
                            if (firstMove) {
                                placeMines(row,column);
                                placeWarnings();
                                firstMove = false;
                            }
                            // If under clicked button is not a mine, reveal other buttons and check if game is won
                            if(!checkForMine(row, column, gamePane)) {
                                if (labels[row][column].getText().equals("")) {
                                    revealEmptyNeighbours(row, column, gamePane);
                                }
                                checkForWin(gamePane);
                            }
                        }
                        if(SwingUtilities.isRightMouseButton(e)) {
                            markMine(row, column);
                        }
                    }
                });
            }
        }
    }

    // Method to check win conditions after each 'non mine step click' by counting the remaining (unclicked) buttons
    // and comparing that count to the number of mines
    private void checkForWin(JLayeredPane gamePane) {
        int count = 0;
        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++)
                if (buttons[i][j] != null)
                    count++;
        if (count == numMines) {
            // Disable all remaining buttons
            for (int i = 0; i < buttons.length; i++) {
                for (int j = 0; j < buttons.length; j++) {
                    if (buttons[i][j] != null) {
                        buttons[i][j].setEnabled(false);
                        buttons[i][j].setBackground(Color.GREEN);
                        if (labels[i][j].getText().equals("M"))
                            buttons[i][j].setText("X");
                    }
                }
            }
            timer.stop();
            // Add result to database if user is logged in
            if (!user.getText().equals("Guest")) {
                try {
                    // Retrieve user id from users table
                    PreparedStatement statement = conn.prepareStatement("SELECT user_id FROM users WHERE username IN (?)");
                    statement.setString(1, user.getText());
                    ResultSet set = statement.executeQuery();
                    set.next();
                    int userId = set.getInt("user_id");
                    // Add score into results table
                    statement = conn.prepareStatement("INSERT INTO results (user_id, score, date, level) VALUES (?, ?, ?, ?)");
                    statement.setInt(1, userId);
                    statement.setInt(2, Integer.parseInt(timeScore.getText()));
                    java.util.Date date = new java.util.Date();
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String currentDateTime = formatter.format(date);
                    statement.setString(3, currentDateTime);
                    switch (difficulty) {
                        case EASY: statement.setString(4, "Easy"); break;
                        case MEDIUM: statement.setString(4, "Medium"); break;
                        case HARD: statement.setString(4, "Hard"); break;
                    }
                    statement.execute();
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(this, "Ooops, something went wrong!");
                }
            }
            JOptionPane.showMessageDialog(gamePane, "Congratulations " + user.getText() + ", you won!\nYour score: " + timeScore.getText());
        }
    }

    // Method to mark labels with a number of mines that surrounds them
    private void placeWarnings() {
        // For each field, count how many neighbour fields are mines
        for (int i = 0; i < size; i++){
            for (int j =0; j < size; j++) {
                int neighbourMines = 0;
                if (!labels[i][j].getText().equals("M")) {
                    for (int nr = Math.max(0, i - 1); nr <= Math.min(i + 1, size - 1); nr++){
                        for (int nc = Math.max(0, j - 1); nc <= Math.min(j + 1, size - 1); nc++) {
                            if (!(nr==i && nc==j))  {  // don't process field itself
                                // Here every neighbour is checked
                                if (labels[nr][nc].getText().equals("M"))
                                    neighbourMines++;
                            }
                        }
                    }
                    // After check, mark field with counted number
                    if (neighbourMines != 0) {
                        labels[i][j].setText(String.valueOf(neighbourMines));
                        labels[i][j].setHorizontalAlignment(SwingConstants.CENTER);
                        switch (neighbourMines) {
                            case 1: labels[i][j].setForeground(Color.BLUE); break;
                            case 2: labels[i][j].setForeground(Color.GREEN); break;
                            case 3: labels[i][j].setForeground(Color.RED); break;
                            case 4: labels[i][j].setForeground(Color.PINK); break;
                            case 5: labels[i][j].setForeground(Color.MAGENTA); break;
                            case 6: labels[i][j].setForeground(Color.YELLOW); break;
                            case 7: labels[i][j].setForeground(Color.CYAN); break;
                            case 8: labels[i][j].setForeground(Color.ORANGE); break;
                        }
                    }
                }
            }
        }
    }

    // Method to toggle button at (x,y) coordinates as 'marked as mine'
    private void markMine(int row, int column) {
        if (buttons[row][column].getText().equals("")) {
            buttons[row][column].setText("X");
        }
        else buttons[row][column].setText("");
    }

    // Method to reveal all neighbouring fields of (x,y) button if they are no mines under them,
    // so if u click on an empty field, all neighbouring buttons will be removed until it reaches warning field
    private void revealEmptyNeighbours(int row, int column, JLayeredPane gamePane) {
        for (int nr = Math.max(0, row - 1); nr <= Math.min(row + 1, size - 1); nr++) {
            for (int nc = Math.max(0, column - 1); nc <= Math.min(column + 1, size - 1); nc++) {
                if (!(nr == row && nc == column)) {  // don't process field itself
                    // Here processing one neighbour at a time (if that neighbour button wasn't removed earlier)
                    if (buttons[nr][nc] != null) {
                        // If under neighbour button is an empty label, remove button and repeat with its own neighbours
                        if (!labels[nr][nc].getText().equals("M") && labels[nr][nc].getText().equals("")) {
                            gamePane.remove(buttons[nr][nc]);
                            buttons[nr][nc] = null;
                            revealEmptyNeighbours(nr,nc,gamePane);
                        }
                        // If under neighbour button is a warning number, just remove that neighbour button
                        else if (!labels[nr][nc].getText().equals("M") && !labels[nr][nc].getText().equals("")) {
                            gamePane.remove(buttons[nr][nc]);
                            buttons[nr][nc] = null;
                        }
                    }
                }
            }
        }
    }

    // Method to check if under button at (x,y) coordinates is a mine
    private boolean checkForMine(int row, int column, JLayeredPane gamePane) {
        if (labels[row][column].getText().equals("M")) {
            labels[row][column].setBackground(Color.RED);
            // Disable all remaining buttons
            for (int i = 0; i < buttons.length; i++) {
                for (int j = 0; j < buttons.length; j++) {
                    if (buttons[i][j] != null) {
                        buttons[i][j].setEnabled(false);
                        buttons[i][j].setBackground(Color.PINK);
                        if (labels[i][j].getText().equals("M"))
                            buttons[i][j].setText("X");
                        else buttons[i][j].setText("");
                    }
                }
            }
            timer.stop();
            JOptionPane.showMessageDialog(gamePane, "Game over!");
            return true;
        }
        return false;
    }

    // Method to randomly position mines (changing text of randomly chosen labels in 2D array to "M")
    // (arguments are row and column of the first clicked button, to prevent stepping on a mine on first click)
    private void placeMines(int buttonRow, int buttonColumn) {
        for (int i = 0; i < numMines; i++) {
            int x,y;
            do {
                x = new Random().nextInt(0,size);
                y = new Random().nextInt(0,size);
            } while (labels[x][y].getText() == "M" || (x == buttonRow && y == buttonColumn));
            labels[x][y].setText("M");
            labels[x][y].setHorizontalAlignment(SwingConstants.CENTER);
        }
    }

    // Method to fill 2D arrays with labels and buttons
    private void fillButtonAndLabelArrays() {
        buttons = new JButton[size][size];
        labels = new JLabel[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                buttons[i][j] = new JButton();
                buttons[i][j].setBounds(i*30,j*30,30,30);
                buttons[i][j].setMargin(new Insets(0,0,0,0));
                labels[i][j] = new JLabel();
                labels[i][j].setBounds(i*30,j*30,30,30);
                labels[i][j].setBorder(BorderFactory.createBevelBorder(1));
            }
        }
    }

    // Method to add component to Game Frame, with their GridBagConstraints
    private void addComponent (Component comp, int gridx, int gridy, int gridwidth, int gridheight, int fill, int ipadx, int ipady, Insets insets, int anchor, double weightx, double weighty) {
        GridBagConstraints con = new GridBagConstraints();
        con.gridx = gridx;
        con.gridy = gridy;
        con.gridwidth = gridwidth;
        con.gridheight = gridheight;
        con.fill = fill;
        con.ipadx = ipadx;
        con.ipady = ipady;
        con.insets = insets;
        con.anchor = anchor;
        con.weightx = weightx;
        con.weighty = weighty;
        add(comp, con);
    }
}
