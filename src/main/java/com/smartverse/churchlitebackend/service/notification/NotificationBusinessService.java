package com.smartverse.churchlitebackend.service.notification;

import com.potatotech.authorization.exception.ServiceException;
import com.smartverse.churchlitebackend.config.context.RequestUserContext;
import com.smartverse.churchlitebackend.repository.notification.NotificationCustomRepository;
import com.smartverse.churchlitebackend_gen.converters.NotificationDTOConverter;
import com.smartverse.churchlitebackend_gen.dtos.NotificationDTO;
import com.smartverse.churchlitebackend_gen.entities.NotificationEntity;
import com.smartverse.churchlitebackend_gen.entities.UserConfigurationEntity;
import com.smartverse.churchlitebackend_gen.enums.AppointmentStatus;
import com.smartverse.churchlitebackend_gen.enums.NotificationType;
import com.smartverse.churchlitebackend_gen.repositories.AppointmentsRepository;
import com.smartverse.churchlitebackend_gen.repositories.UserConfigurationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationBusinessService {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
    private static final String PUBLIC_FIELDS = "id,type,title,message,createdAt,readAt,referenceType,referenceId,actionUrl";

    private final NotificationCustomRepository repository;
    private final UserConfigurationRepository userRepository;
    private final AppointmentsRepository appointmentsRepository;
    private final NotificationDTOConverter converter;

    public NotificationBusinessService(NotificationCustomRepository repository,
                                       UserConfigurationRepository userRepository,
                                       AppointmentsRepository appointmentsRepository,
                                       NotificationDTOConverter converter) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.appointmentsRepository = appointmentsRepository;
        this.converter = converter;
    }

    @Transactional
    public List<NotificationDTO> listCurrentUser(boolean unreadOnly) {
        var recipient = currentUser();
        materializeAppointmentReminders(recipient);
        var notifications = unreadOnly
                ? repository.findAllByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(recipient.getId())
                : repository.findAllByRecipientIdOrderByCreatedAtDesc(recipient.getId());
        return converter.toDTO(notifications, PUBLIC_FIELDS);
    }

    public int unreadCount() {
        return Math.toIntExact(repository.countByRecipientIdAndReadAtIsNull(currentUser().getId()));
    }

    @Transactional
    public NotificationDTO markRead(UUID notificationId) {
        var recipient = currentUser();
        var notification = repository.findById(notificationId)
                .filter(item -> item.getRecipient().getId().equals(recipient.getId()))
                .orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "notification_not_found"));
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            repository.save(notification);
        }
        return converter.toDTO(notification, PUBLIC_FIELDS);
    }

    @Transactional
    public void markAllRead() {
        var now = LocalDateTime.now();
        var notifications = repository.findAllByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(currentUser().getId());
        notifications.forEach(item -> item.setReadAt(now));
        repository.saveAll(notifications);
    }

    @Transactional
    public void create(UserConfigurationEntity recipient, NotificationType type, String title, String message,
                       String referenceType, UUID referenceId, String actionUrl, String deduplicationKey) {
        if (recipient == null || repository.existsByDeduplicationKey(deduplicationKey)) return;
        var notification = new NotificationEntity();
        notification.setRecipient(recipient);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setReferenceType(referenceType);
        notification.setReferenceId(referenceId);
        notification.setActionUrl(actionUrl);
        notification.setDeduplicationKey(deduplicationKey);
        repository.save(notification);
    }

    private UserConfigurationEntity currentUser() {
        var hash = RequestUserContext.getRequired();
        return userRepository.findAll().stream()
                .filter(item -> hash.equals(item.getHash()))
                .findFirst()
                .orElseThrow(() -> new ServiceException(HttpStatus.FORBIDDEN, "user_configuration_not_found"));
    }

    private void materializeAppointmentReminders(UserConfigurationEntity recipient) {
        var now = LocalDateTime.now();
        appointmentsRepository.findAll().stream()
                .filter(item -> item.getUserConfiguration() != null
                        && recipient.getId().equals(item.getUserConfiguration().getId())
                        && item.getInitialDate() != null
                        && item.getInitialDate().isAfter(now)
                        && item.getStatus() != AppointmentStatus.CANCELLED)
                .forEach(item -> {
                    var remaining = Duration.between(now, item.getInitialDate());
                    var eventName = item.getEventsType() == null ? item.getDescription() : item.getEventsType().getName();
                    if (remaining.compareTo(Duration.ofHours(1)) <= 0) {
                        create(recipient, NotificationType.APPOINTMENT_1_HOUR, "Compromisso em até 1 hora",
                                eventName + " — " + item.getInitialDate().format(DATE_TIME), "APPOINTMENT", item.getId(),
                                "/home/scheduler", "appointment:" + item.getId() + ":1h");
                    } else if (remaining.compareTo(Duration.ofHours(24)) <= 0) {
                        create(recipient, NotificationType.APPOINTMENT_24_HOURS, "Compromisso nas próximas 24 horas",
                                eventName + " — " + item.getInitialDate().format(DATE_TIME), "APPOINTMENT", item.getId(),
                                "/home/scheduler", "appointment:" + item.getId() + ":24h");
                    }
                });
    }
}
