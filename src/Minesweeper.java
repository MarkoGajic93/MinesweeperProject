import javax.swing.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Minesweeper {

    public static void main(String[] args) {

        // Requesting user input (server name, port number, mysql username and password) to try to establish a connection with mysql database:
        // Setting up JOptionPane to use for user input
        boolean notConnected = true;
        JTextField server = new JTextField();
        JTextField port = new JTextField();
        JTextField mysqlUsername = new JTextField();
        JPasswordField mysqlPassword = new JPasswordField();
        mysqlPassword.setEchoChar('*');
        JCheckBox toggle = new JCheckBox("(show password)");
        toggle.addActionListener(e -> {
            if (mysqlPassword.getEchoChar() == (char) 0)
                mysqlPassword.setEchoChar('*');
            else
                mysqlPassword.setEchoChar((char) 0);
        });
        Object[] input = {"Server name (on which mysql is running): ", server, "Port: ", port, "Username: ", mysqlUsername, "Password: ", mysqlPassword, toggle};
        Object[] opts = {"Connect", "Cancel"};
        do {
            int res = JOptionPane.showOptionDialog(null, input, "Enter data to connect to a database", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, opts, null);
            // If user chose 'cancel' or 'x' - exit program
            if (res == -1 || res == 1) System.exit(0);
            // If user chose 'connect' - try to connect
            String url = "jdbc:mysql://" + server.getText() + ":" + port.getText() + "/minesweeper";
            String username = mysqlUsername.getText();
            String password = String.valueOf(mysqlPassword.getPassword());
            try {
                Connection conn = DriverManager.getConnection(url, username, password);
                // If everything is ok - start game
                notConnected = false;
                SwingUtilities.invokeLater(() -> new GameFrame(conn));
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "Could not connect to a database");
            }
        } while (notConnected);
    }
}
