import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseConnection {
    public Connection databaseConnection;

    public Connection getConnection() throws Exception {
        String databaseName = "authIHC";
        String databaseUser = "root";
        String databasePassword = "root1234";
        String url = "jdbc:mysql://localhost:3306/" + databaseName;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            databaseConnection = DriverManager.getConnection(url, databaseUser, databasePassword);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return databaseConnection;
    }
}
