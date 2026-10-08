# Connected-User Ownership Authorization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace `EmbeddedAutomationAuthorizationSkipFilter` with a single ownership decision for connected users, so the
1051 RBAC gates apply to the embedded surface.

**Architecture:**
- The embedded tokens carry the connected user and its environment.
- A `ConnectedUserAccessDecider` SPI answers every resource, workflow and workspace check for a connected user: GRANT
  only for their own project's resources in their environment, DENY otherwise. For anyone else it answers
  NOT_GOVERNED.
- The EE `PermissionServiceImpl` consults the decider before anything else. The expression root and evaluator stop
  reading the skip flag directly and ask `PermissionService.isAuthorizationSkipped()`.
- A LOG mode keeps today's behaviour while logging would-be denials. ENFORCE makes the decider authoritative, and the
  filter is deleted last.

**Tech Stack:** Java 25, Spring Boot 4 / Spring Security 7 method security, JUnit 5, Mockito, AssertJ, Testcontainers.

**Spec:** `docs/superpowers/specs/2026-09-26-connected-user-ownership-authorization-design.md`

## Global Constraints

- Follow `CLAUDE.md`:
  - EE license header and `@version ee` on new files under `server/ee/`.
  - A blank line before control statements and after a variable modification.
  - No method chaining outside the allowed DSLs, descriptive names.
  - Test classes end in `Test` / `IntTest`; camelCase test methods; no `Impl` in test names.
  - Use `@ExtendWith(ObjectMapperSetupExtension.class)` where JsonUtils is used.
- Commits: one per task, subject-only, `1051 <imperative description>`. No body, no `Co-Authored-By`, no "Generated
  with". Commit by explicit path.
- No new explanatory code comments. A one-line factual Javadoc on a new public API type or method is allowed.
- Gradle: prefix `export SDKMAN_DIR="$HOME/.sdkman"; source "$SDKMAN_DIR/bin/sdkman-init.sh" >/dev/null 2>&1;`, use
  `--configure-on-demand`, run one Gradle build at a time, and scope tests with `--tests`. For Testcontainers also
  `export DOCKER_HOST="unix://$HOME/.orbstack/run/docker.sock"; export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock`
  and `--no-daemon`. Run `:module:spotlessApply` on each touched module before committing.
- Every regression test is proven: revert the production change, see the test FAIL, restore, see it PASS.
- A new `bytechef.*` property must be a field in `server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java`
  (strict binding: `ignoreUnknownFields = false`).

## Review Focus

1. **External id equal to a platform login.** A connected user whose external id equals a platform user's login must
   get the decider's answer, never that user's workspace scopes. Test in Task 4:
   `testGovernedPrincipalNeverReachesWorkspaceScopes`.
2. **The same JWT sent with a different `X-ENVIRONMENT`.** It must be refused, not treated as another auto-created
   connected user. Test in Task 1: `testJwtEnvironmentClaimWinsAndMismatchingHeaderIsRefused`.
3. **Builder sends `environmentId`=DEVELOPMENT while the principal is PRODUCTION.** The connected user must still
   reach their own PRODUCTION project resources. Test in Task 3: `testRequestedEnvironmentIsIgnoredForOwnProject`.
4. **The decider's lookup throws** (database error, missing test job). It must deny, not return a 500. Test in Task 3:
   `testLookupFailureDenies`.
5. **A connected user with no connected-user project yet in this environment.** Every resource check must DENY, and
   the decider must never create one. Test in Task 3: `testNoConnectedUserProjectDenies`.

---

## File Structure

| File | Responsibility |
|---|---|
| `server/libs/platform/platform-security-web/platform-security-web-api/src/main/java/com/bytechef/platform/security/web/authentication/ConnectedUserAuthentication.java` (new) | Interface every connected-user token implements: `connectedUserId()`, `externalUserId()`, `environmentId()` |
| `…/platform-security-web-api/…/authentication/ConnectedUserAuthentications.java` (new) | `Optional<ConnectedUserAuthentication> fetchCurrent()` from the SecurityContext |
| `…/platform-security-web-api/…/authentication/AbstractApiKeyAuthenticationToken.java` (modify) | new `(long environmentId, User user)` constructor that keeps the environment |
| `server/ee/libs/embedded/embedded-security-web/embedded-security-web-impl/…/authentication/EmbeddedApiKeyAuthenticationToken.java` (modify) | implements `ConnectedUserAuthentication`; authenticated constructor carries env + connected user |
| `…/embedded-security-web-impl/…/authentication/EmbeddedApiKeyAuthenticationProvider.java` (modify) | builds the authenticated token with env + connected user |
| `…/embedded-security-web-impl/…/configurer/EmbeddedApiKeyAuthenticationConverter.java` (modify) | JWT `environmentId` claim authoritative when present |
| `server/ee/libs/embedded/embedded-ai/embedded-ai-mcp-server/…/authentication/EmbeddedMcpServerApiKeyAuthentication{Token,Provider}.java` (modify) | same as the embedded token/provider |
| `server/libs/automation/automation-configuration/automation-configuration-api/…/security/ResourceOwnershipResolver.java` (modify) | `default OptionalLong resolveProjectId(Serializable id)` |
| 7 ownership resolvers (modify) | override `resolveProjectId`: Project, ProjectWorkflow, ProjectDeployment, ProjectDeploymentWorkflow, TestJob (automation-configuration-service `…/security/`), Job, TriggerExecution (automation-workflow-execution-service `…/security/`) |
| `…/automation-configuration-api/…/security/ConnectedUserAccessDecider.java` (new) | SPI + `Decision` enum |
| `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/…/security/ConnectedUserAccessDeciderImpl.java` (new) | the ownership rules |
| `…/automation-configuration-api/…/service/PermissionService.java` (modify) | `boolean isAuthorizationSkipped()` |
| CE `PermissionServiceImpl`, EE `PermissionServiceImpl`, `RemotePermissionServiceClient` (modify) | implement it; EE consults the decider first |
| `…/automation-configuration-service/…/security/AutomationMethodSecurityExpressionRoot.java`, `AutomationPermissionEvaluator.java` (modify) | `permissionService.isAuthorizationSkipped()` instead of `AutomationAuthorizationContext.isSkipChecks()` |
| `ApplicationProperties.java` (modify) | `security.connectedUserAuthorizationMode` = LOG / ENFORCE |
| `EmbeddedAutomationAuthorizationSkipFilter.java`, `@SkipAutomationAuthorization` on connected-user facades, copilot skips (delete/modify, Task 8) | removal |

