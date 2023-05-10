import java.awt.*;
import java.sql.*;

import com.rabbitmq.client.* ;
import javax.swing.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.Vector;


public class Ho extends  JFrame{
    private static final long serialVersionUID = 1L;
    private static JTable table;
    private JScrollPane scrollPane;
    private JPanel buttonPanel;
    private JButton insertButton;
    private JButton updateButton;
    private JButton deleteButton;
    private static String url = "jdbc:mysql://localhost:3307/bo";
    private static String user = "root@";
    private static String password = "";
    public Ho() {
        super("Head Office Database Table Content");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);

        // Create JTable and add it to a scroll pane
        table = new JTable();
        scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);


        Common.refreshTableData(table,url,user,password);
    }
    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        com.rabbitmq.client.Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        String queueName = "my-queue-1";
        channel.queueDeclare(queueName, false, false, false, null);

        String exchangeName1 = "my-exchange-1";
        String routingKey1 = "my-routing-key-1";
        channel.exchangeDeclare(exchangeName1, "direct", true);
        channel.queueBind(queueName, exchangeName1, routingKey1);

        String exchangeName2 = "my-exchange-2";
        String routingKey2 = "my-routing-key-2";
        channel.exchangeDeclare(exchangeName2, "direct", true);
        channel.queueBind(queueName, exchangeName2, routingKey2);
        java.sql.Connection dbConnection = DriverManager.getConnection(url, user, password);
        dbConnection.setAutoCommit(false);

        Ho frame = new Ho();
        frame.setVisible(true);

        // Set up consumer and start consuming messages
        channel.basicConsume(queueName, false, new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                // Convert message body to string
                String message = new String(body, "UTF-8");
                System.out.println((message));
                PreparedStatement statement = null;
                try {
                    //



                    statement = dbConnection.prepareStatement(message);
                    System.out.println("statement"+statement);
                int newId= 0;

                    try {
                        newId = Common.getNewId(dbConnection,  "productsales");
                        System.out.println("id"+newId);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                    if(!((message.toLowerCase().startsWith("delete")) || (message.toLowerCase().startsWith("update")))) {
                        System.out.println("setting id");
                        // Insert message data into database
                        statement.setInt(1, newId);
                    }

                    //try this
                    //!!
                    // channel.txSelect();
                    int res=statement.executeUpdate();
                    System.out.println(res+"= nbr rows affected");

                    // Acknowledge message to RabbitMQ
                    channel.basicAck(envelope.getDeliveryTag(), false);

                    System.out.println("ack sent");


                    //try this
                    // !!
                    // channel.txCommit();


                    // Commit JDBC transaction
                    dbConnection.commit();
                    System.out.println("db tx");

                    Common.refreshTableData(table,url,user,password);
                    System.out.println("refresh");


                } catch (Exception e) {

                    // Rollback JDBC transaction and reject message to RabbitMQ if there is an error

                    try {
                        dbConnection.rollback();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }

                    channel.basicNack(envelope.getDeliveryTag(), false, true);

                    // Log error
                    System.err.println("Error saving message to database: " + e.getMessage());
                }
                //Common.refreshTableData(table,url,user,password);
                System.out.println("refresh again");

            }

        });



// Close database connection and RabbitMQ channel
       // dbConnection.close();
        //channel.close();
        //connection.close();

    }
    public static int extractLastNumber(String input) {
        int lastIndex = input.lastIndexOf("'");
        int secondLastIndex = input.lastIndexOf("'", lastIndex - 1);
        String numberString = input.substring(secondLastIndex + 1, lastIndex);
        return Integer.parseInt(numberString);
    }
}
