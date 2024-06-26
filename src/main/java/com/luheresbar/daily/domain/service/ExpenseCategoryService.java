package com.luheresbar.daily.domain.service;

import com.luheresbar.daily.domain.ExpenseCategory;
import com.luheresbar.daily.domain.dto.CategoryDto;
import com.luheresbar.daily.domain.repository.IExpenseCategoryRepository;
import com.luheresbar.daily.persistence.entity.ExpenseCategoryPK;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ExpenseCategoryService {

    private final IExpenseCategoryRepository categoryRepository;

    public ExpenseCategoryService(IExpenseCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<ExpenseCategory> getByUser(Integer userId) {
        return this.categoryRepository.getByUser(userId);
    }

    public List<ExpenseCategory> getEnabledCategoriesByUser(Integer userId) {
        return this.categoryRepository.getEnabledCategoriesByUser(userId);
    }
    public List<ExpenseCategory> getDisabledCategoriesByUser(Integer userId) {
        return this.categoryRepository.getDisabledCategoriesByUser(userId);
    }

    public Optional<ExpenseCategory> getById(String categoryName, Integer userId) {
        return this.categoryRepository.getById(categoryName, userId);
    }
    public boolean exists(ExpenseCategoryPK expenseCategoryPK) {
        return this.categoryRepository.exists(expenseCategoryPK);
    }

    public ExpenseCategory save(ExpenseCategory expenseCategory) {
        return this.categoryRepository.save(expenseCategory);
    }

    public void delete(ExpenseCategoryPK expenseCategoryPK) {
        this.categoryRepository.delete(expenseCategoryPK);
    }

    public List<CategoryDto> expenseCategoriesToDto(List<ExpenseCategory> expenseCategories) {
        List<CategoryDto> categoryDtos = new ArrayList<>();

        // Convertir Transfer a TransactionDetail
        for (ExpenseCategory category : expenseCategories) {
            CategoryDto categoryDto = new CategoryDto();
            categoryDto.setCategoryType("expense");
            categoryDto.setUserId(category.getUserId());
            categoryDto.setAvailable(category.getAvailable());
            categoryDto.setCategoryName(category.getCategoryName());

            categoryDtos.add(categoryDto);
        }
        return categoryDtos;
    }

    public void updateNameCategory(String categoryName, String newCategoryName, Integer userId) {
        this.categoryRepository.updateNameCategory(categoryName, newCategoryName, userId);
    }


}
