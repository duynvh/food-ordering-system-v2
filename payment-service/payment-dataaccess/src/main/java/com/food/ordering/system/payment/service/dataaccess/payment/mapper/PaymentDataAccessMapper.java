package com.food.ordering.system.payment.service.dataaccess.payment.mapper;

import org.springframework.stereotype.Component;

import com.food.ordering.system.domain.valueobject.CustomerId;
import com.food.ordering.system.domain.valueobject.Money;
import com.food.ordering.system.domain.valueobject.OrderId;
import com.food.ordering.system.payment.service.dataaccess.payment.entity.PaymentEntity;
import com.food.ordering.system.payment.service.domain.entity.Payment;
import com.food.ordering.system.payment.service.domain.valueobject.PaymentId;

@Component
public class PaymentDataAccessMapper {
	public Payment paymentEntityToPayment(PaymentEntity paymentEntity) {
		return Payment.builder()
				.paymentId(new PaymentId(paymentEntity.getId()))
				.orderId(new OrderId(paymentEntity.getOrderId()))
				.customerId(new CustomerId(paymentEntity.getCustomerId()))
				.price(new Money(paymentEntity.getPrice()))
				.createdAt(paymentEntity.getCreatedAt())
				.build();
	}

	public PaymentEntity paymentToPaymentEntity(Payment payment) {
		return PaymentEntity.builder()
				.id(payment.getId().getValue())
				.orderId(payment.getOrderId().getValue())
				.customerId(payment.getCustomerId().getValue())
				.price(payment.getPrice().getAmount())
				.status(payment.getPaymentStatus())
				.createdAt(payment.getCreatedAt())
				.build();
	}
}
