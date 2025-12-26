package net.ikwa.aiengineerpractice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor

public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // "user", "ai", "admin"
    private String sender;

    @Column(columnDefinition = "TEXT")
    private String content;

    // NEW: store image URL if uploaded (e.g. Cloudinary, base64, etc.)
    private String imageUrl;

    private LocalDateTime createdAt;

    // User identifier for grouping
    private String conversationId;   // phone number

    // NEW: extra clarity
    private String phoneNumber;
    private String username;

    public ChatMessage() {}

    // Constructor for text messages
    public ChatMessage(String sender, String content, String conversationId, String phoneNumber, String username) {
        this.sender = sender;
        this.content = content;
        this.conversationId = conversationId; // phoneNumber
        this.phoneNumber = phoneNumber;
        this.username = username;
        this.createdAt = LocalDateTime.now();
    }

    // Constructor for image messages
    public ChatMessage(String sender, String content, String imageUrl, String conversationId, String phoneNumber, String username) {
        this.sender = sender;
        this.content = content;
        this.imageUrl = imageUrl;
        this.conversationId = conversationId;
        this.phoneNumber = phoneNumber;
        this.username = username;
        this.createdAt = LocalDateTime.now();
    }

    // getters & setters
    public Long getId() {
        return id;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}