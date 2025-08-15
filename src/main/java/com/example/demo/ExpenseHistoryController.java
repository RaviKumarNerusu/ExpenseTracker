package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/history")
@CrossOrigin("*")
public class ExpenseHistoryController {

    @Autowired
    private ExpenseHistoryRepository expenseHistoryRepository;

    @GetMapping("/{email}")
    public ResponseEntity<?> getHistory(@PathVariable String email) {
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Email is required");
        }

        List<ExpenseHistory> history = expenseHistoryRepository.findByUserEmailOrderByDateDesc(email);

        if (history.isEmpty()) {
            return ResponseEntity.ok().body("No history found");
        }

        return ResponseEntity.ok(history);
    }
}
