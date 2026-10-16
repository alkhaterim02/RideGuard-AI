package com.rideguard.ai.messaging;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.rideguard.ai.data.FirebaseRepository;
import com.rideguard.ai.util.NotificationHelper;

import java.util.Map;

/**
 * Receives push notifications sent by the backend (Cloud Function) when an
 * emergency is created. Expected data keys: title, body, incidentId.
 */
public class RideGuardMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        Map<String, String> data = message.getData();
        String title = data.get("title");
        String body = data.get("body");
        if (message.getNotification() != null) {
            if (title == null) title = message.getNotification().getTitle();
            if (body == null) body = message.getNotification().getBody();
        }
        NotificationHelper.show(this, title == null ? "RideGuard AI" : title,
                body == null ? "" : body, data.get("incidentId"));
    }

    @Override
    public void onNewToken(@NonNull String token) {
        FirebaseRepository.saveFcmToken(token);
    }
}
