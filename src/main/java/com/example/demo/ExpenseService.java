package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private ExpenseHistoryRepository expenseHistoryRepository;

    public void saveExpense(User user, String item, double amount) {
        // Save expense in main expenses table
        Expense expense = new Expense();
        expense.setUser(user);
        expense.setItem(item);
        expense.setAmount(amount);
        expenseRepository.save(expense);

        // Also save a record in expense_history
        ExpenseHistory history = new ExpenseHistory();
        history.setUserEmail(user.getEmail());
        history.setItem(item);
        history.setAmount(amount);
        history.setDate(expense.getDate());
        expenseHistoryRepository.save(history);
    }
}
