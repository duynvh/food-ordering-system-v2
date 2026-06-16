package com.food.ordering.system.order.service.domain;

import java.util.List;

import com.food.ordering.system.domain.event.publisher.DomainEventPublisher;
import com.food.ordering.system.order.service.domain.entity.Order;
import com.food.ordering.system.order.service.domain.entity.Restaurant;
import com.food.ordering.system.order.service.domain.event.OrderCancelledEvent;
import com.food.ordering.system.order.service.domain.event.OrderCreatedEvent;
import com.food.ordering.system.order.service.domain.event.OrderPaidEvent;

public interface OrderDomainService {
	OrderCreatedEvent validateAndInitiateOrder(Order order, Restaurant restaurant,
			final DomainEventPublisher<OrderCreatedEvent> orderCreatedEventDomainEventPublisher);

	OrderPaidEvent payOrder(Order order, final DomainEventPublisher<OrderPaidEvent> orderPaidEventDomainEventPublisher);

	void approveOrder(Order order);

	OrderCancelledEvent cancelOrderPayment(Order order, List<String> failureMessages,
			final DomainEventPublisher<OrderCancelledEvent> orderCancelledEventDomainEventPublisher);

	void cancelOrder(Order order, List<String> failureMessages);
}
