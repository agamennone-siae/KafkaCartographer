package com.example.notificationservice;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import com.example.shared.SharedTopics;

@Service
public class NotificationService {

    @KafkaListener(topics = {
        SharedTopics.ORDERS_CREATED,
        SharedTopics.PAYMENTS_PROCESSED,
        SharedTopics.SHIPPING_CREATED
    }, groupId = "notification-group")
    public void notifyUser(String message) {
        System.out.println("Sending notification for event: " + message);
    }
}
