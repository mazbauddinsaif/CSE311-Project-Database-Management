package marketplace;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Single point of contact with the MariaDB instance shipped with XAMPP.
 * Edit USER / PASSWORD here if the local MySQL root account has a password.
 */
public final class Db {

    private static final String URL =
        "jdbc:mysql://127.0.0.1:3306/marketplace_db"
        + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Dhaka";

    private static final String USER     = "root";
    private static final String PASSWORD = "";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(
                "MySQL JDBC driver not found. Put mysql-connector-j-8.0.33.jar on the classpath.");
        }
    }

    private Db() { }

    public static Connection open() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
