package com.pixfactory.repo;

import com.pixfactory.domain.BankConnection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankConnectionRepository extends JpaRepository<BankConnection, Long> {}
