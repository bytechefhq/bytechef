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

package com.bytechef.platform.security.web.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedMethod;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Discovers every REST handler whose request mapping binds an {@code {externalUserId}} path variable and checks that it
 * acts only for the calling connected user.
 *
 * @author Ivica Cardic
 */
public final class ConnectedUserPathBindings {

    public static final String OTHER_EXTERNAL_USER_ID = "bob";
    public static final String OWN_EXTERNAL_USER_ID = "alice";

    private static final String EXTERNAL_USER_ID = "externalUserId";
    private static final String EXTERNAL_USER_ID_PATH_VARIABLE = "{" + EXTERNAL_USER_ID + "}";

    private ConnectedUserPathBindings() {
    }

    public static List<ExternalUserIdEndpoint> findExternalUserIdEndpoints(String basePackage) {
        List<ExternalUserIdEndpoint> externalUserIdEndpoints = new ArrayList<>();

        for (Class<?> controllerClass : findControllerClasses(basePackage)) {
            for (Method method : controllerClass.getDeclaredMethods()) {
                if (!Modifier.isPublic(method.getModifiers()) || method.isSynthetic() || method.isBridge() ||
                    !isMappedToExternalUserIdPath(method)) {

                    continue;
                }

                externalUserIdEndpoints.add(
                    new ExternalUserIdEndpoint(controllerClass, method, getExternalUserIdParameterIndex(method)));
            }
        }

        externalUserIdEndpoints.sort(Comparator.comparing(ExternalUserIdEndpoint::toString));

        return externalUserIdEndpoints;
    }

    public static void assertRefusesAnotherConnectedUser(ExternalUserIdEndpoint externalUserIdEndpoint)
        throws ReflectiveOperationException {

        assertRefused(externalUserIdEndpoint, TestConnectedUserAuthentication.of(OWN_EXTERNAL_USER_ID));
    }

    public static void assertRefusesAPlatformSession(ExternalUserIdEndpoint externalUserIdEndpoint)
        throws ReflectiveOperationException {

        assertRefused(
            externalUserIdEndpoint,
            new UsernamePasswordAuthenticationToken(OTHER_EXTERNAL_USER_ID, "", List.of()));
    }

