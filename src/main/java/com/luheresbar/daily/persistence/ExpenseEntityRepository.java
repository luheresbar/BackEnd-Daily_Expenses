package com.luheresbar.daily.persistence;

import com.luheresbar.daily.domain.Expense;
import com.luheresbar.daily.domain.repository.IExpenseRepository;
import com.luheresbar.daily.persistence.crud.IExpenseCrudRepository;
import com.luheresbar.daily.persistence.entity.ExpenseEntity;
import com.luheresbar.daily.persistence.mapper.IExpenseMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Repository
public class ExpenseEntityRepository implements IExpenseRepository {

    private final IExpenseCrudRepository expenseCrudRepository;
    private final IExpenseMapper expenseMapper;

    public ExpenseEntityRepository(IExpenseCrudRepository expenseCrudRepository, IExpenseMapper expenseMapper) {
        this.expenseCrudRepository = expenseCrudRepository;
        this.expenseMapper = expenseMapper;
    }

//    @Override
//    public List<Expense> getUserExpenses(Integer userId) {
//        // Metodo usando JdbcTemplate
//        String sql = "SELECT * FROM expenses WHERE user_id = ? ORDER BY expense_date DESC";
//        List<ExpenseEntity> expenseEntities = jdbcTemplate.query(sql, new Object[]{userId}, new BeanPropertyRowMapper<>(ExpenseEntity.class));
//        return this.expenseMapper.toExpenses(expenseEntities);
//    }

    @Override
    public List<Expense> getUserExpenses(Integer userId) {
        List<ExpenseEntity> expenseEntities = this.expenseCrudRepository.findAllByUserIdOrderByExpenseDate(userId);
        return this.expenseMapper.toExpenses(expenseEntities);
    }

    @Override
    public List<Expense> getAccountExpenses(String accountName, Integer userId) {
        return this.expenseMapper.toExpenses(this.expenseCrudRepository.getAccountExpenses(accountName, userId));
    }

    @Override
    public List<Expense> findByDateBetween(LocalDateTime startDate, LocalDateTime endDate, int userId) {
        List<ExpenseEntity> expenseEntities = this.expenseCrudRepository.findByDateBetween(startDate, endDate, userId);

        return this.expenseMapper.toExpenses(expenseEntities);
    }

    @Override
    public Double getMonthlyExpenseTotal(LocalDateTime startDate, LocalDateTime endDate, Integer userId) {
        return this.expenseCrudRepository.getMonthlyExpenseTotal(startDate, endDate, userId);
    }

    @Override
    public Expense save(Expense expense) {
        ExpenseEntity expenseEntity = expenseMapper.toExpenseEntity(expense);
        return expenseMapper.toExpense(expenseCrudRepository.save(expenseEntity));
    }

    @Override
    public Optional<Expense> getById(int expenseId) {
        Optional<ExpenseEntity> expenseEntity = this.expenseCrudRepository.findById(expenseId);
        return expenseEntity.map(expense -> this.expenseMapper.toExpense(expense));
    }


    @Override
    public boolean delete(int expenseId, Integer userId) {
        ExpenseEntity expenseEntity = this.expenseCrudRepository.findById(expenseId).orElse(null);
        if(expenseEntity != null && expenseEntity.getUserId().equals(userId)) {
            this.expenseCrudRepository.delete(expenseEntity);
            return true;
        }
        return false;
    }


}
