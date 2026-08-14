package com.pixfactory.repo;

import com.pixfactory.domain.Payable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayableRepository extends JpaRepository<Payable, Long> {}
