package com.demo.mall.common;

import org.springframework.dao.DataAccessException;

import java.sql.SQLException;

public final class SqlStateUtils {

    private SqlStateUtils() {
    }

    public static String extract(DataAccessException ex) {
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof SQLException sqlException) {
                return sqlException.getSQLState();
            }
            cause = cause.getCause();
        }
        return null;
    }
}
