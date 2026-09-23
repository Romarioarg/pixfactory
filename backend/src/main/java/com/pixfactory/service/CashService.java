package com.pixfactory.service;

import com.pixfactory.domain.CashMovement;
import com.pixfactory.domain.CashSession;
import com.pixfactory.exception.ApiException;
import com.pixfactory.exception.NotFoundException;
import com.pixfactory.repo.CashMovementRepository;
import com.pixfactory.repo.CashSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CashService {
    private final CashSessionRepository sessionRepository;
    private final CashMovementRepository movementRepository;

    public CashService(CashSessionRepository sessionRepository, CashMovementRepository movementRepository) {
        this.sessionRepository = sessionRepository;
        this.movementRepository = movementRepository;
    }

    public List<Map<String, Object>> history() {
        return sessionRepository.findAllByOrderByDateDesc().stream().map(this::session).toList();
    }

    public Map<String, Object> today() {
        return sessionRepository.findFirstByDateAndStatus(LocalDate.now(), "aberto")
                .or(() -> sessionRepository.findFirstByStatusOrderByOpenedAtDesc("aberto"))
                .map(this::detail)
                .orElse(Map.of("aberto", false, "message", "Nenhum caixa aberto hoje."));
    }

    @Transactional
    public Map<String, Object> open(BigDecimal opening, String operator) {
        if (sessionRepository.findFirstByDateAndStatus(LocalDate.now(), "aberto").isPresent()) {
            throw new ApiException(409, "Já existe um caixa aberto hoje.");
        }
        if (opening == null || opening.signum() < 0) {
            throw new ApiException(400, "Saldo de abertura inválido.");
        }
        CashSession session = new CashSession();
        session.setDate(LocalDate.now());
        session.setOpening(opening);
        session.setExpected(opening);
        session.setOperator(operator == null ? "operador" : operator);
        session.setStatus("aberto");
        return detail(sessionRepository.save(session));
    }

    @Transactional
    public Map<String, Object> addMovement(String tipo, BigDecimal valor, String origem, String descricao, Long chargeId, Long contractId) {
        CashSession session = openSession();
        if (valor == null || valor.signum() <= 0) {
            throw new ApiException(400, "Valor do movimento inválido.");
        }
        String kind = "saida".equalsIgnoreCase(tipo) ? "saida" : "entrada";
        CashMovement movement = new CashMovement();
        movement.setSessionId(session.getId());
        movement.setTipo(kind);
        movement.setOrigem(origem == null ? "manual" : origem);
        movement.setValor(valor);
        movement.setDescricao(descricao);
        movement.setChargeId(chargeId);
        movement.setContractId(contractId);
        movementRepository.save(movement);
        if ("entrada".equals(kind)) {
            session.setExpected(session.getExpected().add(valor));
        } else {
            session.setExpected(session.getExpected().subtract(valor));
        }
        sessionRepository.save(session);
        return detail(session);
    }

    @Transactional
    public void recordIfOpen(String tipo, BigDecimal valor, String origem, String descricao, Long chargeId, Long contractId) {
        if (sessionRepository.findFirstByDateAndStatus(LocalDate.now(), "aberto").isEmpty()) {
            return;
        }
        addMovement(tipo, valor, origem, descricao, chargeId, contractId);
    }

    @Transactional
    public Map<String, Object> close(BigDecimal informed, String notes) {
        CashSession session = openSession();
        if (informed == null || informed.signum() < 0) {
            throw new ApiException(400, "Informe o saldo contado no caixa.");
        }
        session.setInformed(informed);
        session.setDifference(informed.subtract(session.getExpected()));
        session.setNotes(notes);
        session.setStatus("fechado");
        session.setClosedAt(Instant.now());
        return detail(sessionRepository.save(session));
    }

    private CashSession openSession() {
        return sessionRepository.findFirstByDateAndStatus(LocalDate.now(), "aberto")
                .orElseThrow(() -> new NotFoundException("Não há caixa aberto. Abra o caixa do dia primeiro."));
    }

    private Map<String, Object> detail(CashSession session) {
        Map<String, Object> map = session(session);
        List<CashMovement> movements = movementRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
        BigDecimal entradas = movements.stream().filter(m -> "entrada".equals(m.getTipo())).map(CashMovement::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal saidas = movements.stream().filter(m -> "saida".equals(m.getTipo())).map(CashMovement::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        map.put("aberto", "aberto".equals(session.getStatus()));
        map.put("entradas", entradas);
        map.put("saidas", saidas);
        map.put("movimentos", movements.stream().map(this::movement).toList());
        return map;
    }

    private Map<String, Object> session(CashSession session) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", String.valueOf(session.getId()));
        map.put("date", session.getDate());
        map.put("status", session.getStatus());
        map.put("opening", session.getOpening());
        map.put("expected", session.getExpected());
        map.put("informed", session.getInformed());
        map.put("difference", session.getDifference());
        map.put("operator", session.getOperator());
        map.put("notes", session.getNotes());
        map.put("openedAt", session.getOpenedAt());
        map.put("closedAt", session.getClosedAt());
        return map;
    }

    private Map<String, Object> movement(CashMovement movement) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", String.valueOf(movement.getId()));
        map.put("tipo", movement.getTipo());
        map.put("origem", movement.getOrigem());
        map.put("valor", movement.getValor());
        map.put("descricao", movement.getDescricao());
        map.put("createdAt", movement.getCreatedAt());
        return map;
    }
}
