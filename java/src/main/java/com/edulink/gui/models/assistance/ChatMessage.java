package com.edulink.gui.models.assistance;

import java.sql.Timestamp;

/**
 * A single message sent inside a tutoring session.
 * Stores AI-enriched metadata: toxicity flag, sentiment, language.
 */
public class ChatMessage {
    private int id;
    private int sessionId;
    private int senderId;
    private String content;
    private boolean isToxic;
    private String sentiment;      // e.g. POSITIVE, NEGATIVE, NEUTRAL
    private String detectedLanguage; // e.g. en, fr, ar
    private Timestamp timestamp;

    // Transient display field
    private String senderName;
    private boolean isMine; // set by controller, true when senderId == current user

    public ChatMessage() {
        this.isToxic = false;
        this.sentiment = "NEUTRAL";
        this.detectedLanguage = "en";
        this.timestamp = new Timestamp(System.currentTimeMillis());
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSessionId() { return sessionId; }
    public void setSessionId(int sessionId) { this.sessionId = sessionId; }

    public int getSenderId() { return senderId; }
    public void setSenderId(int senderId) { this.senderId = senderId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public boolean isToxic() { return isToxic; }
    public void setToxic(boolean toxic) { isToxic = toxic; }

    public String getSentiment() { return sentiment; }
    public void setSentiment(String sentiment) { this.sentiment = sentiment; }

    public String getDetectedLanguage() { return detectedLanguage; }
    public void setDetectedLanguage(String detectedLanguage) { this.detectedLanguage = detectedLanguage; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public boolean isMine() { return isMine; }
    public void setMine(boolean mine) { isMine = mine; }
}
