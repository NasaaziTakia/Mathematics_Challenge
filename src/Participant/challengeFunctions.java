package Participant;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

import DatabaseConnection.DatabaseConnection;

public class challengeFunctions {
        public static void ViewChallenges(Scanner scanner) throws SQLException, InterruptedException, IOException {
            String query = "SELECT challengeNumber, end_date, duration, num_questions FROM challenge WHERE end_date > CURDATE()";

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            
                    PreparedStatement stmt = connection.prepareStatement(query);
                    ResultSet rs = stmt.executeQuery();
       
                
                   while (rs.next()) {
                       String challengeNumber = rs.getString("challengeNumber");
                       Date startDate = rs.getDate("end_date");
                       Date endDate = rs.getDate("end_date");
                       int duration = rs.getInt("duration");
                       int numQuestions = rs.getInt("num_questions");
                    System.out.println("Challenge Number: "+challengeNumber+", Start Date: "+startDate+", End Date: "+endDate+", Duration: "+duration+", Number of Questions: "+numQuestions+"\nEnter the command 'attemptChallenge challengeNumber' to attempt a challenge:");
                    String option = scanner.nextLine().trim();

                    if (option.equals("activate")){

                    }

                    
                   }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public static String checkChallenge(String name, String reg_number) {
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
