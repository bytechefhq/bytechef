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

package com.bytechef.platform.component.jdbc.operation;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.exception.ComponentErrorType;
import com.bytechef.platform.component.jdbc.constant.JdbcConstants;
import java.math.BigDecimal;
import java.sql.JDBCType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.CallableStatementCreatorFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlInOutParameter;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlTypeValue;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterUtils;
import org.springframework.jdbc.core.namedparam.ParsedSql;

/**
 * @author Igor Beslic
 */
public class CallJdbcOperation implements JdbcOperation<Map<String, Object>> {

    public static final String MODE_IN_OUT = "IN_OUT";
    public static final String MODE_OUT = "OUT";

    public static final List<JDBCType> OUTPUT_PARAMETER_TYPES = List.of(
        JDBCType.BIGINT, JDBCType.BOOLEAN, JDBCType.CHAR, JDBCType.DATE, JDBCType.DECIMAL, JDBCType.DOUBLE,
        JDBCType.FLOAT, JDBCType.INTEGER, JDBCType.NUMERIC, JDBCType.NVARCHAR, JDBCType.REAL, JDBCType.SMALLINT,
        JDBCType.TIME, JDBCType.TIMESTAMP, JDBCType.TIMESTAMP_WITH_TIMEZONE, JDBCType.VARCHAR);

    @Override
    public Map<String, Object> execute(Map<String, ?> inputParameters, DataSource dataSource) {
        String statement = MapUtils.getRequiredString(inputParameters, JdbcConstants.CALL);
        Map<String, @Nullable Object> nameParameterValues = getNameParameterValues(inputParameters);
        Map<String, ResolvedOutputParameter> nameResolvedOutputParameters = getNameResolvedOutputParameters(
            inputParameters);

        ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(statement);

        MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource(nameParameterValues);

        List<SqlParameter> sqlParameters = NamedParameterUtils.buildSqlParameterList(
            parsedSql, mapSqlParameterSource);

        List<SqlParameter> declaredSqlParameters = createDeclaredParameters(
            sqlParameters, nameParameterValues, nameResolvedOutputParameters);

        String sql = NamedParameterUtils.substituteNamedParameters(parsedSql, mapSqlParameterSource);

        CallableStatementCreatorFactory callableStatementCreatorFactory = new CallableStatementCreatorFactory(
            sql, declaredSqlParameters);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.setSkipUndeclaredResults(true);

        Map<String, Object> nameCallResultValues = jdbcTemplate.call(
            callableStatementCreatorFactory.newCallableStatementCreator(nameParameterValues),
            declaredSqlParameters);

        Map<String, Object> nameOutputValues = new LinkedHashMap<>();

        for (String outputParameterName : nameResolvedOutputParameters.keySet()) {
            nameOutputValues.put(outputParameterName, nameCallResultValues.get(outputParameterName));
        }

        return nameOutputValues;
    }

    public static int getJdbcTypeNumber(String typeName) {
        try {
            JDBCType jdbcType = JDBCType.valueOf(typeName.toUpperCase(Locale.ROOT));

            if (!OUTPUT_PARAMETER_TYPES.contains(jdbcType)) {
                throw new IllegalArgumentException(typeName);
            }

            return jdbcType.getVendorTypeNumber();
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new ConfigurationException(
                "Unsupported output parameter type: " + typeName + ". Supported types are " +
                    OUTPUT_PARAMETER_TYPES + ".",
                ComponentErrorType.UNSUPPORTED_PROPERTY_TYPE);
        }
    }

    private static Map<String, @Nullable Object> getNameParameterValues(Map<String, ?> inputParameters) {
        Map<String, ?> nameValues = MapUtils.getMap(inputParameters, JdbcConstants.PARAMETERS, Map.of());

        Map<String, @Nullable Object> nameParameterValues = new HashMap<>();

        for (Map.Entry<String, ?> nameValueEntry : nameValues.entrySet()) {
            Object parameterValue = nameValueEntry.getValue();

            if (parameterValue instanceof Double || parameterValue instanceof Float) {
                parameterValue = new BigDecimal(parameterValue.toString());
            }

            nameParameterValues.put(nameValueEntry.getKey(), parameterValue);
        }

        return nameParameterValues;
    }

