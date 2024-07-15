package Participant;

public class Challenge {
    private int id;
    private String questionText;
    private int marks;
    private int challengeId;

    // Constructor
    public Challenge(int id, String title, int marks, int challengeId) {
        this.id = id;
        this.questionText = questionText;
        this.marks = marks;
        this.challengeId = challengeId;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getQuestionText() {
        return questionText;
    }

    public int getMarks() {
        return marks;
    }

    public int getChallengeId() {
        return challengeId;
    }

    // Override toString for easy display
    @Override
    public String toString() {
        return "Question{" +
                "id=" + id +
                ", questionText='" + questionText + '\'' +
                ", marks=" + marks +
                ", challengeId=" + challengeId +
                '}';
    }
    
}
