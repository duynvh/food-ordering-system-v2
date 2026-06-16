package com.food.ordering.system.payment.service.dataaccess.payment.exception;

public class PaymentDataaccessException extends RuntimeException {
	public PaymentDataaccessException(final String message) {
		super(message);
	}

	public PaymentDataaccessException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
