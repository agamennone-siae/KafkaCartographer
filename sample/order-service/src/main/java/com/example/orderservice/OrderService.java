package com.example.orderservice;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public OrderService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void createOrder(String orderId) {
        System.out.println("Creating order " + orderId);
        // Publishes to constant
        kafkaTemplate.send(SharedTopics.ORDERS_CREATED, orderId);
    }

    public void updateOrder(String orderId) {
        kafkaTemplate.send(SharedTopics.ORDERS_UPDATED, orderId);
    }
}