---

### Task 1: The connected-user principal carries its connected user and environment

**Files:**
- Create: `server/libs/platform/platform-security-web/platform-security-web-api/src/main/java/com/bytechef/platform/security/web/authentication/ConnectedUserAuthentication.java`
- Create: `server/libs/platform/platform-security-web/platform-security-web-api/src/main/java/com/bytechef/platform/security/web/authentication/ConnectedUserAuthentications.java`
- Modify: `…/platform-security-web-api/…/authentication/AbstractApiKeyAuthenticationToken.java`
- Modify: `server/ee/libs/embedded/embedded-security-web/embedded-security-web-impl/src/main/java/com/bytechef/ee/embedded/security/web/authentication/EmbeddedApiKeyAuthenticationToken.java`, `EmbeddedApiKeyAuthenticationProvider.java`, `…/configurer/EmbeddedApiKeyAuthenticationConverter.java`
- Modify: `server/ee/libs/embedded/embedded-ai/embedded-ai-mcp-server/src/main/java/com/bytechef/ee/embedded/ai/mcp/server/security/web/authentication/EmbeddedMcpServerApiKeyAuthenticationToken.java`, `EmbeddedMcpServerApiKeyAuthenticationProvider.java`
- Test: `…/embedded-security-web-impl/src/test/java/com/bytechef/ee/embedded/security/web/authentication/EmbeddedApiKeyAuthenticationProviderTest.java`, `…/configurer/EmbeddedApiKeyAuthenticationConverterTest.java`, `…/embedded-ai-mcp-server/src/test/java/…/EmbeddedMcpServerApiKeyAuthenticationProviderTest.java`, `…/platform-security-web-api/src/test/java/…/ConnectedUserAuthenticationsTest.java`

**Interfaces:**
- Produces: `ConnectedUserAuthentication { long connectedUserId(); String externalUserId(); long environmentId(); }`
  and `ConnectedUserAuthentications.fetchCurrent(): Optional<ConnectedUserAuthentication>`. Tasks 3, 4 and 8 use them.

- [ ] **Step 1: Write the failing tests**

```java
// EmbeddedApiKeyAuthenticationProviderTest
@Test
void testAuthenticatedTokenCarriesConnectedUserAndEnvironment() {
    ConnectedUser connectedUser = new ConnectedUser();

    connectedUser.setId(42L);
    connectedUser.setExternalId("ext-1");
    connectedUser.setEnabled(true);

    when(apiKeyService.exists("secret", 2L, PlatformType.EMBEDDED)).thenReturn(true);
    when(connectedUserService.fetchConnectedUser("ext-1", 2L)).thenReturn(Optional.of(connectedUser));

    Authentication authentication = provider.authenticate(
        new EmbeddedApiKeyAuthenticationToken(2L, "ext-1", "secret", "public"));

    assertThat(authentication).isInstanceOf(ConnectedUserAuthentication.class);

    ConnectedUserAuthentication connectedUserAuthentication = (ConnectedUserAuthentication) authentication;

    assertThat(connectedUserAuthentication.connectedUserId()).isEqualTo(42L);
    assertThat(connectedUserAuthentication.externalUserId()).isEqualTo("ext-1");
    assertThat(connectedUserAuthentication.environmentId()).isEqualTo(2L);
}

// EmbeddedApiKeyAuthenticationConverterTest (Review Focus 2)
@Test
void testJwtEnvironmentClaimWinsAndMismatchingHeaderIsRefused() {
    String jwt = issueBuilderJwt("ext-1", /* environmentId claim */ 0);

    MockHttpServletRequest noHeader = requestWithBearer(jwt, null);

    assertThat(((EmbeddedApiKeyAuthenticationToken) converter.convert(noHeader)).getEnvironmentId()).isEqualTo(0L);

    MockHttpServletRequest matching = requestWithBearer(jwt, "DEVELOPMENT");

    assertThat(((EmbeddedApiKeyAuthenticationToken) converter.convert(matching)).getEnvironmentId()).isEqualTo(0L);

    MockHttpServletRequest mismatching = requestWithBearer(jwt, "PRODUCTION");

    assertThatThrownBy(() -> converter.convert(mismatching)).isInstanceOf(BadCredentialsException.class);
}

@Test
void testCustomerSignedJwtWithoutClaimKeepsHeaderEnvironment() {
    String jwt = issueCustomerJwt("ext-1");

    MockHttpServletRequest staging = requestWithBearer(jwt, "STAGING");

    assertThat(((EmbeddedApiKeyAuthenticationToken) converter.convert(staging)).getEnvironmentId()).isEqualTo(1L);
}

// ConnectedUserAuthenticationsTest
@Test
void testFetchCurrentReturnsEmptyForPlatformUser() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("admin", "", List.of()));

    assertThat(ConnectedUserAuthentications.fetchCurrent()).isEmpty();
}
```

`issueBuilderJwt` signs with the key `JwtTokenService.getPublicKey(kid)` returns and includes the `environmentId`
claim. `issueCustomerJwt` signs with a per-environment `SigningKeyService` key and has no claim. Build both with the
test key pair helpers already used by the existing converter test (read it first). Add the equivalent provider test
for `EmbeddedMcpServerApiKeyAuthenticationProvider`.

- [ ] **Step 2: Run the tests and verify they fail**

Run: `./gradlew --configure-on-demand :server:ee:libs:embedded:embedded-security-web:embedded-security-web-impl:test --tests "*EmbeddedApiKeyAuthentication*"`
Expected: FAIL. Compilation fails because `ConnectedUserAuthentication` does not exist.

- [ ] **Step 3: Implement**

