package harvard.capstone.digitaltherapy.cbt.controller;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class CBTTextController {

    private final SimpMessagingTemplate messagingTemplate;

    // Constructor name should match the class name
    public CBTTextController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/cbt-text")
    public void sendMessage(@Payload String message) {
        // Input validation
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }

        try {
            // Send the received message to all connected clients
            messagingTemplate.convertAndSend("/topic/messages", message.trim());
        } catch (Exception e) {
            // Handle messaging errors
            throw new RuntimeException("Failed to send message", e);
        }
    }
}

