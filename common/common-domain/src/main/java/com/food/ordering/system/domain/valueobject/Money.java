package com.food.ordering.system.domain.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public class Money {
	private final BigDecimal amount;

	public static final Money ZERO = new Money(BigDecimal.ZERO);

	public Money(final BigDecimal amount) {
		this.amount = amount;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public boolean isGreaterThanZero() {
		return this.amount != null && this.amount.compareTo(BigDecimal.ZERO) > 0;
	}

	public boolean isGreaterThan(final Money money) {
		return this.amount != null && this.amount.compareTo(money.getAmount()) > 0;
	}

	public Money add(final Money money) {
		return new Money(setScale(this.amount.add(money.getAmount())));
	}

	public Money subtract(final Money money) {
		return new Money(setScale(this.amount.subtract(money.getAmount())));
	}

	public Money multiply(final int multiplier) {
		return new Money(setScale(this.amount.multiply(new BigDecimal(multiplier))));
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final Money money = (Money) o;
		return Objects.equals(amount, money.amount);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(amount);
	}

	private BigDecimal setScale(final BigDecimal amount) {
		return amount.setScale(2, RoundingMode.HALF_EVEN);
	}
}
