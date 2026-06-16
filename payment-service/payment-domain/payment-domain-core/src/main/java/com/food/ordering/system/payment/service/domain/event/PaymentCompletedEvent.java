package com.food.ordering.system.payment.service.domain.event;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import com.food.ordering.system.domain.event.publisher.DomainEventPublisher;
import com.food.ordering.system.payment.service.domain.entity.Payment;

public class PaymentCompletedEvent extends PaymentEvent {
	private final DomainEventPublisher<PaymentCompletedEvent> paymentCompletedMessagePublisher;

	public PaymentCompletedEvent(final Payment payment, final ZonedDateTime createdAt,
			final DomainEventPublisher<PaymentCompletedEvent> paymentCompletedMessagePublisher) {
		super(payment, createdAt, Collections.emptyList());
		this.paymentCompletedMessagePublisher = paymentCompletedMessagePublisher;
	}

	@Override
	public void fire() {
		paymentCompletedMessagePublisher.publish(this);
	}
}