    private static List<SqlParameter> createDeclaredParameters(
        List<SqlParameter> sqlParameters, Map<String, ?> nameParameterValues,
        Map<String, ResolvedOutputParameter> nameResolvedOutputParameters) {

        Set<String> sqlParameterNames = sqlParameters.stream()
            .map(SqlParameter::getName)
            .collect(Collectors.toSet());

        for (String resolvedOutputParameterName : nameResolvedOutputParameters.keySet()) {
            if (!sqlParameterNames.contains(resolvedOutputParameterName)) {
                throw new ConfigurationException(
                    "Output parameter :" + resolvedOutputParameterName + " is not used in the call statement.",
                    ComponentErrorType.INVALID_CALL_PARAMETER);
            }
        }

        List<SqlParameter> declaredSqlParameters = new ArrayList<>();
        Set<String> createdOutputParameterNames = new HashSet<>();

        for (SqlParameter sqlParameter : sqlParameters) {
            String sqlParameterName = Objects.requireNonNull(sqlParameter.getName());

            ResolvedOutputParameter resolvedOutputParameter = nameResolvedOutputParameters.get(sqlParameterName);

            if (resolvedOutputParameter == null) {
                if (!nameParameterValues.containsKey(sqlParameterName)) {
                    throw new ConfigurationException(
                        "No value for input parameter :" + sqlParameterName + ". Add it to Parameters or declare it " +
                            "as an output parameter.",
                        ComponentErrorType.INVALID_CALL_PARAMETER);
                }

                Object parameterValue = nameParameterValues.get(sqlParameterName);

                if (parameterValue instanceof Collection<?> || parameterValue instanceof Object[]) {
                    throw new ConfigurationException(
                        "Input parameter :" + sqlParameterName + " must be a single value, not a list.",
                        ComponentErrorType.INVALID_CALL_PARAMETER);
                }

                declaredSqlParameters.add(new SqlParameter(sqlParameterName, SqlTypeValue.TYPE_UNKNOWN));

                continue;
            }

            if (!createdOutputParameterNames.add(sqlParameterName)) {
                throw new ConfigurationException(
                    "Output parameter :" + sqlParameterName + " is used more than once in the call statement.",
                    ComponentErrorType.INVALID_CALL_PARAMETER);
            }

            if (resolvedOutputParameter.inOut()) {
                declaredSqlParameters
                    .add(new SqlInOutParameter(sqlParameterName, resolvedOutputParameter.jdbcTypeNumber()));
            } else {
                declaredSqlParameters.add(
                    new SqlOutParameter(sqlParameterName, resolvedOutputParameter.jdbcTypeNumber()));
            }
        }

        return declaredSqlParameters;
    }

    private static Map<String, ResolvedOutputParameter> getNameResolvedOutputParameters(
        Map<String, ?> inputParameters) {

        List<OutputParameter> outputParameters = MapUtils.getList(
            inputParameters, JdbcConstants.OUTPUT_PARAMETERS, OutputParameter.class, List.of());

        Map<String, ResolvedOutputParameter> nameResolvedOutputParameters = new LinkedHashMap<>();

        for (OutputParameter outputParameter : outputParameters) {
            String name = outputParameter.name();

            if (name == null || name.isBlank()) {
                throw new ConfigurationException(
                    "Every output parameter needs a name.", ComponentErrorType.INVALID_CALL_PARAMETER);
            }

            String mode = outputParameter.mode() == null ? MODE_OUT : outputParameter.mode();

            if (!MODE_OUT.equals(mode) && !MODE_IN_OUT.equals(mode)) {
                throw new ConfigurationException(
                    "Output parameter :" + name + " has unsupported mode " + mode + ". Use OUT or IN_OUT.",
                    ComponentErrorType.INVALID_CALL_PARAMETER);
            }

            String type = outputParameter.type();

            if (type == null) {
                throw new ConfigurationException(
                    "Output parameter :" + name + " needs a type.", ComponentErrorType.INVALID_CALL_PARAMETER);
            }

            ResolvedOutputParameter resolvedOutputParameter = new ResolvedOutputParameter(
                getJdbcTypeNumber(type), MODE_IN_OUT.equals(mode));

            if (nameResolvedOutputParameters.put(name, resolvedOutputParameter) != null) {
                throw new ConfigurationException(
                    "Output parameter :" + name + " is declared more than once.",
                    ComponentErrorType.INVALID_CALL_PARAMETER);
            }
        }

        return nameResolvedOutputParameters;
    }

    public record OutputParameter(@Nullable String name, @Nullable String type, @Nullable String mode) {
    }

    private record ResolvedOutputParameter(int jdbcTypeNumber, boolean inOut) {
    }
}
