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

package com.bytechef.server.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.audit.McpServerAuditMapper;
import com.bytechef.platform.mcp.domain.McpServer;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.ast.CompoundExpression;
import org.springframework.expression.spel.ast.PropertyOrFieldReference;
import org.springframework.expression.spel.ast.VariableReference;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * @author Ivica Cardic
 */
class AuditDataContractTest {

    private static final String CLASS_PATTERN = "classpath*:com/bytechef/**/*.class";
    private static final String COMPONENT_PACKAGE_PATH = "/com/bytechef/component/";
    private static final String RESULT_VARIABLE = "result";
    private static final List<String> NON_SECRET_NAME_SUFFIXES = List.of(
        "count", "id", "ids", "name", "names", "type");
    private static final List<String> SECRET_NAME_FRAGMENTS = List.of(
        "apikey", "credential", "parameters", "password", "privatekey", "secret", "token");

    private static final SpelExpressionParser SPEL_EXPRESSION_PARSER = new SpelExpressionParser();

    private static List<Method> auditedMethods;

    @BeforeAll
    static void beforeAll() throws IOException, ClassNotFoundException {
        auditedMethods = findAuditedMethods();
    }

    @Test
    void testAuditedMethodsAreFound() {
        assertThat(auditedMethods).hasSizeGreaterThan(50);
    }

    @Test
    void testEveryAuditDataExpressionReadsADeclaredVariableAndProperty() {
        List<String> violations = new ArrayList<>();

        for (Method method : auditedMethods) {
            Audited audited = method.getAnnotation(Audited.class);

            for (AuditData auditData : audited.data()) {
                checkExpression(method, auditData, violations);
            }
        }

        assertThat(violations).isEmpty();
    }

    @Test
    void testEveryAuditDataKeyIsUsable() {
        List<String> violations = new ArrayList<>();

        for (Method method : auditedMethods) {
            Audited audited = method.getAnnotation(Audited.class);

            Set<String> keys = new HashSet<>();

            for (AuditData auditData : audited.data()) {
                String key = auditData.key();

                if (key.isBlank()) {
                    violations.add(describe(method, auditData) + " has a blank key");
                } else if (AuditData.RESERVED_KEYS.contains(key)) {
                    violations.add(describe(method, auditData) + " uses the reserved key '" + key + "'");
                } else if (!keys.add(key)) {
                    violations.add(describe(method, auditData) + " declares the key '" + key + "' more than once");
                }
            }
        }

        assertThat(violations).isEmpty();
    }

    @Test
    void testEveryAuditedMethodDeclaresAtMostOnePlatform() {
        List<String> violations = new ArrayList<>();

        for (Method method : auditedMethods) {
            Audited audited = method.getAnnotation(Audited.class);

            PlatformType[] platforms = audited.platform();

            if (platforms.length > 1) {
                violations.add(
                    method.getDeclaringClass()
                        .getName() + "#" + method.getName() + " declares " + platforms.length + " platforms");
            }
        }

        assertThat(violations).isEmpty();
    }

    @Test
    void testEveryMcpServerAuditedMethodExposesTheMcpServer() {
        List<String> violations = new ArrayList<>();

        for (Method method : auditedMethods) {
            Audited audited = method.getAnnotation(Audited.class);

            if (audited.mapper() != McpServerAuditMapper.class || McpServer.class.equals(method.getReturnType())) {
                continue;
            }

            boolean exposesMcpServer = false;

            for (Parameter parameter : method.getParameters()) {
                String name = parameter.getName();
                Class<?> type = parameter.getType();

                if ((name.equals("mcpServerId") || name.equals("id")) &&
                    (type.equals(Long.class) || type.equals(long.class))) {

                    exposesMcpServer = true;
                } else if (name.equals("mcpServer") && McpServer.class.equals(type)) {
                    exposesMcpServer = true;
                }
            }

            if (!exposesMcpServer) {
                violations.add(
                    method.getDeclaringClass()
                        .getName() + "#" + method.getName() +
                        " neither returns an McpServer nor takes an mcpServerId, id or mcpServer argument");
            }
        }

        assertThat(violations).isEmpty();
    }

    @Test
    void testNoAuditDataExpressionReadsASecret() {
        List<String> violations = new ArrayList<>();

        for (Method method : auditedMethods) {
            Audited audited = method.getAnnotation(Audited.class);

            for (AuditData auditData : audited.data()) {
                SpelNode rootNode = parse(auditData.value());

                List<String> names = new ArrayList<>();

                collectNames(rootNode, names);

                for (String name : names) {
                    String lowerCaseName = name.toLowerCase(Locale.ENGLISH);

                    if (isSecretName(lowerCaseName)) {
                        violations.add(describe(method, auditData) + " reads the secret '" + name + "'");
                    }
                }

                Class<?> returnType = method.getReturnType();

                if (rootNode instanceof VariableReference && isVariable(rootNode, RESULT_VARIABLE) &&
                    !isScalar(returnType)) {

                    violations.add(
                        describe(method, auditData) + " records a whole " + returnType.getSimpleName() +
                            " result, which may hold a secret");
                }
            }
        }

        assertThat(violations).isEmpty();
    }

    private static boolean isScalar(Class<?> type) {
        return type.isPrimitive() || type.isEnum() || Number.class.isAssignableFrom(type) || type == Boolean.class;
    }

