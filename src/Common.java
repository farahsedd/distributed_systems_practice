import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.io.IOException;
import java.sql.*;
import java.util.Vector;

public class Common {

    public static int getNewId(java.sql.Connection dbConnection, String tableName) throws SQLException {
        int newId = 1;
        String query = "SELECT MAX(id) FROM " + tableName;
        Statement statement = dbConnection.createStatement();
        ResultSet resultSet = statement.executeQuery(query);
        if (resultSet.next()) {
            newId = resultSet.getInt(1) + 1;
        }
        return newId;
    }
    public static void refreshTableData(JTable table, String url,String user,String password) {
        try{
        java.sql.Connection conn = DriverManager.getConnection(url, user, password);
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM productsales");

        Vector<Vector<Object>> data = new Vector<Vector<Object>>();
        Vector<Object> columnNames = new Vector<Object>();
        columnNames.add("ID");
        columnNames.add("date");
        columnNames.add("region");
        columnNames.add("Product");
        columnNames.add("qty");
        columnNames.add("cost");
        columnNames.add("amnt");
        columnNames.add("tax");
        columnNames.add("total");
        columnNames.add("copied");
            columnNames.add("idbranch");

            ResultSetMetaData rsmd = rs.getMetaData();
            while (rs.next()) {
            Vector<Object> row = new Vector<Object>();
            row.add(rs.getInt("id"));
            row.add(rs.getString("date"));
                row.add(rs.getString("region"));
                row.add(rs.getString("product"));
                row.add(rs.getInt("qty"));
                row.add(rs.getString("cost"));
                row.add(rs.getString("amnt"));
                row.add(rs.getString("tax"));
                row.add(rs.getString("total"));
                row.add(rs.getString("copied"));
                int columnCount = rsmd.getColumnCount();
                boolean isHead = false;
                for (int i = 1; i <= columnCount; i++) {
                    if (rsmd.getColumnName(i).equals("idbranch")) {
                        isHead = true;
                        break;
                    }
                }
                if (isHead) {
                    row.add(rs.getString("idbranch"));
                    row.add(rs.getString("bo"));
                }

            data.add(row);
        }
try {
    table.setModel(new javax.swing.table.DefaultTableModel(data, columnNames));
}catch(Exception ex) {
    throw new RuntimeException(ex);
}

    } catch (SQLException ex) {
        System.out.println("SQLException: " + ex.getMessage());
    }
    }
}
