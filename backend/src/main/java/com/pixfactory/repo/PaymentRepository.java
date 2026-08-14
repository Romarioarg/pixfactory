package com.pixfactory.repo;

import com.pixfactory.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByContractIdOrderByCreatedAtDesc(Long contractId);
    Optional<Payment> findByTxid(String txid);
}