    public static void assertLetsTheConnectedUserActOnTheirOwnPath(ExternalUserIdEndpoint externalUserIdEndpoint)
        throws ReflectiveOperationException {

        SecurityContextHolder.getContext()
            .setAuthentication(TestConnectedUserAuthentication.of(OWN_EXTERNAL_USER_ID));

        try {
            Throwable thrown = invoke(externalUserIdEndpoint, new ArrayList<>(), OWN_EXTERNAL_USER_ID);

            assertThat(thrown instanceof AccessDeniedException).isFalse();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private static void assertRefused(ExternalUserIdEndpoint externalUserIdEndpoint, Authentication authentication)
        throws ReflectiveOperationException {

        SecurityContextHolder.getContext()
            .setAuthentication(authentication);

        try {
            List<Object> collaborators = new ArrayList<>();

            Throwable thrown = invoke(externalUserIdEndpoint, collaborators, OTHER_EXTERNAL_USER_ID);

            assertThat(thrown).isInstanceOf(AccessDeniedException.class);
            assertThat(collaborators).allMatch(collaborator -> mockingDetails(collaborator).getInvocations()
                .isEmpty());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private static List<Class<?>> findControllerClasses(String basePackage) {
        PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver =
            new PathMatchingResourcePatternResolver();
        MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(
            pathMatchingResourcePatternResolver);

        List<Class<?>> controllerClasses = new ArrayList<>();

        try {
            Resource[] resources = pathMatchingResourcePatternResolver.getResources(
                ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
                    + ClassUtils.convertClassNameToResourcePath(basePackage) +
                    "/**/*.class");

            for (Resource resource : resources) {
                MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);

                AnnotationMetadata annotationMetadata = metadataReader.getAnnotationMetadata();

                if (annotationMetadata.isAnnotated(Controller.class.getName())) {
                    controllerClasses.add(
                        ClassUtils.resolveClassName(
                            annotationMetadata.getClassName(), ConnectedUserPathBindings.class.getClassLoader()));
                }
            }
        } catch (IOException ioException) {
            throw new UncheckedIOException(ioException);
        }

        return controllerClasses;
    }

    private static int getExternalUserIdParameterIndex(Method method) {
        AnnotatedMethod annotatedMethod = new AnnotatedMethod(method);

        for (MethodParameter methodParameter : annotatedMethod.getMethodParameters()) {
            PathVariable pathVariable = methodParameter.getParameterAnnotation(PathVariable.class);

            if (pathVariable == null) {
                continue;
            }

            String name = pathVariable.value();

            if (name.isEmpty()) {
                methodParameter.initParameterNameDiscovery(new DefaultParameterNameDiscoverer());

                name = methodParameter.getParameterName();
            }

            if (EXTERNAL_USER_ID.equals(name)) {
                return methodParameter.getParameterIndex();
            }
        }

        throw new IllegalStateException(method + " maps {externalUserId} but binds no externalUserId path variable");
    }

    private static Object instantiate(Class<?> controllerClass, List<Object> collaborators)
        throws ReflectiveOperationException {

        Constructor<?> constructor = controllerClass.getDeclaredConstructors()[0];

        constructor.setAccessible(true);

        Class<?>[] parameterTypes = constructor.getParameterTypes();
        Object[] arguments = new Object[parameterTypes.length];

        for (int index = 0; index < arguments.length; index++) {
            Object collaborator = mock(parameterTypes[index]);

            collaborators.add(collaborator);

            arguments[index] = collaborator;
        }

        return constructor.newInstance(arguments);
    }

    private static Throwable invoke(
        ExternalUserIdEndpoint externalUserIdEndpoint, List<Object> collaborators, String externalUserId)
        throws ReflectiveOperationException {

        Object controller = instantiate(externalUserIdEndpoint.controllerClass(), collaborators);
        Method method = externalUserIdEndpoint.method();

        method.setAccessible(true);

        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] arguments = new Object[parameterTypes.length];

        for (int index = 0; index < parameterTypes.length; index++) {
            arguments[index] = index == externalUserIdEndpoint.externalUserIdParameterIndex()
                ? externalUserId : defaultValue(parameterTypes[index]);
        }

        try {
            method.invoke(controller, arguments);

            return null;
        } catch (InvocationTargetException invocationTargetException) {
            return invocationTargetException.getCause();
        }
    }

    private static boolean isMappedToExternalUserIdPath(Method method) {
        MergedAnnotation<RequestMapping> requestMapping = MergedAnnotations.from(method, SearchStrategy.TYPE_HIERARCHY)
            .get(RequestMapping.class);

        return requestMapping.isPresent() && Arrays.stream(requestMapping.getStringArray("path"))
            .anyMatch(path -> path.contains(EXTERNAL_USER_ID_PATH_VARIABLE));
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class || type == Boolean.class) {
            return Boolean.TRUE;
        }

        if (type == long.class || type == Long.class) {
            return 1L;
        }

        if (type == int.class || type == Integer.class) {
            return 1;
        }

        if (type == String.class) {
            return "value";
        }

        if (type == List.class) {
            return List.of();
        }

        if (type.isInterface() || type.isEnum() || Modifier.isFinal(type.getModifiers())) {
            return null;
        }

        return mock(type);
    }

    @SuppressFBWarnings({
        "EI", "EI2"
    })
    public record ExternalUserIdEndpoint(Class<?> controllerClass, Method method, int externalUserIdParameterIndex) {

        @Override
        public String toString() {
            return controllerClass.getSimpleName() + "." + method.getName();
        }
    }
}
