package Participant;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
// import java.util.Scanner;

// code
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
    private String loggedInUsername = null;
    private String loggedInStudentNumber = null;

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
                } else if (loggedIn && command.startsWith("attemptchallenge")) {
                    handleAttemptChallenge(command, loggedInUsername, loggedInStudentNumber);
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
            loggedInUsername = username;
            loggedInStudentNumber = studentNumber;
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

            String studentNumberPrefix = studentNumber.split("/")[0];

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
                    checkStudentStmt.setString(1, studentNumberPrefix);
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
                        .append(", Number of Questions: ").append(numQuestions).append("\n");
            }

            out.println(response.toString());
        } catch (SQLException e) {
            System.out.println(e);
            e.printStackTrace();
            out.println("Error fetching challenge data");
        }
    }

    private void handleAttemptChallenge(String command, String username, String studentNumber) {
        String[] parts = command.split(" ");
        String challengeNumber = "";
        if (parts.length > 1) {
            challengeNumber = parts[1].toUpperCase();
        }

        // Implement handling of challenge attempts here
        Challenge challengeDetails = new Challenge();

        // String challengeNumber = ""; // Example challenge number
        // String username = ""; // Example username
        // String studentNumber = ""; // Example student number

        if (challengeDetails.isAvailable(challengeNumber)) {
            int duration = challengeDetails.getChallengeDuration();
            long start = challengeDetails.getStartDate();
            long end = challengeDetails.getEndDate();
            int numQuestions = challengeDetails.getNumQuestions();

            out.println("Challenge Number: " + challengeNumber);
            out.println("Duration: " + duration + " minutes");
            out.println("Start Date: " + new java.util.Date(start));
            out.println("End Date: " + new java.util.Date(end));
            out.println("Number of Questions: " + numQuestions);

            challengeDetails.presentQuestions(challengeNumber, username, studentNumber);
        } else {
            out.println("Challenge not available or invalid challenge number.");
        }
        // Example: Check if the challenge number is valid and handle the attempt
        // This part would typically involve interactions with the Challenge class or database
    
    }
    class Question {
        int id;
        String text;
        int marks;
    
        public Question(int id, String text, int marks) {
            this.id = id;
            this.text = text;
            this.marks = marks;
        }
    }
    
    public class Challenge {
        private String dbUrl = "jdbc:mysql://localhost:3306/competition";
        private String dbUsername = "root";
        private String dbPassword = "";
        private int challengeDuration;
        private long start_Date;
        private long end_Date;
        private int numQuestions;
    
        public boolean isAvailable(String challengeNumber) {
            String query = "SELECT duration, start_date, end_date, num_questions FROM challenge WHERE challengeNumber = ? AND end_date > CURDATE()";
    
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
                 PreparedStatement stmt = conn.prepareStatement(query)) {
    
                stmt.setString(1, challengeNumber);
                ResultSet rs = stmt.executeQuery();
    
                if (rs.next()) {
                    // Challenge exists
                    challengeDuration = rs.getInt("duration");
                    start_Date = rs.getDate("start_date").getTime();
                    end_Date = rs.getDate("end_date").getTime();
                    numQuestions = rs.getInt("num_questions");
                    return true;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            // Challenge does not exist
            challengeDuration = 0;
            start_Date = 0;
            end_Date = 0;
            numQuestions = 0;
            return false;
        }
    
        // Getters for the stored details
        public int getChallengeDuration() {
            return challengeDuration;
        }
    
        public long getStartDate() {
            return start_Date;
        }
    
        public long getEndDate() {
            return end_Date;
        }
    
        public int getNumQuestions() {
            return numQuestions;
        }
    
        public List<Question> getQuestions(String challengeNumber) {
            List<Question> questions = new ArrayList<>();
            String query = "SELECT id, question_text, marks FROM questions WHERE challengeNumber = ? ORDER BY RAND()";
    
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
                 PreparedStatement stmt = conn.prepareStatement(query)) {
    
                stmt.setString(1, challengeNumber);
                ResultSet rs = stmt.executeQuery();
    
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String text = rs.getString("question_text");
                    int marks = rs.getInt("marks");
                    questions.add(new Question(id, text, marks));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return questions;
        }
    
        public void presentQuestions(String challengeNumber, String username, String studentNumber) {
            // Count the number of attempts made by the participant
            try {
                int attemptCount = countAttempt(challengeNumber, username, studentNumber);
        
                // Check if the participant has not exceeded the maximum attempts (3 attempts allowed)
                if (attemptCount <= 2) {
                    List<Question> questions = getQuestions(challengeNumber);
                    List<String> answers = new ArrayList<>(Collections.nCopies(questions.size(), ""));
                    List<Long> questionTimes = new ArrayList<>(Collections.nCopies(questions.size(), 0L));
                    // double scorePercentage = 0; // Initialize score to zero
                    long startTime = System.currentTimeMillis();
                    long endTime = startTime + challengeDuration * 60000; // challengeDuration is in minutes
                    boolean isComplete = false;
                    int remain =2-attemptCount;
        
                    try {
                        for (int i = 0; i < questions.size(); i++) {
                            if (System.currentTimeMillis() > endTime) {
                                System.out.println("Time is up!");
                                break;
                            }
                            if (i >= numQuestions) {
                                out.println("All questions for this challenge have been presented.");
                                isComplete = true;
                                break;
                            }
        
                            long questionStartTime = System.currentTimeMillis();
                            long remainingTime = (endTime - System.currentTimeMillis()) / 1000; // in seconds
                            out.println("Remaining questions: " + (numQuestions - (i + 1)));
                            out.println("Remaining time: " + remainingTime / 60 + " minutes " + remainingTime % 60 + " seconds");
                            out.println("Question " + (i + 1) + " of " + numQuestions + ":");
                            out.println(questions.get(i).text);
                            out.println("Enter answer: ");
                            String answer = in.readLine().trim();
                            answers.set(i, answer);
                            long questionEndTime = System.currentTimeMillis();
                            questionTimes.set(i, questionEndTime - questionStartTime);
                        }
        
                        // Calculate the score using only the first numQuestions questions
                        long endingTime = System.currentTimeMillis();
                        long totalTime = endingTime - startTime;
                        // Calculate the score using only the first numQuestions questions
                        double scorePercentage = calculateScorePercentage(questions.subList(0, numQuestions), answers.subList(0, numQuestions));
                        out.println("Your score is: " + scorePercentage + "%");
                        out.println("Total Time Taken: " + totalTime / 60000 + " minutes " + (totalTime % 60000) / 1000 + " seconds");
                        out.println("Remaining chances to attempt  this challenge : " + remain);
                        
                        out.println("Available Challenges Are : " );
                        sendChallengeData();
                        // Store the challenge attempt in the database
                        handleAttempt(challengeNumber, username, studentNumber, scorePercentage, isComplete);
        
                        // Generate the report using only the first numQuestions questions
                        ReportEmail( studentNumber,  questions,  answers,  questionTimes,  startTime);
                        generateReport(questions.subList(0, numQuestions), answers.subList(0, numQuestions), questionTimes.subList(0, numQuestions), startTime);
                        
                    } finally {
                        // Close any resources if needed
                    }
                } else {
                    out.println("You have exceeded the maximum number of attempts for this challenge.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        
        public void handleAttempt(String challengeNumber, String username, String studentNumber, double scorePercentage, boolean isComplete) {
            String query = "INSERT INTO attempt (challengeNumber, username, studentNumber, score, isComplete) VALUES (?, ?, ?, ?, ?)";
        
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
                 PreparedStatement stmt = conn.prepareStatement(query)) {
        
                stmt.setString(1, challengeNumber);
                stmt.setString(2, username);
                stmt.setString(3, studentNumber);
                stmt.setDouble(4, scorePercentage);
                stmt.setString(5, isComplete ? "yes" : "no");
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        
         
            public double calculateScorePercentage(List<Question> questions, List<String> answers) {
                int score = 0;
                int totalMarks = 0;
            
                for (int i = 0; i < questions.size(); i++) {
                    String correctAnswer = getCorrectAnswer(questions.get(i).id);
                    String userAnswer = answers.get(i);
                    int marks = questions.get(i).marks;
            
                    if (userAnswer.equals("-")) {
                        // No marks for unsure answers
                    } else if (userAnswer.equals(correctAnswer)) {
                        score += marks;
                    } else {
                        score -= 3;
                    }
                    totalMarks += marks;
                }
            
                return totalMarks > 0 ? (double) score / totalMarks * 100 : 0;
            }
        
        private String getCorrectAnswer(int questionId) {
            String query = "SELECT answer_text FROM answers WHERE question_id = ?";
            String correctAnswer = "";
        
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
                 PreparedStatement stmt = conn.prepareStatement(query)) {
        
                stmt.setInt(1, questionId);
                ResultSet rs = stmt.executeQuery();
        
                if (rs.next()) {
                    correctAnswer = rs.getString("answer_text");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return correctAnswer;
        }
        
        public void generateReport(List<Question> questions, List<String> answers, List<Long> questionTimes, long startTime) {
            long endTime = System.currentTimeMillis();
            long totalTimeTaken = endTime - startTime; // in milliseconds
        
            out.println("\n=== Challenge Report ===");
            out.println("Total Time Taken: " + totalTimeTaken / 60000 + " minutes " + (totalTimeTaken % 60000) / 1000 + " seconds");
        
            for (int i = 0; i < questions.size(); i++) {
                out.println("\nQuestion " + (i + 1) + ": " + questions.get(i).text);
                out.println("Your answer: " + answers.get(i));
                String correctAnswer = getCorrectAnswer(questions.get(i).id);
                int marks = 0;
                if (answers.get(i).equals("-")) {
                    marks = 0;
                } else if (answers.get(i).equals(correctAnswer)) {
                    marks = questions.get(i).marks;
                } else {
                    marks = -3;
                }
                out.println("Marks: " + marks);
                out.println("Time taken: " + questionTimes.get(i) / 1000 + " seconds");
            }
        }
        
        public int countAttempt(String challengeNumber, String username, String studentNumber) {
            int count = 0;
            String query = "SELECT COUNT(*) AS attempt_count FROM attempt WHERE challengeNumber = ? AND username = ? AND studentNumber = ?";
        
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
                 PreparedStatement stmt = conn.prepareStatement(query)) {
        
                stmt.setString(1, challengeNumber);
                stmt.setString(2, username);
                stmt.setString(3, studentNumber);
        
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    count = rs.getInt("attempt_count");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        
            return count;
        }
        public String generateEmailReport(List<Question> questions, List<String> answers, List<Long> questionTimes, long startTime) {
            long endTime = System.currentTimeMillis();
            long totalTimeTaken = endTime - startTime; // in milliseconds
            
            StringBuilder report = new StringBuilder();
            report.append("\n=== Challenge Report ===\n");
            report.append("Total Time Taken: ").append(totalTimeTaken / 60000).append(" minutes ")
                  .append((totalTimeTaken % 60000) / 1000).append(" seconds\n");
            
            for (int i = 0; i < questions.size(); i++) {
                report.append("\nQuestion ").append(i + 1).append(": ").append(questions.get(i).text).append("\n");
                report.append("Your answer: ").append(answers.get(i)).append("\n");
                String correctAnswer = getCorrectAnswer(questions.get(i).id);
                int marks = 0;
                if (answers.get(i).equals("-")) {
                    marks = 0;
                } else if (answers.get(i).equals(correctAnswer)) {
                    marks = questions.get(i).marks;
                } else {
                    marks = -3;
                }
                report.append("Marks: ").append(marks).append("\n");
                report.append("Time taken: ").append(questionTimes.get(i) / 1000).append(" seconds\n");
            }
            
            return report.toString();
        }
        
        public void ReportEmail(String studentNumber, List<Question> questions, List<String> answers, List<Long> questionTimes, long startTime) {
            String from = "tahiatasha23@gmail.com"; // replace with your email
            String host = "smtp.gmail.com";
            String email = getEmailFromDatabase(studentNumber);
        
            if (email == null) {
                System.out.println("No participant found with student number: " + studentNumber);
                return;
            }
        
            Properties properties = System.getProperties();
            properties.setProperty("mail.smtp.host", host);
            properties.setProperty("mail.smtp.port", "587");
            properties.setProperty("mail.smtp.auth", "true");
            properties.setProperty("mail.smtp.starttls.enable", "true");
        
            Session session = Session.getDefaultInstance(properties, new javax.mail.Authenticator() {
                protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                    return new javax.mail.PasswordAuthentication("tahiatasha23@gmail.com", "zdkw aorj fxil ssya"); // replace with your email and password
                }
            });
        
            try {
                String report = generateEmailReport(questions, answers, questionTimes, startTime);
        
                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(from));
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(email));
                message.setSubject("Challenge Report");
                message.setText(report);
        
                Transport.send(message);
                out.println("Report sent successfully to your email " + email);
                System.out.println("Report sent successfully to participant email " + email);
            } catch (MessagingException mex) {
                mex.printStackTrace();
            }
        }
        
        private String getEmailFromDatabase(String studentNumber) {
            String email = null;
            String url = "jdbc:mysql://localhost:3306/competition"; // replace with your database URL
            String user = "root"; // replace with your database username
            String password = ""; // replace with your database password
        
            try (Connection conn = DriverManager.getConnection(url, user, password)) {
                String query = "SELECT email FROM participants WHERE studentNumber = ?";
                PreparedStatement preparedStatement = conn.prepareStatement(query);
                preparedStatement.setString(1, studentNumber);
                ResultSet resultSet = preparedStatement.executeQuery();
        
                if (resultSet.next()) {
                    email = resultSet.getString("email");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        
            return email;
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
                        .append(", Number of Questions: ").append(numQuestions).append("\n");
            }

            out.println(response.toString());
        } catch (SQLException e) {
            System.out.println(e);
            e.printStackTrace();
            out.println("Error fetching challenge data");
        }
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
