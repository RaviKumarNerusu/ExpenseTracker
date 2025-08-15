package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class ExpenseController {

    private static final Logger logger = LoggerFactory.getLogger(ExpenseController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private ExpenseService expenseService; // ✅ Added Service

    // ➕ Add Expense (now saves in both expenses & expense_history)
    @PostMapping("/expense/add")
    public ResponseEntity<?> addExpense(@RequestBody Map<String, Object> payload) {
        logger.info("Received addExpense request with payload: {}", payload);

        if (payload == null || payload.isEmpty()) {
            logger.error("Payload is null or empty");
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Request payload is empty"));
        }

        String email = (String) payload.get("email");
        String item = (String) payload.get("item");
        Object amountObj = payload.get("amount");

        if (email == null || email.trim().isEmpty()) {
            logger.error("Email is missing or empty");
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Email is required"));
        }
        if (item == null || item.trim().isEmpty()) {
            logger.error("Item is missing or empty");
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Item is required"));
        }
        if (amountObj == null) {
            logger.error("Amount is missing");
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Amount is required"));
        }

        double amount;
        try {
            amount = Double.parseDouble(amountObj.toString());
            if (amount < 0) {
                logger.error("Negative amount provided: {}", amount);
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Amount cannot be negative"));
            }
        } catch (NumberFormatException e) {
            logger.error("Invalid amount format: {}", amountObj, e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid amount format"));
        }

        User user = userRepository.findByEmail(email.trim());
        if (user == null) {
            logger.error("User not found for email: {}", email);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "User not found"));
        }

        try {
            expenseService.saveExpense(user, item.trim(), amount); // ✅ Using service to save both tables
            logger.info("Expense added successfully for user: {}", email);
            return ResponseEntity.ok(Map.of("success", true, "message", "Expense added successfully"));
        } catch (Exception e) {
            logger.error("Failed to save expense for user: {}", email, e);
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Failed to add expense: " + e.getMessage()));
        }
    }

    // 📜 Get all expenses by email
    @GetMapping("/expenses/{email}")
    public ResponseEntity<?> getAllExpenses(@PathVariable String email) {
        try {
            List<Expense> expenses = expenseRepository.findByUserEmail(email);
            return ResponseEntity.ok(expenses);
        } catch (Exception e) {
            logger.error("Failed to fetch expenses for email: {}", email, e);
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Failed to fetch expenses: " + e.getMessage()));
        }
    }

    // 📈 Monthly summary of expenses for each month
    @GetMapping("/expenses/monthly/{email}")
    public ResponseEntity<?> getMonthlyExpense(@PathVariable String email) {
        List<Object[]> summary = expenseRepository.getMonthlyExpenseSummary(email);
        double[] monthlyAmounts = new double[12]; // Jan - Dec

        for (Object[] row : summary) {
            int month = (Integer) row[0];
            double total = (Double) row[1];
            monthlyAmounts[month - 1] = total;
        }

        return ResponseEntity.ok(monthlyAmounts);
    }

    // 💰 Get salary and remaining balance
    @GetMapping("/user/salary/{email}")
    public ResponseEntity<?> getSalary(@PathVariable String email) {
        try {
            User user = userRepository.findByEmail(email);
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "User not found"));
            }

            String currentMonth = YearMonth.now().toString();
            double monthlySalary = user.getMonthlySalary() != null ? user.getMonthlySalary() : 0.0;

            List<Expense> expenses = expenseRepository.findByUserEmailAndMonth(
                email, YearMonth.now().getYear(), YearMonth.now().getMonthValue()
            );
            double totalExpenses = expenses != null ? expenses.stream().mapToDouble(Expense::getAmount).sum() : 0.0;
            double remainingSalary = monthlySalary - totalExpenses;

            if (user.getSalaryMonth() == null || !user.getSalaryMonth().equals(currentMonth)) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "salary", monthlySalary,
                    "totalExpenses", totalExpenses,
                    "remainingSalary", remainingSalary,
                    "message", "Please set salary for this month"
                ));
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "salary", monthlySalary,
                "totalExpenses", totalExpenses,
                "remainingSalary", remainingSalary,
                "message", "Salary and expenses retrieved successfully"
            ));
        } catch (Exception e) {
            logger.error("Failed to fetch salary for email: {}", email, e);
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Failed to fetch salary: " + e.getMessage()));
        }
    }

    // 📝 Set monthly salary for current month
    @PostMapping("/user/salary/set")
    public ResponseEntity<?> setSalary(@RequestBody Map<String, Object> body) {
        String email = (String) body.get("email");
        Double salary;
        try {
            salary = Double.parseDouble(body.get("salary").toString());
            if (salary < 0) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Salary cannot be negative"));
            }
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid salary format"));
        }

        String currentMonth = YearMonth.now().toString();

        User user = userRepository.findByEmail(email);
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "User not found"));
        }

        user.setMonthlySalary(salary);
        user.setSalaryMonth(currentMonth);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("success", true, "message", "Salary set successfully"));
    }
}
