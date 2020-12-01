import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Base64;

public class db_factory {
    private static String host;
    private static int port;
    private static String database;
    private static String username;
    private static String password;
    private static String endpoint;

    public db_factory (String host, int port, String database, String username, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;

        endpoint = "jdbc:mysql://" + host + ":" + port + "/" + database;
        System.out.println(endpoint);

        String pass = "gostars99";
        String encodedPassword = Base64.getEncoder().encodeToString(pass.getBytes());
        System.out.println(encodedPassword);


    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(endpoint, username, password);
    }

}
