package com.food.ordering.system.order.service.messaging.publisher.kafka;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFutureCallback;

import com.food.ordering.system.kafka.order.avro.model.PaymentRequestAvroModel;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class OrderKafkaMessageHelper {
	public <T> ListenableFutureCallback<SendResult<String, T>> getKafkaCallback(final String topicName, final T avroModel,
			String orderId, String requestAvroModelName) {
		return new ListenableFutureCallback<SendResult<String, T>>() {
			@Override
			public void onSuccess(@Nullable final SendResult<String, T> result) {
				RecordMetadata metadata = result.getRecordMetadata();
				log.info("Received successful response from Kafka for order id: {}, topic: {}, partition: {}, offset: {}, timestamp: {}",
						orderId,
						metadata.topic(),
						metadata.partition(),
						metadata.offset(),
						metadata.timestamp()
				);
			}

			@Override
			public void onFailure(final Throwable ex) {
				log.error("Error while sending {} message {} to topic {}", requestAvroModelName, topicName, orderId, ex);
			}
		};
	}
}
