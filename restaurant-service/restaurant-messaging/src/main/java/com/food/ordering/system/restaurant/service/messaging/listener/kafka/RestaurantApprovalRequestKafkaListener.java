package com.food.ordering.system.restaurant.service.messaging.listener.kafka;

import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.food.ordering.system.kafka.consumer.KafkaConsumer;
import com.food.ordering.system.kafka.order.avro.model.RestaurantApprovalRequestAvroModel;
import com.food.ordering.system.restaurant.service.domain.ports.input.message.listener.RestaurantApprovalRequestMessageListener;
import com.food.ordering.system.restaurant.service.messaging.mapper.RestaurantMessagingDataMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RestaurantApprovalRequestKafkaListener implements KafkaConsumer<RestaurantApprovalRequestAvroModel> {
	private final RestaurantApprovalRequestMessageListener restaurantApprovalRequestMessageListener;
	private final RestaurantMessagingDataMapper restaurantMessagingDataMapper;

	public RestaurantApprovalRequestKafkaListener(
			final RestaurantApprovalRequestMessageListener restaurantApprovalRequestMessageListener,
			final RestaurantMessagingDataMapper restaurantMessagingDataMapper) {
		this.restaurantApprovalRequestMessageListener = restaurantApprovalRequestMessageListener;
		this.restaurantMessagingDataMapper = restaurantMessagingDataMapper;
	}

	@Override
	@KafkaListener(id = "${kafka-consumer-config.restaurant-approval-consumer-group-id}",
			topics = "${restaurant-service.restaurant-approval-request-topic-name}")
	public void receive(final @Payload List<RestaurantApprovalRequestAvroModel> messages,
			final @Header(KafkaHeaders.RECEIVED_KEY) List<String> keys,
			final @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
			final @Header(KafkaHeaders.OFFSET) List<Long> offsets) {
		log.info("{} number of orders approval requests received with keys {}, partitions {} and offsets {}" +
						", sending for restaurant approval",
				messages.size(),
				keys.toString(),
				partitions.toString(),
				offsets.toString());

		messages.forEach(restaurantApprovalRequestAvroModel -> {
			log.info("Processing order approval for order id: {}", restaurantApprovalRequestAvroModel.getOrderId());
			restaurantApprovalRequestMessageListener.approveOrder(restaurantMessagingDataMapper.
					restaurantApprovalRequestAvroModelToRestaurantApproval(restaurantApprovalRequestAvroModel));
		});

	}
}