```java
// ConnectedUserAuthentication.java (platform-security-web-api)
public interface ConnectedUserAuthentication {

    long connectedUserId();

    String externalUserId();

    long environmentId();
}

// ConnectedUserAuthentications.java
public final class ConnectedUserAuthentications {

    private ConnectedUserAuthentications() {
    }

    public static Optional<ConnectedUserAuthentication> fetchCurrent() {
        SecurityContext securityContext = SecurityContextHolder.getContext();

        if (securityContext.getAuthentication() instanceof ConnectedUserAuthentication connectedUserAuthentication) {
            return Optional.of(connectedUserAuthentication);
        }

        return Optional.empty();
    }
}

// AbstractApiKeyAuthenticationToken: add
@SuppressFBWarnings("EI")
public AbstractApiKeyAuthenticationToken(long environmentId, User user) {
    this(user);

    this.environmentId = environmentId;
}

// EmbeddedApiKeyAuthenticationToken: implements ConnectedUserAuthentication; add field + constructor
private long connectedUserId;

@SuppressFBWarnings("EI")
public EmbeddedApiKeyAuthenticationToken(long environmentId, long connectedUserId, User user) {
    super(environmentId, user);

    this.connectedUserId = connectedUserId;
    this.externalUserId = user.getUsername();
}

@Override
public long connectedUserId() {
    return connectedUserId;
}

@Override
public String externalUserId() {
    return externalUserId;
}

@Override
public long environmentId() {
    return getEnvironmentId();
}

// EmbeddedApiKeyAuthenticationProvider.authenticate: last line becomes
return new EmbeddedApiKeyAuthenticationToken(
    environmentId, connectedUser.getId(), createSpringSecurityUser(externalUserId, connectedUser));
```

Remove the old `EmbeddedApiKeyAuthenticationToken(User)` constructor once no caller remains; grep for callers first.

In `EmbeddedApiKeyAuthenticationConverter.convert`, JWT branch, after parsing:

```java
Claims payload = jws.getPayload();
Integer claimedEnvironmentId = payload.get("environmentId", Integer.class);
long environmentId = environment.ordinal();

if (claimedEnvironmentId != null) {
    String environmentHeader = request.getHeader("X-ENVIRONMENT");

    if (StringUtils.isNotBlank(environmentHeader) && environment.ordinal() != claimedEnvironmentId) {
        throw new BadCredentialsException("X-ENVIRONMENT does not match the token's environment");
    }

    environmentId = claimedEnvironmentId;
}
```

Pass `environmentId` into the token. The ByteChef-issued key comes from `jwtTokenService.getPublicKey(kid)`, which is
not environment-bound, so parsing does not depend on the header. Customer keys are looked up per environment, so a
customer JWT stays bound by its key.

Mirror the token and provider changes in `EmbeddedMcpServerApiKeyAuthenticationToken` / `…Provider`. That token
implements `ConnectedUserAuthentication` the same way.

- [ ] **Step 4: Run the tests and verify they pass**

Run the Step 2 command, then `:server:ee:libs:embedded:embedded-ai:embedded-ai-mcp-server:test --tests "*ApiKeyAuthenticationProvider*"`
and `:server:libs:platform:platform-security-web:platform-security-web-api:test --tests "*ConnectedUserAuthentications*"`.
Expected: PASS. Then run the full `test` of both embedded modules (the token constructor changed).

- [ ] **Step 5: Commit**

```bash
git add <the files above>
git commit -m "1051 Carry the connected user and its environment on the embedded authentication tokens" -- <the files above>
```

---

### Task 2: Ownership resolvers answer the owning project

**Files:**
- Modify: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/security/ResourceOwnershipResolver.java`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/security/{ProjectOwnershipResolver,ProjectWorkflowOwnershipResolver,ProjectDeploymentOwnershipResolver,ProjectDeploymentWorkflowOwnershipResolver,TestJobOwnershipResolver}.java`
- Modify: `server/libs/automation/automation-workflow/automation-workflow-execution/automation-workflow-execution-service/src/main/java/com/bytechef/automation/workflow/execution/security/{JobOwnershipResolver,TriggerExecutionOwnershipResolver}.java`
- Test: the existing `*OwnershipResolverTest` next to each (add a case per resolver)

**Interfaces:**
- Produces: `ResourceOwnershipResolver#resolveProjectId(Serializable id): OptionalLong`. Empty means "no project or
  not found". Task 3 uses it.

- [ ] **Step 1: Write the failing tests** (one per resolver, same shape)

```java
// TestJobOwnershipResolverTest
@Test
void testResolveProjectIdOfATestJob() {
    Project project = new Project();

    project.setId(7L);
    project.setWorkspaceId(3L);

    when(testJobRegistry.fetchTestJob(11L)).thenReturn(Optional.of(new TestJob("wf-uuid", 0)));
    when(projectService.fetchWorkflowProject("wf-uuid")).thenReturn(Optional.of(project));

    assertThat(resolver.resolveProjectId(11L)).hasValue(7L);
}

@Test
void testResolveProjectIdOfAnUnknownTestJobIsEmpty() {
    when(testJobRegistry.fetchTestJob(12L)).thenReturn(Optional.empty());

    assertThat(resolver.resolveProjectId(12L)).isEmpty();
}
```

Match the `TestJob` record's real constructor by reading it. Write the same two cases for Project (id → itself if it
exists), ProjectWorkflow, ProjectDeployment, ProjectDeploymentWorkflow, Job and TriggerExecution. Stub the
repositories and services each resolver already uses.

- [ ] **Step 2: Run and verify they fail**

Run: `./gradlew --configure-on-demand :server:libs:automation:automation-configuration:automation-configuration-service:test --tests "*OwnershipResolverTest"`
Expected: FAIL (compile error: `resolveProjectId` does not exist).

- [ ] **Step 3: Implement**

```java
// ResourceOwnershipResolver: add
default OptionalLong resolveProjectId(Serializable id) {
    return OptionalLong.empty();
}
```

In each of the 7 resolvers, extract the existing `…→ Optional<Project>` chain into a private `fetchProject(long id)`,
then:

```java
@Override
public ResourceOwner resolveOwner(long id) {
    return fetchProject(id)
        .map(Project::getWorkspaceId)
        .map(ResourceOwner::ofWorkspace)
        .orElseGet(ResourceOwner::unknown);
}

@Override
public OptionalLong resolveProjectId(Serializable id) {
    if (!(id instanceof Number number)) {
        return OptionalLong.empty();
    }

    return fetchProject(number.longValue())
        .map(project -> OptionalLong.of(project.getId()))
        .orElseGet(OptionalLong::empty);
}
```

`JobOwnershipResolver` already has `fetchProject(String workflowId)`; reuse it after `jobService.fetchJob(id)`. Keep
every existing log and exception behaviour unchanged.

