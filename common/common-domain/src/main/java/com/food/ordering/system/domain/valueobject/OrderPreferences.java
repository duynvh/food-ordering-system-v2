package com.food.ordering.system.domain.valueobject;

import java.util.List;

import lombok.Builder;

@Builder
public record OrderPreferences(
		List<String> removeIngredients,
		List<String> addIngredients,
		SpiceLevel spiceLevel,
		String specialInstructions,
		String deliveryInstructions
) {

}
