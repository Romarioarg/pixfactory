package com.pixfactory.repo;

import com.pixfactory.domain.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByCpf(String cpf);
    boolean existsByCpf(String cpf);
    List<Client> findByNomeContainingIgnoreCaseOrEmailContainingIgnoreCaseOrTelefoneContainingIgnoreCase(
            String nome, String email, String telefone);
}