- [ ] **Step 4: Run and verify they pass**

Run the Step 2 command, then `:server:libs:automation:automation-workflow:automation-workflow-execution:automation-workflow-execution-service:test --tests "*OwnershipResolverTest"`.
Expected: PASS. Then run `ResourceTokenResolverCoverageTest` with `--rerun`.

- [ ] **Step 5: Commit**

```bash
git commit -m "1051 Let the project-bearing ownership resolvers answer the owning project" -- <files>
```

---

### Task 3: The connected-user access decider

**Files:**
- Create: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/security/ConnectedUserAccessDecider.java`
- Create: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/security/ConnectedUserAccessDeciderImpl.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/build.gradle.kts` (add `platform-security-web-api` if missing)
- Test: `…/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/security/ConnectedUserAccessDeciderTest.java`

**Interfaces:**
- Consumes: `ConnectedUserAuthentications.fetchCurrent()` (Task 1), `ResourceOwnershipResolver#resolveProjectId` (Task 2),
  `ConnectedUserProjectService.fetchConnectUserProject(String externalUserId, Environment)`,
  `ProjectService.fetchWorkflowProject(String)`, `ConnectedUserConnectionService.getConnectionIds(long)`,
  `IntegrationInstanceService.getConnectedUserIntegrationInstances(long, Environment)`,
  `AutomationWorkflowProjectFacade.getPublishedProjects(String, Environment)`.
- Produces:

```java
public interface ConnectedUserAccessDecider {

    enum Decision {
        NOT_GOVERNED, GRANT, DENY
    }

    Decision decide(Serializable id, String resourceType, String scope);

    Decision decideWorkflow(String workflowId, String scope);

    Decision decideWorkspace(long workspaceId, String scope);
}
```

- [ ] **Step 1: Write the failing tests**

```java
@ExtendWith(MockitoExtension.class)
class ConnectedUserAccessDeciderTest {

    private static final long OWN_PROJECT_ID = 100L;

    // mocks for ConnectedUserProjectService, ProjectService, ConnectedUserConnectionService,
    // IntegrationInstanceService, AutomationWorkflowProjectFacade; resolvers passed as a List

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext()
            .setAuthentication(connectedUserToken(42L, "ext-1", Environment.PRODUCTION.ordinal()));

        ConnectedUserProject connectedUserProject = new ConnectedUserProject();

        connectedUserProject.setProjectId(OWN_PROJECT_ID);

        lenient().when(connectedUserProjectService.fetchConnectUserProject("ext-1", Environment.PRODUCTION))
            .thenReturn(Optional.of(connectedUserProject));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testPlatformUserIsNotGoverned() {
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken("admin", "", List.of()));

        assertThat(decider.decide(1L, "Project", "WORKFLOW_EDIT")).isEqualTo(Decision.NOT_GOVERNED);
    }

    @Test
    void testOwnProjectResourceIsGrantedForAnyScope() {
        when(projectDeploymentResolver.resolveProjectId(5L)).thenReturn(OptionalLong.of(OWN_PROJECT_ID));

        assertThat(decider.decide(5L, "ProjectDeployment", "DEPLOYMENT_EDIT")).isEqualTo(Decision.GRANT);
    }

    @Test
    void testAnotherProjectsResourceIsDenied() {
        when(projectDeploymentResolver.resolveProjectId(6L)).thenReturn(OptionalLong.of(999L));

        assertThat(decider.decide(6L, "ProjectDeployment", "DEPLOYMENT_VIEW")).isEqualTo(Decision.DENY);
    }

    @Test
    void testWorkspaceLevelChecksAreDenied() {
        assertThat(decider.decideWorkspace(1049L, "WORKFLOW_VIEW")).isEqualTo(Decision.DENY);
        assertThat(decider.decide(3L, "DataTable", "DATA_TABLE_VIEW")).isEqualTo(Decision.DENY);
    }

    @Test
    void testUnknownResourceTypeIsDenied() {
        assertThat(decider.decide(3L, "Unheard", "X")).isEqualTo(Decision.DENY);
    }

    @Test
    void testWorkflowOfOwnProjectIsGranted() {
        Project project = new Project();

        project.setId(OWN_PROJECT_ID);

        when(projectService.fetchWorkflowProject("wf-own")).thenReturn(Optional.of(project));

        assertThat(decider.decideWorkflow("wf-own", "WORKFLOW_EDIT")).isEqualTo(Decision.GRANT);
    }

    @Test
    void testIntegrationWorkflowWithoutProjectIsDenied() {
        when(projectService.fetchWorkflowProject("wf-integration")).thenReturn(Optional.empty());

        assertThat(decider.decideWorkflow("wf-integration", "WORKFLOW_EDIT")).isEqualTo(Decision.DENY);
    }

    @Test
    void testPublishedTemplateWorkflowIsViewOnly() {
        Project template = new Project();

        template.setId(200L);

        when(projectService.fetchWorkflowProject("wf-template")).thenReturn(Optional.of(template));
        when(automationWorkflowProjectFacade.getPublishedProjects("ext-1", Environment.PRODUCTION))
            .thenReturn(List.of(templateDto(200L)));

        assertThat(decider.decideWorkflow("wf-template", "WORKFLOW_VIEW")).isEqualTo(Decision.GRANT);
        assertThat(decider.decideWorkflow("wf-template", "WORKFLOW_EDIT")).isEqualTo(Decision.DENY);
    }

    @Test
    void testOwnConnectionIsGranted() {
        when(connectedUserConnectionService.getConnectionIds(42L)).thenReturn(List.of(8L));

        assertThat(decider.decide(8L, "Connection", "CONNECTION_VIEW")).isEqualTo(Decision.GRANT);
        assertThat(decider.decide(9L, "Connection", "CONNECTION_VIEW")).isEqualTo(Decision.DENY);
    }

    @Test
    void testRequestedEnvironmentIsIgnoredForOwnProject() {
        Project project = new Project();

        project.setId(OWN_PROJECT_ID);

        when(projectService.fetchWorkflowProject("wf-own")).thenReturn(Optional.of(project));

        assertThat(decider.decideWorkflow("wf-own", "WORKFLOW_EDIT")).isEqualTo(Decision.GRANT);
        verify(connectedUserProjectService).fetchConnectUserProject("ext-1", Environment.PRODUCTION);
    }

    @Test
    void testNoConnectedUserProjectDenies() {
        when(connectedUserProjectService.fetchConnectUserProject("ext-1", Environment.PRODUCTION))
            .thenReturn(Optional.empty());
        lenient().when(projectResolver.resolveProjectId(OWN_PROJECT_ID)).thenReturn(OptionalLong.of(OWN_PROJECT_ID));

        assertThat(decider.decide(OWN_PROJECT_ID, "Project", "WORKFLOW_EDIT")).isEqualTo(Decision.DENY);
        verify(connectedUserProjectService, never()).getConnectUserProject(anyString(), any());
    }

    @Test
    void testLookupFailureDenies() {
        when(projectDeploymentResolver.resolveProjectId(5L)).thenThrow(new IllegalStateException("db down"));

        assertThat(decider.decide(5L, "ProjectDeployment", "DEPLOYMENT_VIEW")).isEqualTo(Decision.DENY);
    }
}
```

