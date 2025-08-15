package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByUserEmail(String email);

    @Query("SELECT MONTH(e.date), SUM(e.amount) FROM Expense e WHERE e.user.email = :email GROUP BY MONTH(e.date)")
    List<Object[]> getMonthlyExpenseSummary(@Param("email") String email);

    @Query("SELECT e FROM Expense e WHERE e.user.email = :email AND YEAR(e.date) = :year AND MONTH(e.date) = :month")
    List<Expense> findByUserEmailAndMonth(@Param("email") String email, @Param("year") int year, @Param("month") int month);
    
    @Query("SELECT e FROM Expense e WHERE e.user.email = :email ORDER BY e.date DESC")
    List<Expense> getFullExpenseHistory(@Param("email") String email);
}