package com.food.ordering.system.order.service.domain;

import static com.food.ordering.system.domain.DomainConstants.UTC;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.food.ordering.system.domain.valueobject.OrderId;
import com.food.ordering.system.domain.valueobject.OrderStatus;
import com.food.ordering.system.domain.valueobject.PaymentStatus;
import com.food.ordering.system.order.service.domain.dto.message.PaymentResponse;
import com.food.ordering.system.order.service.domain.entity.Order;
import com.food.ordering.system.order.service.domain.event.OrderPaidEvent;
import com.food.ordering.system.order.service.domain.exception.OrderDomainException;
import com.food.ordering.system.order.service.domain.exception.OrderNotFoundException;
import com.food.ordering.system.order.service.domain.mapper.OrderDataMapper;
import com.food.ordering.system.order.service.domain.outbox.model.approval.OrderApprovalOutboxMessage;
import com.food.ordering.system.order.service.domain.outbox.model.payment.OrderPaymentOutboxMessage;
import com.food.ordering.system.order.service.domain.outbox.scheduler.approval.ApprovalOutboxHelper;
import com.food.ordering.system.order.service.domain.outbox.scheduler.payment.PaymentOutboxHelper;
import com.food.ordering.system.order.service.domain.ports.output.repository.OrderRepository;
import com.food.ordering.system.outbox.OutboxStatus;
import com.food.ordering.system.saga.SagaStatus;
import com.food.ordering.system.saga.SagaStep;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class OrderPaymentSaga implements SagaStep<PaymentResponse> {
	private final OrderDomainService orderDomainService;
	private final OrderRepository orderRepository;
	private final PaymentOutboxHelper paymentOutboxHelper;
	private final ApprovalOutboxHelper approvalOutboxHelper;
	private final OrderSagaHelper orderSagaHelper;
	private final OrderDataMapper orderDataMapper;

	public OrderPaymentSaga(
			final OrderDomainService orderDomainService,
			final OrderRepository orderRepository,
			final PaymentOutboxHelper paymentOutboxHelper, final ApprovalOutboxHelper approvalOutboxHelper, final OrderSagaHelper orderSagaHelper,
			final OrderDataMapper orderDataMapper) {
		this.orderDomainService = orderDomainService;
		this.orderRepository = orderRepository;
		this.paymentOutboxHelper = paymentOutboxHelper;
		this.approvalOutboxHelper = approvalOutboxHelper;
		this.orderSagaHelper = orderSagaHelper;
		this.orderDataMapper = orderDataMapper;
	}

	@Override
	@Transactional
	public void process(final PaymentResponse paymentResponse) {
		Optional<OrderPaymentOutboxMessage> orderPaymentOutboxMessageResponse = paymentOutboxHelper.getPaymentOutboxMessageBySagaIdAndSagaStatus(
			UUID.fromString(paymentResponse.getSagaId()),
				SagaStatus.STARTED
		);

		if (orderPaymentOutboxMessageResponse.isEmpty()) {
			log.info("An outbox message with saga id: {} is already processed!", paymentResponse.getSagaId());
			return;
		}

		OrderPaymentOutboxMessage orderPaymentOutboxMessage = orderPaymentOutboxMessageResponse.get();
		OrderPaidEvent domainEvent = completePaymentForOrder(paymentResponse);

		OrderStatus orderStatus = domainEvent.getOrder().getOrderStatus();
		SagaStatus sagaStatus = orderSagaHelper.orderStatusToSagaStatus(orderStatus);
		paymentOutboxHelper.save(getUpdatedPaymentOutboxMessage(orderPaymentOutboxMessage,
				orderStatus, sagaStatus));

		approvalOutboxHelper.saveApprovalOutboxMessage(
				orderDataMapper.orderPaidEventToOrderApprovalEventPayload(domainEvent),
				orderStatus,
				sagaStatus,
				OutboxStatus.STARTED,
				UUID.fromString(paymentResponse.getSagaId()));

		log.info("Order with id: {} is paid", domainEvent.getOrder().getId().getValue());
	}

	private OrderPaymentOutboxMessage getUpdatedPaymentOutboxMessage(
			final OrderPaymentOutboxMessage orderPaymentOutboxMessage,
			final OrderStatus orderStatus,
			final SagaStatus sagaStatus) {
		orderPaymentOutboxMessage.setProcessedAt(ZonedDateTime.now(ZoneId.of(UTC)));
		orderPaymentOutboxMessage.setOrderStatus(orderStatus);
		orderPaymentOutboxMessage.setSagaStatus(sagaStatus);
		return orderPaymentOutboxMessage;
	}

	private OrderPaidEvent completePaymentForOrder(PaymentResponse paymentResponse) {
		log.info("Completing payment for order with id: {}", paymentResponse.getOrderId());
		Order order = findOrder(paymentResponse.getOrderId());
		OrderPaidEvent domainEvent = orderDomainService.payOrder(order);
		orderRepository.save(order);
		return domainEvent;
	}

	@Override
	@Transactional
	public void rollback(final PaymentResponse paymentResponse) {
		Optional<OrderPaymentOutboxMessage> orderPaymentOutboxMessageResponse = paymentOutboxHelper.getPaymentOutboxMessageBySagaIdAndSagaStatus(
				UUID.fromString(paymentResponse.getSagaId()),
				getCurrentSagaStatus(paymentResponse.getPaymentStatus())
		);

		if (orderPaymentOutboxMessageResponse.isEmpty()) {
			log.info("An outbox message with saga id: {} is already roll backed!", paymentResponse.getSagaId());
			return;
		}

		OrderPaymentOutboxMessage orderPaymentOutboxMessage = orderPaymentOutboxMessageResponse.get();
		Order order = rollbackPaymentForOrder(paymentResponse);
		SagaStatus sagaStatus = orderSagaHelper.orderStatusToSagaStatus(order.getOrderStatus());
		paymentOutboxHelper.save(getUpdatedPaymentOutboxMessage(orderPaymentOutboxMessage,
				order.getOrderStatus(), sagaStatus));

		if (paymentResponse.getPaymentStatus() == PaymentStatus.CANCELLED) {
			approvalOutboxHelper.save(getUpdatedApprovalOutboxMessage(paymentResponse.getSagaId(),
					order.getOrderStatus(), sagaStatus));
		}
		log.info("Order with id: {} is cancelled", order.getId().getValue());
	}

	private OrderApprovalOutboxMessage getUpdatedApprovalOutboxMessage(final String sagaId,
			final OrderStatus orderStatus,
			final SagaStatus sagaStatus) {
		Optional<OrderApprovalOutboxMessage> orderApprovalOutboxMessageResponse =
				approvalOutboxHelper.getApprovalOutboxMessageBySagaIdAndSagaStatus(
						UUID.fromString(sagaId),
						SagaStatus.COMPENSATING
				);

		if (orderApprovalOutboxMessageResponse.isEmpty()) {
			throw new OrderDomainException("Approval outbox message could not be found in " + SagaStatus.COMPENSATING.name() + " status!");
		}

		OrderApprovalOutboxMessage orderApprovalOutboxMessage = orderApprovalOutboxMessageResponse.get();
		orderApprovalOutboxMessage.setProcessedAt(ZonedDateTime.now(ZoneId.of(UTC)));
		orderApprovalOutboxMessage.setOrderStatus(orderStatus);
		orderApprovalOutboxMessage.setSagaStatus(sagaStatus);
		return orderApprovalOutboxMessage;
	}

	private SagaStatus[] getCurrentSagaStatus(final PaymentStatus paymentStatus) {
		return switch (paymentStatus) {
			case COMPLETED -> new SagaStatus[] {SagaStatus.STARTED};
			case CANCELLED -> new SagaStatus[] {SagaStatus.PROCESSING};
			case FAILED -> new SagaStatus[] { SagaStatus.STARTED, SagaStatus.PROCESSING};
		};
	}

	private Order rollbackPaymentForOrder(PaymentResponse paymentResponse) {
		log.info("Cancelling order with id: {}", paymentResponse.getOrderId());
		Order order = findOrder(paymentResponse.getOrderId());
		orderDomainService.cancelOrder(order, paymentResponse.getFailureMessages());
		orderRepository.save(order);
		return order;
	}

	private Order findOrder(String orderId) {
		Optional<Order> orderResponse = orderRepository.findById(new OrderId(UUID.fromString(orderId)));
		if (orderResponse.isEmpty()) {
			log.error("Order with id: {} could not be found!", orderId);
			throw new OrderNotFoundException("Order with id " + orderId + " could not be found!");
		}
		return orderResponse.get();
	}
}