`connectedUserToken(...)` builds an `EmbeddedApiKeyAuthenticationToken(environmentId, connectedUserId, user)`. If
that class is not on this module's test classpath, use a tiny test-only `ConnectedUserAuthentication` implementation
extending `AbstractAuthenticationToken`. Resolver mocks return their `resourceType()` (`"Project"`,
`"ProjectDeployment"`, …) so the decider can index them.

- [ ] **Step 2: Run and verify they fail**

Run: `./gradlew --configure-on-demand :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests "*ConnectedUserAccessDeciderTest"`
Expected: FAIL (compile error: types missing).

- [ ] **Step 3: Implement the SPI (code above) and the EE implementation**

```java
@Component
public class ConnectedUserAccessDeciderImpl implements ConnectedUserAccessDecider {

    private static final Logger log = LoggerFactory.getLogger(ConnectedUserAccessDeciderImpl.class);

    private static final Set<String> PROJECT_BEARING_TYPES = Set.of(
        "Project", "ProjectWorkflow", "ProjectDeployment", "ProjectDeploymentWorkflow", "Job", "TestJob",
        "TriggerExecution");

    private static final String CONNECTION = "Connection";
    private static final String WORKFLOW_VIEW = "WORKFLOW_VIEW";

    private final AutomationWorkflowProjectFacade automationWorkflowProjectFacade;
    private final ConnectedUserConnectionService connectedUserConnectionService;
    private final ConnectedUserProjectService connectedUserProjectService;
    private final IntegrationInstanceService integrationInstanceService;
    private final ProjectService projectService;
    private final Map<String, ResourceOwnershipResolver> resourceOwnershipResolvers;

    // constructor: index resolvers by resourceType(), like EE PermissionServiceImpl does

    @Override
    public Decision decide(Serializable id, String resourceType, String scope) {
        Optional<ConnectedUserAuthentication> connectedUser = ConnectedUserAuthentications.fetchCurrent();

        if (connectedUser.isEmpty()) {
            return Decision.NOT_GOVERNED;
        }

        try {
            return decideForConnectedUser(connectedUser.get(), id, resourceType);
        } catch (RuntimeException exception) {
            log.error("Denying {} {} for a connected user: the ownership lookup failed", resourceType, id, exception);

            return Decision.DENY;
        }
    }

    @Override
    public Decision decideWorkflow(String workflowId, String scope) {
        Optional<ConnectedUserAuthentication> connectedUser = ConnectedUserAuthentications.fetchCurrent();

        if (connectedUser.isEmpty()) {
            return Decision.NOT_GOVERNED;
        }

        try {
            Optional<Project> project = projectService.fetchWorkflowProject(workflowId);

            if (project.isEmpty()) {
                return Decision.DENY;
            }

            long projectId = project.get()
                .getId();

            if (isOwnProject(connectedUser.get(), projectId)) {
                return Decision.GRANT;
            }

            if (WORKFLOW_VIEW.equals(scope) && isPublishedTemplate(connectedUser.get(), projectId)) {
                return Decision.GRANT;
            }

            return Decision.DENY;
        } catch (RuntimeException exception) {
            log.error("Denying workflow {} for a connected user: the ownership lookup failed", workflowId, exception);

            return Decision.DENY;
        }
    }

    @Override
    public Decision decideWorkspace(long workspaceId, String scope) {
        return ConnectedUserAuthentications.fetchCurrent()
            .map(connectedUser -> Decision.DENY)
            .orElse(Decision.NOT_GOVERNED);
    }

    private Decision decideForConnectedUser(
        ConnectedUserAuthentication connectedUser, Serializable id, String resourceType) {

        if (CONNECTION.equals(resourceType)) {
            return ownsConnection(connectedUser, id) ? Decision.GRANT : Decision.DENY;
        }

        if (!PROJECT_BEARING_TYPES.contains(resourceType)) {
            return Decision.DENY;
        }

        ResourceOwnershipResolver resolver = resourceOwnershipResolvers.get(resourceType);

        if (resolver == null) {
            return Decision.DENY;
        }

        OptionalLong projectId = resolver.resolveProjectId(id);

        if (projectId.isEmpty()) {
            return Decision.DENY;
        }

        return isOwnProject(connectedUser, projectId.getAsLong()) ? Decision.GRANT : Decision.DENY;
    }

    private boolean isOwnProject(ConnectedUserAuthentication connectedUser, long projectId) {
        Environment environment = Environment.values()[(int) connectedUser.environmentId()];

        return connectedUserProjectService.fetchConnectUserProject(connectedUser.externalUserId(), environment)
            .map(connectedUserProject -> connectedUserProject.getProjectId() == projectId)
            .orElse(false);
    }

    private boolean isPublishedTemplate(ConnectedUserAuthentication connectedUser, long projectId) {
        Environment environment = Environment.values()[(int) connectedUser.environmentId()];

        return automationWorkflowProjectFacade.getPublishedProjects(connectedUser.externalUserId(), environment)
            .stream()
            .anyMatch(publishedProject -> publishedProject.id() == projectId);
    }

    private boolean ownsConnection(ConnectedUserAuthentication connectedUser, Serializable id) {
        if (!(id instanceof Number number)) {
            return false;
        }

        long connectionId = number.longValue();
        List<Long> connectionIds = connectedUserConnectionService.getConnectionIds(connectedUser.connectedUserId());

        if (connectionIds.contains(connectionId)) {
            return true;
        }

        Environment environment = Environment.values()[(int) connectedUser.environmentId()];

        return integrationInstanceService
            .getConnectedUserIntegrationInstances(connectedUser.connectedUserId(), environment)
            .stream()
            .anyMatch(integrationInstance -> Objects.equals(integrationInstance.getConnectionId(), connectionId));
    }
}
```

