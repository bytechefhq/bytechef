/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.component.util;

import static com.bytechef.component.definition.ComponentDsl.option;

import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Property.Type;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.exception.ComponentErrorType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class SqlUtils {

    private static final Logger log = LoggerFactory.getLogger(SqlUtils.class);

    private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = Arrays.asList(
        DateTimeFormatter.ISO_OFFSET_DATE_TIME,
        DateTimeFormatter.ISO_DATE_TIME,
        DateTimeFormatter.ISO_DATE,
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
        DateTimeFormatter.ofPattern("MM-dd-yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yy"),
        DateTimeFormatter.ofPattern("MM/dd/yy"),
        DateTimeFormatter.ofPattern("dd.MM.yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("EEE MMM dd yyyy HH:mm:ss"),
        DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss yyyy"),
        DateTimeFormatter.ofPattern("MMMM dd yyyy HH:mm:ss"),
        DateTimeFormatter.ofPattern("MMMM dd yyyy"),
        DateTimeFormatter.ofPattern("MMM dd yyyy"));

    /**
     * Quotes a SQL identifier (table, column, schema name) using double quotes to prevent SQL injection. Any embedded
     * double quotes are escaped by doubling them per the SQL standard.
     */
    public static String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    /**
     * Validates a free-form SQL condition (a {@code WHERE} clause) authored by the workflow creator. The condition is
     * an intentional SQL fragment, not a bindable value, so it cannot be fully parameterized. This guard rejects the
     * constructs that let an attacker escalate a single boolean predicate into stacked statements or comment-based
     * truncation (statement terminators and SQL comment markers), which limits the SQL-injection blast radius while
     * preserving legitimate single-expression conditions.
     *
     * @param condition the user-provided condition
     * @return the same condition, when it passes validation
     * @throws IllegalArgumentException if the condition is blank or contains statement separators or SQL comments
     */
    public static String validateCondition(String condition) {
        if (condition == null || condition.isBlank()) {
            throw new IllegalArgumentException("Condition must not be empty");
        }

        if (condition.contains(";") || condition.contains("--") || condition.contains("/*") ||
            condition.contains("*/")) {

            throw new IllegalArgumentException(
                "Condition must be a single boolean expression and may not contain statement separators (';') or " +
                    "SQL comments ('--', '/*', '*/')");
        }

        return condition;
    }

    @SuppressFBWarnings("ODR")
    public static void checkColumnTypes(
        String schema, String table, List<Map<String, Object>> rows, DataSource dataSource) {

        try {
            Connection connection = dataSource.getConnection();

            DatabaseMetaData databaseMetaData = connection.getMetaData();

            ResultSet resultSet = databaseMetaData.getColumns(null, schema, table, null);

            while (resultSet.next()) {
                String columnName = resultSet.getString("COLUMN_NAME");
                int columnDataType = resultSet.getInt("DATA_TYPE");

                JDBCType jdbcType = JDBCType.valueOf(columnDataType);

                if (jdbcType == JDBCType.DATE) {
                    for (Map<String, Object> row : rows) {
                        if (row.containsKey(columnName)) {
                            Object value = row.get(columnName);

                            if (value instanceof String string) {
                                row.put(columnName, parseUnknownFormat(string));
                            } else if (value instanceof Number number) {
                                row.put(columnName, new Date(number.longValue()));
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Option<String>> getTypeOptions() {
        List<Type> types = List.of(
            Type.ARRAY, Type.BOOLEAN, Type.DATE, Type.DATE_TIME, Type.INTEGER, Type.NUMBER, Type.OBJECT, Type.STRING,
            Type.TIME);

        return types.stream()
            .map(type -> option(type.name(), type.name()))
            .collect(Collectors.toList());
    }

    /**
     * Attempts to parse a date/time string using a list of predefined formats. Tries to parse as LocalDateTime first,
     * then LocalDate.
     *
     * @param dateString The string to parse.
     * @return A LocalDateTime or LocalDate object, or null if parsing fails with all formats.
     */
    private static Object parseUnknownFormat(String dateString) {
        Objects.requireNonNull(dateString, "Date string cannot be null");

        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(dateString, formatter);
            } catch (DateTimeParseException e1) {
                try {
                    return LocalDate.parse(dateString, formatter);
                } catch (DateTimeParseException e2) {
                    if (log.isTraceEnabled()) {
                        log.trace("Unable to parse date string: {} with formatter: {}", dateString, formatter);
                    }
                }
            }
        }

        throw new ConfigurationException(
            "Unable to parse date string: " + dateString, ComponentErrorType.INVALID_DATE_STRING);
    }
}
