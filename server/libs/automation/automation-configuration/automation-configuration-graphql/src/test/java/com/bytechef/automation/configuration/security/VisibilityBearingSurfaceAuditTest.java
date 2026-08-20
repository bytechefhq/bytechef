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

package com.bytechef.automation.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * The endpoint-keyed counterpart to the method-keyed {@code PermissionService} entry-point audit in §17 of
 * {@code docs/superpowers/specs/2026-08-17-project-visibility-design.md}.
 *
 * <p>
 * That table proves every {@code PermissionService} method either runs the visibility precondition or has a stated
 * reason not to. It cannot, by construction, see a surface that never calls {@code PermissionService} at all — and that
 * is exactly how {@code ProjectWorkflowFacadeImpl.getProjectWorkflows()} and {@code getProjectWorkflow(long)} stayed
 * unguarded until the last review of the branch that introduced {@code PRIVATE}. This test inverts the axis: it starts
 * from the surfaces and asks whether each one is locked, rather than starting from the lock and asking whether it is
 * well made.
 *
 * <h2>Scope, and what it deliberately does not cover</h2>
 *
 * <p>
 * Audited: every public method of the CE automation-configuration <strong>facade implementations</strong>
 * ({@code com.bytechef.automation.configuration.facade}) and <strong>GraphQL controllers</strong>
 * ({@code com.bytechef.automation.configuration.web.graphql}) whose signature mentions a visibility-bearing type. Those
 * are the two layers where the project family's management surfaces live and where the domain types appear in
 * signatures, so a reflective rule can see them.
 *
 * <p>
 * Not audited, each for a reason rather than for convenience:
 *
 * <ul>
 * <li><strong>The REST controllers</strong> ({@code automation-configuration-rest-impl}) return generated
 * {@code *Model} types, so no type-keyed rule can see them; the repo convention is that the facade they delegate to
 * owns authorization. A controller that reaches past its facade into a service is therefore invisible here — and one
 * did: {@code ProjectApiController.getProjectVersions(Long)} called {@code ProjectService.getProjectVersions} with no
 * gate at all, an open by-id read of any project's version history. It now reads through
 * {@code ProjectFacade.getProjectVersions}, which carries the same {@code WORKFLOW_VIEW} gate as {@code getProject};
 * {@code ProjectApiControllerAuthorizationTest} pins the delegation, since this test still cannot express it.</li>
 * <li><strong>Surfaces whose signature carries only ids</strong> — {@code deleteWorkflow(String workflowId)} names no
 * visibility-bearing type — and surfaces that return a purpose-built projection record rather than a domain type or a
 * shared DTO. Both are invisible to a type-keyed rule.</li>
 * <li><strong>EE modules</strong> (the project sharing facade), the <strong>executions</strong> facade
 * ({@code ProjectWorkflowExecutionFacadeImpl}, the {@code Job} surface — three public methods, all
 * {@code @PreAuthorize}d, pinned by its own {@code ProjectWorkflowExecutionFacadeVisibilityTest}), <strong>agent/MCP
 * tools</strong> and the <strong>runtime</strong> trigger controllers. The last of these are anonymous by design: a
 * {@code PRIVATE} project's deployments must keep serving webhook traffic.</li>
 * </ul>
 *
 * <h2>How a surface satisfies the audit</h2>
 *
 * <p>
 * By carrying {@code @PreAuthorize} (on the method or its class); by being a GraphQL field resolver
 * ({@code @SchemaMapping} / {@code @BatchMapping}); by appearing in {@link #FACADE_GUARDED_SURFACES}, which names the
 * guarded facade method the surface delegates to; or by appearing in {@link #EXEMPTIONS} with a reason. A reason must
 * say what protects the surface — "runtime path, anonymous by design" qualifies, "not needed" does not. An exemption
 * added to silence a genuine finding turns this test into the opposite of what it is for.
 *
 * <p>
 * The second of those is the weakest, and it is worth being exact about what it does and does not claim. A field
 * resolver is protected by whatever the root field that produced its parent enforced, and by nothing else — it is a
 * <em>conditional</em> exemption, not a property of the resolver. The condition it depends on is the rest of this test:
 * {@link #testEveryVisibilityBearingSurfaceIsGuardedFilteredOrExempt()} over the root fields of the audited
 * controllers, resting in turn on the type partitions being total, which is what
 * {@link #testEveryDomainTypeIsClassified()} and {@link #testEveryDtoIsClassified()} enforce. When that condition
 * failed, so did the exemption: {@code ProjectDeploymentWorkflowGraphQlController.projectDeploymentWorkflow(String)}
 * was an ungated root query, so {@code ProjectWorkflowGraphQlController.projectWorkflow(ProjectDeploymentWorkflow)},
 * {@code workflow(ProjectWorkflow)} and {@code staticWebhookUrl(...)} were reached down a chain in which nothing had
 * authorized anything, and the whole workflow definition of any deployment in the tenant came back. It went unseen
 * because the domain type was in neither partition and the partitions covered only the DTO package.
 *
 * <p>
 * One part of the condition this test still cannot check: a root field <em>outside</em> the two audited packages that
 * returns one of these parent types would feed the same resolvers with the same absence of a gate. Nothing here would
 * notice. The GraphQL schema files are the place to look when adding one.
 *
 * <p>
 * The third of those is the repo convention for a controller and deserves its own mechanism rather than a line of
 * prose: this codebase puts authorization on the API facade, so a controller that does nothing but delegate is guarded
 * — but only for as long as the facade method it names still carries the annotation. An entry in
 * {@link #FACADE_GUARDED_SURFACES} is therefore <em>checked</em>, not asserted: the named method must exist among the
 * audited facade implementations and must carry {@code @PreAuthorize}. Moving a guard off a facade fails this test
 * instead of quietly emptying it, which is the one thing a prose exemption cannot do. What it cannot check is that the
 * controller still <em>calls</em> the method it names — reflection sees signatures, not bodies. That half is pinned per
 * controller by the {@code *GraphQlControllerAuthorizationTest} classes, which assert the facade was called and that
 * none of the services the controller holds were touched; without them, a controller could go back to assembling its
 * answer out of services while its entry here stayed green.
 *
 * <p>
 * The two mechanisms are not interchangeable, and choosing between them is the point. {@link #FACADE_GUARDED_SURFACES}
 * is for a surface whose protection really is an annotation somewhere else; {@link #EXEMPTIONS} is for one whose
 * protection is something this test cannot check, said plainly. A tenant-wide listing has no id to gate on, so the only
 * expression an annotation could carry is {@code isAuthenticated()} — naming such a method in the guarded map would
 * make this table assert a property that annotation does not provide. An audit that overclaims is worse than one that
 * admits a gap, because the overclaim is what stops the next reader looking.
 *
 * @author Ivica Cardic
 */
class VisibilityBearingSurfaceAuditTest {

    private static final String FACADE_PACKAGE = "com.bytechef.automation.configuration.facade";
    private static final String GRAPH_QL_PACKAGE = "com.bytechef.automation.configuration.web.graphql";
    private static final String DOMAIN_PACKAGE = "com.bytechef.automation.configuration.domain";
    private static final String DTO_PACKAGE = "com.bytechef.automation.configuration.dto";

    /**
     * The resource types the project visibility model covers, plus the shared DTOs that project them. A DTO counts
     * because it carries the same content: {@code ProjectDTO} is a flattened projection rather than a wrapper, so
     * closing over record components would not find it.
     */
    private static final Set<String> VISIBILITY_BEARING_TYPE_NAMES = Set.of(
        "com.bytechef.atlas.configuration.domain.Workflow",
        "com.bytechef.atlas.execution.domain.Job",
        "com.bytechef.automation.configuration.domain.Project",
        "com.bytechef.automation.configuration.domain.ProjectDeployment",
        "com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow",
        "com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection",
        "com.bytechef.automation.configuration.domain.ProjectVersion",
        "com.bytechef.automation.configuration.domain.ProjectWorkflow",
        "com.bytechef.automation.configuration.dto.ProjectDTO",
        "com.bytechef.automation.configuration.dto.ProjectDeploymentDTO",
        "com.bytechef.automation.configuration.dto.ProjectDeploymentWorkflowDTO",
        "com.bytechef.automation.configuration.dto.ProjectWorkflowDTO",
        "com.bytechef.automation.configuration.dto.WorkspaceProjectWorkflowDTO",
        "com.bytechef.platform.configuration.dto.WorkflowDTO");

    /**
     * Every other type in the shared DTO package, with why it carries nothing a hidden project would leak. The audit
     * asserts this partition is total, so a DTO added to that package fails the test until it is classified.
     */
    private static final Map<String, String> NON_VISIBILITY_BEARING_DTOS = Map.of(
        "ProjectTemplateDTO",
        "Template catalogue entry — an exported, publishable artefact identified by a template id, not a project row.",
        "WorkflowTemplateDTO",
        "Template catalogue entry, as above.",
        "SharedProjectDTO",
        "Share-link metadata (description, exported flag, version, public url) keyed by a share uuid the owner minted "
            +
            "by exporting; carries no project row.",
        "SharedWorkflowDTO",
        "Share-link metadata, as above.");

    /**
     * The domain counterpart of {@link #NON_VISIBILITY_BEARING_DTOS}, and the entry that closes the hole this audit
     * shipped with. Only the DTO package was partitioned, so a <em>domain</em> type could be absent from both lists and
     * leave the audit green — which is what happened to {@code ProjectDeploymentWorkflow}: it was in neither list, so
     * {@code ProjectDeploymentWorkflowGraphQlController.projectDeploymentWorkflow(String)}, a root query returning a
     * deployment's inputs, its connection bindings and (through {@code projectWorkflow.workflow}) the whole workflow
     * definition of any deployment in the tenant, was invisible here and passed unannotated for the whole life of the
     * audit.
     */
    private static final Map<String, String> NON_VISIBILITY_BEARING_DOMAIN_TYPES = Map.of(
        "ErrorWorkflowDispatch",
        "A runtime dispatch envelope the coordinator builds when a run fails, on no read surface at all; the runtime " +
            "path it belongs to is anonymous by design, since a PRIVATE project's deployments must keep running.",
        "ProjectDeploymentTag",
        "Relation row carrying a tag id and nothing else.",
        "ProjectTag",
        "Relation row carrying a tag id and nothing else.",
        "SharedTemplate",
        "The stored side of a share link: a uuid and the exported file entry the owner minted by exporting. The same " +
            "family as SharedProjectDTO, and keyed by the share uuid rather than by a project.",
        "SystemProjects",
        "Not an entity — the naming convention and predicates that let list surfaces hide auto-provisioned projects.",
        "Workspace",
        "The container the project family is scoped BY, not project content: name, description, owner. Reads of it " +
            "are gated on workspace membership, which is the other half of every project check rather than a thing " +
            "project visibility hides.",
        "WorkspaceApiKey",
        "Workspace-to-api-key relation row; no project row and no workflow content.",
        "WorkspaceConnection",
        "Workspace-to-connection relation row; connection visibility is its own model, checked under 'Connection'.");

    /**
     * Surfaces that mention a visibility-bearing type and carry no annotation, keyed by signature, with what protects
     * them instead.
     */
    private static final Map<String, String> EXEMPTIONS = Map.of(
        "ProjectWorkflowFacadeImpl#getProjectWorkflows()",
        "List surface with no id to annotate on: filtered through ProjectVisibilityFilter.visibleProjectIds in one " +
            "batched call. Pinned by ProjectWorkflowFacadeVisibilityTest.",
        "ProjectDeploymentFacadeImpl#createProjectDeployment(ProjectDeployment,String,List)",
        "On no automation HTTP or GraphQL surface — the overload those reach, createProjectDeployment(" +
            "ProjectDeploymentDTO), carries hasPermission(#projectDeploymentDTO, 'WORKFLOW_EDIT'). Its only callers " +
            "are the EMBEDDED provisioning facades (ConnectedUserProjectFacadeImpl, " +
            "ConnectedUserCodeWorkflowReferenceFacadeImpl), which carry no @PreAuthorize of their own: embedded " +
            "authorizes by api key and connected user at its own REST boundary, not by workspace RBAC, and that " +
            "model is outside this audit's scope. Stated as the gap it is rather than as a guard that exists.",
        "ProjectDeploymentFacadeImpl#updateProjectDeployment(long,int,String,List,Long)",
        "As above, and the same single embedded caller: ConnectedUserProjectFacadeImpl.publishProjectWorkflow " +
            "reaches this one and createProjectDeployment(ProjectDeployment,String,List) from the two arms of one " +
            "branch. On no automation HTTP or GraphQL surface.",
        "ProjectDeploymentFacadeImpl#createProjectDeployment(ProjectDeployment,List,List)",
        "Internal delegation target with no caller outside this class — the method the annotated DTO overload " +
            "delegates into, which is where the guard is.",
        "ProjectDeploymentFacadeImpl#updateProjectDeployment(ProjectDeployment,List,List)",
        "Internal delegation target with no caller outside this class, as above.",
        "ProjectFacadeImpl#getProjectRows()",
        "List surface with no id to annotate on: narrowed to the workspaces the caller holds WORKFLOW_VIEW in and " +
            "then filtered through ProjectVisibilityFilter.visibleProjectIds in one batched call — the two halves " +
            "hasResourceScope composes for a project. Pinned by ProjectFacadeRowVisibilityTest.",
        "ProjectGraphQlController#projects()",
        "Delegates to the exempt list surface above and adds nothing of its own; the narrowing, and the reason it is " +
            "a filter rather than a gate, live there.");

    /**
     * Controller surfaces that carry no annotation because they delegate to a guarded facade method, keyed by
     * controller signature and valued with that method's signature on the facade implementation. Every value is
     * resolved and checked by {@link #testEveryFacadeGuardedSurfaceNamesAGuardedFacadeMethod()}, so this is a pointer
     * to a guard rather than a claim that one exists somewhere.
     */
    private static final Map<String, String> FACADE_GUARDED_SURFACES = Map.of(
        "ProjectDeploymentGraphQlController#workspaceProjectDeployments(long,long,Long,Long)",
        "ProjectDeploymentFacadeImpl#getWorkspaceProjectDeployments(long,long,Long,Long)",
        "ProjectDeploymentWorkflowGraphQlController#projectDeploymentWorkflow(String)",
        "ProjectDeploymentFacadeImpl#getProjectDeploymentWorkflow(WorkflowExecutionId)",
        "ProjectGraphQlController#project(long)",
        "ProjectFacadeImpl#getProjectRow(long)",
        "ProjectWorkflowGraphQlController#workspaceProjectWorkflows(long)",
        "ProjectFacadeImpl#getWorkspaceLatestProjectWorkflows(long)");

    private static final Set<String> GUARD_ANNOTATIONS = Set.of(
        "org.springframework.security.access.prepost.PreAuthorize");

    private static final Set<String> FIELD_RESOLVER_ANNOTATIONS = Set.of(
        "org.springframework.graphql.data.method.annotation.SchemaMapping",
        "org.springframework.graphql.data.method.annotation.BatchMapping");

    private static final Set<String> IGNORED_METHOD_NAMES = Set.of("equals", "hashCode", "toString");

    @Test
    void testEveryVisibilityBearingSurfaceIsGuardedFilteredOrExempt() {
        Map<String, String> unguardedSurfaces = new LinkedHashMap<>();

        for (Class<?> auditedClass : getAuditedClasses()) {
            for (Method method : getPublicMethods(auditedClass)) {
                if (!mentionsVisibilityBearingType(method)) {
                    continue;
                }

                if (isAnnotated(auditedClass.getAnnotations(), GUARD_ANNOTATIONS) ||
                    isAnnotated(method.getAnnotations(), GUARD_ANNOTATIONS) ||
                    isAnnotated(method.getAnnotations(), FIELD_RESOLVER_ANNOTATIONS)) {

                    continue;
                }

                String signature = toSignature(method);

                if (!EXEMPTIONS.containsKey(signature) && !FACADE_GUARDED_SURFACES.containsKey(signature)) {
                    unguardedSurfaces.put(signature, auditedClass.getName());
                }
            }
        }

        assertThat(unguardedSurfaces)
            .describedAs(
                "These surfaces return or accept a visibility-bearing type and carry no @PreAuthorize, no field-" +
                    "resolver annotation, no guarded facade method and no exemption. Guard or filter them; name the " +
                    "facade method when a controller delegates to one; add an exemption only when something else " +
                    "genuinely protects them, and say what.")
            .isEmpty();
    }

    /**
     * The entry that makes {@link #FACADE_GUARDED_SURFACES} worth more than an exemption. Both halves are resolved
     * against the audited classes: the controller signature must still exist, and the facade method it names must still
     * carry the guard the controller is relying on.
     */
    @Test
    void testEveryFacadeGuardedSurfaceNamesAGuardedFacadeMethod() {
        Map<String, Method> auditedMethods = new LinkedHashMap<>();

        for (Class<?> auditedClass : getAuditedClasses()) {
            for (Method method : getPublicMethods(auditedClass)) {
                auditedMethods.put(toSignature(method), method);
            }
        }

        assertThat(FACADE_GUARDED_SURFACES).allSatisfy((controllerSignature, facadeSignature) -> {
            assertThat(auditedMethods)
                .describedAs("controller surface %s no longer exists", controllerSignature)
                .containsKey(controllerSignature);

            Method facadeMethod = auditedMethods.get(facadeSignature);

            assertThat(facadeMethod)
                .describedAs(
                    "%s says its guard lives on %s, which is not an audited facade method",
                    controllerSignature, facadeSignature)
                .isNotNull();
            assertThat(isAnnotated(facadeMethod.getAnnotations(), GUARD_ANNOTATIONS))
                .describedAs(
                    "%s carries no guard of its own because %s does — and that one has lost its @PreAuthorize",
                    controllerSignature, facadeSignature)
                .isTrue();
        });
    }

    @Test
    void testEveryExemptionStillMatchesASurface() {
        Set<String> auditedSignatures = new LinkedHashSet<>();

        for (Class<?> auditedClass : getAuditedClasses()) {
            for (Method method : getPublicMethods(auditedClass)) {
                if (mentionsVisibilityBearingType(method)) {
                    auditedSignatures.add(toSignature(method));
                }
            }
        }

        assertThat(EXEMPTIONS.keySet())
            .describedAs("An exemption naming a method that no longer exists hides whatever replaced it")
            .allSatisfy(signature -> assertThat(auditedSignatures).contains(signature));
    }

    @Test
    void testEveryDtoIsClassified() {
        assertThat(getUnclassifiedTypeNames(DTO_PACKAGE, NON_VISIBILITY_BEARING_DTOS))
            .describedAs(
                "A DTO in %s is neither listed as visibility-bearing nor recorded as carrying nothing a hidden " +
                    "project would leak. Classify it — silence would quietly shrink the audit.",
                DTO_PACKAGE)
            .isEmpty();
    }

    /**
     * The same partition over the domain package, and the reason it exists: without it a domain type could be absent
     * from {@link #VISIBILITY_BEARING_TYPE_NAMES} without anything failing, which silently removes every surface that
     * mentions only that type from {@link #testEveryVisibilityBearingSurfaceIsGuardedFilteredOrExempt()}. That is not
     * hypothetical — it is how the ungated {@code projectDeploymentWorkflow(id)} root query survived the audit that was
     * written to find exactly it.
     */
    @Test
    void testEveryDomainTypeIsClassified() {
        assertThat(getUnclassifiedTypeNames(DOMAIN_PACKAGE, NON_VISIBILITY_BEARING_DOMAIN_TYPES))
            .describedAs(
                "A domain type in %s is neither listed as visibility-bearing nor recorded as carrying nothing a " +
                    "hidden project would leak. An unclassified type is not neutral: every surface whose signature " +
                    "mentions only it drops out of this audit without a word.",
                DOMAIN_PACKAGE)
            .isEmpty();
    }

    private static List<String> getUnclassifiedTypeNames(String packageName, Map<String, String> nonBearingTypes) {
        Set<String> classifiedNames = new LinkedHashSet<>(nonBearingTypes.keySet());

        VISIBILITY_BEARING_TYPE_NAMES.stream()
            .map(typeName -> typeName.substring(typeName.lastIndexOf('.') + 1))
            .forEach(classifiedNames::add);

        return scanPackage(packageName)
            .stream()
            .filter(candidate -> !candidate.isInterface())
            .map(Class::getSimpleName)
            .filter(simpleName -> !classifiedNames.contains(simpleName))
            .toList();
    }

    /**
     * A classpath change that empties the scan would make every other assertion here pass vacuously, which is the one
     * way this test could rot without anyone noticing.
     */
    @Test
    void testTheScanReachesBothAuditedLayers() {
        List<String> auditedClassNames = getAuditedClasses()
            .stream()
            .map(Class::getSimpleName)
            .toList();

        assertThat(auditedClassNames)
            .contains(
                "ProjectFacadeImpl", "ProjectWorkflowFacadeImpl", "ProjectDeploymentFacadeImpl",
                "ProjectGraphQlController", "ProjectWorkflowGraphQlController", "ProjectDeploymentGraphQlController");
    }

    private static List<Class<?>> getAuditedClasses() {
        List<Class<?>> auditedClasses = new ArrayList<>();

        for (Class<?> candidate : scanPackage(FACADE_PACKAGE)) {
            if (isConcrete(candidate) && isFacadeImplementation(candidate)) {
                auditedClasses.add(candidate);
            }
        }

        for (Class<?> candidate : scanPackage(GRAPH_QL_PACKAGE)) {
            if (isConcrete(candidate) && isController(candidate)) {
                auditedClasses.add(candidate);
            }
        }

        return auditedClasses;
    }

    private static boolean isConcrete(Class<?> candidate) {
        int modifiers = candidate.getModifiers();

        return Modifier.isPublic(modifiers) && !Modifier.isAbstract(modifiers) && !candidate.isInterface() &&
            !candidate.isEnum();
    }

    private static boolean isFacadeImplementation(Class<?> candidate) {
        for (Class<?> implementedInterface : candidate.getInterfaces()) {
            String simpleName = implementedInterface.getSimpleName();

            if (simpleName.endsWith("Facade")) {
                return true;
            }
        }

        return false;
    }

    private static boolean isController(Class<?> candidate) {
        for (Annotation annotation : candidate.getAnnotations()) {
            Class<? extends Annotation> annotationType = annotation.annotationType();

            if (Objects.equals(annotationType.getName(), "org.springframework.stereotype.Controller")) {
                return true;
            }
        }

        return false;
    }

    private static List<Method> getPublicMethods(Class<?> auditedClass) {
        Method[] methods;

        try {
            methods = auditedClass.getDeclaredMethods();
        } catch (NoClassDefFoundError noClassDefFoundError) {
            throw new AssertionError(
                "%s is in the audited scope but its signatures cannot be resolved on this test classpath, so the " +
                    "audit would silently skip it. Add the missing module as a test dependency.".formatted(
                        auditedClass.getName()),
                noClassDefFoundError);
        }

        List<Method> publicMethods = new ArrayList<>();

        for (Method method : methods) {
            if (!Modifier.isPublic(method.getModifiers()) || method.isSynthetic() || method.isBridge() ||
                IGNORED_METHOD_NAMES.contains(method.getName())) {

                continue;
            }

            publicMethods.add(method);
        }

        return publicMethods;
    }

    private static boolean mentionsVisibilityBearingType(Method method) {
        Set<String> mentionedTypeNames = new LinkedHashSet<>();

        collectTypeNames(method.getGenericReturnType(), mentionedTypeNames);

        for (Type parameterType : method.getGenericParameterTypes()) {
            collectTypeNames(parameterType, mentionedTypeNames);
        }

        for (String mentionedTypeName : mentionedTypeNames) {
            if (VISIBILITY_BEARING_TYPE_NAMES.contains(mentionedTypeName)) {
                return true;
            }
        }

        return false;
    }

    private static void collectTypeNames(Type type, Set<String> typeNames) {
        switch (type) {
            case Class<?> rawClass -> {
                Class<?> componentClass = rawClass.isArray() ? rawClass.getComponentType() : rawClass;

                typeNames.add(componentClass.getName());

                // A subclass of a visibility-bearing DTO carries the same content — ProjectWorkflowDTO extends
                // WorkflowDTO, which holds the Workflow itself.
                Class<?> superClass = componentClass.getSuperclass();

                while (superClass != null && !Objects.equals(superClass, Object.class)) {
                    typeNames.add(superClass.getName());

                    superClass = superClass.getSuperclass();
                }
            }
            case ParameterizedType parameterizedType -> {
                collectTypeNames(parameterizedType.getRawType(), typeNames);

                for (Type argumentType : parameterizedType.getActualTypeArguments()) {
                    collectTypeNames(argumentType, typeNames);
                }
            }
            case GenericArrayType genericArrayType ->
                collectTypeNames(genericArrayType.getGenericComponentType(), typeNames);
            case WildcardType wildcardType -> {
                for (Type boundType : wildcardType.getUpperBounds()) {
                    collectTypeNames(boundType, typeNames);
                }
            }
            default -> {
                // Type variables carry no concrete type to classify.
            }
        }
    }

    private static String toSignature(Method method) {
        Class<?> declaringClass = method.getDeclaringClass();

        String parameterTypes = java.util.Arrays.stream(method.getParameterTypes())
            .map(Class::getSimpleName)
            .collect(Collectors.joining(","));

        return "%s#%s(%s)".formatted(declaringClass.getSimpleName(), method.getName(), parameterTypes);
    }

    private static boolean isAnnotated(Annotation[] annotations, Set<String> annotationTypeNames) {
        for (Annotation annotation : annotations) {
            Class<? extends Annotation> annotationType = annotation.annotationType();

            if (annotationTypeNames.contains(annotationType.getName())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Enumerates the classes a package contributes from every classpath entry that carries it — the same package name
     * appears in more than one module, and the interfaces and their implementations live in different ones. Nested
     * classes are skipped: they are value carriers, not surfaces.
     */
    private static List<Class<?>> scanPackage(String packageName) {
        String packagePath = packageName.replace('.', '/');

        ClassLoader classLoader = VisibilityBearingSurfaceAuditTest.class.getClassLoader();

        Set<String> classNames = new LinkedHashSet<>();

        try {
            Enumeration<URL> resources = classLoader.getResources(packagePath);

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();

                if (Objects.equals(resource.getProtocol(), "file")) {
                    collectDirectoryClassNames(packageName, resource, classNames);
                } else if (Objects.equals(resource.getProtocol(), "jar")) {
                    collectJarClassNames(packageName, packagePath, resource, classNames);
                }
            }
        } catch (IOException ioException) {
            throw new AssertionError("Failed to scan package " + packageName, ioException);
        }

        List<Class<?>> classes = new ArrayList<>();

        for (String className : classNames) {
            try {
                classes.add(Class.forName(className, false, classLoader));
            } catch (ClassNotFoundException classNotFoundException) {
                throw new AssertionError("Failed to load " + className, classNotFoundException);
            }
        }

        return classes;
    }

    // The path comes from the JVM's own classpath, not from user input; SpotBugs cannot tell those apart.
    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    private static void collectDirectoryClassNames(String packageName, URL resource, Set<String> classNames) {
        File directory = new File(URLDecoder.decode(resource.getFile(), StandardCharsets.UTF_8));

        File[] files = directory.listFiles();

        if (files == null) {
            return;
        }

        for (File file : files) {
            String fileName = file.getName();

            if (fileName.endsWith(".class") && !fileName.contains("$")) {
                classNames.add(packageName + "." + fileName.substring(0, fileName.length() - ".class".length()));
            }
        }
    }

    // The URL comes from ClassLoader.getResources, so the connection is to this JVM's own classpath entry.
    @SuppressFBWarnings("URLCONNECTION_SSRF_FD")
    private static void collectJarClassNames(
        String packageName, String packagePath, URL resource, Set<String> classNames) throws IOException {

        JarURLConnection jarUrlConnection = (JarURLConnection) resource.openConnection();

        try (JarFile jarFile = jarUrlConnection.getJarFile()) {
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();

                String entryName = entry.getName();

                if (!entryName.startsWith(packagePath + "/") || !entryName.endsWith(".class") ||
                    entryName.contains("$")) {

                    continue;
                }

                String simpleName = entryName.substring(
                    packagePath.length() + 1, entryName.length() - ".class".length());

                if (!simpleName.contains("/")) {
                    classNames.add(packageName + "." + simpleName);
                }
            }
        }
    }
}
