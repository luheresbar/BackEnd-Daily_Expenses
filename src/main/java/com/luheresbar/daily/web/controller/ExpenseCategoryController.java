package com.luheresbar.daily.web.controller;

import com.luheresbar.daily.domain.ExpenseCategory;
import com.luheresbar.daily.domain.dto.CategoryDto;
import com.luheresbar.daily.domain.dto.SummaryCategoryDto;
import com.luheresbar.daily.domain.dto.UpdateCategoryDto;
import com.luheresbar.daily.domain.service.ExpenseCategoryService;
import com.luheresbar.daily.persistence.entity.ExpenseCategoryPK;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/expense-categories")
public class ExpenseCategoryController {

    private final ExpenseCategoryService expenseCategoryService;
    private Integer currentUser;

    public ExpenseCategoryController(ExpenseCategoryService expenseCategoryService) {
        this.expenseCategoryService = expenseCategoryService;
    }

    @ModelAttribute
    private void extractUserFromToken() {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();
        String userToken = (String) authentication.getPrincipal();
        this.currentUser = Integer.valueOf(userToken);
    }

    @GetMapping
    public ResponseEntity<SummaryCategoryDto> viewCategoriesUser() {
        List<ExpenseCategory> enabledCategories = this.expenseCategoryService.getEnabledCategoriesByUser(this.currentUser);
        List<ExpenseCategory> disabledCategories = this.expenseCategoryService.getDisabledCategoriesByUser(this.currentUser);

        List<CategoryDto> enabledCategoriesToDto = this.expenseCategoryService.expenseCategoriesToDto(enabledCategories);
        List<CategoryDto> disabledCategoriesToDto = this.expenseCategoryService.expenseCategoriesToDto(disabledCategories);

        return ResponseEntity.ok(new SummaryCategoryDto(enabledCategoriesToDto, disabledCategoriesToDto));
    }

    @PostMapping("/create")
    public ResponseEntity<CategoryDto> add(@RequestBody ExpenseCategory expenseCategory) {
        expenseCategory.setUserId(this.currentUser);
        expenseCategory.setAvailable(true);
        ExpenseCategoryPK expenseCategoryPK = new ExpenseCategoryPK(expenseCategory.getCategoryName(), expenseCategory.getUserId());

        if (!this.expenseCategoryService.exists(expenseCategoryPK)) {
            ExpenseCategory newExpenseCategory = this.expenseCategoryService.save(expenseCategory);
            List<ExpenseCategory> categoryList = Collections.singletonList(newExpenseCategory);
            List<CategoryDto> categoryDto = this.expenseCategoryService.expenseCategoriesToDto(categoryList);
            return ResponseEntity.ok(categoryDto.get(0));
        }
        return ResponseEntity.badRequest().build();
    }

    @PutMapping("/update")
    public ResponseEntity<CategoryDto> updateCategory(@RequestBody UpdateCategoryDto updateCategoryDto) {
        updateCategoryDto.setUserId(currentUser);

        ExpenseCategoryPK expenseCategoryPK = new ExpenseCategoryPK(updateCategoryDto.getCategoryName(), updateCategoryDto.getUserId());

        if (!this.expenseCategoryService.exists(expenseCategoryPK)) {
            return ResponseEntity.notFound().build();
        }

        if (!Objects.equals(updateCategoryDto.getCategoryName(), updateCategoryDto.getNewCategoryName())) {
            this.expenseCategoryService.updateNameCategory(updateCategoryDto.getCategoryName(), updateCategoryDto.getNewCategoryName(), this.currentUser);
        }

        Optional<ExpenseCategory> optionalCategoryInDb = this.expenseCategoryService.getById(updateCategoryDto.getNewCategoryName(), this.currentUser);

        // Verificar si el Optional contiene un valor antes de extraerlo y asignar valores predeterminados
        ExpenseCategory category = new ExpenseCategory();
        category.setUserId(updateCategoryDto.getUserId());
        category.setCategoryName(updateCategoryDto.getNewCategoryName());
        category.setAvailable(updateCategoryDto.getAvailable());

        if (optionalCategoryInDb.isPresent()) {
            ExpenseCategory categoryInDb = optionalCategoryInDb.get();

            if (categoryInDb.equals(category)) {
                List<ExpenseCategory> categoryList = Collections.singletonList(category);
                List<CategoryDto> categoryDto = this.expenseCategoryService.expenseCategoriesToDto(categoryList);
                return ResponseEntity.ok(categoryDto.get(0));
            }
        }

        // Guardar la categoría actualizada o nueva
        ExpenseCategory updatedCategory = this.expenseCategoryService.save(category);
        List<ExpenseCategory> categoryList = Collections.singletonList(updatedCategory);
        List<CategoryDto> categoryDto = this.expenseCategoryService.expenseCategoriesToDto(categoryList);
        return ResponseEntity.ok(categoryDto.get(0));
    }


    @DeleteMapping("/delete")
    public ResponseEntity<Void> delete(@RequestBody ExpenseCategoryPK categoryPK) {
        categoryPK.setUserId(this.currentUser);
        if (this.expenseCategoryService.exists(categoryPK)) {
            if (!categoryPK.getCategoryName().equals("Others")) {
                this.expenseCategoryService.delete(categoryPK);
                return ResponseEntity.ok().build();
            }
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.notFound().build();
    }

}
