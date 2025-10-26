package com.giapho.coffee_shop_backend.domain.repository;

import com.giapho.coffee_shop_backend.domain.entity.Ingredient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

    Optional<Ingredient> findByName(String name);

    boolean existsByName(String name);

    List<Ingredient> findByQuantityOnHandLessThanEqual(BigDecimal threshold);

    Page<Ingredient> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
