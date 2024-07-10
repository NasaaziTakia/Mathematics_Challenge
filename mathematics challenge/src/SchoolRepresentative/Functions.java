package SchoolRepresentative;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Functions {

    public static void DisplayOptions(String school_id) throws SQLException, InterruptedException, IOException {
        try (Scanner scanner = new Scanner(System.in)) {
            int option = 0; // Initialize option outside the loop

            while (true) {
                // Display menu options
                System.out.println("VIEW PARTICIPANTS MENU");
                System.out.println(
                        "1. Display all confirmed participants\n2. Display pending participants\n3. Display rejected participants");
                System.out.print("Enter display option: ");

                // Read and handle input
                try {
                    option = Integer.parseInt(scanner.nextLine().trim()); // Read input as String and convert to int
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input. Please enter a number.");
                    continue; // Restart loop if input is invalid
                }

                switch (option) {
                    case 1:
                        DisplayAllParticipants(school_id);
                        break;
                    case 2:
                        DisplayPendingParticipants(school_id, scanner);
                        break;
                    case 3:
                        DisplayRejectedParticipants(school_id);
                        break;
                    default:
                        System.out.println("Invalid option. Please choose again. Try again");
                        break;
                }
            }
        }
    }

    public static void DisplayAllParticipants(String school_id) throws SQLException, InterruptedException, IOException {
        String getParticipants = "SELECT * FROM participants WHERE studentNumber = ?";

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(getParticipants)) {
            preparedStatement.setString(1, school_id);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (!resultSet.isBeforeFirst()) { // Check if there are any rows at all
                System.out.println("No participants found.");
            } else {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
                while (resultSet.next()) {
                    int id = resultSet.getInt("id");
                    String username = resultSet.getString("username");
                    String firstname = resultSet.getString("firstname");
                    String lastname = resultSet.getString("lastname");
                    String email = resultSet.getString("email");
                    System.out.println(id + " - " + firstname + " " + lastname + " - " + email + " - ");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void DisplayPendingParticipants(String school_id, Scanner scanner) throws SQLException {
        Path path = Paths.get("src/Participants.txt");
        boolean record = false;

        try {
            // Read all lines from the file
            List<String> lines = Files.readAllLines(path);

            // Process each line
            for (String line : lines) {
                // Split the line by comma
                String[] parts = line.split(",");
                if (parts.length == 7) {
                    if (school_id.equals(parts[5].trim())) {
                        record = true;

                        // Assign each part to a variable
                        String username = parts[0].trim();
                        String firstname = parts[1].trim();
                        String lastname = parts[2].trim();
                        String emailAddress = parts[3].trim();
                        String dateOfBirth = parts[4].trim();
                        String schoolRegistrationNumber = parts[5].trim();
                        String imageFile = parts[6].trim();

                        // Display the variables
                        System.out.println(username + " - " + firstname + " - " + lastname + " - " + lastname + " - "
                                + emailAddress + " - " + dateOfBirth + " - " + schoolRegistrationNumber + " - "
                                + imageFile);
                    }

                } else {
                    System.out.println("Invalid line format: " + line);
                }
            }
            if (record) {
                System.out.print(
                        "Do you wish to activate or reject a participant? Type 'activate' or 'reject'. Press enter to go back: ");
                String option = scanner.nextLine().trim().toLowerCase();

                if (option.equals("activate")) {
                    System.out
                            .print("Enter student username from the list of pending students above you wish to activate: ");
                    boolean userFound = false;
                    String username = scanner.nextLine().trim().toLowerCase();
                    for (String line : lines) {

                        String[] parts = line.split(",");
                        if (parts.length == 7) {
                            // Assign each part to a variable
                            String participantusername = parts[0].trim();

                            if (username.equals(participantusername)) {
                                userFound = true;

                                String firstname = parts[1].trim();
                                String lastname = parts[2].trim();
                                String emailAddress = parts[3].trim();
                                String dateOfBirth = parts[4].trim();
                                String schoolRegistrationNumber = parts[5].trim();
                                String imageFile = parts[6].trim();
                                boolean insert = InsertParticipant(participantusername, firstname, lastname,
                                        emailAddress,
                                        dateOfBirth,
                                        schoolRegistrationNumber, imageFile, true);
                                if (insert) {
                                    deleteLineByUsername("src/Participants.txt", username);
                                }
                                Mailer.send(emailAddress,
                                        "You have been accepted and activated by your school representative");

                                break;

                            }

                        } else {
                            System.out.println("Invalid line format: " + line);
                        }
                    }

                } else if (option.equals("reject")) {
                    System.out
                            .print("Enter student username from the list of pending students above you wish to reject: ");
                    boolean userFound = false;
                    String username = scanner.nextLine().trim().toLowerCase();
                    for (String line : lines) {

                        String[] parts = line.split(",");
                        if (parts.length == 7) {
                            // Assign each part to a variable
                            String participantusername = parts[0].trim();

                            if (username.equals(participantusername)) {
                                userFound = true;
                                String firstname = parts[1].trim();
                                String lastname = parts[2].trim();
                                String emailAddress = parts[3].trim();
                                String dateOfBirth = parts[4].trim();
                                String schoolRegistrationNumber = parts[5].trim();
                                String imageFile = parts[6].trim();
                                boolean delete = InsertParticipant(participantusername, firstname, lastname,
                                        emailAddress,
                                        dateOfBirth,
                                        schoolRegistrationNumber, imageFile, false);
                                if (delete) {
                                    deleteLineByUsername("src/Participants.txt", username);
                                }
                                Mailer.send(emailAddress,
                                        "You have been rejected and deactivated by your school representative");

                                break;

                            }

                        } else {
                            System.out.println("Invalid line format: " + line);
                        }
                    }
                    if (!userFound) {
                        System.out.println("User with username not found in the file");
                    }

                } else {
                    System.out.println("Invalid option");
                }

            } else {
                System.out.println("No participants matching to your school registration number where found");
            }
        } catch (IOException e) {
            // Handle potential I/O errors
            e.printStackTrace();
        }
    }

    public static boolean InsertParticipant(String username, String firstname, String lastname, String email,
            String dateOfBirth, String school_id, String imageFile, Boolean option) throws SQLException {

        String insertParticipant = "";
        if (option) {
            insertParticipant = "INSERT INTO participants (firstname, lastname, email, date_of_birth, image_path,username, studentNumber) VALUES (?, ?, ?, ?, ?, ?, ?)";
        } else {
            insertParticipant = "INSERT INTO rejected (firstname, lastname, email, date_of_birth, image_path,username, studentNumber) VALUES (?, ?, ?, ?, ?, ?, ?)";
        }

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(insertParticipant)) {

            preparedStatement.setString(1, firstname);
            preparedStatement.setString(2, lastname);
            preparedStatement.setString(3, email);
            preparedStatement.setString(4, dateOfBirth);
            preparedStatement.setString(5, imageFile);
            preparedStatement.setString(6, username);

            preparedStatement.setString(7, school_id);

            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected > 0) {
                if (option) {
                    System.out.println("Participant accepted successfully.");
                } else {
                    System.out.println("Participant rejected successfully.");
                }
                // System.out.println("Participant inserted successfully.");
                return true;
            } else {
                System.out.println("Failed to insert participant.");
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void DisplayRejectedParticipants(String school_id)
            throws SQLException, InterruptedException, IOException {
        String getParticipants = "SELECT * FROM rejected WHERE studentNumber = ?";

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(getParticipants)) {
            preparedStatement.setString(1, school_id);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (!resultSet.isBeforeFirst()) { // Check if there are any rows at all
                System.out.println("No rejected participants found.");
            } else {
                // new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
                while (resultSet.next()) {
                    int id = resultSet.getInt("id");
                    String username = resultSet.getString("username");
                    String firstname = resultSet.getString("firstname");
                    String lastname = resultSet.getString("lastname");
                    String email = resultSet.getString("email");
                    String status = resultSet.getString("studentNumber");
                    System.out.println(id + " - " + firstname + " " + lastname + " - " + email + " - " + status);
                }
            }

        } catch (SQLException e) {
            System.out.println("An Exception was caught.");
        }
    }

    public static void deleteLineByUsername(String filePath, String usernameToDelete) {
        Path path = Paths.get(filePath);
        List<String> lines = new ArrayList<>();

        try {
            // Read all lines from the file
            lines = Files.readAllLines(path);

            // Create a new list to store updated lines (without the line to delete)
            List<String> updatedLines = new ArrayList<>();

            // Flag to check if username is found and deleted
            boolean deleted = false;

            // Iterate through the lines and add to updatedLines if not the line to delete
            for (String line : lines) {
                if (line.contains(usernameToDelete)) {
                    // Skip the line to delete
                    deleted = true;
                } else {
                    updatedLines.add(line);
                }
            }

            // If username was not found, print a message
            if (!deleted) {
                System.out.println("Username '" + usernameToDelete + "' not found in the file.");
                return;
            }

            // Write the updated lines back to the file
            Files.write(path, updatedLines);

            System.out.println("Username '" + usernameToDelete + "' deleted from the file.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
