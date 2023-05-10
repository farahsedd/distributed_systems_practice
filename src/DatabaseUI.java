import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class DatabaseUI extends JFrame {

    private JTable table;
    private DefaultTableModel tableModel;

    private JButton insertButton;
    private JButton updateButton;
    private JButton deleteButton;

    public DatabaseUI() {
        super("Database UI");

        // Create UI components
        table = new JTable();
        tableModel = new DefaultTableModel();
        table.setModel(tableModel);

        insertButton = new JButton("Insert");
        updateButton = new JButton("Update");
        deleteButton = new JButton("Delete");

        // Add components to frame
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(insertButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Add event handlers for buttons
        insertButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Execute INSERT operation
                // Refresh tableModel with new data
            }
        });

        updateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Execute UPDATE operation
                // Refresh tableModel with new data
            }
        });

        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Execute DELETE operation
                // Refresh tableModel with new data
            }
        });

        // Set frame properties
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setVisible(true);

        // Populate table with data
        loadData();
    }

    private void loadData() {
        // Retrieve data from database table
        try {
            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3307/bo", "root@", "");
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM productsales");

            // Populate tableModel with data
            while (resultSet.next()) {
                Object[] row = new Object[2];
                row[0] = resultSet.getInt("id");
                row[1] = resultSet.getString("product");
                tableModel.addRow(row);
            }

            // Close resources
            resultSet.close();
            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new DatabaseUI();
    }
}
