package com.example.inventoryservice;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.example.shared.SharedTopics;

@Service
public class InventoryService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public InventoryService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = SharedTopics.ORDERS_CREATED, groupId = "inventory-group")
    public void reserveInventory(String orderId) {
        System.out.println("Reserving inventory for order: " + orderId);
        
        // Simulating logic
        kafkaTemplate.send(SharedTopics.INVENTORY_RESERVED, orderId);
    }
}
