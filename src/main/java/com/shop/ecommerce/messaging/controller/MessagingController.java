package com.shop.ecommerce.messaging.controller;

import com.shop.ecommerce.common.config.OpenApiConfig;
import com.shop.ecommerce.messaging.dto.ConversationResponse;
import com.shop.ecommerce.messaging.dto.MessageResponse;
import com.shop.ecommerce.messaging.dto.SendMessageRequest;
import com.shop.ecommerce.messaging.dto.StartConversationRequest;
import com.shop.ecommerce.messaging.service.MessagingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@Tag(name = "Messages", description = "Listing conversations between users")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class MessagingController {

    private final MessagingService messagingService;

    public MessagingController(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @GetMapping
    @Operation(summary = "List the current user's conversations")
    public List<ConversationResponse> getConversations(Principal principal) {
        return messagingService.getConversations(principal.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Start a conversation about a listing")
    public ConversationResponse startConversation(
            Principal principal,
            @Valid @RequestBody StartConversationRequest request
    ) {
        return messagingService.startConversation(
                principal.getName(),
                request
        );
    }

    @GetMapping("/{conversationId}/messages")
    @Operation(summary = "Read messages and mark received messages as read")
    public List<MessageResponse> getMessages(
            Principal principal,
            @PathVariable Long conversationId
    ) {
        return messagingService.getMessages(
                principal.getName(),
                conversationId
        );
    }

    @PostMapping("/{conversationId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Send a message in a conversation")
    public MessageResponse sendMessage(
            Principal principal,
            @PathVariable Long conversationId,
            @Valid @RequestBody SendMessageRequest request
    ) {
        return messagingService.sendMessage(
                principal.getName(),
                conversationId,
                request
        );
    }
}
