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
 * Calls a stored procedure or function through a {@link java.sql.CallableStatement}.
 *
 * <p>
 * The statement uses the same {@code :name} placeholders as the other JDBC actions, so it can be written in the JDBC
 * escape syntax ({@code {:result = call balance(:accountId)}}, {@code {call transfer(:from, :to, :status)}}) or as a
 * vendor block ({@code BEGIN :result := balance(:accountId); END;}). A placeholder is an IN parameter unless it is
 * declared in the output parameters list, where it is registered as OUT or IN_OUT with the declared JDBC type. Values
 * for IN and IN_OUT parameters come from the parameters map.
 *
 * <p>
 * The result is a map of the OUT and IN_OUT parameter values keyed by parameter name. Result sets and update counts the
 * call returns without a declared parameter are skipped.
 *
 * @author Igor Beslic
 */
public class CallJdbcOperation implements JdbcOperation<Map<String, Object>> {

    public static final String MODE_IN_OUT = "IN_OUT";
    public static final String MODE_OUT = "OUT";

    /**
     * JDBC types an output parameter can be registered with. Kept to the scalar types that every supported driver
     * returns as a JSON-friendly value.
     */
    public static final List<JDBCType> OUTPUT_PARAMETER_TYPES = List.of(
        JDBCType.BIGINT, JDBCType.BOOLEAN, JDBCType.CHAR, JDBCType.DATE, JDBCType.DECIMAL, JDBCType.DOUBLE,
        JDBCType.FLOAT, JDBCType.INTEGER, JDBCType.NUMERIC, JDBCType.NVARCHAR, JDBCType.REAL, JDBCType.SMALLINT,
        JDBCType.TIME, JDBCType.TIMESTAMP, JDBCType.TIMESTAMP_WITH_TIMEZONE, JDBCType.VARCHAR);

    @Override
    public Map<String, Object> execute(Map<String, ?> inputParameters, DataSource dataSource) {
        String statement = MapUtils.getRequiredString(inputParameters, JdbcConstants.CALL);
        Map<String, @Nullable Object> parameterValues = getParameterValues(inputParameters);
        Map<String, ResolvedOutputParameter> outputParameters = getOutputParameters(inputParameters);

        ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(statement);

        MapSqlParameterSource parameterSource = new MapSqlParameterSource(parameterValues);

        List<SqlParameter> placeholders = NamedParameterUtils.buildSqlParameterList(parsedSql, parameterSource);

        List<SqlParameter> declaredParameters = createDeclaredParameters(
            placeholders, parameterValues, outputParameters);

        String sql = NamedParameterUtils.substituteNamedParameters(parsedSql, parameterSource);

        CallableStatementCreatorFactory callableStatementCreatorFactory = new CallableStatementCreatorFactory(
            sql, declaredParameters);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.setSkipUndeclaredResults(true);

        Map<String, Object> callResult = jdbcTemplate.call(
            callableStatementCreatorFactory.newCallableStatementCreator(parameterValues),
            declaredParameters);

        Map<String, Object> outputValues = new LinkedHashMap<>();

        for (String name : outputParameters.keySet()) {
            outputValues.put(name, callResult.get(name));
        }

        return outputValues;
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

    /**
     * Binds fractional numbers as {@link BigDecimal}. A {@link Double} is sent as double precision, which PostgreSQL
     * does not implicitly cast to a numeric routine argument, so the routine is not found; numeric casts implicitly to
     * every other numeric type, and Oracle NUMBER takes it natively.
     */
    private static Map<String, @Nullable Object> getParameterValues(Map<String, ?> inputParameters) {
        Map<String, ?> parameters = MapUtils.getMap(inputParameters, JdbcConstants.PARAMETERS, Map.of());

        Map<String, @Nullable Object> parameterValues = new HashMap<>();

        for (Map.Entry<String, ?> entry : parameters.entrySet()) {
            Object value = entry.getValue();

            if (value instanceof Double || value instanceof Float) {
                value = new BigDecimal(value.toString());
            }

            parameterValues.put(entry.getKey(), value);
        }

        return parameterValues;
    }

    private static List<SqlParameter> createDeclaredParameters(
        List<SqlParameter> placeholders, Map<String, ?> parameterValues,
        Map<String, ResolvedOutputParameter> outputParameters) {

        Set<String> placeholderNames = placeholders.stream()
            .map(SqlParameter::getName)
            .collect(Collectors.toSet());

        for (String outputParameterName : outputParameters.keySet()) {
            if (!placeholderNames.contains(outputParameterName)) {
                throw new ConfigurationException(
                    "Output parameter :" + outputParameterName + " is not used in the call statement.",
                    ComponentErrorType.INVALID_CALL_PARAMETER);
            }
        }

        List<SqlParameter> declaredParameters = new ArrayList<>();
        Set<String> declaredOutputParameterNames = new HashSet<>();

        for (SqlParameter placeholder : placeholders) {
            String name = placeholder.getName();

            ResolvedOutputParameter outputParameter = outputParameters.get(name);

            if (outputParameter == null) {
                if (!parameterValues.containsKey(name)) {
                    throw new ConfigurationException(
                        "No value for input parameter :" + name + ". Add it to Parameters or declare it as an " +
                            "output parameter.",
                        ComponentErrorType.INVALID_CALL_PARAMETER);
                }

                Object value = parameterValues.get(name);

                if (value instanceof Collection<?> || value instanceof Object[]) {
                    throw new ConfigurationException(
                        "Input parameter :" + name + " must be a single value, not a list.",
                        ComponentErrorType.INVALID_CALL_PARAMETER);
                }

                declaredParameters.add(new SqlParameter(name, SqlTypeValue.TYPE_UNKNOWN));

                continue;
            }

            if (!declaredOutputParameterNames.add(name)) {
                throw new ConfigurationException(
                    "Output parameter :" + name + " is used more than once in the call statement.",
                    ComponentErrorType.INVALID_CALL_PARAMETER);
            }

            if (outputParameter.inOut()) {
                declaredParameters.add(new SqlInOutParameter(name, outputParameter.jdbcTypeNumber()));
            } else {
                declaredParameters.add(new SqlOutParameter(name, outputParameter.jdbcTypeNumber()));
            }
        }

        return declaredParameters;
    }

    private static Map<String, ResolvedOutputParameter> getOutputParameters(Map<String, ?> inputParameters) {
        List<OutputParameter> outputParameterList = MapUtils.getList(
            inputParameters, JdbcConstants.OUTPUT_PARAMETERS, OutputParameter.class, List.of());

        Map<String, ResolvedOutputParameter> outputParameters = new LinkedHashMap<>();

        for (OutputParameter outputParameter : outputParameterList) {
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

            if (outputParameters.put(name, resolvedOutputParameter) != null) {
                throw new ConfigurationException(
                    "Output parameter :" + name + " is declared more than once.",
                    ComponentErrorType.INVALID_CALL_PARAMETER);
            }
        }

        return outputParameters;
    }

    /**
     * An OUT or IN_OUT parameter declaration: the placeholder name, its JDBC type name and its mode.
     */
    public record OutputParameter(@Nullable String name, @Nullable String type, @Nullable String mode) {
    }

    private record ResolvedOutputParameter(int jdbcTypeNumber, boolean inOut) {
    }
}
