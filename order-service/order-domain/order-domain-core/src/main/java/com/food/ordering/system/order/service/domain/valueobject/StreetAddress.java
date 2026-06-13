package com.food.ordering.system.order.service.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public class StreetAddress {
	private final UUID id;
	private final String street;
	private final String city;
	private final String postalCode;

	public StreetAddress(final UUID id, final String street, final String city, final String postalCode) {
		this.id = id;
		this.street = street;
		this.city = city;
		this.postalCode = postalCode;
	}

	public UUID getId() {
		return id;
	}
	public String getStreet() {
		return street;
	}

	public String getCity() {
		return city;
	}

	public String getPostalCode() {
		return postalCode;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final StreetAddress that = (StreetAddress) o;
		return Objects.equals(street, that.street) && Objects.equals(city, that.city) && Objects.equals(postalCode,
				that.postalCode);
	}

	@Override
	public int hashCode() {
		return Objects.hash(street, city, postalCode);
	}
}
