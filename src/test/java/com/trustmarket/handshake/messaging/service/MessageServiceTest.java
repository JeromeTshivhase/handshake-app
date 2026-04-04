package com.trustmarket.handshake.messaging.service;

import com.trustmarket.handshake.messaging.entity.Conversation;
import com.trustmarket.handshake.messaging.entity.ConversationStatus;
import com.trustmarket.handshake.messaging.entity.Message;
import com.trustmarket.handshake.messaging.entity.MessageType;
import com.trustmarket.handshake.messaging.repository.ConversationRepository;
import com.trustmarket.handshake.messaging.repository.MessageRepository;
import com.trustmarket.handshake.messaging.repository.UserBlockRepository;
import com.trustmarket.handshake.shared.exception.BadRequestException;
import com.trustmarket.handshake.shared.rate.RateLimiterService;
import com.trustmarket.handshake.user.entity.User;
import com.trustmarket.handshake.user.repository.UserRepository;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessageServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserBlockRepository userBlockRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private MessageSafetyService messageSafetyService;

    @Mock
    private Bucket bucket;

    private MessageService messageService;

    @BeforeEach
    void setUp() {
        messageService = new MessageService(
                conversationRepository,
                messageRepository,
                userBlockRepository,
                userRepository,
                eventPublisher,
                rateLimiterService,
                messagingTemplate,
                messageSafetyService
        );
        ReflectionTestUtils.setField(messageService, "messagesPerMinute", 30);
        ReflectionTestUtils.setField(messageService, "previewLength", 100);
        ReflectionTestUtils.setField(messageService, "maxContentLength", 5000);
        ReflectionTestUtils.setField(messageService, "safetyMode", "warn");
        ReflectionTestUtils.setField(messageService, "sanitizeContent", true);
        ReflectionTestUtils.setField(messageService, "blockSocialHandles", false);
        ReflectionTestUtils.setField(messageService, "blockPhoneNumbers", false);
        when(messageSafetyService.assess(any())).thenAnswer(i -> MessageSafetyService.MessageSafetyAssessment.safe(i.getArgument(0)));
    }

    @Test
    void sendThrowsWhenParticipantsAreBlocked() {
        UUID conversationId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();

        User sender = User.builder().id(senderId).email("a@example.com").passwordHash("x").firstName("A").lastName("B").build();
        User recipient = User.builder().id(recipientId).email("b@example.com").passwordHash("x").firstName("C").lastName("D").build();

        Conversation c = Conversation.builder()
                .id(conversationId)
                .buyer(sender)
                .seller(recipient)
                .status(ConversationStatus.ACTIVE)
                .build();

        when(rateLimiterService.resolveBucket(any(), any(Integer.class), any())).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(true);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(c));
        when(userBlockRepository.existsByBlocker_IdAndBlocked_Id(senderId, recipientId)).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> messageService.send(conversationId, senderId, "Hello", MessageType.TEXT, null));

        verify(userRepository, never()).findById(any());
        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    void sendThrowsWhenSafetyModeBlocksMessage() {
        UUID conversationId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();

        User sender = User.builder().id(senderId).email("a@example.com").passwordHash("x").firstName("A").lastName("B").build();
        User recipient = User.builder().id(recipientId).email("b@example.com").passwordHash("x").firstName("C").lastName("D").build();

        Conversation c = Conversation.builder()
                .id(conversationId)
                .buyer(sender)
                .seller(recipient)
                .status(ConversationStatus.ACTIVE)
                .build();

        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));

        ReflectionTestUtils.setField(messageService, "safetyMode", "block");

        when(rateLimiterService.resolveBucket(any(), any(Integer.class), any())).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(true);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(c));
        when(userBlockRepository.existsByBlocker_IdAndBlocked_Id(senderId, recipientId)).thenReturn(false);
        when(userBlockRepository.existsByBlocker_IdAndBlocked_Id(recipientId, senderId)).thenReturn(false);
        when(messageSafetyService.assess(eq("contact me @offplatform")))
                .thenReturn(new MessageSafetyService.MessageSafetyAssessment(
                        "contact me @offplatform",
                        java.util.Set.of("SOCIAL_MEDIA_HANDLE"),
                        true
                ));

        assertThrows(BadRequestException.class,
                () -> messageService.send(conversationId, senderId, "contact me @offplatform", MessageType.TEXT, null));

        verify(userRepository, never()).findById(any());
        verify(messageRepository, never()).save(any(Message.class));
    }
}

