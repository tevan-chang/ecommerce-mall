package com.demo.mall.db;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 以與 docker-compose 相同的方式（掛載 /DB 的三支 sql 到 initdb.d）啟動 MySQL 8 容器，
 * 驗證 SP 的實際行為，而非在測試中重寫 SQL 邏輯。
 */
@Testcontainers
abstract class AbstractDbTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("mall")
            .withUsername("mall_app")
            .withPassword("test_password")
            .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci")
            .withCopyFileToContainer(MountableFile.forHostPath("../DB/01_ddl.sql"),
                    "/docker-entrypoint-initdb.d/01_ddl.sql")
            .withCopyFileToContainer(MountableFile.forHostPath("../DB/02_stored_procedures.sql"),
                    "/docker-entrypoint-initdb.d/02_stored_procedures.sql")
            .withCopyFileToContainer(MountableFile.forHostPath("../DB/03_dml_seed.sql"),
                    "/docker-entrypoint-initdb.d/03_dml_seed.sql");

    static Connection connection;

    @BeforeAll
    static void startContainerAndConnect() throws SQLException {
        connection = DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
    }

    /** 每個測試前重設商品庫存與訂單序號，避免測試互相影響（不依賴執行順序）。 */
    @BeforeEach
    void resetData() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("SET FOREIGN_KEY_CHECKS=0");
            st.execute("DELETE FROM order_detail");
            st.execute("DELETE FROM orders");
            st.execute("DELETE FROM order_seq");
            st.execute("DELETE FROM product_seq");
            st.execute("DELETE FROM product");
            st.execute("SET FOREIGN_KEY_CHECKS=1");
            st.execute("INSERT INTO product (product_id, product_name, price, quantity) VALUES "
                    + "('P001','osii 舒壓按摩椅',98000,5),"
                    + "('P002','網友最愛起司蛋糕',1200,50),"
                    + "('P003','真愛密碼項鍊',8500,20)");
            st.execute("INSERT INTO product_seq (seq_key, last_seq) VALUES ('P', 3)");
        }
    }
}
