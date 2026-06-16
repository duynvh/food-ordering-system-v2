package com.food.ordering.system.payment.service.domain.event;

import java.time.ZonedDateTime;
import java.util.List;

import com.food.ordering.system.domain.event.publisher.DomainEventPublisher;
import com.food.ordering.system.payment.service.domain.entity.Payment;

public class PaymentFailedEvent extends PaymentEvent {
	private final DomainEventPublisher<PaymentFailedEvent> paymentFailedEventDomainEventPublisher;

	public PaymentFailedEvent(final Payment payment,
			final ZonedDateTime createdAt,
			final List<String> failureMessages,
			final DomainEventPublisher<PaymentFailedEvent> paymentFailedEventDomainEventPublisher) {
		super(payment, createdAt, failureMessages);
		this.paymentFailedEventDomainEventPublisher = paymentFailedEventDomainEventPublisher;
	}

	@Override
	public void fire() {
		paymentFailedEventDomainEventPublisher.publish(this);
	}
}
