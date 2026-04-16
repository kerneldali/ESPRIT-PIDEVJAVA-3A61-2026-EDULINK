package com.edulink.gui.models.challenge;

import java.time.LocalDateTime;

public class ChallengeParticipation {

    public enum Status {
        JOINED,     // Rejoint, pas encore soumis
        SUBMITTED,  // Soumis, en attente de validation admin
        COMPLETED,  // Validé par l'admin → XP accordés
        REJECTED    // Rejeté par l'admin → peut resoumettre
    }

    private int id;
    private int challengeId;
    private int userId;
    private Status status;
    private LocalDateTime joinedAt;
    private LocalDateTime completedAt;

    // Champs de soumission
    private String submissionText;
    private String submissionFilePath;

    public ChallengeParticipation() {}

    public ChallengeParticipation(int challengeId, int userId) {
        this.challengeId = challengeId;
        this.userId      = userId;
        this.status      = Status.JOINED;
        this.joinedAt    = LocalDateTime.now();
    }

    // Getters / Setters
    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public int getChallengeId()                 { return challengeId; }
    public void setChallengeId(int v)           { this.challengeId = v; }

    public int getUserId()                      { return userId; }
    public void setUserId(int v)                { this.userId = v; }

    public Status getStatus()                   { return status; }
    public void setStatus(Status status)        { this.status = status; }

    public LocalDateTime getJoinedAt()          { return joinedAt; }
    public void setJoinedAt(LocalDateTime v)    { this.joinedAt = v; }

    public LocalDateTime getCompletedAt()       { return completedAt; }
    public void setCompletedAt(LocalDateTime v) { this.completedAt = v; }

    public String getSubmissionText()           { return submissionText; }
    public void setSubmissionText(String v)     { this.submissionText = v; }

    public String getSubmissionFilePath()       { return submissionFilePath; }
    public void setSubmissionFilePath(String v) { this.submissionFilePath = v; }

    public boolean isCompleted()  { return status == Status.COMPLETED; }
    public boolean isSubmitted()  { return status == Status.SUBMITTED; }
    public boolean isRejected()   { return status == Status.REJECTED; }
}
