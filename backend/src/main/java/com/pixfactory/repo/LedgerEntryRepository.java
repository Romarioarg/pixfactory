package com.pixfactory.repo;

import com.pixfactory.domain.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findByChargeIdOrderByCreatedAtDesc(Long chargeId);
    List<LedgerEntry> findByContractIdOrderByCreatedAtDesc(Long contractId);
    boolean existsByChargeIdAndTipoAndValorAndCreatedAtAfter(Long chargeId, String tipo, java.math.BigDecimal valor, Instant after);
}
