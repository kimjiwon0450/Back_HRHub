package com.playdata.hrservice.hr.dto;

// ChatGPTMessagesRequest.java
import lombok.Data;
import java.util.List;

@Data
public class ChatGPTMessagesRequest {
    private List<Message> messages;

    @Data
    public static class Message {
        private String role;    // "system", "user", "assistant"
        private String content;
    }
}


