package com.food.ordering.system.order.service.dataaccess.order.entity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import com.food.ordering.system.domain.valueobject.OrderPreferences;
import com.food.ordering.system.domain.valueobject.OrderStatus;

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
@Table(name = "orders")
public class OrderEntity {
	@Id
	private UUID id;
	private UUID customerId;
	private UUID restaurantId;
	private UUID trackingId;
	private BigDecimal price;

	@Enumerated(EnumType.STRING)
	private OrderStatus orderStatus;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "order_preferences")
	private OrderPreferences orderPreferences;
	private String failureMessages;

	@OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
	private OrderAddressEntity address;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
	private List<OrderItemEntity> items;

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final OrderEntity that = (OrderEntity) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}
}
