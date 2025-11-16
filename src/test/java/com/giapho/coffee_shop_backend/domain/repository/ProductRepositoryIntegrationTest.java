package com.giapho.coffee_shop_backend.domain.repository;

import com.giapho.coffee_shop_backend.domain.entity.Category;
import com.giapho.coffee_shop_backend.domain.entity.Product;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductRepositoryIntegrationTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private SessionFactory sessionFactory;

    @DynamicPropertySource
    static void jpaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.hbm2ddl.auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.generate_statistics", () -> "true");
        registry.add("spring.jpa.show-sql", () -> "true");
    }

    @BeforeEach
    void setUp() {
        sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        sessionFactory.getStatistics().clear();
        sessionFactory.getStatistics().setStatisticsEnabled(true);
        entityManager.clear();
    }

    @Test
    void findAll_shouldFetchCategoriesInSingleSelectQuery() {
        Category category = new Category();
        category.setName("Coffee");
        category.setDescription("Hot drinks");
        entityManager.persist(category);

        IntStream.rangeClosed(1, 5).forEach(i -> {
            Product product = Product.builder()
                    .name("Product " + i)
                    .code("CODE" + i)
                    .price(BigDecimal.valueOf(1000 + i))
                    .category(category)
                    .build();
            entityManager.persist(product);
        });

        entityManager.flush();
        entityManager.clear();

        Statistics statistics = sessionFactory.getStatistics();
        statistics.clear();

        Page<Product> page = productRepository.findAll(PageRequest.of(0, 10));
        page.getContent().forEach(product -> assertThat(product.getCategory().getName()).isEqualTo("Coffee"));

        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(2);
    }
}
