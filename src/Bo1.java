import java.awt.*;
import java.sql.*;

import com.rabbitmq.client.* ;
import javax.swing.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.TimeoutException;


public class Bo1 extends  JFrame{
    private static final long serialVersionUID = 1L;
    private JTable table;
    private JScrollPane scrollPane;
    private JPanel buttonPanel;
    private JButton insertButton;
    private JButton updateButton;
    private JButton deleteButton;
    private String url = "jdbc:mysql://localhost:3307/ho1";
    private String user = "root@";
    private String password = "";

    public Bo1() throws IOException, TimeoutException {
        super("Branch Office 1 Database Table Content");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);

        // Create JTable and add it to a scroll pane
        table = new JTable();
        scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // Create button panel and buttons
        buttonPanel = new JPanel();
        insertButton = new JButton("Insert");
        updateButton = new JButton("Update");
        deleteButton = new JButton("Delete");
        buttonPanel.add(insertButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        add(buttonPanel, BorderLayout.SOUTH);

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        com.rabbitmq.client.Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        Set<String> nackMessages = new HashSet<>();

        String queueName = "my-queue";
        channel.queueDeclare(queueName, false, false, false, null);
        String exchangeName = "my-exchange-1";
        String routingKey = "my-routing-key-1";
        channel.exchangeDeclare(exchangeName, "direct", true);
        channel.queueBind(queueName, exchangeName, routingKey);


        insertButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JTextField dateField = new JTextField(15);
                JTextField regionField = new JTextField(15);
                JTextField productNameField = new JTextField(20);
                JTextField qtyField = new JTextField(10);
                JTextField costField = new JTextField(10);
                JTextField amntField = new JTextField(10);
                JTextField taxField = new JTextField(10);
                JTextField totalField = new JTextField(10);

                JPanel formPanel = new JPanel();
                formPanel.add(Box.createHorizontalStrut(10));
                formPanel.add(new JLabel("Date"));
                formPanel.add(dateField);
                formPanel.add(new JLabel("region:"));
                formPanel.add(regionField);
                formPanel.add(new JLabel("Product :"));
                formPanel.add(productNameField);
                formPanel.add(new JLabel("qty:"));
                formPanel.add(qtyField);
                formPanel.add(new JLabel("cost:"));
                formPanel.add(costField);
                formPanel.add(new JLabel("amnt:"));
                formPanel.add(amntField);
                formPanel.add(new JLabel("tax:"));
                formPanel.add(taxField);
                formPanel.add(new JLabel("total:"));
                formPanel.add(totalField);

                int result = JOptionPane.showConfirmDialog(null, formPanel, "Insert new sale record", JOptionPane.OK_CANCEL_OPTION);
                if (result == JOptionPane.OK_OPTION) {
                    // Get the values entered by the user=
                    String date = dateField.getText();
                    String region = regionField.getText();
                    String productName = productNameField.getText();
                    String qty = (qtyField.getText());
                    String  cost = (costField.getText());
                    String amnt = (amntField.getText());
                    String tax = (taxField.getText());
                    String total = totalField.getText();

                    // Insert the new product into the database
                    java.sql.Connection conn = null;
                    try {
                        conn = DriverManager.getConnection(url, user, password);
                        conn.setAutoCommit(false);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                    PreparedStatement insertStatement = null;
                    try {
                        insertStatement = conn.prepareStatement("INSERT INTO productsales (id, date, region, product, qty, cost, amnt, tax, total, copied) VALUES (?, ?, ? ,? ,? ,? ,? ,? ,? ,?)");
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }

                    try {
                        int newId = 0;
                        try {
                            newId = Common.getNewId(conn, "productsales");
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                        insertStatement.setInt(1, newId);
                        insertStatement.setString(2, date);
                        insertStatement.setString(3, region);
                        insertStatement.setString(4, productName);
                        insertStatement.setString(5, qty);
                        insertStatement.setString(6, cost);
                        insertStatement.setString(7, amnt);
                        insertStatement.setString(8, tax);
                        insertStatement.setString(9, total);
                        insertStatement.setString(10, "0");

                        insertStatement.executeUpdate();
                        conn.commit();

                        JOptionPane.showMessageDialog(null, "Product inserted successfully.");

                        // send inserted data to exchange with confirmation listener
                        try {
                            channel.confirmSelect(); // enable publisher confirms

                            int finalNewId = newId;
                            String message= "INSERT INTO productsales (id, date, region, product, qty, cost, amnt, tax, total, copied, idbranch, bo) VALUES (?, '"+date+"', '"+region+"' ,'"+productName+"' ,"+qty+" ,"+cost+" ,"+amnt+" ,"+tax+" ,"+total+",1 ,"+newId+" ,1)";
                            channel.addConfirmListener(new ConfirmListener() {
                                public void handleAck(long deliveryTag, boolean multiple) throws IOException {
                                    // The message was successfully acknowledged
                                    System.out.println("Message " + deliveryTag + " was successfully acknowledged.");
                                    try {
                                        // Insert the new product into the database
                                        java.sql.Connection conn1 = null;
                                        try {
                                            conn1 = DriverManager.getConnection(url, user, password);
                                            conn1.setAutoCommit(false);
                                        } catch (SQLException ex) {
                                            ex.printStackTrace();
                                        }
                                        PreparedStatement updateStatement = null;
                                        updateStatement=conn1.prepareStatement("UPDATE productsales SET id=?, copied=1 WHERE id=?");
                                        updateStatement.setInt(1, finalNewId);
                                        updateStatement.setInt(2, finalNewId);
                                        updateStatement.executeUpdate();
                                        conn1.commit();
                                        System.out.println(finalNewId+"is copied in HO");

                                    } catch (SQLException ex) {
                                        ex.printStackTrace();
                                    }

                                }
                                public void handleNack(long deliveryTag, boolean multiple) throws IOException {
                                    // The message was rejected, so we need to resend it
                                    System.out.println("Message " + deliveryTag + " was rejected.");
                                    nackMessages.add(message);
                                }
                            });
                            Common.refreshTableData(table, url, user, password);
                            channel.basicPublish(exchangeName, routingKey, null, message.getBytes("UTF-8"));
                            if (!channel.waitForConfirms(5000)) {
                                nackMessages.add(message);
                            }
                            while (!nackMessages.isEmpty()) {
                                ArrayList<String> unackedMessages = new ArrayList<String>(nackMessages);
                                nackMessages.clear();
                                for (String m : unackedMessages) {
                                    channel.basicPublish(exchangeName, routingKey, null, m.getBytes("UTF-8"));
                                    if (!channel.waitForConfirms(5000)) {
                                        nackMessages.add(m);
                                    }
                                }
                            }
                            Common.refreshTableData(table, url, user, password);

                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        Common.refreshTableData(table, url, user, password);
                    } catch (SQLException ex) {
                        // Rollback the transaction and display an error message
                        try {
                            conn.rollback();
                        } catch (SQLException ex2) {
                            ex2.printStackTrace();
                        }
                        JOptionPane.showMessageDialog(null, "Error inserting product: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
                Common.refreshTableData(table, url, user, password);


            }
        });

        updateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow == -1) {
                    JOptionPane.showMessageDialog(null, "Please select a row to update.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                // Get the values of the selected row
                int id = (int) table.getValueAt(selectedRow, 0);
                String old_productName = (String) table.getValueAt(selectedRow, 3);
                String old_date = (String) table.getValueAt(selectedRow, 1);
                String old_region = (String) table.getValueAt(selectedRow, 2);
                int  old_qty = (int) table.getValueAt(selectedRow, 4);
                String old_cost = (String) table.getValueAt(selectedRow, 5);
                String old_amnt = (String) table.getValueAt(selectedRow, 6);
                String old_tax = (String) table.getValueAt(selectedRow, 7);
                String old_total = (String) table.getValueAt(selectedRow, 8);

                // Create the form to update the product
                JPanel formPanel = new JPanel();
                formPanel.add(Box.createHorizontalStrut(10));


                JTextField dateField = new JTextField(old_date,15);
                JTextField regionField = new JTextField(old_region,15);
                JTextField productNameField = new JTextField(old_productName,20);
                JTextField qtyField = new JTextField(Integer.toString(old_qty),10);
                JTextField costField = new JTextField((old_cost),10);
                JTextField amntField = new JTextField((old_amnt),10);
                JTextField taxField = new JTextField((old_tax),10);
                JTextField totalField = new JTextField((old_total),10);

                formPanel.add(new JLabel("Date"));
                formPanel.add(dateField);
                formPanel.add(new JLabel("region:"));
                formPanel.add(regionField);
                formPanel.add(new JLabel("Product :"));
                formPanel.add(productNameField);
                formPanel.add(new JLabel("qty:"));
                formPanel.add(qtyField);
                formPanel.add(new JLabel("cost:"));
                formPanel.add(costField);
                formPanel.add(new JLabel("amnt:"));
                formPanel.add(amntField);
                formPanel.add(new JLabel("tax:"));
                formPanel.add(taxField);
                formPanel.add(new JLabel("total:"));
                formPanel.add(totalField);

                int result = JOptionPane.showConfirmDialog(null, formPanel, "Update Product", JOptionPane.OK_CANCEL_OPTION);
                if (result == JOptionPane.OK_OPTION) {
                    // Get the values entered by the user
                    String date = dateField.getText();
                    String region = regionField.getText();
                    String newProductName = productNameField.getText();
                    String qty = (qtyField.getText());
                    String  cost = (costField.getText());
                    String amnt = (amntField.getText());
                    String tax = (taxField.getText());
                    String total = totalField.getText();

                    // Update the product in the database
                    java.sql.Connection conn = null;
                    try {
                        conn = DriverManager.getConnection(url, user, password);
                        conn.setAutoCommit(false);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                    PreparedStatement updateStatement = null;
                    try {
                        updateStatement = conn.prepareStatement("UPDATE productsales SET id=?, date=?, region=?, product=?, qty=?, cost=?, amnt=?, tax=?, total=?, copied=0 WHERE id=?");
                        updateStatement.setInt(1, id);
                        updateStatement.setString(2, date);
                        updateStatement.setString(3, region);
                        updateStatement.setString(4, newProductName);
                        updateStatement.setString(5, qty);
                        updateStatement.setString(6, cost);
                        updateStatement.setString(7, amnt);
                        updateStatement.setString(8, tax);
                        updateStatement.setString(9, total);
                        updateStatement.setInt(10, id);
                        updateStatement.executeUpdate();
                        conn.commit();
                        JOptionPane.showMessageDialog(null, "Product updated successfully.");
                        // send inserted data to exchange with confirmation listener
                        try {
                            channel.confirmSelect(); // enable publisher confirms
                            String message= "UPDATE productsales SET date='"+date+"', region='"+region+"', product='"+newProductName+"', qty='"+qty+"', cost='"+cost+"', amnt='"+amnt+"', tax='"+tax+"', total='"+total+"' WHERE idbranch='"+id+"' AND bo='1'";
                            channel.addConfirmListener(new ConfirmListener() {
                                public void handleAck(long deliveryTag, boolean multiple) throws IOException {
                                    // The message was successfully acknowledged
                                    System.out.println("Message " + deliveryTag + " was successfully acknowledged.");
                                    int finalNewId = id;
                                    try {
                                        // Insert the new product into the database
                                        java.sql.Connection conn1 = null;
                                        try {
                                            conn1 = DriverManager.getConnection(url, user, password);
                                            conn1.setAutoCommit(false);
                                        } catch (SQLException ex) {
                                            ex.printStackTrace();
                                        }
                                        PreparedStatement updateStatement = null;
                                        updateStatement=conn1.prepareStatement("UPDATE productsales SET id=?, copied=1 WHERE id=?");
                                        updateStatement.setInt(1, finalNewId);
                                        updateStatement.setInt(2, finalNewId);
                                        updateStatement.executeUpdate();
                                        conn1.commit();
                                        System.out.println("updated"+finalNewId);

                                    } catch (SQLException ex) {
                                        ex.printStackTrace();
                                    }
                                }
                                public void handleNack(long deliveryTag, boolean multiple) throws IOException {
                                    // The message was rejected, so we need to resend it
                                    System.out.println("Message " + deliveryTag + " was rejected.");
                                    nackMessages.add(message);
                                }
                            });
                            Common.refreshTableData(table, url, user, password);
                            channel.basicPublish(exchangeName, routingKey, null, message.getBytes("UTF-8"));
                            if (!channel.waitForConfirms(5000)) {
                                nackMessages.add(message);
                            }
                            while (!nackMessages.isEmpty()) {
                                ArrayList<String> unackedMessages = new ArrayList<String>(nackMessages);
                                nackMessages.clear();
                                for (String m : unackedMessages) {
                                    channel.basicPublish(exchangeName, routingKey, null, m.getBytes("UTF-8"));
                                    if (!channel.waitForConfirms(5000)) {
                                        nackMessages.add(m);
                                    }
                                }
                            }
                            Common.refreshTableData(table, url, user, password);

                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        Common.refreshTableData(table, url, user, password);
                    } catch (SQLException ex) {
                        // Rollback the transaction and display an error message
                        try {
                            conn.rollback();
                        } catch (SQLException ex2) {
                            ex2.printStackTrace();
                        }
                        JOptionPane.showMessageDialog(null, "Error updating product: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
                Common.refreshTableData(table, url, user, password);

            }
        });

        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow == -1) {
                    JOptionPane.showMessageDialog(null, "Please select a row to delete.");
                    return;
                }

                int productId = (int) table.getValueAt(selectedRow, 0);
                String productName = (String) table.getValueAt(selectedRow, 1);

                int confirm = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete product \"" + productName + "\"?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    // Delete the product from the database
                    java.sql.Connection conn = null;
                    try {
                        conn = DriverManager.getConnection(url, user, password);
                        conn.setAutoCommit(false);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                    PreparedStatement deleteStatement = null;
                    try {
                        deleteStatement = conn.prepareStatement("DELETE FROM productsales WHERE id = ?");
                        deleteStatement.setInt(1, productId);
                        deleteStatement.executeUpdate();
                        conn.commit();
                        JOptionPane.showMessageDialog(null, "Product deleted successfully.");

                        // send inserted data to exchange with confirmation listener
                        try {
                            channel.confirmSelect();// enable publisher confirms
                            String message="DELETE FROM productsales WHERE idbranch='"+productId+"' AND bo='1'";
                                    channel.addConfirmListener(new ConfirmListener() {
                                public void handleAck(long deliveryTag, boolean multiple) throws IOException {
                                    // The message was successfully acknowledged
                                    System.out.println("Message " + deliveryTag + " was successfully acknowledged.");

                                }
                                public void handleNack(long deliveryTag, boolean multiple) throws IOException {
                                    // The message was rejected, so we need to resend it
                                    System.out.println("Message " + deliveryTag + " was rejected.");
                                    nackMessages.add(productName);
                                }
                            });
                            channel.basicPublish(exchangeName, routingKey, null, message.getBytes("UTF-8"));
                            if (!channel.waitForConfirms(5000)) {
                                nackMessages.add(message);
                            }
                            while (!nackMessages.isEmpty()) {
                                ArrayList<String> unackedMessages = new ArrayList<String>(nackMessages);
                                nackMessages.clear();
                                for (String m : unackedMessages) {
                                    channel.basicPublish(exchangeName, routingKey, null, m.getBytes("UTF-8"));
                                    if (!channel.waitForConfirms(5000)) {
                                        nackMessages.add(m);
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        Common.refreshTableData(table, url, user, password);
                    } catch (SQLException ex) {
                        // Rollback the transaction and display an error message
                        try {
                            conn.rollback();
                        } catch (SQLException ex2) {
                            ex2.printStackTrace();
                        }
                        JOptionPane.showMessageDialog(null, "Error deleting product: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                    Common.refreshTableData(table, url, user, password);
                }
            }
        });

        Common.refreshTableData(table, url, user, password);


    }

    public static void main(String[] args) throws Exception {
        Bo1 frame = new Bo1();
        frame.setVisible(true);
    }
}
