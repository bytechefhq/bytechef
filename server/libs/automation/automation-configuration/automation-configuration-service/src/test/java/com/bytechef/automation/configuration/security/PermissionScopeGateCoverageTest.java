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

import com.bytechef.automation.configuration.security.MainSourceScan.GuardExpressionScan;
import com.bytechef.automation.configuration.security.MainSourceScan.ResolvedGuardExpression;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The counterpart to {@link ResourceTokenResolverCoverageTest}, one level up: that one asks whether a guard's resource
 * token can be resolved, this one asks whether a catalogued permission scope is named by any guard at all.
 * <p>
 * The {@code permissionScopeGroups} query hands the custom-role editor every name in the catalogue, and a tenant admin
 * composing a role has no way to tell which of them decide anything. Most do not: a scope no {@code @PreAuthorize}
 * mentions neither grants nor denies, so a role built out of such scopes reads as a considered set of permissions and
 * behaves as an empty one — and a role built to <em>withhold</em> one of them withholds nothing. That gap is a
 * deliberate, known state of this branch rather than a bug to be fixed here, so it is written down in
 * {@link #KNOWN_UNENFORCED_SCOPES} and made self-checking instead of left to be rediscovered.
 * <p>
 * Three ways the catalogue and the gates can drift are therefore checked. A new catalogue entry with neither a gate nor
 * a pin fails {@link #testEveryCatalogueScopeIsEitherEnforcedOrKnownToBeUnenforced}. A pin whose scope has since
 * acquired a gate fails {@link #testEveryPinnedUnenforcedScopeIsStillUnenforced}, so the list shrinks as enforcement
 * arrives instead of ossifying. A gate naming a scope the catalogue does not contain fails
 * {@link #testEveryGateScopeIsInTheCatalogue} — that one is not cosmetic: a custom role can only be composed from
 * catalogue names, so a gate demanding a scope outside it can be satisfied by nothing except the tenant-admin
 * short-circuit.
 * <p>
 * Guard expressions come from {@link MainSourceScan}; the catalogue is read from the enum sources for the same reason,
 * since the scope enums live in EE and this module's test classpath cannot see them.
 *
 * @author Ivica Cardic
 */
class PermissionScopeGateCoverageTest {

    /**
     * Catalogue scopes that today appear in <em>no</em> {@code @PreAuthorize} anywhere in the tree, pinned so the
     * assertions below describe reality and fail on any <em>new</em> unenforced scope. Every entry is a scope the
     * custom-role editor offers and nothing honours; none is an exemption anybody wants to keep.
     * <p>
     * Removing an entry is the whole cost of enforcing one: write the gate, delete the line.
     */
    private static final Set<String> KNOWN_UNENFORCED_SCOPES = Set.of();

    /**
     * Every SpEL function a {@code @PreAuthorize} in this tree calls. Pinned because the scope patterns below read
     * scopes out of five of them by argument position: a new authorization helper that carries a scope would otherwise
     * make every scope it names invisible to this test, and the coverage assertion would pass by not looking. Failing
     * here is the prompt to decide whether the new function carries a scope and, if it does, to teach
     * {@link #SCOPE_PATTERNS} where.
     */
    private static final Set<String> KNOWN_GATE_FUNCTIONS = Set.of(
        "@aiAgentEvalWorkflowResolver.getEnvironmentId",
        "@aiAgentEvalWorkflowResolver.getWorkflowId",
        "@permissionService.hasResourceRole",
        "@permissionService.isResourceOwner",
        "hasAuthority",
        "hasPermission",
        "hasResourceScopeInEnvironment",
        "hasWorkspaceScopeInEnvironment",
        "hasWorkspaceScopeInEnvironmentId",
        "hasWorkflowScopeIfProjectWorkflowInEnvironment",
        "hasWorkflowScopeIfProjectWorkflowInEnvironmentId",
        "hasWorkflowScopeInEnvironment",
        "hasWorkspaceScopeInEveryEnvironment",
        "isAuthenticated",
        "isCurrentUser",
        "isTenantAdmin");

    // A scope name is upper snake case, which is what separates it from a resource token ('Project', 'Connection') and
    // lets these patterns key off argument position without also matching the token beside it. Should the two ever
    // share a shape, testEveryGateScopeIsInTheCatalogue is what notices: a token misread as a scope is not a catalogue
    // name.
    private static final String SCOPE_GROUP = "'([A-Z][A-Z0-9_]*)'";

    private static final Pattern BEAN_CALL = Pattern.compile("@[A-Za-z_][A-Za-z0-9_.]*\\([^()]*\\)");

    /**
     * Where a scope sits in each gate function that takes one:
     * <ul>
     * <li>{@code hasPermission(id, 'Type', 'SCOPE')} — third argument, the four-argument {@code PermissionEvaluator}
     * overload.</li>
     * <li>{@code hasPermission(target, 'SCOPE')} — second argument, the three-argument overload that reads the resource
     * type off the object.</li>
     * <li>{@code hasWorkspaceScopeInEveryEnvironment(workspaceId, 'SCOPE')} — second argument.</li>
     * <li>{@code hasWorkspaceScopeInEnvironment(workspaceId, 'SCOPE', environment)} — second of three.</li>
     * <li>{@code hasWorkspaceScopeInEnvironmentId(workspaceId, 'SCOPE', environmentId)} — second of three. Needs its
     * own pattern rather than sharing the one above: that pattern ends at a literal {@code (}, which the {@code Id}
     * suffix defeats, so a shared pattern would read no scope at all out of this function and every scope named only
     * here would look uncatalogued.</li>
     * <li>{@code hasResourceScopeInEnvironment(id, 'Type', 'SCOPE', environment)} — third of four.</li>
     * <li>{@code hasWorkflowScopeInEnvironment(workflowId, 'SCOPE', environment)} — second of three.</li>
     * <li>{@code hasWorkflowScopeIfProjectWorkflowInEnvironment(workflowId, 'SCOPE', environment)} and its
     * {@code InEnvironmentId} sibling — second of three, each with its own pattern for the same reason as above. The
     * workflow id may be a single bean call that looks it up, such as
     * {@code @aiAgentEvalWorkflowResolver.getWorkflowId('AiAgentEvalTest', #id)}; {@link #BEAN_CALL} erases such a call
     * before these patterns run, so the argument stays parenthesis-free.</li>
     * </ul>
     */
    private static final List<Pattern> SCOPE_PATTERNS = List.of(
        Pattern.compile("hasWorkflowScopeInEnvironment\\(\\s*[^,()]+?,\\s*" + SCOPE_GROUP + "\\s*,"),
        Pattern.compile("hasWorkflowScopeIfProjectWorkflowInEnvironment\\(\\s*[^,()]+?,\\s*" + SCOPE_GROUP + "\\s*,"),
        Pattern.compile(
            "hasWorkflowScopeIfProjectWorkflowInEnvironmentId\\(\\s*[^,()]+?,\\s*" + SCOPE_GROUP + "\\s*,"),
        Pattern.compile("hasPermission\\(\\s*[^,()]+?,\\s*'[A-Za-z][A-Za-z0-9]*'\\s*,\\s*" + SCOPE_GROUP + "\\s*\\)"),
        Pattern.compile("hasPermission\\(\\s*[^,()]+?,\\s*" + SCOPE_GROUP + "\\s*\\)"),
        Pattern.compile("hasWorkspaceScopeInEveryEnvironment\\(\\s*[^,()]+?,\\s*" + SCOPE_GROUP + "\\s*\\)"),
        Pattern.compile("hasWorkspaceScopeInEnvironment\\(\\s*[^,()]+?,\\s*" + SCOPE_GROUP + "\\s*,"),
        Pattern.compile("hasWorkspaceScopeInEnvironmentId\\(\\s*[^,()]+?,\\s*" + SCOPE_GROUP + "\\s*,"),
        Pattern.compile(
            "hasResourceScopeInEnvironment\\(\\s*[^,()]+?,\\s*'[A-Za-z][A-Za-z0-9]*'\\s*,\\s*" + SCOPE_GROUP
                + "\\s*,"));

    private static final Pattern GATE_FUNCTION_CALL = Pattern.compile("(@?[A-Za-z_][A-Za-z0-9_.]*)\\(");

    /**
     * SpEL's type-reference operator together with the static call made through it — {@code T(a.b.C).method(args)} —
     * and, second, a bare {@code T(a.b.C)}. Erased before functions are collected, because {@code GATE_FUNCTION_CALL}
     * otherwise reads {@code T} and the method name as two authorization functions the scan has never seen. Neither is
     * one: {@code T(...)} names a class, and the call on it is an ordinary static method used to coerce an argument
     * before a real gate function receives it.
     * <p>
     * Erased rather than pinned, so that pinning stays reserved for functions that genuinely decide access — putting
     * {@code toLong} in {@link #KNOWN_GATE_FUNCTIONS} would also bless the next static call to appear there, whatever
     * it did.
     * <p>
     * Two flat patterns rather than one with an optional trailing group: quantifiers nested inside an optional group
     * are the shape a ReDoS detector rejects, and splitting them removes it without needing a suppression. Each is
     * anchored at both ends and contains no quantifier that can overlap its neighbour, so matching is linear.
     * <p>
     * Neither tolerates whitespace inside the construct, and the argument list is matched as parenthesis-free. Both are
     * deliberate: every such guard in this tree is written without spaces and without a nested call, and an expression
     * that breaks either assumption is left unerased, which fails {@link #testEveryGateFunctionIsOneTheScanKnows}
     * loudly rather than erasing more than intended.
     */
    private static final Pattern TYPE_REFERENCE_STATIC_CALL =
        Pattern.compile("\\bT\\([A-Za-z_][A-Za-z0-9_.]*\\)\\.[A-Za-z_][A-Za-z0-9_]*\\([^()]*\\)");

    private static final Pattern TYPE_REFERENCE = Pattern.compile("\\bT\\([A-Za-z_][A-Za-z0-9_.]*\\)");

    private static final Pattern SCOPE_ENUM_DECLARATION =
        Pattern.compile("enum\\s+(\\w+)\\s+implements\\s+[^{]*\\bPermissionScopeType\\b[^{]*\\{");
    private static final Pattern SCOPE_CONSTANT = Pattern.compile("\\b([A-Z][A-Z0-9_]{2,})\\b");

    private static Map<String, Set<String>> catalogueScopesByEnum;
    private static Map<String, Set<String>> gateScopesByFile;
    private static Set<String> gateFunctions;

    // Scanned once for the whole class: JUnit builds a fresh test instance per method, and the tree is several thousand
    // files.
    @BeforeAll
    static void scanTheTree() {
        List<Path> mainSourceFiles = MainSourceScan.collectMainSourceFiles(MainSourceScan.serverRoot());

        GuardExpressionScan guardExpressionScan = MainSourceScan.scanGuardExpressions(mainSourceFiles);

        List<ResolvedGuardExpression> guardExpressions = guardExpressionScan.guardExpressions();

        catalogueScopesByEnum = collectCatalogueScopes(mainSourceFiles);
        gateScopesByFile = collectGateScopes(guardExpressions);
        gateFunctions = collectGateFunctions(guardExpressions);
    }

    @Test
    void testTheScanFoundTheCatalogueAndTheGates() {
        // A scan that silently found nothing would make every assertion below vacuously true, which is the one outcome
        // worse than a failure here.
        assertThat(catalogueScopesByEnum)
            .as("no enum implementing PermissionScopeType was found anywhere in the tree, so the scan is broken")
            .isNotEmpty();

        assertThat(catalogueScopes())
            .as("the scope catalogue came back implausibly small, so the enum body scan is broken: %s",
                catalogueScopesByEnum)
            .hasSizeGreaterThan(20);

        assertThat(gateScopesByFile)
            .as("no @PreAuthorize scope was found anywhere in the tree, so the scope patterns are broken")
            .isNotEmpty();
    }

    /**
     * A scope named by a gate but absent from the catalogue can be held by nobody: a custom role is composed from
     * catalogue names, and a built-in role's scopes come from the same providers. The gate would then be satisfied only
     * by the tenant-admin short-circuit — a silent lockout of exactly the shape
     * {@link ResourceTokenResolverCoverageTest} guards against on the resolver side. A typo in a gate expression lands
     * here too.
     */
    @Test
    void testEveryGateScopeIsInTheCatalogue() {
        Set<String> catalogueScopes = catalogueScopes();
        Map<String, Set<String>> uncataloguedScopes = new TreeMap<>();

        for (Map.Entry<String, Set<String>> entry : gateScopesByFile.entrySet()) {
            if (!catalogueScopes.contains(entry.getKey())) {
                uncataloguedScopes.put(entry.getKey(), entry.getValue());
            }
        }

        assertThat(uncataloguedScopes)
            .as(
                "these @PreAuthorize expressions name a scope no PermissionScopeProvider declares, so no role can "
                    + "carry it and the gate admits only tenant admins. Catalogue: %s. Uncatalogued scopes and the "
                    + "files that name them: %s",
                catalogueScopes, uncataloguedScopes)
            .isEmpty();
    }

    @Test
    void testEveryCatalogueScopeIsEitherEnforcedOrKnownToBeUnenforced() {
        Set<String> undocumentedUnenforcedScopes = new TreeSet<>();

        for (String scope : catalogueScopes()) {
            if (gateScopesByFile.containsKey(scope) || KNOWN_UNENFORCED_SCOPES.contains(scope)) {
                continue;
            }

            undocumentedUnenforcedScopes.add(scope);
        }

        assertThat(undocumentedUnenforcedScopes)
            .as(
                "the custom-role editor offers these scopes and no @PreAuthorize names any of them, so a role built "
                    + "from them grants nothing and a role built to withhold them withholds nothing. Either add the "
                    + "gate or add the scope to KNOWN_UNENFORCED_SCOPES with the reason. Scopes that are gated today: "
                    + "%s",
                new TreeSet<>(gateScopesByFile.keySet()))
            .isEmpty();
    }

    /**
     * The direction the pin list rots in. Without this, enforcing a scope would leave its exemption behind, and the
     * exemption would then hide the gate being deleted again later.
     */
    @Test
    void testEveryPinnedUnenforcedScopeIsStillUnenforced() {
        Map<String, Set<String>> stalePins = new TreeMap<>();

        for (String scope : KNOWN_UNENFORCED_SCOPES) {
            Set<String> gateFiles = gateScopesByFile.get(scope);

            if (gateFiles != null) {
                stalePins.put(scope, gateFiles);
            }
        }

        assertThat(stalePins)
            .as(
                "these scopes are now named by a @PreAuthorize, so remove them from KNOWN_UNENFORCED_SCOPES — "
                    + "otherwise the pin would hide the gate being dropped again. Scopes and the files that gate them: "
                    + "%s",
                stalePins)
            .isEmpty();
    }

    /**
     * Every pinned scope must still be a catalogue name. A scope deleted from its provider but left pinned would keep
     * the list describing a catalogue that no longer exists, and the reader would take the entry for a live gap.
     */
    @Test
    void testEveryPinnedUnenforcedScopeIsStillInTheCatalogue() {
        Set<String> catalogueScopes = catalogueScopes();
        Set<String> pinsForScopesThatNoLongerExist = new TreeSet<>();

        for (String scope : KNOWN_UNENFORCED_SCOPES) {
            if (!catalogueScopes.contains(scope)) {
                pinsForScopesThatNoLongerExist.add(scope);
            }
        }

        assertThat(pinsForScopesThatNoLongerExist)
            .as(
                "these KNOWN_UNENFORCED_SCOPES entries name no catalogue scope any more, so the pin outlived what it "
                    + "described — remove the entry. Catalogue: %s",
                catalogueScopes)
            .isEmpty();
    }

    @Test
    void testEveryGateFunctionIsOneTheScanKnows() {
        Set<String> unknownGateFunctions = new TreeSet<>(gateFunctions);

        unknownGateFunctions.removeAll(KNOWN_GATE_FUNCTIONS);

        assertThat(unknownGateFunctions)
            .as(
                "a @PreAuthorize calls an authorization function this scan has never seen. If it takes a permission "
                    + "scope, add its argument position to SCOPE_PATTERNS before adding it to KNOWN_GATE_FUNCTIONS — "
                    + "otherwise every scope it names is invisible to the coverage assertions above and they pass by "
                    + "not looking. Functions in use: %s",
                new TreeSet<>(gateFunctions))
            .isEmpty();
    }

    private static Set<String> catalogueScopes() {
        Set<String> scopes = new TreeSet<>();

        for (Set<String> enumScopes : catalogueScopesByEnum.values()) {
            scopes.addAll(enumScopes);
        }

        return scopes;
    }

    /**
     * Scope names keyed by scope and valued by the set of files whose {@code @PreAuthorize} annotations name them.
     */
    private static Map<String, Set<String>> collectGateScopes(List<ResolvedGuardExpression> guardExpressions) {
        Map<String, Set<String>> scopesByFile = new TreeMap<>();

        for (ResolvedGuardExpression guardExpression : guardExpressions) {
            String expression = BEAN_CALL.matcher(guardExpression.expression())
                .replaceAll("#beanCallResult");

            for (Pattern scopePattern : SCOPE_PATTERNS) {
                Matcher matcher = scopePattern.matcher(expression);

                while (matcher.find()) {
                    Set<String> files = scopesByFile.computeIfAbsent(matcher.group(1), scope -> new TreeSet<>());

                    files.add(guardExpression.fileName());
                }
            }
        }

        return scopesByFile;
    }

    private static Set<String> collectGateFunctions(List<ResolvedGuardExpression> guardExpressions) {
        Set<String> functions = new TreeSet<>();

        for (ResolvedGuardExpression guardExpression : guardExpressions) {
            String expression = TYPE_REFERENCE_STATIC_CALL.matcher(guardExpression.expression())
                .replaceAll("#coercedArgument");

            expression = TYPE_REFERENCE.matcher(expression)
                .replaceAll("#typeReferenceOnly");

            Matcher matcher = GATE_FUNCTION_CALL.matcher(expression);

            while (matcher.find()) {
                functions.add(matcher.group(1));
            }
        }

        return functions;
    }

    /**
     * The constants of every enum implementing {@code PermissionScopeType}, keyed by enum name. Read from source text
     * because the enums live in the EE automation-configuration module, which this module's test classpath does not
     * include — the same reason {@link MainSourceScan} reads guards as text.
     */
    private static Map<String, Set<String>> collectCatalogueScopes(List<Path> sourceFiles) {
        Map<String, Set<String>> scopesByEnum = new TreeMap<>();

        for (Path sourceFile : sourceFiles) {
            String source = MainSourceScan.readSource(sourceFile);

            if (!source.contains("PermissionScopeType")) {
                continue;
            }

            String strippedSource = MainSourceScan.stripComments(source);

            Matcher declarationMatcher = SCOPE_ENUM_DECLARATION.matcher(strippedSource);

            while (declarationMatcher.find()) {
                String enumName = declarationMatcher.group(1);
                String constantList = extractConstantList(strippedSource, declarationMatcher.end() - 1);

                Set<String> scopes = new TreeSet<>();

                Matcher constantMatcher = SCOPE_CONSTANT.matcher(constantList);

                while (constantMatcher.find()) {
                    scopes.add(constantMatcher.group(1));
                }

                scopesByEnum.put(enumName, scopes);
            }
        }

        return scopesByEnum;
    }

    /**
     * The constant-declaration part of an enum body: everything from the opening brace at {@code bodyStartIndex} to the
     * matching closing brace, cut at the {@code ;} that ends the constant list when the enum also declares members.
     */
    private static String extractConstantList(String strippedSource, int bodyStartIndex) {
        int depth = 0;
        int index = bodyStartIndex;

        while (index < strippedSource.length()) {
            char character = strippedSource.charAt(index);

            if (character == '{') {
                depth++;
            } else if (character == '}') {
                depth--;

                if (depth == 0) {
                    break;
                }
            }

            index++;
        }

        String body = strippedSource.substring(bodyStartIndex + 1, index);

        int constantListEnd = body.indexOf(';');

        return constantListEnd < 0 ? body : body.substring(0, constantListEnd);
    }
}
