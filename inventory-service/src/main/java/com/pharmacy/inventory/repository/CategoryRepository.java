package com.pharmacy.inventory.repository;

import com.pharmacy.inventory.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {}
