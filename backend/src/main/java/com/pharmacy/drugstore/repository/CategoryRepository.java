package com.pharmacy.drugstore.repository;

import com.pharmacy.drugstore.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
