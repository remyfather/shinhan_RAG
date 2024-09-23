package com.flutter.DataPreprocessingService.controller.ui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChatController {

    @GetMapping("/chat")
    public String getChatPage() {
        return "chat";  // chat.html 파일을 반환
    }

    @GetMapping("/enhancedChat")
    public String getEnhancedChatPage() {
        return "enhancedChat";  // chat.html 파일을 반환
    }
}
