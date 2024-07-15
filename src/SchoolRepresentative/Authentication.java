package SchoolRepresentative;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import DatabaseConnection.DatabaseConnection;

public class Authentication {
    public static String validateUser(String name, String reg_number) {
        String VALIDATE_USER_SQL = "SELECT * FROM schools WHERE registration_number = ? AND representative_name = ?";

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(VALIDATE_USER_SQL)) {
            preparedStatement.setString(1, reg_number);
            preparedStatement.setString(2, name);

            ResultSet resultSet = preparedStatement.executeQuery();
            String school_id = "";
            while (resultSet.next()) {
                school_id = resultSet.getString("registration_number");
            }
            return school_id;
        } catch (SQLException e) {

            return "connectionfailed";
        }
    }

}
