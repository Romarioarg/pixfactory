package com.pixfactory.repo;

import com.pixfactory.domain.Charge;
import com.pixfactory.domain.ChargeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ChargeRepository extends JpaRepository<Charge, Long> {
    List<Charge> findByContractIdOrderByNumeroAsc(Long contractId);

    List<Charge> findByClientIdOrderByVencimentoAsc(Long clientId);

    List<Charge> findByVencimento(LocalDate vencimento);

    List<Charge> findByStatusInOrderByVencimentoAsc(List<ChargeStatus> statuses);

    long countByContractId(Long contractId);

    @Modifying
    @Query("delete from Charge c where c.client.id = :clientId")
    void deleteByClientId(@Param("clientId") Long clientId);

    @Modifying
    @Query("delete from Charge c where c.contract.id = :contractId")
    void deleteByContractId(@Param("contractId") Long contractId);
}
