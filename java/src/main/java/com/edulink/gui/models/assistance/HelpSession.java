package com.edulink.gui.models.assistance;

import java.sql.Timestamp;

/**
 * Represents a live tutoring session tied to a HelpRequest.
 * Tracks escrow, message count, and quality score for anti-farming.
 */
public class HelpSession {
    private int id;
    private int helpRequestId;
    private int tutorId;
    private int studentId;
    private String jitsiRoomId;
    private String summary;
    private boolean isActive;
    private int bountyEscrowed;
    private boolean bountyPaid;
    private int qualityScore;
    private int messageCount;
    private Timestamp startedAt;
    private Timestamp endedAt;

    // Transient display fields (joined from user table)
    private String tutorName;
    private String studentName;

    public HelpSession() {
        this.isActive = true;
        this.bountyPaid = false;
        this.qualityScore = 0;
        this.messageCount = 0;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getHelpRequestId() { return helpRequestId; }
    public void setHelpRequestId(int helpRequestId) { this.helpRequestId = helpRequestId; }

    public int getTutorId() { return tutorId; }
    public void setTutorId(int tutorId) { this.tutorId = tutorId; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public String getJitsiRoomId() { return jitsiRoomId; }
    public void setJitsiRoomId(String jitsiRoomId) { this.jitsiRoomId = jitsiRoomId; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public int getBountyEscrowed() { return bountyEscrowed; }
    public void setBountyEscrowed(int bountyEscrowed) { this.bountyEscrowed = bountyEscrowed; }

    public boolean isBountyPaid() { return bountyPaid; }
    public void setBountyPaid(boolean bountyPaid) { this.bountyPaid = bountyPaid; }

    public int getQualityScore() { return qualityScore; }
    public void setQualityScore(int qualityScore) { this.qualityScore = qualityScore; }

    public int getMessageCount() { return messageCount; }
    public void setMessageCount(int messageCount) { this.messageCount = messageCount; }

    public Timestamp getStartedAt() { return startedAt; }
    public void setStartedAt(Timestamp startedAt) { this.startedAt = startedAt; }

    public Timestamp getEndedAt() { return endedAt; }
    public void setEndedAt(Timestamp endedAt) { this.endedAt = endedAt; }

    public String getTutorName() { return tutorName; }
    public void setTutorName(String tutorName) { this.tutorName = tutorName; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    /** Duration in minutes */
    public long getDurationMinutes() {
        if (startedAt == null) return 0;
        long end = (endedAt != null) ? endedAt.getTime() : System.currentTimeMillis();
        return (end - startedAt.getTime()) / 60000;
    }
}
