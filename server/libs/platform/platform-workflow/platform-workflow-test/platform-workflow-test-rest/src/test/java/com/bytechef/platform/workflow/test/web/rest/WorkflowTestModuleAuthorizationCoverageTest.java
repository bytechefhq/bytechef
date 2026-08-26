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

package com.bytechef.platform.workflow.test.web.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Default-deny enumeration over every request-mapped method declared by the controllers in
 * {@code platform-workflow-test-rest}. This test exists because ticket 1051's original fix closed
 * {@link WorkflowTestApiController}'s three operations but missed that {@link AiAgentTestApiController} — same module,
 * same {@code /internal} base path — was just as ungated: {@code testAiAgent} executed any workflow's AI agent node,
 * with that workflow's stored credentials and tool access, for any authenticated principal in the tenant. A per-method
 * reflection-pin test (as in {@code WorkflowTestApiControllerAuthorizationTest}) only proves the methods it was told to
 * look at are gated; it says nothing about a method nobody wrote a test for.
 *
 * <p>
 * A first version of this test reproduced exactly that failure mode one level up: it enumerated methods over a
 * hardcoded two-class list, so a third controller landing in this package would be invisible to every assertion here
 * and would ship ungated without failing anything. {@link #discoverControllers()} replaces that list with a classpath
 * scan for {@code @Controller} (which {@code @RestController} is meta-annotated with, so both controllers below are
 * matched by it) over {@link #CONTROLLER_BASE_PACKAGE} — so the per-method assertions automatically extend to any new
 * controller in this package. {@link #testDiscoveredControllersMatchTheExpectedSet()} additionally pins the discovered
 * set against {@link #EXPECTED_CONTROLLERS} so a new controller is a loud, deliberate addition (update
 * {@code EXPECTED_CONTROLLERS} and write its own authorization test) rather than something that silently widens
 * coverage unnoticed.
 *
 * @author Ivica Cardic
 */
class WorkflowTestModuleAuthorizationCoverageTest {

    private static final String CONTROLLER_BASE_PACKAGE = "com.bytechef.platform.workflow.test.web.rest";

    private static final Set<Class<?>> EXPECTED_CONTROLLERS = Set.of(WorkflowTestApiController.class,
        AiAgentTestApiController.class);

    /**
     * The single named exemption. {@code AiAgentTestApiController#stopAiAgentTest} takes a server-minted
     * {@code UUID.randomUUID()} {@code testId} held only in an in-memory, per-instance cache with no owning
     * workflow/workspace recorded anywhere resolvable — there is nothing to key a {@code hasPermission(...)} check on
     * without adding a side table purely to authorize this one stop call. See the Javadoc on the method itself for the
     * full unguessability rationale. Any new entry here must carry the same kind of justification, not just a bare
     * method name.
     */
    private static final Set<String> UNGATED_OPT_OUT = Set.of("AiAgentTestApiController#stopAiAgentTest");

    /**
     * Pins the classpath scan against the known set by name rather than by reference, so a THIRD controller landing in
     * {@link #CONTROLLER_BASE_PACKAGE} fails here loudly instead of being silently absorbed into
     * {@link #requestMappedMethods()}'s coverage. This is what closes the gap the original, hardcoded-list version of
     * this test had: per-method coverage alone would still pass for a new controller whose methods all happen to carry
     * {@code @PreAuthorize} (or opt out), never telling anyone a new controller showed up in this module at all.
     */
    @Test
    void testDiscoveredControllersMatchTheExpectedSet() {
        assertThat(discoverControllers().stream()
            .map(Class::getName)
            .sorted()
            .toList())
                .as("controllers discovered by scanning %s for @Controller (an empty or mismatched result here "
                    + "means either the scan is broken, or a controller was added/removed and EXPECTED_CONTROLLERS "
                    + "was not updated to match)", CONTROLLER_BASE_PACKAGE)
                .containsExactlyElementsOf(EXPECTED_CONTROLLERS.stream()
                    .map(Class::getName)
                    .sorted()
                    .toList());
    }

    @Test
    void testDiscoveryFindsExactlyTheKnownRequestMappedMethods() {
        assertThat(requestMappedMethods().stream()
            .map(WorkflowTestModuleAuthorizationCoverageTest::qualifiedName)
            .sorted()
            .toList())
                .as("request-mapped methods across WorkflowTestApiController and AiAgentTestApiController (empty "
                    + "here means the discovery filter is broken, not that all is well)")
                .containsExactly(
                    "AiAgentTestApiController#stopAiAgentTest", "AiAgentTestApiController#testAiAgent",
                    "WorkflowTestApiController#attachWorkflowTest", "WorkflowTestApiController#startWorkflowTest",
                    "WorkflowTestApiController#stopWorkflowTest");
    }

    @Test
    void testEveryRequestMappedMethodIsGatedOrExplicitlyOptedOut() {
        List<String> ungated = requestMappedMethods().stream()
            .filter(entry -> !UNGATED_OPT_OUT.contains(qualifiedName(entry)))
            .filter(entry -> !entry.getValue()
                .isAnnotationPresent(PreAuthorize.class))
            .map(WorkflowTestModuleAuthorizationCoverageTest::qualifiedName)
            .sorted()
            .toList();

        assertThat(ungated)
            .as("request-mapped methods without @PreAuthorize and without a named, justified entry in "
                + "UNGATED_OPT_OUT")
            .isEmpty();
    }

    /**
     * {@code issueWorkflowTestVoiceSessionToken} is declared on the generated {@code WorkflowTestApi} interface with a
     * default implementation that unconditionally returns {@code 501 NOT_IMPLEMENTED} and runs no business logic;
     * {@link WorkflowTestApiController} does not override it anywhere in CE or EE, so it never appears in
     * {@link Class#getDeclaredMethods()} and this test's enumeration above cannot see it. This test is the trap for
     * that gap: it fails the moment someone DOES implement the override, forcing them to either add
     * {@code @PreAuthorize} and a dedicated authorization test, or consciously update this test — rather than silently
     * shipping a fourth ungated operation the way {@code AiAgentTestApiController} shipped a second one.
     */
    @Test
    void testIssueWorkflowTestVoiceSessionTokenStaysUnimplemented() {
        boolean overridden = Arrays.stream(WorkflowTestApiController.class.getDeclaredMethods())
            .anyMatch(method -> method.getName()
                .equals("issueWorkflowTestVoiceSessionToken"));

        assertThat(overridden)
            .as("issueWorkflowTestVoiceSessionToken must stay unimplemented (inherited 501 stub) until it is "
                + "authorized -- update this test and add a gate + authorization test together when it is built")
            .isFalse();
    }

    private static List<SimpleEntry<Class<?>, Method>> requestMappedMethods() {
        return discoverControllers().stream()
            .flatMap(controllerClass -> Arrays.stream(controllerClass.getDeclaredMethods())
                .filter(method -> AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class))
                .map(method -> new SimpleEntry<Class<?>, Method>(controllerClass, method)))
            .toList();
    }

    /**
     * Discovers every {@code @Controller} (including the meta-annotated {@code @RestController}) in
     * {@link #CONTROLLER_BASE_PACKAGE} from the classpath, instead of trusting a maintained list -- so a new controller
     * in this package is automatically pulled into {@link #requestMappedMethods()}'s coverage rather than requiring
     * someone to remember to add it here.
     */
    private static Set<Class<?>> discoverControllers() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);

        scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));

        return scanner.findCandidateComponents(CONTROLLER_BASE_PACKAGE)
            .stream()
            .map(WorkflowTestModuleAuthorizationCoverageTest::loadClass)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Class<?> loadClass(BeanDefinition beanDefinition) {
        String className = beanDefinition.getBeanClassName();

        try {
            return Class.forName(className);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Failed to load scanned candidate class " + className, exception);
        }
    }

    private static String qualifiedName(SimpleEntry<Class<?>, Method> entry) {
        Class<?> controllerClass = entry.getKey();
        Method method = entry.getValue();

        return controllerClass.getSimpleName() + "#" + method.getName();
    }
}
