package com.smartverse.churchlitebackend.controller.notification;

import com.smartverse.churchlitebackend.service.notification.NotificationBusinessService;
import com.smartverse.churchlitebackend_gen.endpoints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationHandlerImpl implements GetNotifications, MarkNotificationRead, MarkAllNotificationsRead {
    private final NotificationBusinessService service;

    public NotificationHandlerImpl(NotificationBusinessService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<GetNotificationsOutput> getNotifications(boolean unreadOnly) {
        var output = new GetNotificationsOutput();
        output.notifications = service.listCurrentUser(unreadOnly);
        output.unreadCount = service.unreadCount();
        return ResponseEntity.ok(output);
    }

    @Override
    public ResponseEntity<MarkNotificationReadOutput> markNotificationRead(MarkNotificationReadInput input) {
        var output = new MarkNotificationReadOutput();
        output.notification = service.markRead(input.notificationId);
        return ResponseEntity.ok(output);
    }

    @Override
    public ResponseEntity<MarkAllNotificationsReadOutput> markAllNotificationsRead(MarkAllNotificationsReadInput input) {
        service.markAllRead();
        var output = new MarkAllNotificationsReadOutput();
        output.unreadCount = 0;
        return ResponseEntity.ok(output);
    }
}
