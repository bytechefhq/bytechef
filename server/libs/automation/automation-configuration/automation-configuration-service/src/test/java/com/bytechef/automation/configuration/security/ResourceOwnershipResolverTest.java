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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The one guard that covers the whole tree rather than one module: every resource-type token named by a production
 * {@code @PreAuthorize} must have a registered {@code ResourceOwnershipResolver}.
 * <p>
 * {@code PermissionService.hasResourceScope} and {@code isResourceOwner} both deny unconditionally when the resolver
 * registry has no entry for the named type. So a token typo, a lost {@code @Component}, or a resolver that moved into a
 * module the deployment does not include is a <em>silent lockout</em> for every non-tenant-admin — and it passes every
 * test in the suite, because the guard tests mock {@code PermissionService} and the tenant-admin short-circuit in the
 * real implementation runs before the registry is ever consulted. Manual verification as an admin cannot see it either.
 * <p>
 * Both directions are asserted. The reverse direction — every registered resolver is named by some gate — catches the
 * mirror-image mistake of renaming a token and leaving its resolver behind answering for a discriminator nobody asks
 * about.
 * <p>
 * {@link #testEveryOwnerPopulatingResolverHasAMatchingVisibilityProvider} pins the adjacent invariant: a resolver may
 * fill {@code ownerUserId} only for a type that also registers a {@code ResourceVisibilityProvider}, since without one
 * CE falls through to owner-isolation and hides the resource from everyone but its creator.
 * <p>
 * Guard expressions come from {@link MainSourceScan}, which reads them as source text rather than off the classpath:
 * tokens and resolvers are spread across CE, EE and the AI modules, and no single module's test classpath contains them
 * all. Reading the whole tree is what makes this a tree-wide assertion instead of a per-module one.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings(
    value = "REDOS",
    justification = "The patterns scan this repository's own source files, and their only quantified groups are "
        + "optional (?), which cannot backtrack catastrophically.")
class ResourceOwnershipResolverTest {

    /**
     * Resource tokens that today have <em>no</em> registered {@code ResourceOwnershipResolver}, pinned so the
     * assertions below describe reality and fail on any <em>new</em> unresolved token. Every entry is a real defect
     * awaiting its own fix, not an exemption.
     *
     * {@code Connection} and {@code McpServer} were both pinned here, each claimed by an environment resolver while no
     * ownership resolver answered for it, until {@code ConnectionOwnershipResolver} and
     * {@code McpServerOwnershipResolver} were added. Before that, the four connection sharing guards in
     * {@code WorkspaceConnectionFacadeImpl} denied even the owner they name.
     *
     * <p>
     * Each entry records the grounds it is pinned on, and both directions of rot are checked, so the map cannot become
     * a permanent exemption: {@link #testEveryPinnedUnresolvedTokenIsStillUnresolved} fails once a resolver exists, and
     * {@link #testEveryPinnedUnresolvedTokenStillHasEveryGroundItIsPinnedOn} fails once any recorded ground no longer
     * holds. Naming the grounds rather than merely "something still references this" is what makes the second check
     * bite: delete a token's guards and it could still be claimed by an environment resolver, so a plain reference
     * check would stay green while the guard-side exemption lived on to silently re-admit the next guard written for
     * it. Either way the fix is the same one line — remove the entry.
     */
    private static final Map<String, Set<PinGrounds>> KNOWN_UNRESOLVED_TOKENS = Map.of();

    private static final Pattern OWNERSHIP_RESOLVER_DECLARATION =
        Pattern.compile("implements\\s+[^{;]*\\bResourceOwnershipResolver\\b");
    private static final Pattern ENVIRONMENT_RESOLVER_DECLARATION =
        Pattern.compile("implements\\s+[^{;]*\\bResourceEnvironmentResolver\\b");
    private static final Pattern VISIBILITY_PROVIDER_DECLARATION =
        Pattern.compile("implements\\s+[^{;]*\\bResourceVisibilityProvider\\b");
    // ofWorkspace and unknown do not match: after "of" comes a word character, so there is no boundary there.
    private static final Pattern OWNER_POPULATING_FACTORY =
        Pattern.compile("ResourceOwner\\s*(?:\\.|::)\\s*(?:of|ofUser)\\b");
    private static final Pattern SPRING_STEREOTYPE = Pattern.compile("@(?:Component|Service)\\b");
    private static final Pattern CLASS_KEYWORD = Pattern.compile("\\bclass\\b");
    private static final Pattern RESOURCE_TYPE_METHOD =
        Pattern.compile("String\\s+resourceType\\(\\)\\s*\\{\\s*return\\s+\"([^\"]+)\"\\s*;");

    // hasPermission(#id, 'Type', 'SCOPE') and the two PermissionService methods callable as @permissionService.x(...):
    // the token is always the second argument.
    private static final Pattern TOKEN_AS_SECOND_ARGUMENT = Pattern.compile(
        "(?:hasPermission|hasResourceScope(?:InEnvironment(?:Id)?)?|hasResourceRole)"
            + "\\(\\s*[^,()]+?,\\s*'([A-Za-z][A-Za-z0-9]*)'\\s*,");
    // isResourceOwner has two shapes: the SpEL root built-in takes (id, 'Type') and the bean method takes ('Type', id).
    private static final Pattern OWNER_TOKEN_AS_SECOND_ARGUMENT =
        Pattern.compile("isResourceOwner\\(\\s*[^,()']+?,\\s*'([A-Za-z][A-Za-z0-9]*)'\\s*\\)");
    private static final Pattern OWNER_TOKEN_AS_FIRST_ARGUMENT =
        Pattern.compile("isResourceOwner\\(\\s*'([A-Za-z][A-Za-z0-9]*)'\\s*,");

    private static List<Path> mainSourceFiles;
    private static Map<String, Set<String>> tokensByGateFile;
    private static Map<String, Set<String>> unreadableGuardArgumentsByFile;
    private static Map<String, String> ownershipResolverTypes;
    private static Map<String, String> environmentResolverTypes;
    /**
     * Owner-populating resolvers whose type has no visibility provider on purpose. Owner-isolation is the intended
     * answer for these, not the lockout the assertion below guards against.
     * <ul>
     * <li>{@code ApiKey} — a key authenticates as the user who owns it. CE {@code hasResourceScope} answering
     * {@code isCurrentUser(ownerUserId)} is exactly the rule that keeps a colleague's key out of reach, and
     * {@code ApiKeyFacadeImpl} restricts every read and write to the owner for the same reason. A visibility provider
     * that relaxed it would hand every member someone else's credential.</li>
     * <li>{@code ApprovalTask} — a task is addressed to its assignee, whom the resolver records as the owner. CE
     * {@code hasResourceScope} answering {@code isCurrentUser(ownerUserId)} keeps an approval with the person asked to
     * give it; the approval list applies the same owner-or-scope rule through {@code isResourceOwner}.</li>
     * </ul>
     */
    private static final Set<String> OWNER_ISOLATED_BY_DESIGN = Set.of("ApiKey", "ApprovalTask");

    private static Map<String, String> ownerPopulatingResolverTypes;
    private static Map<String, String> visibilityProviderTypes;

    // Scanned once for the whole class: JUnit builds a fresh test instance per method, and the tree is several thousand
    // files.
    @BeforeAll
    static void scanTheTree() {
        mainSourceFiles = MainSourceScan.collectMainSourceFiles(MainSourceScan.serverRoot());

        GuardExpressionScan guardExpressionScan = MainSourceScan.scanGuardExpressions(mainSourceFiles);

        tokensByGateFile = collectTokensByGateFile(guardExpressionScan.guardExpressions());
        unreadableGuardArgumentsByFile = guardExpressionScan.unreadableArgumentsByFile();
        ownershipResolverTypes = collectResolverTypes(mainSourceFiles, OWNERSHIP_RESOLVER_DECLARATION);
        environmentResolverTypes = collectResolverTypes(mainSourceFiles, ENVIRONMENT_RESOLVER_DECLARATION);
        ownerPopulatingResolverTypes = collectResolverTypes(
            mainSourceFiles, OWNERSHIP_RESOLVER_DECLARATION,
            strippedSource -> OWNER_POPULATING_FACTORY.matcher(strippedSource)
                .find());
        visibilityProviderTypes = collectResolverTypes(mainSourceFiles, VISIBILITY_PROVIDER_DECLARATION);
    }

    @Test
    void testTheScanFoundTheTreeItIsSupposedToScan() {
        // A scan that silently found nothing would make every assertion below vacuously true, which is the one outcome
        // worse than a failure here.
        assertThat(mainSourceFiles)
            .as("the main-source scan under %s found no Java files", MainSourceScan.serverRoot())
            .hasSizeGreaterThan(1000);

        assertThat(tokensByGateFile)
            .as("no @PreAuthorize resource token was found anywhere in the tree, so the scan is broken")
            .isNotEmpty();

        assertThat(ownershipResolverTypes)
            .as("no ResourceOwnershipResolver implementation was found, so the scan is broken")
            .isNotEmpty();

        assertThat(visibilityProviderTypes)
            .as("no ResourceVisibilityProvider implementation was found, so the scan is broken")
            .isNotEmpty();

        assertThat(ownerPopulatingResolverTypes)
            .as("no resolver was seen populating an owner, so testEveryOwnerPopulatingResolverHasAMatchingVisibility"
                + "Provider would be vacuously true")
            .isNotEmpty();
    }

    /**
     * A guard expression this scan cannot read in full names resource tokens no assertion below can see, so the
     * incompleteness is reported rather than passing as an empty expression. An annotation value must be a compile-time
     * constant, so the only non-literal shape possible is a constant reference; the scan resolves the
     * {@code "…" + SomeConstants.NAME + "…"} form that ten facades in this tree already use, as well as a constant
     * declared in the guarded file itself. What lands here is a constant this text scan cannot place — imported without
     * its qualifier, or named by several files that disagree about its value. That is the signal to teach the scanner
     * the new shape, not to relax this assertion.
     */
    @Test
    void testEveryPreAuthorizeArgumentIsFullyReadable() {
        assertThat(unreadableGuardArgumentsByFile)
            .as(
                "these @PreAuthorize arguments contain a fragment this scan could not resolve to text, so the resource "
                    + "tokens they name are invisible to the coverage assertions. Files and unresolved fragments: %s",
                unreadableGuardArgumentsByFile)
            .isEmpty();
    }

    @Test
    void testEveryGuardTokenHasARegisteredOwnershipResolver() {
        Map<String, Set<String>> unresolvedTokens = new TreeMap<>();

        for (Map.Entry<String, Set<String>> entry : tokensByGateFile.entrySet()) {
            String token = entry.getKey();

            if (ownershipResolverTypes.containsKey(token) || KNOWN_UNRESOLVED_TOKENS.containsKey(token)) {
                continue;
            }

            unresolvedTokens.put(token, entry.getValue());
        }

        assertThat(unresolvedTokens)
            .as(
                "every resource token named by a @PreAuthorize must have a ResourceOwnershipResolver, or the guard "
                    + "denies every non-tenant-admin instead of checking anything. Registered resolvers: %s. "
                    + "Unresolved tokens and the files that name them: %s",
                new TreeSet<>(ownershipResolverTypes.keySet()), unresolvedTokens)
            .isEmpty();
    }

    @Test
    void testEveryRegisteredOwnershipResolverIsNamedBySomeGuard() {
        Set<String> unreachableResolverTypes = new TreeSet<>();

        for (Map.Entry<String, String> entry : ownershipResolverTypes.entrySet()) {
            if (!tokensByGateFile.containsKey(entry.getKey())) {
                unreachableResolverTypes.add(entry.getKey() + " (" + entry.getValue() + ")");
            }
        }

        assertThat(unreachableResolverTypes)
            .as(
                "a resolver whose resourceType no @PreAuthorize names answers for a discriminator nobody asks about — "
                    + "usually a token that was renamed on the guard side only. Tokens in use: %s",
                new TreeSet<>(tokensByGateFile.keySet()))
            .isEmpty();
    }

    @Test
    void testEveryEnvironmentResolverHasAMatchingOwnershipResolver() {
        Set<String> strandedEnvironmentResolvers = new TreeSet<>();

        for (Map.Entry<String, String> entry : environmentResolverTypes.entrySet()) {
            String resourceType = entry.getKey();

            if (ownershipResolverTypes.containsKey(resourceType) || KNOWN_UNRESOLVED_TOKENS.containsKey(resourceType)) {
                continue;
            }

            strandedEnvironmentResolvers.add(resourceType + " (" + entry.getValue() + ")");
        }

        assertThat(strandedEnvironmentResolvers)
            .as(
                "an environment resolver is consulted only after an ownership resolver has produced a workspace, so "
                    + "one without a matching ownership resolver is dead code")
            .isEmpty();
    }

    /**
     * The invariant that makes an owner-populating resolver safe, and whose violation would be the worst outcome in
     * this file: resources silently invisible to everyone but whoever created them.
     * <p>
     * CE {@code PermissionServiceImpl.hasResourceScope} branches on whether the type registered a
     * {@code ResourceVisibilityProvider}. With one, it stops as soon as the resource exists, because visibility has
     * already answered the stricter question — a colleague the list has shown a WORKSPACE connection is not then asked
     * to own it. Without one, it falls through to owner-isolation and answers {@code isCurrentUser(ownerUserId)}, which
     * hides every row from all but its creator, by-id and list alike. So filling {@code ownerUserId} is safe for
     * {@code Connection} precisely because {@code ConnectionVisibilityProvider} exists, and would be catastrophic for a
     * type with no provider.
     * <p>
     * It holds by construction today, which is exactly why it is asserted: nothing else would notice a provider being
     * moved, renamed or dropped while its ownership resolver kept filling an owner. A resolver that fills only the
     * workspace — {@code ProjectOwnershipResolver} deliberately does, via {@code ofWorkspace} — never reaches that
     * branch and is not subject to this. Populating is read off the factory the resolver calls, so a resolver that has
     * no owner to give should say {@code ofWorkspace} or {@code unknown} rather than
     * {@code of(…, OptionalLong.empty())}.
     */
    @Test
    void testEveryOwnerPopulatingResolverHasAMatchingVisibilityProvider() {
        Set<String> ownerPopulatingTypesWithoutProvider = new TreeSet<>();

        for (Map.Entry<String, String> entry : ownerPopulatingResolverTypes.entrySet()) {
            if (visibilityProviderTypes.containsKey(entry.getKey()) ||
                OWNER_ISOLATED_BY_DESIGN.contains(entry.getKey())) {

                continue;
            }

            ownerPopulatingTypesWithoutProvider.add(entry.getKey() + " (" + entry.getValue() + ")");
        }

        assertThat(ownerPopulatingTypesWithoutProvider)
            .as(
                "a resolver that fills ownerUserId for a type with no ResourceVisibilityProvider puts CE "
                    + "hasResourceScope on its owner-isolation branch, hiding every one of those resources from "
                    + "everyone but its creator. Registered providers: %s",
                new TreeSet<>(visibilityProviderTypes.keySet()))
            .isEmpty();
    }

    /**
     * A resolver Spring never instantiates is the same lockout as a resolver that does not exist: the constructor
     * collects {@code List<ResourceOwnershipResolver>} from the context, so an implementation with no stereotype is
     * simply absent from the registry. Should a resolver ever be registered by an explicit {@code @Bean} factory method
     * instead, relax this assertion for that class rather than dropping it.
     * <p>
     * The stereotype must sit on the declaration that implements the resolver interface, not merely somewhere in the
     * file. A file-wide search passes on any file that happens to contain an unrelated {@code @Component} — a nested
     * helper, a second class, or a resolver that lost its own annotation while a sibling kept one — which is precisely
     * the case worth catching.
     */
    @Test
    void testEveryResolverCarriesASpringStereotype() {
        Set<String> resolverFilesWithoutStereotype = new TreeSet<>();

        for (Path sourceFile : mainSourceFiles) {
            String source = MainSourceScan.readSource(sourceFile);

            if (!source.contains("Resolver") && !source.contains("Provider")) {
                continue;
            }

            String strippedSource = MainSourceScan.stripComments(source);

            for (Pattern declarationPattern : List.of(
                OWNERSHIP_RESOLVER_DECLARATION, ENVIRONMENT_RESOLVER_DECLARATION,
                VISIBILITY_PROVIDER_DECLARATION)) {

                Matcher declarationMatcher = declarationPattern.matcher(strippedSource);

                while (declarationMatcher.find()) {
                    if (!hasStereotypeOnDeclaringClass(strippedSource, declarationMatcher.start())) {
                        resolverFilesWithoutStereotype.add(MainSourceScan.fileName(sourceFile));
                    }
                }
            }
        }

        assertThat(resolverFilesWithoutStereotype)
            .as(
                "a resolver or visibility provider with no @Component/@Service on its own class declaration is never "
                    + "contributed to the registry, so its token behaves exactly as an unregistered one does")
            .isEmpty();
    }

    /**
     * Whether the class declaration containing {@code implementsIndex} carries a Spring stereotype. The declaration
     * header runs from the end of the preceding member — the last {@code ;} or {@code }} before the {@code class}
     * keyword, comments already stripped — up to that keyword, which is exactly the span holding the annotations and
     * modifiers of this class and of no other.
     */
    private static boolean hasStereotypeOnDeclaringClass(String strippedSource, int implementsIndex) {
        Matcher classKeywordMatcher = CLASS_KEYWORD.matcher(strippedSource.substring(0, implementsIndex));

        int classKeywordIndex = -1;

        while (classKeywordMatcher.find()) {
            classKeywordIndex = classKeywordMatcher.start();
        }

        if (classKeywordIndex < 0) {
            return false;
        }

        int headerStart = Math.max(
            strippedSource.lastIndexOf(';', classKeywordIndex), strippedSource.lastIndexOf('}', classKeywordIndex));

        String header = strippedSource.substring(headerStart + 1, classKeywordIndex);

        return SPRING_STEREOTYPE.matcher(header)
            .find();
    }

    @Test
    void testEveryPinnedUnresolvedTokenIsStillUnresolved() {
        Set<String> stalePins = new TreeSet<>();

        for (String token : KNOWN_UNRESOLVED_TOKENS.keySet()) {
            if (ownershipResolverTypes.containsKey(token)) {
                stalePins.add(token);
            }
        }

        assertThat(stalePins)
            .as(
                "these tokens now have a ResourceOwnershipResolver, so remove them from KNOWN_UNRESOLVED_TOKENS — "
                    + "otherwise the pin would hide a later regression on the very token it was added for")
            .isEmpty();
    }

    /**
     * The other direction a pin rots in. {@link #testEveryPinnedUnresolvedTokenIsStillUnresolved} catches the entry
     * whose resolver arrived; this one catches the entry whose recorded reason for existing went away. Without it, a
     * pinned token's guards could be deleted outright and the guard-side exemption would live on, ready to silently
     * re-admit the next guard someone writes for that token.
     */
    @Test
    void testEveryPinnedUnresolvedTokenStillHasEveryGroundItIsPinnedOn() {
        Map<String, Set<PinGrounds>> stalePinGrounds = new TreeMap<>();

        for (Map.Entry<String, Set<PinGrounds>> entry : KNOWN_UNRESOLVED_TOKENS.entrySet()) {
            String token = entry.getKey();

            Set<PinGrounds> staleGrounds = new TreeSet<>();

            for (PinGrounds grounds : entry.getValue()) {
                boolean stillHolds = switch (grounds) {
                    case NAMED_BY_A_GUARD -> tokensByGateFile.containsKey(token);
                    case CLAIMED_BY_AN_ENVIRONMENT_RESOLVER -> environmentResolverTypes.containsKey(token);
                };

                if (!stillHolds) {
                    staleGrounds.add(grounds);
                }
            }

            if (!staleGrounds.isEmpty()) {
                stalePinGrounds.put(token, staleGrounds);
            }
        }

        assertThat(stalePinGrounds)
            .as(
                "these KNOWN_UNRESOLVED_TOKENS entries are pinned on grounds that no longer hold, so the exemption "
                    + "outlived its reason — remove the entry rather than leaving it to re-admit the next guard "
                    + "silently. Stale grounds per token: %s. Tokens named by a guard: %s. Environment resolver "
                    + "discriminators: %s",
                stalePinGrounds, new TreeSet<>(tokensByGateFile.keySet()),
                new TreeSet<>(environmentResolverTypes.keySet()))
            .isEmpty();
    }

    /**
     * Why a token is exempt from {@link #testEveryGuardTokenHasARegisteredOwnershipResolver} and
     * {@link #testEveryEnvironmentResolverHasAMatchingOwnershipResolver}. Recorded per entry so an exemption cannot
     * outlive the thing it was granted for.
     */
    private enum PinGrounds {

        NAMED_BY_A_GUARD, CLAIMED_BY_AN_ENVIRONMENT_RESOLVER
    }

    /**
     * Resource tokens keyed by name and valued by the set of files whose {@code @PreAuthorize} annotations name them.
     */
    private static Map<String, Set<String>> collectTokensByGateFile(
        List<ResolvedGuardExpression> guardExpressions) {

        Map<String, Set<String>> tokensByFile = new TreeMap<>();

        for (ResolvedGuardExpression guardExpression : guardExpressions) {
            addTokens(tokensByFile, guardExpression, TOKEN_AS_SECOND_ARGUMENT);
            addTokens(tokensByFile, guardExpression, OWNER_TOKEN_AS_SECOND_ARGUMENT);
            addTokens(tokensByFile, guardExpression, OWNER_TOKEN_AS_FIRST_ARGUMENT);
        }

        return tokensByFile;
    }

    private static void addTokens(
        Map<String, Set<String>> tokensByFile, ResolvedGuardExpression guardExpression, Pattern pattern) {

        Matcher matcher = pattern.matcher(guardExpression.expression());

        while (matcher.find()) {
            Set<String> files = tokensByFile.computeIfAbsent(matcher.group(1), token -> new TreeSet<>());

            files.add(guardExpression.fileName());
        }
    }

    /**
     * The {@code resourceType()} discriminator of every class implementing the given resolver interface, keyed by
     * discriminator so a duplicate is visible as a collision rather than silently overwriting — the production
     * registries build a {@code Map} the same way and would throw on a duplicate key.
     */
    private static Map<String, String> collectResolverTypes(List<Path> sourceFiles, Pattern declarationPattern) {
        return collectResolverTypes(sourceFiles, declarationPattern, strippedSource -> true);
    }

    /**
     * As above, but counting only implementations whose source also satisfies {@code sourceRequirement} — used to pick
     * out the resolvers that populate an owner.
     */
    private static Map<String, String> collectResolverTypes(
        List<Path> sourceFiles, Pattern declarationPattern, Predicate<String> sourceRequirement) {

        Map<String, String> resolverTypes = new LinkedHashMap<>();

        for (Path sourceFile : sourceFiles) {
            String source = MainSourceScan.readSource(sourceFile);

            if (!source.contains("Resolver") && !source.contains("Provider")) {
                continue;
            }

            String strippedSource = MainSourceScan.stripComments(source);

            Matcher declarationMatcher = declarationPattern.matcher(strippedSource);

            if (!declarationMatcher.find()) {
                continue;
            }

            if (!sourceRequirement.test(strippedSource)) {
                continue;
            }

            Matcher resourceTypeMatcher = RESOURCE_TYPE_METHOD.matcher(strippedSource);

            while (resourceTypeMatcher.find()) {
                resolverTypes.put(resourceTypeMatcher.group(1), MainSourceScan.fileName(sourceFile));
            }
        }

        return resolverTypes;
    }
}
