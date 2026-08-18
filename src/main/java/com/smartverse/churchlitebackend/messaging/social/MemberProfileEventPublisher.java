package com.smartverse.churchlitebackend.messaging.social;

import com.smartverse.churchlitebackend_gen.messaging.pub.MemberProfileSyncedPub;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class MemberProfileEventPublisher {
    private final MemberProfileSyncedPub publisher;

    public MemberProfileEventPublisher(MemberProfileSyncedPub publisher) {
        this.publisher = publisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(MemberProfileEventDispatcher.MemberProfileSyncRequested event) {
        publisher.publish(event.payload());
    }
}
