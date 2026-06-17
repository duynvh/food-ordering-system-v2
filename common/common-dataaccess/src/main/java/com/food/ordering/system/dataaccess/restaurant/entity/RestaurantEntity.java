package com.food.ordering.system.dataaccess.restaurant.entity;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@IdClass(RestaurantEntityId.class)
@Table(name = "order_restaurant_m_view", schema = "restaurant")
public class RestaurantEntity {
	@Id
	private UUID restaurantId;
	@Id
	private UUID productId;
	private String restaurantName;
	private Boolean restaurantActive;
	private String productName;
	private BigDecimal productPrice;
	private Boolean productAvailable;

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final RestaurantEntity that = (RestaurantEntity) o;
		return Objects.equals(restaurantId, that.restaurantId) && Objects.equals(productId, that.productId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(restaurantId, productId);
	}
}
