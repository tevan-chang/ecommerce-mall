package com.demo.mall.integration;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * 以真實 MySQL（DDL + 6 支 SP）啟動 Spring 完整上下文，驗證 OrderService 在真實 Transaction 下的
 * 防超賣與 rollback 行為（案例 4、5）。每個測試自行插入所需商品資料，不依賴共用狀態或執行順序。
 *
 * 容器採「Singleton container」模式（靜態區塊啟動、不主動 stop，交給 Ryuk 於 JVM 結束時回收）：
 * 若改用 @Container + @Testcontainers，擴充套件會在各子類別的 afterAll 停止容器，但 Spring 的
 * ApplicationContext 快取仍沿用舊連線資訊，導致第二個子類別連到已關閉的容器而連線逾時。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
abstract class AbstractOrderIntegrationTest {

    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("mall")
            .withUsername("mall_app")
            .withPassword("test_password")
            .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci")
            .withCopyFileToContainer(MountableFile.forHostPath("../DB/01_ddl.sql"),
                    "/docker-entrypoint-initdb.d/01_ddl.sql")
            .withCopyFileToContainer(MountableFile.forHostPath("../DB/02_stored_procedures.sql"),
                    "/docker-entrypoint-initdb.d/02_stored_procedures.sql");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        // 10 個 thread 各自佔用一條交易連線，池子加大避免併發測試互相排擠。
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "20");
    }

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetData() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=0");
        jdbcTemplate.update("DELETE FROM order_detail");
        jdbcTemplate.update("DELETE FROM orders");
        jdbcTemplate.update("DELETE FROM order_seq");
        jdbcTemplate.update("DELETE FROM product");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=1");
    }
}
