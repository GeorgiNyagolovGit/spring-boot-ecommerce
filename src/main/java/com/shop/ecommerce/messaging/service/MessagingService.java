package com.shop.ecommerce.messaging.service;

import com.shop.ecommerce.auth.entity.User;
import com.shop.ecommerce.auth.repository.UserRepository;
import com.shop.ecommerce.common.exception.NotFoundException;
import com.shop.ecommerce.messaging.dto.ConversationResponse;
import com.shop.ecommerce.messaging.dto.MessageResponse;
import com.shop.ecommerce.messaging.dto.SendMessageRequest;
import com.shop.ecommerce.messaging.dto.StartConversationRequest;
import com.shop.ecommerce.messaging.entity.Conversation;
import com.shop.ecommerce.messaging.entity.Message;
import com.shop.ecommerce.messaging.repository.ConversationRepository;
import com.shop.ecommerce.messaging.repository.MessageRepository;
import com.shop.ecommerce.product.entity.Product;
import com.shop.ecommerce.product.repository.ProductRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessagingService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public MessagingService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            ProductRepository productRepository,
            UserRepository userRepository
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversations(String email) {
        User currentUser = findUser(email);
        return conversationRepository.findAllForUser(currentUser.getId())
                .stream()
                .map(conversation -> toConversationResponse(
                        conversation,
                        currentUser
                ))
                .toList();
    }

    @Transactional
    public ConversationResponse startConversation(
            String email,
            StartConversationRequest request
    ) {
        User buyer = findUser(email);
        Product listing = productRepository
                .findByIdAndActiveTrue(request.listingId())
                .orElseThrow(() -> new NotFoundException(
                        "Listing not found"
                ));
        User seller = listing.getOwner();

        if (seller == null) {
            throw new IllegalArgumentException(
                    "This listing does not have a contactable seller"
            );
        }
        if (seller.getId().equals(buyer.getId())) {
            throw new IllegalArgumentException(
                    "You cannot message yourself about your own listing"
            );
        }

        Conversation conversation = conversationRepository
                .findByListingIdAndBuyerId(listing.getId(), buyer.getId())
                .orElseGet(() -> createConversation(
                        listing,
                        buyer,
                        seller
                ));

        addMessage(conversation, buyer, request.message());
        return toConversationResponse(conversation, buyer);
    }

    @Transactional
    public List<MessageResponse> getMessages(
            String email,
            Long conversationId
    ) {
        User currentUser = findUser(email);
        Conversation conversation = findConversation(
                conversationId,
                currentUser.getId()
        );

        messageRepository.markAsRead(
                conversation.getId(),
                currentUser.getId(),
                LocalDateTime.now()
        );

        return messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversation.getId())
                .stream()
                .map(message -> toMessageResponse(message, currentUser))
                .toList();
    }

    @Transactional
    public MessageResponse sendMessage(
            String email,
            Long conversationId,
            SendMessageRequest request
    ) {
        User sender = findUser(email);
        Conversation conversation = findConversation(
                conversationId,
                sender.getId()
        );
        Message message = addMessage(conversation, sender, request.body());
        return toMessageResponse(message, sender);
    }

    private Conversation createConversation(
            Product listing,
            User buyer,
            User seller
    ) {
        Conversation conversation = new Conversation();
        conversation.setListing(listing);
        conversation.setBuyer(buyer);
        conversation.setSeller(seller);
        return conversationRepository.save(conversation);
    }

    private Message addMessage(
            Conversation conversation,
            User sender,
            String body
    ) {
        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setBody(body.trim());
        conversation.setUpdatedAt(LocalDateTime.now());
        return messageRepository.save(message);
    }

    private Conversation findConversation(
            Long conversationId,
            Long userId
    ) {
        return conversationRepository
                .findForParticipant(conversationId, userId)
                .orElseThrow(() -> new AccessDeniedException(
                        "You are not a participant in this conversation"
                ));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private ConversationResponse toConversationResponse(
            Conversation conversation,
            User currentUser
    ) {
        User otherUser = conversation.getBuyer().getId()
                .equals(currentUser.getId())
                ? conversation.getSeller()
                : conversation.getBuyer();
        Message lastMessage = messageRepository
                .findFirstByConversationIdOrderByCreatedAtDesc(
                        conversation.getId()
                )
                .orElse(null);
        long unreadCount = messageRepository
                .countByConversationIdAndSenderIdNotAndReadAtIsNull(
                        conversation.getId(),
                        currentUser.getId()
                );

        return new ConversationResponse(
                conversation.getId(),
                conversation.getListing().getId(),
                conversation.getListing().getName(),
                conversation.getListing().getImageUrl(),
                otherUser.getId(),
                fullName(otherUser),
                lastMessage == null ? null : lastMessage.getBody(),
                lastMessage == null
                        ? conversation.getUpdatedAt()
                        : lastMessage.getCreatedAt(),
                unreadCount
        );
    }

    private MessageResponse toMessageResponse(
            Message message,
            User currentUser
    ) {
        User sender = message.getSender();
        return new MessageResponse(
                message.getId(),
                sender.getId(),
                fullName(sender),
                message.getBody(),
                sender.getId().equals(currentUser.getId()),
                message.getReadAt(),
                message.getCreatedAt()
        );
    }

    private String fullName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }
}
