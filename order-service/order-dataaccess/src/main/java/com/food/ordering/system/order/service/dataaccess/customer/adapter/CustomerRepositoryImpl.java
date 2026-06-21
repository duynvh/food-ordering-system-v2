package com.food.ordering.system.order.service.dataaccess.customer.adapter;

import java.util.Optional;
import java.util.UUID;

import com.food.ordering.system.order.service.dataaccess.customer.mapper.CustomerDataAccessMapper;
import com.food.ordering.system.order.service.dataaccess.customer.repository.CustomerJpaRepository;
import com.food.ordering.system.order.service.domain.entity.Customer;
import com.food.ordering.system.order.service.domain.ports.output.repository.CustomerRepository;

public class CustomerRepositoryImpl implements CustomerRepository {
	private final CustomerJpaRepository customerJpaRepository;
	private final CustomerDataAccessMapper customerDataAccessMapper;

	public CustomerRepositoryImpl(CustomerJpaRepository customerJpaRepository, CustomerDataAccessMapper customerDataAccessMapper) {
		this.customerJpaRepository = customerJpaRepository;
		this.customerDataAccessMapper = customerDataAccessMapper;
	}

	@Override
	public Optional<Customer> findCustomer(final UUID customerId) {
		return customerJpaRepository.findById(customerId).map(customerDataAccessMapper::customerEntityToCustomer);
	}

	@Override
	public Customer save(final Customer customer) {
		return customerDataAccessMapper.customerEntityToCustomer(
				customerJpaRepository.save(customerDataAccessMapper.customerToCustomerEntity(customer)));
	}
}
