package com.kafkacartographer;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ElasticsearchExporter {

    private final String serverUrl;

    public ElasticsearchExporter() {
        String envHost = System.getenv("ELASTICSEARCH_HOSTS");
        this.serverUrl = (envHost != null && !envHost.isEmpty()) ? envHost : "http://localhost:9200";
    }

    public void export(Map<String, Object> data) {
        System.out.println("Connecting to Elasticsearch at " + serverUrl + "...");

        try (RestClient restClient = RestClient.builder(HttpHost.create(serverUrl)).build()) {
            ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
            ElasticsearchClient client = new ElasticsearchClient(transport);

            String indexName = "kafka-topic-map";
            
            BulkRequest.Builder bulk = new BulkRequest.Builder();

            List<KafkaScanner.TopicOccurrence> literals = (List<KafkaScanner.TopicOccurrence>) data.get("literals");
            if (literals != null) {
                for (KafkaScanner.TopicOccurrence lit : literals) {
                    bulk.operations(op -> op
                        .index(idx -> idx
                            .index(indexName)
                            .document(lit)
                        )
                    );
                }
            }

            List<KafkaScanner.TopicLink> links = (List<KafkaScanner.TopicLink>) data.get("links");
            if (links != null) {
                for (KafkaScanner.TopicLink link : links) {
                    bulk.operations(op -> op
                        .index(idx -> idx
                            .index(indexName)
                            .document(link)
                        )
                    );
                }
            }

            if (bulk.build().operations().isEmpty()) {
                System.out.println("Nothing to export to Elasticsearch.");
                return;
            }

            System.out.println("Sending " + bulk.build().operations().size() + " documents to Elasticsearch...");
            BulkResponse response = client.bulk(bulk.build());

            if (response.errors()) {
                System.err.println("Bulk export had errors.");
                response.items().forEach(item -> {
                    if (item.error() != null) {
                        System.err.println(item.error().reason());
                    }
                });
            } else {
                System.out.println("Export to Elasticsearch completed successfully.");
            }

        } catch (IOException e) {
            System.err.println("Failed to export to Elasticsearch: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
