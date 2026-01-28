package com.example.paymentservice;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.example.shared.SharedTopics;

@Service
public class PaymentService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public PaymentService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = SharedTopics.ORDERS_CREATED, groupId = "payment-group")
    public void processPayment(String orderId) {
        System.out.println("Processing payment for order: " + orderId);
        if (orderId.hashCode() % 10 == 0) {
            kafkaTemplate.send(SharedTopics.PAYMENTS_FAILED, orderId);
        } else {
            kafkaTemplate.send(SharedTopics.PAYMENTS_PROCESSED, orderId);
        }
    }
}
