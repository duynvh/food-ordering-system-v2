package com.food.ordering.system.order.service.domain;

import static com.food.ordering.system.domain.DomainConstants.UTC;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import com.food.ordering.system.domain.event.publisher.DomainEventPublisher;
import com.food.ordering.system.order.service.domain.entity.Order;
import com.food.ordering.system.order.service.domain.entity.Product;
import com.food.ordering.system.order.service.domain.entity.Restaurant;
import com.food.ordering.system.order.service.domain.event.OrderCancelledEvent;
import com.food.ordering.system.order.service.domain.event.OrderCreatedEvent;
import com.food.ordering.system.order.service.domain.event.OrderPaidEvent;
import com.food.ordering.system.order.service.domain.exception.OrderDomainException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrderDomainServiceImpl implements OrderDomainService {
	@Override
	public OrderCreatedEvent validateAndInitiateOrder(final Order order, final Restaurant restaurant,
			final DomainEventPublisher<OrderCreatedEvent> orderCreatedEventDomainEventPublisher) {
		validateRestaurant(restaurant);
		setOrderProductInformation(order, restaurant);
		order.validateOrder();
		order.initializeOrder();
		log.info("Order with id: {} is initiated", order.getId().getValue());
		return new OrderCreatedEvent(order, ZonedDateTime.now(ZoneId.of(UTC)), orderCreatedEventDomainEventPublisher);
	}

	@Override
	public OrderPaidEvent payOrder(final Order order,
			final DomainEventPublisher<OrderPaidEvent> orderPaidEventDomainEventPublisher) {
		order.pay();
		log.info("Order with id: {} is paid", order.getId().getValue());
		return new OrderPaidEvent(order, ZonedDateTime.now(ZoneId.of(UTC)), orderPaidEventDomainEventPublisher);
	}

	@Override
	public void approveOrder(final Order order) {
		order.approve();
		log.info("Order with id: {} is approved", order.getId().getValue());
	}

	@Override
	public OrderCancelledEvent cancelOrderPayment(final Order order, final List<String> failureMessages,
			final DomainEventPublisher<OrderCancelledEvent> orderCancelledEventDomainEventPublisher) {
		order.initCancel(failureMessages);
		log.info("Order payment is cancelling for order id: {}", order.getId().getValue());
		return new OrderCancelledEvent(order, ZonedDateTime.now(ZoneId.of(UTC)), orderCancelledEventDomainEventPublisher);
	}

	@Override
	public void cancelOrder(final Order order, final List<String> failureMessages) {
		order.cancel(failureMessages);
		log.info("Order with id: {} is cancelled", order.getId().getValue());
	}

	private void validateRestaurant(Restaurant restaurant) {
		if (!restaurant.isActive()) {
			throw new OrderDomainException("Restaurant with id " + restaurant.getId().getValue() +
					" is currently not active!");
		}
	}

	private void setOrderProductInformation(Order order, Restaurant restaurant) {
		order.getItems().forEach(orderItem -> restaurant.getProducts().forEach(restaurantProduct -> {
			Product currentProduct = orderItem.getProduct();
			if (currentProduct.equals(restaurantProduct)) {
				currentProduct.updateWithConfirmedNameAndPrice(restaurantProduct.getName(), restaurantProduct.getPrice());
			}
		}));
	}
}
