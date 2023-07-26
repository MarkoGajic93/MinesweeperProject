import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class ResultsFrame extends JFrame {
    private static ResultsFrame instance;
    private JTable resultsTable;
    private JScrollPane tableScroll;
    private JButton myRes, allRes, removeRes;
    private String currUser;

    // Private constructor because of singleton pattern being implemented
    private ResultsFrame (Connection conn, String username) {
        // Setting basics of frame
        super("Results");
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        // Setting layout
        GridBagLayout gb = new GridBagLayout();
        this.setLayout(gb);

        currUser = username; // Defining who is viewing results

        displayTable(conn, true); // true means that results of all users will be displayed

        // Adding buttons and their action listeners
        myRes = new JButton("My Results");
        myRes.addActionListener(e -> { remove(tableScroll); displayTable(conn, false); pack(); });
        allRes = new JButton("All Results");
        allRes.addActionListener(e -> { remove(tableScroll); displayTable(conn, true); pack(); });
        removeRes = new JButton("Remove Result(s)");
        removeRes.addActionListener(e -> {
            if (resultsTable.getSelectedRows().length == 0)
                JOptionPane.showMessageDialog(this, "Select result you wish to remove");
            else {
                String selectedName;
                boolean selectionOk = true; // Used to check if user has selected only his results
                // Check value of every selected row at column 1 (name)
                for (int i = 0; i < resultsTable.getSelectedRows().length; i++) {
                    selectedName = (String) resultsTable.getValueAt(resultsTable.getSelectedRows()[i], 1);
                    if (!selectedName.equals(username)) {
                        JOptionPane.showMessageDialog(this, "You can only remove YOUR results!");
                        selectionOk = false;
                        break;
                    }
                }
                if (selectionOk) {
                    // Store value at column 3 (date) from every selected row into array and then delete those results from database
                    // (date field is used for check because it must be unique)
                    int selectedRowsNum = resultsTable.getSelectedRowCount();
                    String[] dates = new String[selectedRowsNum];
                    for (int i = 0; i < resultsTable.getSelectedRows().length; i++) {
                        dates[i] = (String) resultsTable.getValueAt(resultsTable.getSelectedRows()[i], 3);
                        try {
                            PreparedStatement statement = conn.prepareStatement("DELETE FROM results WHERE date = ?");
                            statement.setString(1, dates[i]);
                            statement.execute();
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                    }
                    // After table is changed, remove old one and display again
                    remove(tableScroll);
                    displayTable(conn, true);
                    pack();
                }
            }
        });
        addComponent(myRes,0,1,1,1, GridBagConstraints.NONE, 0, 0, new Insets(10,10,10,10), GridBagConstraints.LAST_LINE_START, 0.5, 0.0);
        addComponent(removeRes,1,1,1,1, GridBagConstraints.NONE, 0, 0, new Insets(10,10,10,10), GridBagConstraints.PAGE_END, 0.5, 0.0);
        addComponent(allRes,2,1,1,1, GridBagConstraints.NONE, 0, 0, new Insets(10,10,10,10), GridBagConstraints.LAST_LINE_END, 0.5, 0.0);

        // Finishing settings of frame (size and position)
        this.setPreferredSize(new Dimension(700,400));
        int screanX = (Toolkit.getDefaultToolkit().getScreenSize().width - this.getPreferredSize().width)/2;
        int screanY = (Toolkit.getDefaultToolkit().getScreenSize().height - this.getPreferredSize().height)/2;
        this.setLocation(screanX, screanY);
        pack();
    }
    // Method to get a new or an existing instance of ResultFrame class
    // (called instead of ResultsFrame constructor, to prevent creating more than one objects/opening more than one ResultFrame window)
    public static ResultsFrame getInstance(Connection conn, String username) {
        if (instance == null)
            instance = new ResultsFrame(conn, username);
        return instance;
    }

    // Method to create table and add it to a frame (allResults value determines if all or only currUser results will be displayed)
    private void displayTable(Connection conn, boolean allResults) {
        try {
            ResultSet set;
            if (allResults) {
                set = conn.createStatement().executeQuery("SELECT u.username, r.score, r.date, r.level FROM users u " +
                                                            "JOIN results r USING (user_id) ORDER BY r.level, r.score");
            }
            else {
                PreparedStatement statement = conn.prepareStatement("SELECT u.username, r.score, r.date, r.level FROM users u " +
                                                                        "JOIN results r USING (user_id) WHERE u.username IN (?) " +
                                                                        "ORDER BY r.level, r.score");
                statement.setString(1, currUser);
                set = statement.executeQuery();
            }

            // After executing query, column rank is being added to every row of result set, and all rows from set are added into arraylist
            ArrayList<String[]> tableRows = new ArrayList<>();
            int rank = 1;
            while (set.next()) {
                String[] result = {String.valueOf(rank), set.getString("username"), set.getString("score"),
                                    set.getString("date"), set.getString("level")};
                tableRows.add(result);
                rank++;
            }

            // Defining table model which will be used to create JTable, and setting cells to be uneditable
            DefaultTableModel model = new DefaultTableModel() {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            // Adding columns to a model
            model.addColumn("Rank");
            model.addColumn("Player name");
            model.addColumn("Score");
            model.addColumn("Date");
            model.addColumn("Level");
            // Adding all rows from previously created arraylist to a model
            for (String[] row : tableRows)
                model.addRow(row);
            // Creating JTable and setting center alignment to every cell
            resultsTable = new JTable(model);
            DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
            centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
            for (int i = 0; i < resultsTable.getColumnModel().getColumnCount(); i++)
                resultsTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
            tableScroll = new JScrollPane(resultsTable);
            addComponent(tableScroll,0,0,3,1, GridBagConstraints.BOTH, 0, 0, new Insets(10,10,10,10), GridBagConstraints.CENTER, 1.0, 1.0);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ooops, something went wrong!");
        }
    }

    // Method to add component to frame, with their GridBagConstraints
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