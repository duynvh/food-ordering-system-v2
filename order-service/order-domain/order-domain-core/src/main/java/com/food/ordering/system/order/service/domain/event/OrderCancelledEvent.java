package com.food.ordering.system.order.service.domain.event;

import java.time.ZonedDateTime;

import com.food.ordering.system.domain.event.publisher.DomainEventPublisher;
import com.food.ordering.system.order.service.domain.entity.Order;

public class OrderCancelledEvent extends OrderEvent {
	private final DomainEventPublisher<OrderCancelledEvent> orderCancelledEventDomainEventPublisher;

	public OrderCancelledEvent(final Order order, final ZonedDateTime createdAt,
			final DomainEventPublisher<OrderCancelledEvent> orderCancelledEventDomainEventPublisher) {
		super(order, createdAt);
		this.orderCancelledEventDomainEventPublisher = orderCancelledEventDomainEventPublisher;
	}

	@Override
	public void fire() {
		orderCancelledEventDomainEventPublisher.publish(this);
	}
}
