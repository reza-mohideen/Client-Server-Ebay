import javafx.stage.Stage;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.Base64;

public class Login implements ActionListener {

    private static JLabel userLabel;
    private static JTextField userText;
    private static JLabel passwordLabel;
    private static JPasswordField passwordText;
    private static JButton loginButton;
    private static JCheckBox existingUserCheckBox;
    private static JLabel success;
    private static Connection conn;
/*
    private static void setDBConn() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.jdbc.Driver");
        Connection conn = DriverManager.getConnection(
                "jdbc:mysql://auctiodb.cq2ovdkgqk2v.us-east-1.rds.amazonaws.com:3306/auctionDB","admin","gostars99");

        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("select * from users");

        while (rs.next()) {
            int id = rs.getInt("id");
            String username = rs.getString("username");
            String password = rs.getString("password");


            // print the results
            System.out.format("%s, %s", id, username, password);
        }
    }
*/
    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        JPanel panel = new JPanel();
        JFrame frame = new JFrame("Login Window");
        frame.setSize(350,200);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.add(panel);

        panel.setLayout(null);

        userLabel = new JLabel("User");
        userLabel.setBounds(10,20,80,25);
        panel.add(userLabel);

        userText = new JTextField(20);
        userText.setBounds(100,20,165,25);
        panel.add(userText);

        passwordLabel = new JLabel("Password");
        passwordLabel.setBounds(10,50,80,25);
        panel.add(passwordLabel);

        passwordText = new JPasswordField();
        passwordText.setBounds(100,50,165,25);
        panel.add(passwordText);

        loginButton = new JButton("Login");
        loginButton.setBounds(10,80,80,25);
        loginButton.addActionListener(new Login());
        panel.add(loginButton);

        existingUserCheckBox = new JCheckBox("Existing User");
        existingUserCheckBox.setBounds(100,80,165,25);
        existingUserCheckBox.setSelected(true);
        panel.add(existingUserCheckBox);

        success = new JLabel("");
        success.setBounds(10,110,300,25);
        panel.add(success);

        frame.setVisible(true);

        db_factory aws = new db_factory("auctiodb.cq2ovdkgqk2v.us-east-1.rds.amazonaws.com",
                3306,"auctionDB","admin","gostars99");

        conn = aws.getConnection();
    }

    /**
     * Invoked when an action occurs.
     *
     * @param e
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        String username = userText.getText();
        String password = new String(passwordText.getPassword());
        String encodedPassword = Base64.getEncoder().encodeToString(password.getBytes());

        // if user says they already have an account find the user in db
        if (existingUserCheckBox.isSelected()) {
            String query = "SELECT * FROM users";

            try {
                Statement s = conn.createStatement();
                ResultSet rs = s.executeQuery(query);

                boolean foundUser = false;
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String user = rs.getString("username");
                    String pass = rs.getString("password");

                    // found user
                    if (user.equals(username) && pass.equals(encodedPassword)) {
                        foundUser = true;
                        success.setText("Login Successful!!!");
                        try {
                            new Client().run(username);
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                        break;
                    }
                }

                // no matches found
                if (foundUser == false) {
                    success.setText("Username or Password Incorrect. Try Again.");
                }
                // if user was found, set flag back to false
                else {
                    foundUser = false;
                }



            }
            catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }

        // insert new account into database
        else {
            String query = "SELECT * FROM users";

            // check if username is already taken
            try {
                Statement s = conn.createStatement();
                ResultSet rs = s.executeQuery(query);

                boolean foundUser = false;
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String user = rs.getString("username");
                    String pass = rs.getString("password");

                    // found user
                    if (user.equals(username)) {
                        foundUser = true;
                        break;
                    }
                }

                // no matches found
                if (foundUser == false) {
                    PreparedStatement insert = conn.prepareStatement("INSERT INTO users (username,password) VALUES (?,?)");
                    insert.setString(1,username);
                    insert.setString(2,encodedPassword);
                    insert.executeUpdate();

                    try {
                        new Client().run(username);
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
                // if user was found, set flag back to false
                else {
                    foundUser = false;
                    success.setText("Username already taken.");
                }



            }
            catch (SQLException throwables) {
                throwables.printStackTrace();
            }

        }

    }




}
