package com.food.ordering.system.order.service.domain;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.food.ordering.system.order.service.domain.dto.create.CreateOrderCommand;
import com.food.ordering.system.order.service.domain.dto.create.CreateOrderResponse;
import com.food.ordering.system.order.service.domain.dto.track.TrackOrderQuery;
import com.food.ordering.system.order.service.domain.dto.track.TrackOrderResponse;
import com.food.ordering.system.order.service.domain.ports.input.service.OrderApplicationService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Validated
@Service
public class OrderApplicationServiceImpl implements OrderApplicationService {
	private final OrderTrackCommandHandler orderTrackCommandHandler;
	private final OrderCreateCommandHandler orderCreateCommandHandler;

	public OrderApplicationServiceImpl(final OrderTrackCommandHandler orderTrackCommandHandler,
			final OrderCreateCommandHandler orderCreateCommandHandler) {
		this.orderTrackCommandHandler = orderTrackCommandHandler;
		this.orderCreateCommandHandler = orderCreateCommandHandler;
	}

	@Override
	public CreateOrderResponse createOrder(final CreateOrderCommand createOrderCommand) {
		return orderCreateCommandHandler.createOrder(createOrderCommand);
	}

	@Override
	public TrackOrderResponse trackOrder(final TrackOrderQuery trackOrderQuery) {
		return orderTrackCommandHandler.trackOrder(trackOrderQuery);
	}
}
