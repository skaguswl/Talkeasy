package com.talkeasy.server.service.location;

import com.talkeasy.server.dto.LocationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;


@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {
    private final PostgresKafkaService postgresKafkaService;
    private final ConsumerFactory<String, LocationDto> consumerFactory;
    private final ConsumerFactory<String, LocationDto> kafkaListenerContainerfactory;

    public LocalDateTime convertTimestampToLocalDateTime(long timeStamp) {

        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timeStamp), ZoneId.systemDefault());
    }

    public void consume() {
        try (KafkaConsumer<String, LocationDto> kafkaConsumer = new KafkaConsumer<>(kafkaListenerContainerfactory.getConfigurationProperties())) {
            org.apache.kafka.common.TopicPartition topicPartition = new TopicPartition("topic-test-03", 0);


            // set로 동작
            kafkaConsumer.assign(Collections.singletonList(topicPartition));
            kafkaConsumer.seek(topicPartition, 0);

            List<LocationDto> locationDtos = new ArrayList<>();

            ConsumerRecords<String, LocationDto> records = kafkaConsumer.poll(Duration.ofSeconds(20));
            kafkaConsumer.commitSync();
            log.info("==================== record count : {}", records.count());

            for (ConsumerRecord<String, LocationDto> record : records) {
                if (record.value() != null) {
                    LocationDto locationDto = record.value();
                    locationDto.setDateTime(convertTimestampToLocalDateTime(record.timestamp()));
                    locationDtos.add(record.value());
                    log.info("Topic : {}, Partition : {}, Offset : {}, Timestamp : {}, Key : {}", record.topic(), record.partition(), record.offset(), convertTimestampToLocalDateTime(record.timestamp()), record.key());
                }
            }
            try {
                if (!locationDtos.isEmpty()) {
//                    postgresKafkaService.bulk(locationDtos);
                }
            } catch (Exception e) {
                log.error("error : {}", e.getMessage());
            }

        } catch (Exception e) {
            log.error("error : {}", e.getMessage());
        }
    }

    // 카프카 리스너
//    @KafkaListener(topicPartitions = {@org.springframework.kafka.annotation.TopicPartition(topic = "topic-test-02", partitions = {"0"})},groupId = "my-group5", properties = {"auto.offset.reset:earliest"})
////    @KafkaListener(topicPartitions = {@TopicPartition(topic = "topic-test-02", partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0"))
////    }, groupId = "my-group", properties = {"auto.offset.reset:earliest"})
//    @KafkaListener(topics = "topic-test-02", groupId = "my-group", containerFactory = "kafkaListenerContainerFactory", properties = {"auto.offset.reset:earliest"})
    @KafkaListener(topics = "topic-test-03", containerFactory = "kafkaListenerContainerFactory")
    public void consume(ConsumerRecord<String, LocationDto> record) {
        log.info("========== [Consumed message] value : {}, topic : {}, partition : {}, offset : {}, timestamp : {}, key : {}", record.value().toString(), record.topic(), record.partition(), record.offset(), convertTimestampToLocalDateTime(record.timestamp()), record.key());
    }

}

