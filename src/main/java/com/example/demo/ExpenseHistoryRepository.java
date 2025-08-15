package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExpenseHistoryRepository extends JpaRepository<ExpenseHistory, Long> {
    List<ExpenseHistory> findByUserEmailOrderByDateDesc(String userEmail);
}
