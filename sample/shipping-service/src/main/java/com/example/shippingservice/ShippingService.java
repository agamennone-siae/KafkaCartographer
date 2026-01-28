package com.example.shippingservice;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.example.shared.SharedTopics;

@Service
public class ShippingService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public ShippingService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = {SharedTopics.PAYMENTS_PROCESSED, SharedTopics.INVENTORY_RESERVED}, groupId = "shipping-group")
    public void prepareShipment(String message) {
        System.out.println("Received event for shipping: " + message);
        // In reality, would likely aggregate. Here just simple forward.
        kafkaTemplate.send(SharedTopics.SHIPPING_CREATED, message);
    }
}