    private static boolean isSecretName(String lowerCaseName) {
        for (String nonSecretNameSuffix : NON_SECRET_NAME_SUFFIXES) {
            if (lowerCaseName.endsWith(nonSecretNameSuffix)) {
                return false;
            }
        }

        for (String secretNameFragment : SECRET_NAME_FRAGMENTS) {
            if (lowerCaseName.contains(secretNameFragment)) {
                return true;
            }
        }

        return false;
    }

    private static void checkExpression(Method method, AuditData auditData, List<String> violations) {
        Map<String, Class<?>> variableTypes = getVariableTypes(method);

        SpelNode rootNode = parse(auditData.value());

        List<String> variableNames = new ArrayList<>();

        collectVariableNames(rootNode, variableNames);

        for (String variableName : variableNames) {
            if (!variableTypes.containsKey(variableName)) {
                violations.add(describe(method, auditData) + " names the undeclared variable #" + variableName);
            }
        }

        if (!(rootNode instanceof CompoundExpression) || rootNode.getChildCount() < 2) {
            return;
        }

        SpelNode variableNode = rootNode.getChild(0);
        SpelNode propertyNode = rootNode.getChild(1);

        if (!(variableNode instanceof VariableReference) ||
            !(propertyNode instanceof PropertyOrFieldReference propertyOrFieldReference)) {

            return;
        }

        String variableReference = variableNode.toStringAST();

        Class<?> variableType = variableTypes.get(variableReference.substring(1));

        if (variableType == null || variableType == Object.class || Map.class.isAssignableFrom(variableType)) {
            return;
        }

        String propertyName = propertyOrFieldReference.getName();

        if (!hasReadableProperty(variableType, propertyName)) {
            violations.add(
                describe(method, auditData) + " reads '" + propertyName + "', which " + variableType.getName() +
                    " does not expose");
        }
    }

    private static void collectNames(SpelNode spelNode, List<String> names) {
        if (spelNode instanceof VariableReference) {
            String variableReference = spelNode.toStringAST();

            names.add(variableReference.substring(1));
        }

        if (spelNode instanceof PropertyOrFieldReference propertyOrFieldReference) {
            names.add(propertyOrFieldReference.getName());
        }

        for (int index = 0; index < spelNode.getChildCount(); index++) {
            collectNames(spelNode.getChild(index), names);
        }
    }

    private static void collectVariableNames(SpelNode spelNode, List<String> variableNames) {
        if (spelNode instanceof VariableReference) {
            String variableReference = spelNode.toStringAST();

            variableNames.add(variableReference.substring(1));
        }

        for (int index = 0; index < spelNode.getChildCount(); index++) {
            collectVariableNames(spelNode.getChild(index), variableNames);
        }
    }

    private static String describe(Method method, AuditData auditData) {
        Class<?> declaringClass = method.getDeclaringClass();

        return declaringClass.getName() + "." + method.getName() + " data '" + auditData.key() + "' = '" +
            auditData.value() + "'";
    }

    private static List<Method> findAuditedMethods() throws IOException, ClassNotFoundException {
        PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver =
            new PathMatchingResourcePatternResolver();
        SimpleMetadataReaderFactory simpleMetadataReaderFactory = new SimpleMetadataReaderFactory();

        ClassLoader classLoader = AuditDataContractTest.class.getClassLoader();

        List<Method> methods = new ArrayList<>();

        for (Resource resource : pathMatchingResourcePatternResolver.getResources(CLASS_PATTERN)) {
            URL url = resource.getURL();

            String externalForm = url.toExternalForm();

            if (externalForm.contains(COMPONENT_PACKAGE_PATH)) {
                continue;
            }

            MetadataReader metadataReader = simpleMetadataReaderFactory.getMetadataReader(resource);

            AnnotationMetadata annotationMetadata = metadataReader.getAnnotationMetadata();

            if (!annotationMetadata.hasAnnotatedMethods(Audited.class.getName())) {
                continue;
            }

            Class<?> auditedClass = Class.forName(annotationMetadata.getClassName(), false, classLoader);

            for (Method method : auditedClass.getDeclaredMethods()) {
                if (!method.isSynthetic() && method.isAnnotationPresent(Audited.class)) {
                    methods.add(method);
                }
            }
        }

        return methods;
    }

    private static Map<String, Class<?>> getVariableTypes(Method method) {
        Map<String, Class<?>> variableTypes = new HashMap<>();

        for (Parameter parameter : method.getParameters()) {
            variableTypes.put(parameter.getName(), parameter.getType());
        }

        Class<?> returnType = method.getReturnType();

        if (returnType != void.class) {
            variableTypes.put(RESULT_VARIABLE, returnType);
        }

        return variableTypes;
    }

    private static boolean hasReadableProperty(Class<?> type, String propertyName) {
        String capitalizedName = StringUtils.capitalize(propertyName);

        Set<String> accessorNames = Set.of("get" + capitalizedName, "is" + capitalizedName, propertyName);

        for (Method method : type.getMethods()) {
            if (method.getParameterCount() == 0 && accessorNames.contains(method.getName())) {
                return true;
            }
        }

        Field field;

        try {
            field = type.getField(propertyName);
        } catch (NoSuchFieldException noSuchFieldException) {
            return false;
        }

        return Modifier.isPublic(field.getModifiers());
    }

    private static boolean isVariable(SpelNode spelNode, String variableName) {
        String variableReference = spelNode.toStringAST();

        return variableReference.equals("#" + variableName);
    }

    private static SpelNode parse(String expressionString) {
        SpelExpression spelExpression = SPEL_EXPRESSION_PARSER.parseRaw(expressionString.strip());

        return spelExpression.getAST();
    }
}
