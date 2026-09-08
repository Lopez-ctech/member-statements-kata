package com.cu.api.controller;

import com.cu.api.service.StatementService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatementsController {

    private final StatementService statements;

    StatementsController(StatementService statements) {
        this.statements = statements;
    }

    /**
     * The body is bound as a plain {@code Object} so that the service does the
     * checking and reports every broken rule at once, rather than the framework
     * rejecting the request on the first one.
     */
    @PostMapping("/statements")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> postStatement(@RequestBody Object payload) {
        return Map.of("statementId", statements.recordStatement(payload));
    }
}
