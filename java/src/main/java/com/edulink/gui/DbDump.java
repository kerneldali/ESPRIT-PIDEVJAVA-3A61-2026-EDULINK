package com.edulink.gui;

import com.edulink.gui.util.MyConnection;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DbDump {
    public static void main(String[] args) {
        try {
            Connection cnx = MyConnection.getInstance().getCnx();
            DatabaseMetaData metaData = cnx.getMetaData();
            String[] types = {"TABLE"};
            ResultSet tables = metaData.getTables(null, null, "%", types);
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                System.out.println("TABLE: " + tableName);
                ResultSet columns = metaData.getColumns(null, null, tableName, "%");
                while (columns.next()) {
                    String columnName = columns.getString("COLUMN_NAME");
                    String columnType = columns.getString("TYPE_NAME");
                    System.out.println("  - " + columnName + " (" + columnType + ")");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
