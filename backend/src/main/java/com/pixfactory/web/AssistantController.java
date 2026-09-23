package com.pixfactory.web;

import com.pixfactory.service.AssistantService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/assistente")
public class AssistantController {
    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping
    public Map<String, Object> ask(@RequestBody Map<String, Object> body) {
        return assistantService.ask(body.get("pergunta") == null ? "" : String.valueOf(body.get("pergunta")));
    }
}
