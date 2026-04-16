import java.sql.*;

public class CheckEnrollments {
    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection cnx = DriverManager.getConnection("jdbc:mysql://localhost:3306/edulinkpi", "root", "");
            
            System.out.println("--- ENROLLMENT TABLE ---");
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM enrollment");
            int count = 0;
            while(rs.next()) {
                System.out.println("ID: " + rs.getInt("id") + ", Student: " + rs.getInt("student_id") + ", Course: " + rs.getInt("cours_id"));
                count++;
            }
            System.out.println("Total: " + count);
            
            System.out.println("\n--- RESOURCE_COMPLETION TABLE ---");
            rs = st.executeQuery("SELECT * FROM resource_completion");
            count = 0;
            while(rs.next()) {
                System.out.println("ID: " + rs.getInt("id") + ", Student: " + rs.getInt("student_id") + ", Resource: " + rs.getInt("resource_id"));
                count++;
            }
            System.out.println("Total: " + count);
            
            cnx.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
