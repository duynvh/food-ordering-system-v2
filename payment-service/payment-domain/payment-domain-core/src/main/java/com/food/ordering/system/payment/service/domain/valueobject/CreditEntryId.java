package com.food.ordering.system.payment.service.domain.valueobject;

import java.util.UUID;

import com.food.ordering.system.domain.valueobject.BaseId;

public class CreditEntryId extends BaseId<UUID> {
	public CreditEntryId(final UUID value) {
		super(value);
	}
}
