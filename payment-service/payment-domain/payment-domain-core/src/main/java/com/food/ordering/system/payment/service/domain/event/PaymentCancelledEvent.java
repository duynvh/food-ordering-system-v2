package com.food.ordering.system.payment.service.domain.event;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import com.food.ordering.system.domain.event.publisher.DomainEventPublisher;
import com.food.ordering.system.payment.service.domain.entity.Payment;

public class PaymentCancelledEvent extends PaymentEvent {
	final DomainEventPublisher<PaymentCancelledEvent> paymentCancelledEventDomainEventPublisher;

	public PaymentCancelledEvent(final Payment payment, final ZonedDateTime createdAt,
			final DomainEventPublisher<PaymentCancelledEvent> paymentCancelledEventDomainEventPublisher) {
		super(payment, createdAt, Collections.emptyList());
		this.paymentCancelledEventDomainEventPublisher = paymentCancelledEventDomainEventPublisher;
	}

	@Override
	public void fire() {
		paymentCancelledEventDomainEventPublisher.publish(this);
	}
}
