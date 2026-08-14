package com.pixfactory.repo;

import com.pixfactory.domain.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContractRepository extends JpaRepository<Contract, Long> {
    List<Contract> findByClientId(Long clientId);

    @Modifying
    @Query("delete from Contract c where c.client.id = :clientId")
    void deleteByClientId(@Param("clientId") Long clientId);
}
