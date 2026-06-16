package com.food.ordering.system.kafka.producer;

import java.util.function.BiConsumer;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class KafkaMessageHelper {
	public <T, U> BiConsumer<SendResult<String, T>, Throwable> getKafkaCallback(final String topicName, final T avroModel,
			String orderId, String avroModelName) {
		return (result, ex) -> {
			if (ex == null) {
				RecordMetadata metadata = result.getRecordMetadata();
				log.info("Received successful response from Kafka for order id: {}, topic: {}, partition: {}, offset: {}, timestamp: {}",
						orderId,
						metadata.topic(),
						metadata.partition(),
						metadata.offset(),
						metadata.timestamp()
				);
			} else {
				log.error("Error while sending {} message {} to topic {}", avroModelName, avroModel.toString(), orderId, ex);
			}
		};
	}
}