Adapt the accessor names (`ConnectedUserProject#getProjectId`, the published-project DTO's id accessor,
`IntegrationInstance#getConnectionId`) to the real classes. Memoise the connected-user project lookup per request with
a `@RequestScope` helper, or with a small cache keyed by `(externalUserId, environment)` held in a request attribute
through `RequestContextHolder`, whichever the module already uses. If neither pattern exists, skip memoisation and
note it.

Embedded MCP servers and tools: in `decideForConnectedUser`, before the project-bearing check, grant
`"McpServer"` / `"McpTool"` with scope `MCP_VIEW` iff the server (for a tool, its component's server) has
`PlatformType.EMBEDDED`, and deny every other scope. `ConnectedUserIntegrationFacadeImpl` reads these through
gated services while listing integrations the user has not enabled yet, so a reachability rule would break the
connect dialog. Look the type up through `McpServerService` / `McpToolService` / `McpComponentService`. Add
`testEmbeddedMcpServerIsViewOnly` (EMBEDDED server, `MCP_VIEW` → GRANT, `MCP_EDIT` → DENY; AUTOMATION server
→ DENY) to Step 1.

- [ ] **Step 4: Run and verify they pass**

Run the Step 2 command. Expected: PASS (14 tests).

- [ ] **Step 5: Commit**

```bash
git commit -m "1051 Decide a connected user's access by ownership of their own project" -- <files>
```

---

### Task 4: Consult the decider first in the permission service (LOG mode)

**Files:**
- Modify: `server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java` (`Security`: add `connectedUserAuthorizationMode`, enum `ConnectedUserAuthorizationMode { LOG, ENFORCE }`, default `LOG`, getter/setter)
- Modify: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/service/PermissionService.java` (add `boolean isAuthorizationSkipped();`)
- Modify: CE `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/service/PermissionServiceImpl.java` (return `AutomationAuthorizationContext.isSkipChecks()`)
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-remote-client/src/main/java/com/bytechef/ee/automation/configuration/remote/client/service/RemotePermissionServiceClient.java` (throw `UnsupportedOperationException`, like its siblings)
- Modify: EE `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/service/PermissionServiceImpl.java`
- Modify: `…/automation-configuration-service/…/security/AutomationMethodSecurityExpressionRoot.java`, `AutomationPermissionEvaluator.java`
- Test: EE `…/service/PermissionServiceConnectedUserTest.java` (new), `AutomationMethodSecurityExpressionRootTest.java` and `AutomationPermissionEvaluator` test (extend)

**Interfaces:**
- Consumes: `ConnectedUserAccessDecider` (Task 3) via `ObjectProvider<ConnectedUserAccessDecider>` (absent in CE and
  in non-embedded EE apps), `ApplicationProperties.Security#getConnectedUserAuthorizationMode()`.
- Produces: `PermissionService#isAuthorizationSkipped()`. Task 8 relies on the precedence rule: the decider always
  runs before skip.

- [ ] **Step 1: Write the failing tests**

```java
// PermissionServiceConnectedUserTest (EE)
@Test
void testLogModeKeepsSkipBehaviourAndLogsTheWouldBeDenial() throws Throwable {
    givenMode(ConnectedUserAuthorizationMode.LOG);
    when(decider.decide(5L, "ProjectDeployment", "DEPLOYMENT_EDIT")).thenReturn(Decision.DENY);

    boolean allowed = AutomationAuthorizationContext.callSkippingChecks(
        () -> permissionService.hasResourceScope(5L, "ProjectDeployment", "DEPLOYMENT_EDIT"));

    assertThat(allowed).isTrue();
    assertThat(logAppender.messages()).anyMatch(message -> message.contains("would deny"));
}

@Test
void testEnforceModeReturnsTheDecision() throws Throwable {
    givenMode(ConnectedUserAuthorizationMode.ENFORCE);
    when(decider.decide(5L, "ProjectDeployment", "DEPLOYMENT_EDIT")).thenReturn(Decision.DENY);

    boolean allowed = AutomationAuthorizationContext.callSkippingChecks(
        () -> permissionService.hasResourceScope(5L, "ProjectDeployment", "DEPLOYMENT_EDIT"));

    assertThat(allowed).isFalse();
}

@Test
void testNotGovernedFallsThroughToTodaysLogic() {
    givenMode(ConnectedUserAuthorizationMode.ENFORCE);
    when(decider.decideWorkspace(1L, "WORKFLOW_VIEW")).thenReturn(Decision.NOT_GOVERNED);
    when(workspaceScopeCacheService.getWorkspaceScopes(anyLong(), eq(1L))).thenReturn(Set.of("WORKFLOW_VIEW"));

    assertThat(permissionService.hasWorkspaceScope(1L, "WORKFLOW_VIEW")).isTrue();
}

@Test
void testGovernedPrincipalNeverReachesWorkspaceScopes() {
    givenMode(ConnectedUserAuthorizationMode.ENFORCE);
    when(decider.decideWorkspace(1L, "WORKFLOW_VIEW")).thenReturn(Decision.DENY);

    assertThat(permissionService.hasWorkspaceScope(1L, "WORKFLOW_VIEW")).isFalse();
    verifyNoInteractions(currentUserResolver, workspaceScopeCacheService);
}

@Test
void testIsAuthorizationSkippedIsFalseForAGovernedPrincipalEvenUnderSkip() throws Throwable {
    givenGovernedPrincipal();

    assertThat(AutomationAuthorizationContext.callSkippingChecks(permissionService::isAuthorizationSkipped)).isFalse();
}
```

Match the real `getWorkspaceScopes` signature of `WorkspaceScopeCacheService`. `givenGovernedPrincipal()` makes
`decider.decideWorkspace(anyLong(), anyString())` return DENY; `isAuthorizationSkipped` asks
`decideWorkspace(0L, "")` to learn whether the principal is governed. Extend the expression-root and evaluator tests
with one case each: under `callSkippingChecks` and a mocked `permissionService.isAuthorizationSkipped()` = false, the
root delegates to the permission service instead of returning `true`.

- [ ] **Step 2: Run and verify they fail**

Run: `./gradlew --configure-on-demand :server:ee:libs:automation:automation-configuration:automation-configuration-service:test --tests "*PermissionServiceConnectedUserTest"`
Expected: FAIL (compile error: missing method and enum).

- [ ] **Step 3: Implement**

EE `PermissionServiceImpl`: add constructor parameters `ObjectProvider<ConnectedUserAccessDecider>
connectedUserAccessDeciderProvider` and `ApplicationProperties applicationProperties`, plus:

```java
private final Set<String> loggedWouldBeDenials = ConcurrentHashMap.newKeySet();

@Override
public boolean isAuthorizationSkipped() {
    if (isGovernedPrincipal()) {
        return false;
    }

    return AutomationAuthorizationContext.isSkipChecks();
}

private boolean isGovernedPrincipal() {
    ConnectedUserAccessDecider decider = connectedUserAccessDeciderProvider.getIfAvailable();

    if (decider == null) {
        return false;
    }

    return decider.decideWorkspace(0L, "") != Decision.NOT_GOVERNED;
}

private Optional<Boolean> decideForConnectedUser(
    Function<ConnectedUserAccessDecider, Decision> decision, String subject) {

    ConnectedUserAccessDecider decider = connectedUserAccessDeciderProvider.getIfAvailable();

    if (decider == null) {
        return Optional.empty();
    }

    Decision outcome = decision.apply(decider);

    if (outcome == Decision.NOT_GOVERNED) {
        return Optional.empty();
    }

    ApplicationProperties.Security security = applicationProperties.getSecurity();

    if (security.getConnectedUserAuthorizationMode() == ConnectedUserAuthorizationMode.ENFORCE) {
        return Optional.of(outcome == Decision.GRANT);
    }

    boolean skipped = AutomationAuthorizationContext.isSkipChecks();

    if (outcome == Decision.DENY && skipped && loggedWouldBeDenials.add(subject)) {
        log.warn("Connected-user authorization would deny {} (LOG mode; skip still applies)", subject);
    }

    return Optional.of(skipped || outcome == Decision.GRANT);
}
```

Put this as the FIRST statement of each skip-reading method, before any other check, using the right decider call.
The spec's precedence rule depends on it being first. Examples:

```java
@Override
public boolean hasResourceScope(Serializable id, String resourceType, String scope) {
    Optional<Boolean> connectedUserDecision = decideForConnectedUser(
        decider -> decider.decide(id, resourceType, scope), resourceType + ":" + scope);

    if (connectedUserDecision.isPresent()) {
        return connectedUserDecision.get();
    }

    // existing body unchanged
}

@Override
public boolean hasWorkspaceScope(long workspaceId, String scope) {
    Optional<Boolean> connectedUserDecision = decideForConnectedUser(
        decider -> decider.decideWorkspace(workspaceId, scope), "Workspace:" + scope);

    if (connectedUserDecision.isPresent()) {
        return connectedUserDecision.get();
    }

    // existing body unchanged
}
```

Which decider method each check uses:
- `decideWorkspace`: both `hasWorkspaceScope` overloads, `canUseConnectionInWorkspace`.
- `decide(projectId, "Project", scope)`: both `hasWorkspaceScopeForProject` overloads.
- `decide(id, type, scope)`: `hasResourceScope`, `hasResourceScopeInEnvironment`.
- `decideWorkflow(workflowId, scope)`: both `hasWorkflowScope` overloads and `hasWorkflowScopeIfProjectWorkflow`.
- `canUseConnectionInWorkflow`: `decide(connectionId, "Connection", "CONNECTION_VIEW")` AND
  `decideWorkflow(workflowId, "WORKFLOW_EDIT")`. Both must GRANT; if either is governed, the answer is governed.

The checks that already ignore skip (`cbe11ea60b5`) must never go through the LOG semantics: that would re-open for
connected users what they already deny. These are `isTenantAdmin`, `isCurrentUser`, `isResourceOwner`,
`hasWorkspaceRole`, `hasResourceRole`, `hasWorkspaceScopeInEveryEnvironment` and `getMyWorkspace*`. Their first
statement becomes:

```java
if (isGovernedPrincipal()) {
    return false;
}
```

This returns an empty set/`null` for the `getMyWorkspace*` readers. It also closes Review Focus 1: a connected user
whose external id equals a platform login never reaches `CurrentUserResolver`. Add
`testGovernedPrincipalIsNeverTenantAdminOrCurrentUser` to Step 1: with a governed principal and a
`CurrentUserResolver` that would resolve a user, `isTenantAdmin()` and `isCurrentUser(thatUserId)` are false in both
modes.

`AutomationMethodSecurityExpressionRoot` and `AutomationPermissionEvaluator`: replace every
`AutomationAuthorizationContext.isSkipChecks()` with `permissionService.isAuthorizationSkipped()`.

`ApplicationProperties.Security`:

```java
private ConnectedUserAuthorizationMode connectedUserAuthorizationMode = ConnectedUserAuthorizationMode.LOG;

public ConnectedUserAuthorizationMode getConnectedUserAuthorizationMode() {
    return connectedUserAuthorizationMode;
}

public void setConnectedUserAuthorizationMode(ConnectedUserAuthorizationMode connectedUserAuthorizationMode) {
    this.connectedUserAuthorizationMode = connectedUserAuthorizationMode;
}

public enum ConnectedUserAuthorizationMode {
    LOG, ENFORCE
}
```

After the constructor change, grep every test and Spring config that builds EE `PermissionServiceImpl` and add the
two parameters.

- [ ] **Step 4: Run and verify they pass**

Run the Step 2 command, then the full `test` of CE and EE `automation-configuration-service`, then `testIntegration`
for `RealPermissionServiceEnforcementIntTest`, `RealImplProxyEnforcementIntTest` and `PerEnvironmentRoleIntTest`, one
at a time. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git commit -m "1051 Consult the connected-user access decider before skip mode, logging would-be denials" -- <files>
```

---

### Task 5: End-to-end proof with real rows (LOG and ENFORCE)

**Files:**
- Create: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/security/ConnectedUserOwnershipIntTest.java`

**Interfaces:**
- Consumes: Tasks 1–4. Reuse the Spring test configuration of an existing embedded-configuration IntTest (read one
  first) and `PostgreSQLContainerConfiguration`.

- [ ] **Step 1: Write the IntTest**

Seed real rows: two connected users `alice` and `bob` in PRODUCTION, each with a `connected_user_project` →
`__EMBEDDED__<ext>` project in the default workspace, a workflow, and a deployment. Also seed a regular automation
project in another workspace and a data table. Through the real `ProjectDeploymentFacade`, `ProjectWorkflowFacade`
and the data table facade proxies, with `alice`'s `EmbeddedApiKeyAuthenticationToken` in the SecurityContext, in
ENFORCE mode:
- `alice` updates her own workflow and enables her own deployment: allowed.
- `alice` on `bob`'s deployment, on the other workspace's project, and dropping the data table: `AccessDeniedException`.
- The same calls inside `AutomationAuthorizationContext.callSkippingChecks`: still denied (precedence).

In LOG mode, `alice`'s call on `bob`'s deployment under `callSkippingChecks` is allowed and logs "would deny".

- [ ] **Step 2: Run it**

Run: `export DOCKER_HOST=…; export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock; ./gradlew --no-daemon --configure-on-demand :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:testIntegration --tests "*ConnectedUserOwnershipIntTest"`
Expected: PASS.

- [ ] **Step 3: Prove it bites**

Make `ConnectedUserAccessDeciderImpl.isOwnProject` return `true` and run: the cross-user assertions FAIL. Restore it
and run: PASS.

- [ ] **Step 4: Commit**

```bash
git commit -m "1051 Test connected-user ownership end to end against real rows" -- <file>
```

---

### Task 6: Observe a real builder session in LOG mode

**Files:**
- Modify: `ConnectedUserAccessDeciderImpl.java` only if the session shows a rule gap (plus a test in `ConnectedUserAccessDeciderTest` per gap)

- [ ] **Step 1:** Start the server with `BYTECHEF_SECURITY_CONNECTEDUSERAUTHORIZATIONMODE=LOG` (default) and the
  embedded sample app (or the embedded builder via the SDK). As one connected user, run: open the builder, add a
  trigger and an action, set a connection, test a node, run the workflow test, attach and stop, open logs, publish,
  enable, disable, delete. Also run the connect dialog (integrations, integration instances, MCP tools) and one copilot
  chat.
- [ ] **Step 2:** Collect every `Connected-user authorization` line (both `would deny` under skip and `denied … outside
  skip mode`) from the server log, and the matching DEBUG lines of `ConnectedUserAccessDeciderImpl`
  (`Connected user <id> in environment <n>: <Type> <id> <SCOPE> -> <decision>`) to identify the resource each names.
- [ ] **Step 3:** Classify each line. It is either a rule gap (an own resource the decider cannot map: add the rule
  with a unit test) or a real hole (keep denying and list it in the commit message's PR notes).
- [ ] **Step 4:** Commit any rule fixes:
  `git commit -m "1051 Grant the connected-user resources the builder session showed the decider could not map" -- <files>`.
  If there were none, record "no gaps" in the handoff.

This task needs a human-driven session. The executing agent prepares the run instructions and the log grep, and the
user performs the session.

---

### Task 7: Enforce

**Files:**
- Modify: `ApplicationProperties.java` (default `ENFORCE`)
- Test: `ConnectedUserOwnershipIntTest` (runs ENFORCE by default)

- [ ] **Step 1:** Flip the default to `ENFORCE`. Run the full `test` of EE automation-configuration-service and
  embedded-configuration-service, and `ConnectedUserOwnershipIntTest`.
- [ ] **Step 2:** Commit: `git commit -m "1051 Enforce connected-user ownership by default" -- <files>`.

---

### Task 8: Delete the skip for connected users

**Gate:** Task 7 done.

**Files:**
- Delete: `server/ee/libs/embedded/embedded-security-web/embedded-security-web-impl/src/main/java/com/bytechef/ee/embedded/security/web/filter/EmbeddedAutomationAuthorizationSkipFilter.java` and its test
- Modify: `…/configurer/EmbeddedApiKeySecurityConfigurer.java` (drop `addFilterAfter`)
- Modify: remove `@SkipAutomationAuthorization` from `ConnectedUserProjectFacadeImpl`, `ConnectedUserProjectWorkflowManager`, `AutomationWorkflowProjectFacadeImpl` (embedded-configuration-service)
- Modify: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/agent/WorkflowEditorSpringAIAgent.java`. The access check calls `permissionService.hasWorkflowScope(workflowId, "WORKFLOW_VIEW")` under `SecurityUtils.runAs(authentication, …)` instead of returning early, and the action runs under `runAs` without `callSkippingChecks`.
- Modify: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/util/CopilotToolContextUtils.java` (`skipAutomationAuthorization` = false for a connected-user authentication) and `RehydrateContextToolCallback.java` (no skip when the carried authentication is a `ConnectedUserAuthentication`)
- Modify: `ApplicationProperties.java` (remove `connectedUserAuthorizationMode`) and EE `PermissionServiceImpl` (decider answer always authoritative; drop the LOG branch and `loggedWouldBeDenials`)
- Keep: `SkipAutomationAuthorizationAspect` and `callSkippingChecks` for the trusted system paths (MCP runtime, MCP listeners)
- Test: `ConnectedUserOwnershipIntTest` (the SecurityConfiguration no longer registers the filter), copilot agent tests

- [ ] **Step 1:** Write a test that the security filter chain contains no `EmbeddedAutomationAuthorizationSkipFilter`,
  and a copilot test where a connected user's agent run on another user's workflow is denied. Run: FAIL.
- [ ] **Step 2:** Make the deletions and changes listed above.
- [ ] **Step 3:** Run the full `test` of every touched module, one at a time, then `ConnectedUserOwnershipIntTest`.
  Also `grep -rn "callSkippingChecks\|SkipAutomationAuthorization" server --include=*.java | grep -v /test/`; the
  only remaining hits may be the trusted system paths. Expected: PASS.
- [ ] **Step 4:** Update the spec's Rollout section to mark all steps done. Commit:
  `git commit -m "1051 Remove the embedded skip filter now that connected users are authorized by ownership" -- <files>`.
