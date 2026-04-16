package com.edulink.gui.scratch;

import com.edulink.gui.util.MyConnection;
import java.sql.*;

public class CheckUserColumns {
    public static void main(String[] args) {
        try {
            Connection cnx = MyConnection.getInstance().getCnx();
            DatabaseMetaData md = cnx.getMetaData();
            ResultSet rs = md.getColumns(null, null, "user", null);
            System.out.println("Columns in table 'user':");
            while (rs.next()) {
                System.out.println("- " + rs.getString("COLUMN_NAME") + " (" + rs.getString("TYPE_NAME") + ")");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
