package Participant;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

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
        int attemptCount = countAttempt(challengeNumber, username, studentNumber);

        // Check if the participant has not exceeded the maximum attempts (3 attempts allowed)
        if (attemptCount <= 3) {
            List<Question> questions = getQuestions(challengeNumber);
            List<String> answers = new ArrayList<>(Collections.nCopies(questions.size(), ""));
            List<Long> questionTimes = new ArrayList<>(Collections.nCopies(questions.size(), 0L));
            int score = 0; // Initialize score to zero
            long startTime = System.currentTimeMillis();
            long endTime = startTime + challengeDuration * 60000; // challengeDuration is in minutes
            Scanner scanner = new Scanner(System.in);
            boolean isComplete = false;

            try {
                for (int i = 0; i < questions.size(); i++) {
                    if (System.currentTimeMillis() > endTime) {
                        System.out.println("Time is up!");
                        break;
                    }
                    if (i >= numQuestions) {
                        System.out.println("All questions for this challenge have been presented.");
                        isComplete = true;
                        break;
                    }

                    long questionStartTime = System.currentTimeMillis();
                    long remainingTime = (endTime - System.currentTimeMillis()) / 1000; // in seconds
                    System.out.println("Remaining questions: " + (numQuestions - (i + 1)));
                    System.out.println("Remaining time: " + remainingTime / 60 + " minutes " + remainingTime % 60 + " seconds");
                    System.out.println(" Question " + (i + 1) + " of " + numQuestions + ":");
                    System.out.println(questions.get(i).text);
                    System.out.print("Enter answer: ");
                    answers.set(i, scanner.nextLine());
                    long questionEndTime = System.currentTimeMillis();
                    questionTimes.set(i, questionEndTime - questionStartTime);
                }

                // Calculate the score
                score = calculateScore(questions, answers);
                System.out.println("Your score is: " + score);

                // Store the challenge attempt in the database
                handleAttempt(challengeNumber, username, studentNumber, score, isComplete);

                // Generate the report
                generateReport(questions, answers, questionTimes, startTime);
            } finally {
                // Close the scanner to release resources
                scanner.close();
            }
        } else {
            System.out.println("You have exceeded the maximum number of attempts for this challenge.");
        }
    }
    
    public void handleAttempt(String challengeNumber, String username, String studentNumber, int score, boolean isComplete) {
        String query = "INSERT INTO attempt (challengeNumber, username, studentNumber, score, isComplete) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, challengeNumber);
            stmt.setString(2, username);
            stmt.setString(3, studentNumber);
            stmt.setInt(4, score);
            stmt.setString(5, isComplete ? "yes" : "no");
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int calculateScore(List<Question> questions, List<String> answers) {
        int score = 0;

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
        }
        return score;
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

        System.out.println("\n=== Challenge Report ===");
        System.out.println("Total Time Taken: " + totalTimeTaken / 60000 + " minutes " + (totalTimeTaken % 60000) / 1000 + " seconds");

        for (int i = 0; i < questions.size(); i++) {
            System.out.println("\nQuestion " + (i + 1) + ": " + questions.get(i).text);
            System.out.println("Your answer: " + answers.get(i));
            String correctAnswer = getCorrectAnswer(questions.get(i).id);
            // System.out.println("Correct answer: " + correctAnswer);
            int marks = 0;
            if (answers.get(i).equals("-")) {
                marks = 0;
            } else if (answers.get(i).equals(correctAnswer)) {
                marks = questions.get(i).marks;
            } else {
                marks = -3;
            }
            System.out.println("Marks: " + marks);
            System.out.println("Time taken: " + questionTimes.get(i) / 1000 + " seconds");
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

    public static void main(String[] args) {
        Challenge challengeDetails = new Challenge();

        String challengeNumber = ""; // Example challenge number
        String username = ""; // Example username
        String studentNumber = ""; // Example student number

        if (challengeDetails.isAvailable(challengeNumber)) {
            int duration = challengeDetails.getChallengeDuration();
            long start = challengeDetails.getStartDate();
            long end = challengeDetails.getEndDate();
            int numQuestions = challengeDetails.getNumQuestions();

            System.out.println("Challenge Number: " + challengeNumber);
            System.out.println("Duration: " + duration + " minutes");
            System.out.println("Start Date: " + new java.util.Date(start));
            System.out.println("End Date: " + new java.util.Date(end));
            System.out.println("Number of Questions: " + numQuestions);

            challengeDetails.presentQuestions(challengeNumber, username, studentNumber);
        } else {
            System.out.println("Challenge not available or invalid challenge number.");
        }
    }
}