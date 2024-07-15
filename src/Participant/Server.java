package Participant;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class Server {

    public static void main(String[] args) {
        try (ServerSocket ss = new ServerSocket(9806)) {
            System.out.println("Server started. Waiting for clients...");

            while (true) {
                try {
                    Socket soc = ss.accept();
                    System.out.println("New client connected: " + soc);

                    ClientHandler clientHandler = new ClientHandler(soc);
                    clientHandler.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class ClientHandler extends Thread {
    private Socket soc;
    private BufferedReader in;
    private PrintWriter out;
    private boolean loggedIn = false;

    public ClientHandler(Socket soc) {
        this.soc = soc;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(soc.getInputStream()));
            out = new PrintWriter(soc.getOutputStream(), true);

            handleClientCommands();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeResources();
        }
    }

    private void handleClientCommands() {
        String command;

        try {
            while ((command = in.readLine()) != null) {
                if (command.startsWith("login")) {
                    handleLogin(command);
                } else if (command.startsWith("register")) {
                    handleRegistration(command);
                } else if (loggedIn && command.equals("view challenge")) {
                    sendChallengeData();
                } else if (loggedIn && command.equals("logout")) {
                    out.println("Logout successful");
                    loggedIn = false;
                    closeResources();
                    break;
                } else {
                    out.println("Invalid command");
                }
            }
        } catch (IOException e) {
            System.out.println("Connection with client lost: " + e.getMessage());
        }
    }

    private void handleLogin(String command) {
        String[] details = command.split(" ");
        if (details.length < 3) {
            out.println("Invalid login data");
            return;
        }

        String username = details[1];
        String studentNumber = details[2];

        if (validateLogin(username, studentNumber)) {
            loggedIn = true;
            out.println("Login successful");
            System.out.println("User '" + username + "' logged in successfully");
        } else {
            out.println("Invalid username or student number. Please register.");
            System.out.println("Invalid login attempt for username '" + username + "'");
        }
    }

    private boolean validateLogin(String username, String studentNumber) {
        String dbUrl = "jdbc:mysql://localhost:3306/competition";
        String dbUsername = "root";
        String dbPassword = "";

        String query = "SELECT username, studentNumber FROM participants WHERE username = ? AND studentNumber = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            stmt.setString(2, studentNumber);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void handleRegistration(String command) {
        try {
            String userDetails = command.substring(9); // Remove "register " prefix
            String[] details = userDetails.split(" ");
            if (details.length < 7) {
                out.println("Invalid input data");
                return;
            }

            String username = details[0];
            String firstname = details[1];
            String lastname = details[2];
            String emailAddress = details[3];
            String dateOfBirth = details[4];
            String studentNumber = details[5];
            String imageFilePath = details[6];

            // Check if the username already exists
            if (usernameExists(username)) {
                out.println("Username '" + username + "' already exists. Please choose a different username.");
                System.out.println("Username '" + username + "' already exists. Registration failed.");
                return;
            }

            // Check if the user has been previously rejected
            if (isRejected(username, studentNumber)) {
                out.println("You were rejected by this school.");
                System.out.println("Registration attempt with rejected username and student number: " + username + ", " + studentNumber);
                return;
            }

            String representativeEmail = null;
            boolean matchFound = false;

            // Check if the student number matches any school registration number
            try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/competition", "root", "")) {
                String checkStudentSql = "SELECT representative_email FROM schools WHERE registration_number = ?";
                try (PreparedStatement checkStudentStmt = conn.prepareStatement(checkStudentSql)) {
                    checkStudentStmt.setString(1, studentNumber);
                    ResultSet rs = checkStudentStmt.executeQuery();
                    if (rs.next()) {
                        representativeEmail = rs.getString("representative_email");
                        matchFound = true;
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                out.println("Database error: " + e.getMessage());
                return;
            }

            if (!matchFound) {
                out.println("No match found for the provided student number.");
                System.out.println("No match found for the provided student number.");
                return;
            }

            // Handle the image file upload
            File imageFile = new File("received_" + imageFilePath.substring(imageFilePath.lastIndexOf(File.separator) + 1));
            try (FileOutputStream fos = new FileOutputStream(imageFile);
                 InputStream is = soc.getInputStream()) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                e.printStackTrace();
                out.println("File handling error: " + e.getMessage());
                return;
            }

            // Write user details to a file
            writeToFile(username, firstname, lastname, emailAddress, dateOfBirth, studentNumber, imageFile.getAbsolutePath());

            // Send confirmation email to the school representative
            sendEmail(representativeEmail, firstname, lastname);

            out.println("User successfully registered");
            System.out.println("User successfully registered");
        } catch (Exception e) {
            e.printStackTrace();
            out.println("An unexpected error occurred: " + e.getMessage());
        }
    }

    private boolean usernameExists(String username) {
        String filePath = "src/Participants.txt";

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] details = line.split(",");
                if (details.length > 0 && details[0].equals(username)) {
                    return true; // Username exists
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false; // Username does not exist
    }

    private boolean isRejected(String username, String studentNumber) {
        String dbUrl = "jdbc:mysql://localhost:3306/competition";
        String dbUsername = "root";
        String dbPassword = "";

        String query = "SELECT username, studentNumber FROM rejected WHERE username = ? AND studentNumber = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            stmt.setString(2, studentNumber);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return true; // Username and student number found in rejected table
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false; // Username and student number not found in rejected table
    }

    private void writeToFile(String username, String firstname, String lastname, String email, String dob, String studentNumber, String imagePath) {
        String filePath = "src/Participants.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write(username + "," + firstname + "," + lastname + "," + email + "," + dob + "," + studentNumber + "," + imagePath);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendEmail(String to, String firstname, String lastname) {
        String from = "tahiatasha23@gmail.com"; // replace with your email
        String host = "smtp.gmail.com";

        Properties properties = System.getProperties();
        properties.setProperty("mail.smtp.host", host);
        properties.setProperty("mail.smtp.port", "587");
        properties.setProperty("mail.smtp.auth", "true");
        properties.setProperty("mail.smtp.starttls.enable", "true");

        Session session = Session.getDefaultInstance(properties, new javax.mail.Authenticator() {
            protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                return new javax.mail.PasswordAuthentication("tahiatasha23@gmail.com", "jgsm dfga ojpd myrt"); // replace with your email and password
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject("Participant Confirmation Needed");
            message.setText("A new participant has registered.\n\nFirst Name: " + firstname + "\nLast Name: " + lastname + "\n\nPlease confirm the participant.");

            Transport.send(message);
            System.out.println("Sent message successfully to " + to);
        } catch (MessagingException mex) {
            mex.printStackTrace();
        }
    }

    private void sendChallengeData() {
        String dbUrl = "jdbc:mysql://localhost:3306/competition";
        String dbUsername = "root";
        String dbPassword = "";

        String query = "SELECT challengeNumber, end_date, duration, num_questions FROM challenge WHERE end_date > CURDATE()";

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            StringBuilder response = new StringBuilder();
            while (rs.next()) {
                String challengeNumber = rs.getString("challengeNumber");
                String endDate = rs.getString("end_date");
                int duration = rs.getInt("duration");
                int numQuestions = rs.getInt("num_questions");

                response.append("Challenge Number: ").append(challengeNumber)
                        .append(", End Date: ").append(endDate)
                        .append(", Duration: ").append(duration)
                        .append(", Number of Questions: ").append(numQuestions).append("\nEnter the command 'attemptChallenge challengeNumber' to attempt a challenge:");
            }

            out.println(response.toString());
        } catch (SQLException e) {
            System.out.println(e);
            e.printStackTrace();
            out.println("Error fetching challenge data");
        }
    }

    private void closeResources() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (soc != null) soc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


