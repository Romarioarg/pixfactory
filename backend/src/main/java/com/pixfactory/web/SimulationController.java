package com.pixfactory.web;

import com.pixfactory.service.LoanEngine;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/api/simulacoes", "/api/simulator"})
public class SimulationController {
    @PostMapping
    public Map<String, Object> simulate(@RequestBody Map<String, Object> body) {
        return LoanEngine.simulate(body);
    }
}
