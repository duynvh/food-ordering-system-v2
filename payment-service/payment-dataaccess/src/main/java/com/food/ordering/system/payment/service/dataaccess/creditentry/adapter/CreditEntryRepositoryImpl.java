package com.food.ordering.system.payment.service.dataaccess.creditentry.adapter;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.food.ordering.system.domain.valueobject.CustomerId;
import com.food.ordering.system.payment.service.dataaccess.creditentry.mapper.CreditEntryDataAccessMapper;
import com.food.ordering.system.payment.service.dataaccess.creditentry.repository.CreditEntryJpaRepository;
import com.food.ordering.system.payment.service.domain.entity.CreditEntry;
import com.food.ordering.system.payment.service.domain.ports.output.repository.CreditEntryRepository;

import jakarta.persistence.EntityManager;

@Component
public class CreditEntryRepositoryImpl implements CreditEntryRepository {
	private final CreditEntryJpaRepository creditEntryJpaRepository;
	private final CreditEntryDataAccessMapper creditEntryDataAccessMapper;
	private final EntityManager entityManager;

	public CreditEntryRepositoryImpl(final CreditEntryJpaRepository creditEntryJpaRepository,
			final CreditEntryDataAccessMapper creditEntryDataAccessMapper, final EntityManager entityManager) {
		this.creditEntryJpaRepository = creditEntryJpaRepository;
		this.creditEntryDataAccessMapper = creditEntryDataAccessMapper;
		this.entityManager = entityManager;
	}

	@Override
	public CreditEntry save(final CreditEntry creditEntry) {
		return creditEntryDataAccessMapper
				.creditEntryEntityToCreditEntry(creditEntryJpaRepository
						.save(creditEntryDataAccessMapper.creditEntryToCreditEntryEntity(creditEntry)));
	}

	@Override
	public Optional<CreditEntry> findByCustomerId(final CustomerId customerId) {
		return creditEntryJpaRepository
				.findByCustomerId(customerId.getValue())
				.map(creditEntryDataAccessMapper::creditEntryEntityToCreditEntry);
	}

	@Override
	public void detach(final CustomerId customerId) {
		entityManager.detach(creditEntryJpaRepository.findByCustomerId(customerId.getValue()).orElseThrow());
	}
}
