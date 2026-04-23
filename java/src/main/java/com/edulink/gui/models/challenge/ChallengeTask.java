package com.edulink.gui.models.challenge;

public class ChallengeTask {

    private int id;
    private int challengeId;
    private String title;
    private String description;
    private int orderIndex;
    private boolean required;

    public ChallengeTask() {}

    public ChallengeTask(int challengeId, String title, String description, int orderIndex, boolean required) {
        this.challengeId = challengeId;
        this.title       = title;
        this.description = description;
        this.orderIndex  = orderIndex;
        this.required    = required;
    }

    public int getId()                  { return id; }
    public void setId(int id)           { this.id = id; }

    public int getChallengeId()              { return challengeId; }
    public void setChallengeId(int v)        { this.challengeId = v; }

    public String getTitle()                 { return title; }
    public void setTitle(String v)           { this.title = v; }

    public String getDescription()           { return description; }
    public void setDescription(String v)     { this.description = v; }

    public int getOrderIndex()               { return orderIndex; }
    public void setOrderIndex(int v)         { this.orderIndex = v; }

    public boolean isRequired()              { return required; }
    public void setRequired(boolean v)       { this.required = v; }

    @Override
    public String toString() { return title; }
}
