# Draft/Publish for Custom Component and Code Workflow Editors — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make editor Save create/update a draft and add an explicit Publish step in both the custom component editor and the code workflow editors (automation + embedded), per `docs/superpowers/specs/2026-08-05-draft-publish-editors-design.md`.

**Architecture:** Custom components get a `status` (`DRAFT`/`PUBLISHED`) column — published rows become immutable, editing a published row spawns a new draft row, a new `publishCustomComponent` mutation flips the draft, and the runtime handler registry filters to published rows. Code workflows stop publishing on save: the editor save reconciles a mutable draft `code_workflow_container` in place (or mints one after a publish, adopting the draft version's duplicated workflows by ProjectWorkflow `uuid`), and publish happens only through the existing project/integration header publish. Upload/deploy REST paths keep today's semantics on both sides (deliberate spec exception).

**Tech Stack:** Spring Boot 4 / Spring Data JDBC, Liquibase, Spring GraphQL, React 19 + TanStack Query + GraphQL codegen, JUnit 5 + Mockito, Vitest.

## Global Constraints

- All touched server files are under `server/ee/` → ByteChef Enterprise license header + `@version ee` Javadoc tag on new classes.
- Enum columns store INT ordinals; new persisted enums are append-only and pinned by an ordinal stability test (`OrdinalStabilityAssertions` from `server/libs/test/test-support`).
- The `custom_component` schema is released (v0.31.2) → schema changes go in a NEW changeset, never edit the init changelog.
- GraphQL enum values are SCREAMING_SNAKE_CASE.
- Java style: one blank line before control statements; blank line between a variable mutation and its use; no trailing blank line before class-closing brace; no chained calls outside builders/streams/Optional.
- Test method names camelCase without underscores; unit test classes end in `Test` (drop `Impl` from the name).
- Client: ESLint sort-keys (manual fix), interface names end `I`/`Props`, icons imported with `Icon` suffix, `twMerge` not `cn()`, no per-mutation `onError` unless custom behavior is needed.
- Before each commit: `./gradlew spotlessApply` for server changes; `cd client && npm run format` for client changes. Commit messages follow `<description>` / `client - <description>` (no ticket number exists for this work; if the user supplies one later, prefix it).
- Run gradle checks by redirecting output to a file and grepping `^> Task .* FAILED` — never judge a piped gradle run by pipeline exit code.

---

## Part 1 — Custom components (backend)

### Task 1: `CustomComponent.Status` enum, columns, Liquibase changeset, ordinal pin

**Files:**
- Modify: `server/ee/libs/platform/platform-custom-component/platform-custom-component-configuration/platform-custom-component-configuration-api/src/main/java/com/bytechef/ee/platform/customcomponent/configuration/domain/CustomComponent.java`
- Create: `server/ee/libs/platform/platform-custom-component/platform-custom-component-configuration/platform-custom-component-configuration-service/src/main/resources/config/liquibase/changelog/platform/custom_component/20260805000001_platform_custom_component_add_status.xml`
- Test: `server/ee/libs/platform/platform-custom-component/platform-custom-component-configuration/platform-custom-component-configuration-service/src/test/java/com/bytechef/ee/platform/customcomponent/configuration/domain/EnumOrdinalStabilityTest.java`
- Maybe modify: `server/ee/libs/platform/platform-custom-component/platform-custom-component-configuration/platform-custom-component-configuration-service/build.gradle.kts` (add `testImplementation(project(":server:libs:test:test-support"))` if absent)

**Interfaces:**
- Produces: `CustomComponent.Status { DRAFT, PUBLISHED }` (INT ordinal), `getStatus()`, `setStatus(Status)`, `getPublishedDate()`, `setPublishedDate(Instant)`. Later tasks rely on exactly these names.

- [ ] **Step 1: Write the failing ordinal-stability test**

```java
/*
 * Copyright 2026 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.configuration.domain;

import com.bytechef.test.assertion.OrdinalStabilityAssertions;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Pins the ordinals of enums persisted as INT columns on {@code custom_component}. Append new values at the end only.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class EnumOrdinalStabilityTest {

    @Test
    void testCustomComponentStatusOrdinals() {
        Map<String, Integer> expected = new LinkedHashMap<>();

        expected.put("DRAFT", 0);
        expected.put("PUBLISHED", 1);

        OrdinalStabilityAssertions.assertOrdinalsMatch(
            CustomComponent.Status.values(), expected, "CustomComponent.Status");
    }

    @Test
    void testCustomComponentLanguageOrdinals() {
        Map<String, Integer> expected = new LinkedHashMap<>();

        expected.put("JAVA", 0);
        expected.put("JAVASCRIPT", 1);
        expected.put("PYTHON", 2);
        expected.put("RUBY", 3);

        OrdinalStabilityAssertions.assertOrdinalsMatch(
            CustomComponent.Language.values(), expected, "CustomComponent.Language");
    }
}
```

(`Language` is persisted as an ordinal today with no pin — pin it while we are here.)

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:ee:libs:platform:platform-custom-component:platform-custom-component-configuration:platform-custom-component-configuration-service:test --tests "*EnumOrdinalStabilityTest" > /tmp/t1.log 2>&1; echo $?; grep "FAILED\|error:" /tmp/t1.log | head`
Expected: compilation failure — `CustomComponent.Status` does not exist. (If `OrdinalStabilityAssertions` is unresolved, add the test-support dependency from the Files list first.)

- [ ] **Step 3: Add Status enum + fields to `CustomComponent`**

After the `Language` enum, add:

```java
    public enum Status {
        DRAFT, PUBLISHED
    }
```

Add fields (keep the existing field ordering style — alphabetical-ish placement near `name`/`title`):

```java
    @Column("published_date")
    private Instant publishedDate;

    @Column
    private int status;
```

Add accessors following the `Language` ordinal pattern:

```java
    public Instant getPublishedDate() {
        return publishedDate;
    }

    public Status getStatus() {
        return Status.values()[status];
    }

    public void setPublishedDate(Instant publishedDate) {
        this.publishedDate = publishedDate;
    }

    public void setStatus(Status status) {
        this.status = status.ordinal();
    }
```

Also extend `toString()` with `", status=" + status` (the dynamic-handler registry uses `toString()` as its cache key; status changes must produce a new key).

- [ ] **Step 4: Write the Liquibase changeset**

`20260805000001_platform_custom_component_add_status.xml` (same XML skeleton as `20240604153121_platform_custom_component_renamed_column_component_file.xml`):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">
    <changeSet id="20260805000001-1" author="Ivica Cardic">
        <preConditions onFail="MARK_RAN">
            <not>
                <columnExists tableName="custom_component" columnName="status"/>
            </not>
        </preConditions>

        <addColumn tableName="custom_component">
            <column name="status" type="INT" defaultValueNumeric="1">
                <constraints nullable="false"/>
            </column>
            <column name="published_date" type="TIMESTAMP">
                <constraints nullable="true"/>
            </column>
        </addColumn>

        <dropDefaultValue tableName="custom_component" columnName="status"/>

        <sql>UPDATE custom_component SET published_date = last_modified_date</sql>
    </changeSet>
</databaseChangeLog>
```

`defaultValueNumeric="1"` backfills existing rows to `PUBLISHED` (ordinal 1), then the default is dropped so raw inserts don't silently default to published. The directory is picked up by the Liquibase `includeAll` scan — no master-changelog edit needed. After creating it, delete any stale copy under `build/resources/` if present.

- [ ] **Step 5: Run test to verify it passes**

Run: same command as Step 2. Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add server/ee/libs/platform/platform-custom-component
git commit -m "Add status and published_date to custom_component"
```

---

### Task 2: Repository finders, service API (`fetchDraft`/`fetchLatest`/`publish`, full-copy `update`), audit event

**Files:**
- Modify: `.../platform-custom-component-configuration-api/.../service/CustomComponentService.java`
- Modify: `.../platform-custom-component-configuration-api/.../audit/CustomComponentAuditEvent.java`
- Modify: `.../platform-custom-component-configuration-api/.../exception/CustomComponentErrorType.java`
- Modify: `.../platform-custom-component-configuration-service/.../repository/CustomComponentRepository.java`
- Modify: `.../platform-custom-component-configuration-service/.../service/CustomComponentServiceImpl.java`
- Test: `.../platform-custom-component-configuration-service/src/test/java/com/bytechef/ee/platform/customcomponent/configuration/service/CustomComponentServiceTest.java` (new)

**Interfaces:**
- Consumes: `CustomComponent.Status` from Task 1.
- Produces (Task 3+ relies on these exact signatures):
  - `Optional<CustomComponent> fetchDraftCustomComponent(String name)`
  - `Optional<CustomComponent> fetchLatestCustomComponent(String name)`
  - `CustomComponent publishCustomComponent(long id)`
  - `CustomComponent update(CustomComponent)` now persists `component`, `componentVersion`, `status`, `publishedDate` in addition to description/icon/title
  - Error types `VERSION_NOT_BUMPED(106)`, `DRAFT_ALREADY_EXISTS(107)`, `VERSION_ALREADY_EXISTS(108)`, `COMPONENT_NOT_DRAFT(109)`
  - Audit event `CUSTOM_COMPONENT_PUBLISHED`

- [ ] **Step 1: Write failing service tests**

New `CustomComponentServiceTest` (plain JUnit 5 + Mockito, matching `CustomComponentAuthorizationTest` style — `mock()`, `when()`, `verify()`):

```java
class CustomComponentServiceTest {

    private final CustomComponentAuditPublisher customComponentAuditPublisher =
        mock(CustomComponentAuditPublisher.class);
    private final CustomComponentRepository customComponentRepository = mock(CustomComponentRepository.class);
    private final CustomComponentServiceImpl customComponentService = new CustomComponentServiceImpl(
        customComponentAuditPublisher, customComponentRepository);

    @Test
    void testPublishCustomComponentFlipsDraftAndStampsPublishedDate() {
        CustomComponent customComponent = new CustomComponent();

        customComponent.setId(1L);
        customComponent.setName("test");
        customComponent.setStatus(CustomComponent.Status.DRAFT);

        when(customComponentRepository.findById(1L)).thenReturn(Optional.of(customComponent));
        when(customComponentRepository.save(any(CustomComponent.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CustomComponent published = customComponentService.publishCustomComponent(1L);

        assertEquals(CustomComponent.Status.PUBLISHED, published.getStatus());
        assertNotNull(published.getPublishedDate());
        verify(customComponentAuditPublisher).publish(
            eq(CustomComponentAuditEvent.CUSTOM_COMPONENT_PUBLISHED), eq(1L), any());
    }

    @Test
    void testPublishCustomComponentRejectsPublishedRow() {
        CustomComponent customComponent = new CustomComponent();

        customComponent.setId(1L);
        customComponent.setStatus(CustomComponent.Status.PUBLISHED);

        when(customComponentRepository.findById(1L)).thenReturn(Optional.of(customComponent));

        ConfigurationException exception = assertThrows(
            ConfigurationException.class, () -> customComponentService.publishCustomComponent(1L));

        assertEquals(CustomComponentErrorType.COMPONENT_NOT_DRAFT, exception.getErrorType());
        verify(customComponentRepository, never()).save(any());
    }

    @Test
    void testUpdatePersistsComponentFileAndVersionAndStatus() {
        CustomComponent existing = new CustomComponent();

        existing.setId(1L);
        existing.setName("test");
        existing.setComponentVersion(1);
        existing.setStatus(CustomComponent.Status.DRAFT);

        when(customComponentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(customComponentRepository.save(any(CustomComponent.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CustomComponent incoming = new CustomComponent();

        incoming.setId(1L);
        incoming.setComponent(new FileEntry("test_2.js", "base64://x"));
        incoming.setComponentVersion(2);
        incoming.setStatus(CustomComponent.Status.DRAFT);
        incoming.setTitle("Test");

        CustomComponent saved = customComponentService.update(incoming);

        assertEquals(2, saved.getComponentVersion());
        assertNotNull(saved.getComponent());
        assertEquals("Test", saved.getTitle());
    }
}
```

(Adjust the `FileEntry` constructor to the real one — check `com.bytechef.file.storage.domain.FileEntry`; it has a `(String filename, String url)` constructor used widely in tests. Also verify `getErrorType()` is the accessor `ConfigurationException` exposes — mirror whatever `CustomComponentFacadeCreateEmptyTest` asserts on.)

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :server:ee:libs:platform:platform-custom-component:platform-custom-component-configuration:platform-custom-component-configuration-service:test --tests "*CustomComponentServiceTest" > /tmp/t2.log 2>&1; grep -E "FAILED|error:" /tmp/t2.log | head`
Expected: compile failure (`publishCustomComponent` undefined, `CUSTOM_COMPONENT_PUBLISHED` undefined).

- [ ] **Step 3: Implement**

`CustomComponentErrorType` — append:

```java
    public static final CustomComponentErrorType VERSION_NOT_BUMPED = new CustomComponentErrorType(106);

    public static final CustomComponentErrorType DRAFT_ALREADY_EXISTS = new CustomComponentErrorType(107);

    public static final CustomComponentErrorType VERSION_ALREADY_EXISTS = new CustomComponentErrorType(108);

    public static final CustomComponentErrorType COMPONENT_NOT_DRAFT = new CustomComponentErrorType(109);
```

`CustomComponentAuditEvent` — append (with Javadoc matching siblings):

```java
    /**
     * A draft custom component was published. Payload: {@code customComponentId}, {@code name}.
     */
    CUSTOM_COMPONENT_PUBLISHED
```

`CustomComponentRepository` — add:

```java
    Optional<CustomComponent> findByNameAndStatus(String name, int status);

    Optional<CustomComponent> findFirstByNameOrderByComponentVersionDesc(String name);
```

`CustomComponentService` — add:

```java
    Optional<CustomComponent> fetchDraftCustomComponent(String name);

    Optional<CustomComponent> fetchLatestCustomComponent(String name);

    CustomComponent publishCustomComponent(long id);
```

`CustomComponentServiceImpl` — implement:

```java
    @Override
    public Optional<CustomComponent> fetchDraftCustomComponent(String name) {
        return customComponentRepository.findByNameAndStatus(name, CustomComponent.Status.DRAFT.ordinal());
    }

    @Override
    public Optional<CustomComponent> fetchLatestCustomComponent(String name) {
        return customComponentRepository.findFirstByNameOrderByComponentVersionDesc(name);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public CustomComponent publishCustomComponent(long id) {
        CustomComponent customComponent = getCustomComponent(id);

        if (customComponent.getStatus() != CustomComponent.Status.DRAFT) {
            throw new ConfigurationException(
                "Only a draft custom component can be published", CustomComponentErrorType.COMPONENT_NOT_DRAFT);
        }

        customComponent.setPublishedDate(Instant.now());
        customComponent.setStatus(CustomComponent.Status.PUBLISHED);

        CustomComponent savedCustomComponent = customComponentRepository.save(customComponent);

        customComponentAuditPublisher.publish(
            CustomComponentAuditEvent.CUSTOM_COMPONENT_PUBLISHED, savedCustomComponent.getId(),
            Map.of("name", savedCustomComponent.getName()));

        return savedCustomComponent;
    }
```

Extend `update` — after the existing three copies, add:

```java
        curCustomComponent.setComponent(customComponent.getComponent());
        curCustomComponent.setComponentVersion(customComponent.getComponentVersion());
        curCustomComponent.setPublishedDate(customComponent.getPublishedDate());
        curCustomComponent.setStatus(customComponent.getStatus());
```

(This also fixes a latent bug: the editor's new `FileEntry` was never persisted before — it only worked because the deterministic `name_version.ext` filename overwrote the blob in place.)

- [ ] **Step 4: Run tests to verify they pass**

Run: same command as Step 2. Expected: PASS. Also run the whole module's tests: `... :platform-custom-component-configuration-service:test` — existing tests (e.g. `CustomComponentFacadeUpdateSourceTest`) must still pass; if any assert on `update` only copying three fields, update them to the new contract.

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/platform/platform-custom-component
git commit -m "Add custom component draft/publish service API"
```

---

### Task 3: Facade save semantics — draft-in-place, published-spawns-draft, upload guard

**Files:**
- Modify: `.../facade/CustomComponentFacade.java` (interface, same package as impl)
- Modify: `.../facade/CustomComponentFacadeImpl.java`
- Modify: `server/ee/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/ee/automation/ai/tool/CustomComponentTools.java` — its `updateCustomComponentSource` tool calls the facade method whose return type changes from `void` to `CustomComponent`; adapt the call site (ignore or surface the returned entity in the tool result) so the module keeps compiling
- Test: extend `.../facade/CustomComponentFacadeUpdateSourceTest.java`, `.../facade/CustomComponentFacadeCreateEmptyTest.java`, and the upload tests in `CustomComponentFacadeAuthorizationTest`/`CustomComponentFacadeUploadDisabledTest` neighborhood (add a new `CustomComponentFacadeUploadStatusTest` if cleaner)

**Interfaces:**
- Consumes: Task 2 service API.
- Produces: `CustomComponent updateCustomComponentSource(long id, String content)` (return type changes from `void`), `CustomComponent publishCustomComponent(long id)` on the facade. Task 5 wires these into GraphQL.

- [ ] **Step 1: Write failing facade tests**

Add to `CustomComponentFacadeUpdateSourceTest` (follow its existing mock setup — it mocks `CustomComponentService` and `CustomComponentFileStorage` and uses a real JS source string; reuse its helpers):

```java
    @Test
    void testDraftSaveUpdatesRowInPlaceAndReReadsVersion() {
        // existing row: DRAFT, version 1; source declares .version(2); no other row owns version 2
        // expect: service.update called with componentVersion == 2 and the new FileEntry;
        //         old FileEntry deleted because the stored filename changed (name_1.js -> name_2.js);
        //         no service.create call
    }

    @Test
    void testDraftSaveVersionCollisionRejected() {
        // existing row: DRAFT, version 1; source declares .version(3); fetchCustomComponent(name, 3)
        // returns a DIFFERENT row -> ConfigurationException VERSION_ALREADY_EXISTS, nothing stored
    }

    @Test
    void testPublishedSaveSpawnsNewDraftRow() {
        // existing row: PUBLISHED, version 1; no draft exists; source declares .version(2);
        // fetchLatestCustomComponent returns the v1 row
        // expect: service.create called with status DRAFT, componentVersion 2, enabled true;
        //         the PUBLISHED row is never passed to service.update; returned CustomComponent is the new row
    }

    @Test
    void testPublishedSaveWithoutVersionBumpRejected() {
        // existing row: PUBLISHED, version 1; no draft; source still declares .version(1)
        // -> ConfigurationException VERSION_NOT_BUMPED, no create, no update, nothing stored
    }

    @Test
    void testPublishedSaveWithExistingDraftRejected() {
        // existing row: PUBLISHED; fetchDraftCustomComponent(name) returns a draft row
        // -> ConfigurationException DRAFT_ALREADY_EXISTS, no create, no update
    }
```

Write these as real tests (the sketch comments above describe arrange/assert; the existing test class shows how to build a loadable JS source with a given name/version — reuse its template strings, substituting `.version(2)`).

Add to `CustomComponentFacadeCreateEmptyTest`:

```java
    @Test
    void testCreateEmptyCustomComponentStartsAsDraft() {
        // capture the CustomComponent passed to service.create; assert getStatus() == Status.DRAFT
        // and getPublishedDate() == null
    }
```

New `CustomComponentFacadeUploadStatusTest`:

```java
    @Test
    void testUploadCreatesPublishedRow() {
        // save(bytes, JAVASCRIPT) with a name/version that has no row
        // -> service.create called with status PUBLISHED and non-null publishedDate
    }

    @Test
    void testUploadOntoDraftOwnedVersionRejected() {
        // fetchCustomComponent(name, version) returns a row with status DRAFT
        // -> ConfigurationException VERSION_ALREADY_EXISTS, no update
    }

    @Test
    void testUploadOntoPublishedVersionUpdatesInPlace() {
        // fetchCustomComponent(name, version) returns a PUBLISHED row
        // -> service.update called (unchanged deploy semantics), status stays PUBLISHED
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :server:ee:libs:platform:platform-custom-component:platform-custom-component-configuration:platform-custom-component-configuration-service:test > /tmp/t3.log 2>&1; grep -E "FAILED|error:" /tmp/t3.log | head`
Expected: compile failure (return type mismatch) and/or test failures.

- [ ] **Step 3: Implement facade changes**

Interface: change `void updateCustomComponentSource(long id, String content)` → `CustomComponent updateCustomComponentSource(long id, String content)`; add `CustomComponent publishCustomComponent(long id)`.

`CustomComponentFacadeImpl`:

```java
    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public CustomComponent publishCustomComponent(long id) {
        return customComponentService.publishCustomComponent(id);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public CustomComponent updateCustomComponentSource(long id, String content) {
        CustomComponent customComponent = customComponentService.getCustomComponent(id);

        Language language = customComponent.getLanguage();

        if (language == Language.JAVA) {
            throw new ConfigurationException(
                "Java custom components have no editable source", CustomComponentErrorType.JAVA_SOURCE_NOT_EDITABLE);
        }

        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        try {
            ComponentDefinition componentDefinition = loadComponentDefinition(language, bytes);

            if (!Objects.equals(componentDefinition.getName(), customComponent.getName())) {
                throw new ConfigurationException(
                    "Renaming a component by editing its source is not supported (expected name '"
                        + customComponent.getName() + "')",
                    CustomComponentErrorType.SOURCE_RENAME_UNSUPPORTED);
            }

            if (customComponent.getStatus() == CustomComponent.Status.DRAFT) {
                return updateDraft(customComponent, componentDefinition, language, bytes);
            }

            return createDraftFromPublished(customComponent, componentDefinition, language, bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private CustomComponent updateDraft(
        CustomComponent customComponent, ComponentDefinition componentDefinition, Language language, byte[] bytes) {

        int newComponentVersion = componentDefinition.getVersion();

        if (newComponentVersion != customComponent.getComponentVersion()) {
            customComponentService.fetchCustomComponent(customComponent.getName(), newComponentVersion)
                .filter(existing -> !Objects.equals(existing.getId(), customComponent.getId()))
                .ifPresent(existing -> {
                    throw new ConfigurationException(
                        "Version " + newComponentVersion + " of component '" + customComponent.getName() +
                            "' already exists",
                        CustomComponentErrorType.VERSION_ALREADY_EXISTS);
                });
        }

        FileEntry oldComponentFileEntry = customComponent.getComponent();

        FileEntry componentFileEntry = customComponentFileStorage.storeCustomComponentFile(
            componentDefinition.getName() + "_" + newComponentVersion + "." + language.getExtension(), bytes);

        customComponent.setComponent(componentFileEntry);
        customComponent.setComponentVersion(newComponentVersion);
        customComponent.setDescription(OptionalUtils.orElse(componentDefinition.getDescription(), null));
        customComponent.setIcon(OptionalUtils.orElse(componentDefinition.getIcon(), null));
        customComponent.setTitle(OptionalUtils.orElse(componentDefinition.getTitle(), null));

        CustomComponent savedCustomComponent = customComponentService.update(customComponent);

        if (oldComponentFileEntry != null &&
            !Objects.equals(oldComponentFileEntry.getName(), componentFileEntry.getName())) {

            customComponentFileStorage.deleteCustomComponentFile(oldComponentFileEntry);
        }

        return savedCustomComponent;
    }

    private CustomComponent createDraftFromPublished(
        CustomComponent publishedCustomComponent, ComponentDefinition componentDefinition, Language language,
        byte[] bytes) {

        String name = publishedCustomComponent.getName();

        customComponentService.fetchDraftCustomComponent(name)
            .ifPresent(draft -> {
                throw new ConfigurationException(
                    "A draft (version " + draft.getComponentVersion() + ") of component '" + name +
                        "' already exists; edit that draft instead",
                    CustomComponentErrorType.DRAFT_ALREADY_EXISTS);
            });

        int latestComponentVersion = customComponentService.fetchLatestCustomComponent(name)
            .map(CustomComponent::getComponentVersion)
            .orElse(0);

        if (componentDefinition.getVersion() <= latestComponentVersion) {
            throw new ConfigurationException(
                "Editing published version " + publishedCustomComponent.getComponentVersion() +
                    " requires bumping .version() above " + latestComponentVersion + " to start a new draft",
                CustomComponentErrorType.VERSION_NOT_BUMPED);
        }

        FileEntry componentFileEntry = customComponentFileStorage.storeCustomComponentFile(
            componentDefinition.getName() + "_" + componentDefinition.getVersion() + "." + language.getExtension(),
            bytes);

        return create(language, componentDefinition, componentDefinition.getVersion(), componentFileEntry,
            CustomComponent.Status.DRAFT, null);
    }
```

Rework the private `create` to take status/publishedDate and return the entity:

```java
    private CustomComponent create(
        Language language, ComponentDefinition componentDefinition, int componentVersion,
        FileEntry componentFileEntry, CustomComponent.Status status, Instant publishedDate) {

        CustomComponent customComponent = new CustomComponent();

        customComponent.setComponentVersion(componentVersion);
        customComponent.setComponent(componentFileEntry);
        customComponent.setDescription(OptionalUtils.orElse(componentDefinition.getDescription(), null));
        customComponent.setEnabled(true);
        customComponent.setIcon(OptionalUtils.orElse(componentDefinition.getIcon(), null));
        customComponent.setName(componentDefinition.getName());
        customComponent.setPublishedDate(publishedDate);
        customComponent.setStatus(status);
        customComponent.setTitle(OptionalUtils.orElse(componentDefinition.getTitle(), null));
        customComponent.setLanguage(language);

        return customComponentService.create(customComponent);
    }
```

`createEmptyCustomComponent`: replace its inline entity construction with a call to the reworked `create(language, componentDefinition, componentDefinition.getVersion(), componentFileEntry, CustomComponent.Status.DRAFT, null)`.

`save(byte[], Language)` (upload): in the `ifPresentOrElse`:
- present branch: first check `customComponent.getStatus() == CustomComponent.Status.DRAFT` → throw `VERSION_ALREADY_EXISTS` ("version N of 'name' is owned by a draft; publish or delete the draft first"); otherwise call the existing `update(customComponent, componentDefinition)` path — but it must now also set the new `FileEntry` on the entity before updating (move the `setComponent` call into a small `updateUploaded(customComponent, componentDefinition, componentFileEntry)` helper that sets component + delegates to the metadata copy + `customComponentService.update`).
- absent branch: `create(language, componentDefinition, version, componentFileEntry, CustomComponent.Status.PUBLISHED, Instant.now())`.

- [ ] **Step 4: Run tests to verify they pass**

Run: same module test command. Expected: all PASS, including pre-existing tests (the rename-rejection and Java-rejection tests still apply — the rename check now runs before branching).

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/platform/platform-custom-component
git commit -m "Custom component editor save creates/updates drafts, never touches published rows"
```

---

### Task 4: Runtime registry filters drafts out

**Files:**
- Modify: `server/ee/libs/platform/platform-custom-component/platform-custom-component-handler/src/main/java/com/bytechef/ee/platform/customcomponent/handler/CustomComponentDynamicComponentHandlerRegistry.java`
- Test: `server/ee/libs/platform/platform-custom-component/platform-custom-component-handler/src/test/java/com/bytechef/ee/platform/customcomponent/handler/CustomComponentDynamicComponentHandlerRegistryTest.java` (new — the module currently has no tests; create the test source dir and check `build.gradle.kts` has JUnit/Mockito test deps, adding the same block the configuration-service module uses if missing)

**Interfaces:**
- Consumes: `CustomComponent.Status` from Task 1.

- [ ] **Step 1: Write failing tests**

Design the tests so the filtered-out path never reaches the real `ComponentHandlerLoader` (which would try to load an actual file): feed only draft/disabled components and assert emptiness.

```java
class CustomComponentDynamicComponentHandlerRegistryTest {

    // construct the registry with mocked CustomComponentService + CustomComponentFileStorage +
    // CacheManager + ApplicationProperties (mirror the real constructor's parameters)

    @Test
    void testGetComponentHandlersExcludesDrafts() {
        CustomComponent draft = new CustomComponent();

        draft.setEnabled(true);
        draft.setName("test");
        draft.setStatus(CustomComponent.Status.DRAFT);

        when(customComponentService.getCustomComponents()).thenReturn(List.of(draft));

        assertTrue(registry.getComponentHandlers().isEmpty());
    }

    @Test
    void testGetComponentHandlersExcludesDisabled() {
        CustomComponent disabled = new CustomComponent();

        disabled.setEnabled(false);
        disabled.setName("test");
        disabled.setStatus(CustomComponent.Status.PUBLISHED);

        when(customComponentService.getCustomComponents()).thenReturn(List.of(disabled));

        assertTrue(registry.getComponentHandlers().isEmpty());
    }

    @Test
    void testFetchComponentHandlerExcludesDraft() {
        CustomComponent draft = new CustomComponent();

        draft.setName("test");
        draft.setStatus(CustomComponent.Status.DRAFT);

        when(customComponentService.fetchCustomComponent("test", 2)).thenReturn(Optional.of(draft));

        assertTrue(registry.fetchComponentHandler("test", 2).isEmpty());
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :server:ee:libs:platform:platform-custom-component:platform-custom-component-handler:test > /tmp/t4.log 2>&1; grep -E "FAILED|error:" /tmp/t4.log | head`
Expected: FAIL — drafts currently load (the draft/disabled inputs would hit the loader and blow up, or return non-empty).

- [ ] **Step 3: Implement the filters**

In `getComponentHandlers()` add after the `isEnabled` filter:

```java
            .filter(customComponent -> customComponent.getStatus() == CustomComponent.Status.PUBLISHED)
```

In `fetchComponentHandler(String name, int componentVersion)` insert before the `.map`:

```java
            .filter(customComponent -> customComponent.getStatus() == CustomComponent.Status.PUBLISHED)
```

(Deliberate: `fetchComponentHandler` keeps NOT filtering on `enabled` — disabled-but-published components stay resolvable for already-referencing workflows, per the pre-existing asymmetry. Drafts are excluded on both paths.)

- [ ] **Step 4: Run tests to verify they pass**

Run: same command. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/platform/platform-custom-component
git commit -m "Exclude draft custom components from the runtime handler registry"
```

---

### Task 5: GraphQL schema + controller

**Files:**
- Modify: `.../platform-custom-component-configuration-graphql/src/main/resources/graphql/custom-component.graphqls`
- Modify: `.../platform-custom-component-configuration-graphql/.../web/graphql/CustomComponentGraphQlController.java`
- Test: `.../platform-custom-component-configuration-graphql/src/test/java/com/bytechef/ee/platform/customcomponent/configuration/web/graphql/CustomComponentGraphQlControllerTest.java` (new; model on `CodeWorkflowGraphQlControllerTest` — plain delegation tests)

**Interfaces:**
- Consumes: facade methods from Task 3.
- Produces: schema used by Task 6's client operations — field names `status`, `publishedDate`; enum `CustomComponentStatus { DRAFT PUBLISHED }`; mutations `updateCustomComponentSource(id: ID!, content: String!): CustomComponent!` and `publishCustomComponent(id: ID!): CustomComponent!`.

- [ ] **Step 1: Write failing controller test**

```java
class CustomComponentGraphQlControllerTest {

    private final CustomComponentFacade customComponentFacade = mock(CustomComponentFacade.class);
    private final CustomComponentService customComponentService = mock(CustomComponentService.class);
    private final CustomComponentGraphQlController controller = new CustomComponentGraphQlController(
        customComponentFacade, customComponentService);

    @Test
    void testUpdateCustomComponentSourceReturnsUpdatedComponent() {
        CustomComponent customComponent = new CustomComponent();

        customComponent.setId(7L);

        when(customComponentFacade.updateCustomComponentSource(1L, "source")).thenReturn(customComponent);

        assertEquals(customComponent, controller.updateCustomComponentSource(1L, "source"));
    }

    @Test
    void testPublishCustomComponentDelegatesToFacade() {
        CustomComponent customComponent = new CustomComponent();

        when(customComponentFacade.publishCustomComponent(1L)).thenReturn(customComponent);

        assertEquals(customComponent, controller.publishCustomComponent(1L));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:ee:libs:platform:platform-custom-component:platform-custom-component-configuration:platform-custom-component-configuration-graphql:test > /tmp/t5.log 2>&1; grep -E "FAILED|error:" /tmp/t5.log | head`
Expected: compile failure.

- [ ] **Step 3: Implement schema + controller**

Schema — in `type CustomComponent` add:

```graphql
    status: CustomComponentStatus
    publishedDate: Long
```

Add:

```graphql
enum CustomComponentStatus {
    DRAFT
    PUBLISHED
}
```

Change/add mutations:

```graphql
    publishCustomComponent(id: ID!): CustomComponent!
    updateCustomComponentSource(id: ID!, content: String!): CustomComponent!
```

Controller — replace the boolean mutation and add publish:

```java
    @MutationMapping
    CustomComponent publishCustomComponent(@Argument Long id) {
        return customComponentFacade.publishCustomComponent(id);
    }

    @MutationMapping
    CustomComponent updateCustomComponentSource(@Argument Long id, @Argument String content) {
        return customComponentFacade.updateCustomComponentSource(id, content);
    }
```

(`CustomComponent.getStatus()` returns the enum, so Spring GraphQL maps it to `CustomComponentStatus` by name; `publishedDate: Long` follows the same Instant→Long mapping the existing `createdDate`/`lastModifiedDate` fields use.)

- [ ] **Step 4: Run tests, then compile the module tree**

Run: same test command → PASS. Then `./gradlew :server:ee:libs:platform:platform-custom-component:platform-custom-component-configuration:platform-custom-component-configuration-graphql:compileJava compileTestJava > /tmp/t5b.log 2>&1; grep "FAILED" /tmp/t5b.log`

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/platform/platform-custom-component
git commit -m "Expose custom component status and publish mutation over GraphQL"
```

---

## Part 2 — Custom components (client)

### Task 6: Editor Publish button, status badges, new-draft navigation

**Files:**
- Modify: `client/src/graphql/platform/custom-component/customComponent.graphql`, `customComponents.graphql`, `updateCustomComponentSource.graphql`
- Create: `client/src/graphql/platform/custom-component/publishCustomComponent.graphql`
- Regenerate: `client/src/shared/middleware/graphql.ts` (+ `graphql-types.ts`) via `npx graphql-codegen`
- Modify: `client/src/ee/pages/settings/platform/custom-components/CustomComponentDetail.tsx`
- Modify: `client/src/ee/pages/settings/platform/custom-components/components/CustomComponentListItem.tsx`
- Test: extend `CustomComponentDetail.test.tsx`, `CustomComponentListItem.test.tsx` (same directories)

**Interfaces:**
- Consumes: Task 5 schema (`status`, `publishedDate`, `CustomComponentStatus`, new mutation shapes).

- [ ] **Step 1: Update GraphQL operations**

`updateCustomComponentSource.graphql`:

```graphql
mutation updateCustomComponentSource($id: ID!, $content: String!) {
    updateCustomComponentSource(id: $id, content: $content) {
        id
        componentVersion
        status
    }
}
```

New `publishCustomComponent.graphql`:

```graphql
mutation publishCustomComponent($id: ID!) {
    publishCustomComponent(id: $id) {
        id
        publishedDate
        status
    }
}
```

Add `status` and `publishedDate` to the selection sets in `customComponent.graphql` and `customComponents.graphql` (alphabetical position within the existing field list).

- [ ] **Step 2: Regenerate codegen**

Run: `cd client && npx graphql-codegen`
Expected: `useUpdateCustomComponentSourceMutation` return type now carries `{id, componentVersion, status}`; new `usePublishCustomComponentMutation` hook exists. Commit generated file separately per repo convention (see Step 6).

- [ ] **Step 3: Write failing vitest tests**

In `CustomComponentDetail.test.tsx` add (mirroring the file's existing render/mocking setup):

```tsx
it('shows a Publish button enabled only when the component is a clean draft', ...);
// matrix: DRAFT + not dirty -> enabled; DRAFT + dirty -> disabled; PUBLISHED -> disabled
it('navigates to the new draft when saving a published component returns a different id', ...);
// mock mutation resolving {id: '99'} while route id is '1'; assert navigate called with path ending '/99'
```

In `CustomComponentListItem.test.tsx`:

```tsx
it('renders a Draft badge for draft components and a Published badge otherwise', ...);
```

Run: `cd client && npm run test -- CustomComponent` → FAIL.

- [ ] **Step 4: Implement UI changes**

`CustomComponentDetail.tsx`:

- Extend `CustomComponentDetailHeader` props with `isPublishDisabled: boolean`, `isPublishing: boolean`, `onPublish: () => void`, `status?: CustomComponentStatus` (sorted). In the `right` slot render Publish before Save:

```tsx
right={
    showSaveButton && (
        <div className="flex items-center gap-2">
            <Button
                disabled={isPublishDisabled}
                label={isPublishing ? 'Publishing...' : 'Publish'}
                onClick={onPublish}
                size="sm"
                variant="outline"
            />

            <Button disabled={isSaveDisabled} label={isSaving ? 'Saving...' : 'Save'} onClick={onSave} size="sm" />
        </div>
    )
}
```

- Status badge next to the existing language `Badge` in the header title block:

```tsx
{customComponent?.status && (
    <Badge variant={customComponent.status === CustomComponentStatus.Draft ? 'secondary' : 'outline'}>
        {customComponent.status === CustomComponentStatus.Draft ? 'Draft' : 'Published'}
    </Badge>
)}
```

(Use the exact generated enum member casing from `graphql-types.ts` — codegen typically emits `CustomComponentStatus.Draft`.)

- Publish wiring in the page component (hook order per convention: state → refs → stores → custom hooks → derived → effects):

```tsx
const publishCustomComponentMutation = usePublishCustomComponentMutation({
    onSuccess: () => {
        queryClient.invalidateQueries({queryKey: ['customComponent', {id}]});
        queryClient.invalidateQueries({queryKey: ['customComponents']});
    },
});

const handlePublish = () => {
    publishCustomComponentMutation.mutate({id});
};
```

`isPublishDisabled = customComponent?.status !== CustomComponentStatus.Draft || isSourceDirty || publishCustomComponentMutation.isPending`.

- Save `onSuccess` handles the new payload and the new-draft jump:

```tsx
const updateCustomComponentSourceMutation = useUpdateCustomComponentSourceMutation({
    onSuccess: (result) => {
        setIsSourceDirty(false);

        const updatedId = result.updateCustomComponentSource.id;

        if (updatedId && updatedId !== id) {
            queryClient.invalidateQueries({queryKey: ['customComponents']});

            navigate(location.pathname.replace(/\/[^/]+$/, `/${updatedId}`));
        } else {
            queryClient.invalidateQueries({queryKey: ['customComponent', {id}]});
            queryClient.invalidateQueries({queryKey: ['customComponentSource', {id}]});
        }
    },
});
```

(`useNavigate` + `useLocation` from react-router-dom; error toasts stay global via the fetch interceptor — no `onError`.)

`CustomComponentListItem.tsx`: next to the language badge (lines ~171-178) add:

```tsx
<Badge className="flex w-24 items-center justify-center" variant={customComponent.status === CustomComponentStatus.Draft ? 'secondary' : 'outline'}>
    {customComponent.status === CustomComponentStatus.Draft ? 'Draft' : 'Published'}
</Badge>
```

- [ ] **Step 5: Run tests + checks**

Run: `cd client && npm run test -- CustomComponent && npm run check`
Expected: PASS (fix sort-keys/lint fallout manually — ESLint does not autofix sort-keys).

- [ ] **Step 6: Commit (operations and generated file separately)**

```bash
git add client/src/graphql/platform/custom-component client/src/ee/pages/settings/platform/custom-components
git commit -m "client - Add custom component publish button, status badges, draft navigation"
git add client/src/shared/middleware/graphql.ts client/src/shared/middleware/graphql-types.ts
git commit -m "client - Regenerate GraphQL client for custom component status"
```

---

## Part 3 — Code workflows (backend)

### Task 7: Container facade draft reconciliation (`update` + reuse-aware `create`)

**Files:**
- Modify: `server/ee/libs/platform/platform-code-workflow/platform-code-workflow-configuration/platform-code-workflow-configuration-api/.../facade/CodeWorkflowContainerFacade.java`
- Modify: `.../platform-code-workflow-configuration-api/.../domain/CodeWorkflowContainer.java`
- Modify: `.../platform-code-workflow-configuration-api/.../service/CodeWorkflowContainerService.java`
- Modify: `.../platform-code-workflow-configuration-service/.../facade/CodeWorkflowContainerFacadeImpl.java`
- Modify: `.../platform-code-workflow-configuration-service/.../service/CodeWorkflowContainerServiceImpl.java`
- Test: `.../platform-code-workflow-configuration-service/src/test/java/com/bytechef/ee/platform/codeworkflow/configuration/facade/CodeWorkflowContainerFacadeTest.java` (new — module has no test dir yet; create it, add test deps to `build.gradle.kts` mirroring the automation-configuration-service test block)

**Interfaces:**
- Consumes: existing `WorkflowService` (`create`, `update(id, definition, version)`, `getWorkflow`, `delete`), `CodeWorkflowFileStorage`.
- Produces (Tasks 8/9 rely on these exact shapes):

```java
    record CodeWorkflowReconciliation(
        CodeWorkflowContainer codeWorkflowContainer,
        Map<String, String> addedWorkflowNameIds,
        Map<String, String> removedWorkflowNameIds) {
    }

    CodeWorkflowReconciliation create(
        String name, String externalVersion, List<WorkflowDefinition> workflowDefinitions, Language language,
        byte[] bytes, PlatformType type, Map<String, String> reusableWorkflowNameIds);

    CodeWorkflowReconciliation update(
        CodeWorkflowContainer codeWorkflowContainer, String externalVersion,
        List<WorkflowDefinition> workflowDefinitions, byte[] bytes, PlatformType type);
```

  plus `CodeWorkflowContainer.removeCodeWorkflow(String workflowName)` and `CodeWorkflowContainerService.update(CodeWorkflowContainer)`.

**Semantics (both methods):** for each incoming `WorkflowDefinition`, a matching name (in `reusableWorkflowNameIds` for `create`, in `getWorkflowNameIds()` for `update`) means the existing Atlas workflow is UPDATED in place (`workflowService.update(id, newDefinition, currentWorkflow.getVersion())`) and its id adopted into the (new or existing) container; an unmatched name creates a new Atlas workflow; a previously-known name absent from the definitions is reported in `removedWorkflowNameIds` (the CALLER deletes the Atlas workflow and its project/integration join row — this facade only removes the `CodeWorkflow` entry from the container set in the `update` case). `addedWorkflowNameIds` contains only newly created workflows. The existing 6-arg `create` keeps its exact behavior by delegating to the 7-arg form with `Map.of()` and returning `.codeWorkflowContainer()`. `update` keeps the container's UUID and stores the blob under the SAME `<uuid>.<ext>` filename (deterministic overwrite — no orphaned blob, and the regenerated definitions keep referencing the same container uuid). Neither method ever publishes anything.

- [ ] **Step 1: Write failing tests**

```java
class CodeWorkflowContainerFacadeTest {

    // mocks: WorkflowService, CodeWorkflowContainerService, CodeWorkflowFileStorage
    // helper: workflowDefinition(name) building a minimal WorkflowDefinition stub/mock

    @Test
    void testUpdateReplacesBlobAndUpdatesMatchedWorkflowsInPlace() {
        // container with codeWorkflows {"wf-a": idA}; definitions [wf-a]
        // expect workflowService.getWorkflow(idA) + workflowService.update(idA, anyString(), version);
        //        no workflowService.create; containerService.update called; container uuid unchanged;
        //        storeCodeWorkflowFile called with "<uuid>.<ext>"
    }

    @Test
    void testUpdateCreatesNewAndReportsRemoved() {
        // container {"wf-a": idA}; definitions [wf-b]
        // expect create for wf-b; result.addedWorkflowNameIds == {wf-b: idB};
        //        result.removedWorkflowNameIds == {wf-a: idA}; container no longer contains wf-a;
        //        workflowService.delete NOT called (caller's job)
    }

    @Test
    void testCreateWithReuseAdoptsExistingWorkflowIds() {
        // reusable {"wf-a": idA}; definitions [wf-a, wf-b]
        // expect update(idA,...) for wf-a and create for wf-b; new container contains BOTH ids;
        //        added == {wf-b: idB}; removed == {}
    }

    @Test
    void testCreateWithReuseReportsDroppedReusableAsRemoved() {
        // reusable {"wf-a": idA, "wf-c": idC}; definitions [wf-a]
        // expect removed == {wf-c: idC}
    }

    @Test
    void testLegacyCreateDelegatesWithNoReuse() {
        // 6-arg create == 7-arg with Map.of(): every definition minted fresh
    }
}
```

Write these fully, mocking `workflowService.create(...)` to return `Workflow` mocks with stubbed `getId()` returning fresh UUID strings (the container's `addCodeWorkflow` requires UUID-parseable ids), and `workflowService.getWorkflow(idA)` returning a mock with `getVersion()`.

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :server:ee:libs:platform:platform-code-workflow:platform-code-workflow-configuration:platform-code-workflow-configuration-service:test > /tmp/t7.log 2>&1; grep -E "FAILED|error:" /tmp/t7.log | head`
Expected: compile failure (new methods/record missing).

- [ ] **Step 3: Implement**

`CodeWorkflowContainer` — add:

```java
    public void removeCodeWorkflow(String workflowName) {
        codeWorkflows.removeIf(codeWorkflow -> Objects.equals(codeWorkflow.getName(), workflowName));
    }
```

`CodeWorkflowContainerService` + impl — add `CodeWorkflowContainer update(CodeWorkflowContainer codeWorkflowContainer)` (assert non-null id, `repository.save`).

`CodeWorkflowContainerFacadeImpl` — implement the two methods sharing one private reconciliation core:

```java
    @Override
    public CodeWorkflowReconciliation create(
        String name, String externalVersion, List<WorkflowDefinition> workflowDefinitions, Language language,
        byte[] bytes, PlatformType type, Map<String, String> reusableWorkflowNameIds) {

        UUID codeWorkflowContainerUuid = UUID.randomUUID();

        CodeWorkflowContainer codeWorkflowContainer = new CodeWorkflowContainer(codeWorkflowContainerUuid);

        Map<String, String> addedWorkflowNameIds = new HashMap<>();

        for (WorkflowDefinition workflowDefinition : workflowDefinitions) {
            String definition = getDefinition(String.valueOf(codeWorkflowContainerUuid), workflowDefinition, type);

            String reusableWorkflowId = reusableWorkflowNameIds.get(workflowDefinition.getName());

            if (reusableWorkflowId == null) {
                Workflow workflow = workflowService.create(definition, Workflow.Format.JSON, Workflow.SourceType.JDBC);

                addedWorkflowNameIds.put(workflowDefinition.getName(), workflow.getId());
                codeWorkflowContainer.addCodeWorkflow(
                    UUID.fromString(Objects.requireNonNull(workflow.getId())), workflowDefinition.getName());
            } else {
                Workflow workflow = workflowService.getWorkflow(reusableWorkflowId);

                workflowService.update(reusableWorkflowId, definition, workflow.getVersion());

                codeWorkflowContainer.addCodeWorkflow(
                    UUID.fromString(reusableWorkflowId), workflowDefinition.getName());
            }
        }

        Map<String, String> removedWorkflowNameIds = removedFrom(reusableWorkflowNameIds, workflowDefinitions);

        codeWorkflowContainer.setExternalVersion(externalVersion);
        codeWorkflowContainer.setLanguage(language);
        codeWorkflowContainer.setName(name);

        FileEntry workflowsFileEntry = codeWorkflowFileStorage.storeCodeWorkflowFile(
            codeWorkflowContainerUuid + "." + language.getExtension(), bytes);

        codeWorkflowContainer.setWorkflows(workflowsFileEntry);

        return new CodeWorkflowReconciliation(
            codeWorkflowContainerService.create(codeWorkflowContainer), addedWorkflowNameIds, removedWorkflowNameIds);
    }

    @Override
    public CodeWorkflowReconciliation update(
        CodeWorkflowContainer codeWorkflowContainer, String externalVersion,
        List<WorkflowDefinition> workflowDefinitions, byte[] bytes, PlatformType type) {

        Map<String, String> existingWorkflowNameIds = codeWorkflowContainer.getWorkflowNameIds();
        Map<String, String> addedWorkflowNameIds = new HashMap<>();

        for (WorkflowDefinition workflowDefinition : workflowDefinitions) {
            String definition = getDefinition(
                String.valueOf(codeWorkflowContainer.getUuid()), workflowDefinition, type);

            String existingWorkflowId = existingWorkflowNameIds.get(workflowDefinition.getName());

            if (existingWorkflowId == null) {
                Workflow workflow = workflowService.create(definition, Workflow.Format.JSON, Workflow.SourceType.JDBC);

                addedWorkflowNameIds.put(workflowDefinition.getName(), workflow.getId());
                codeWorkflowContainer.addCodeWorkflow(
                    UUID.fromString(Objects.requireNonNull(workflow.getId())), workflowDefinition.getName());
            } else {
                Workflow workflow = workflowService.getWorkflow(existingWorkflowId);

                workflowService.update(existingWorkflowId, definition, workflow.getVersion());
            }
        }

        Map<String, String> removedWorkflowNameIds = removedFrom(existingWorkflowNameIds, workflowDefinitions);

        for (String workflowName : removedWorkflowNameIds.keySet()) {
            codeWorkflowContainer.removeCodeWorkflow(workflowName);
        }

        codeWorkflowContainer.setExternalVersion(externalVersion);

        FileEntry workflowsFileEntry = codeWorkflowFileStorage.storeCodeWorkflowFile(
            codeWorkflowContainer.getUuid() + "." + codeWorkflowContainer.getLanguage()
                .getExtension(),
            bytes);

        codeWorkflowContainer.setWorkflows(workflowsFileEntry);

        return new CodeWorkflowReconciliation(
            codeWorkflowContainerService.update(codeWorkflowContainer), addedWorkflowNameIds, removedWorkflowNameIds);
    }

    private static Map<String, String> removedFrom(
        Map<String, String> knownWorkflowNameIds, List<WorkflowDefinition> workflowDefinitions) {

        Set<String> incomingNames = workflowDefinitions.stream()
            .map(WorkflowDefinition::getName)
            .collect(Collectors.toSet());

        return knownWorkflowNameIds.entrySet()
            .stream()
            .filter(entry -> !incomingNames.contains(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
```

Refactor the existing 6-arg `create` to delegate: `return create(name, externalVersion, workflowDefinitions, language, bytes, type, Map.of()).codeWorkflowContainer();` — keep its try/catch RuntimeException wrapper shape by moving the wrapper to both public methods.

- [ ] **Step 4: Run tests to verify they pass**

Run: same command. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/platform/platform-code-workflow
git commit -m "Add draft reconciliation to code workflow container facade"
```

---

### Task 8: Automation editor save drafts; publish only via project header

**Files:**
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-api/.../service/ProjectCodeWorkflowService.java`
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-service/.../service/ProjectCodeWorkflowServiceImpl.java`
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-service/.../facade/ProjectCodeWorkflowFacadeImpl.java`
- Test: extend `.../facade/ProjectCodeWorkflowFacadeSourceTest.java`; new `.../facade/ProjectCodeWorkflowFacadeDraftTest.java`

**Interfaces:**
- Consumes: `CodeWorkflowContainerFacade.create(..., reusableWorkflowNameIds)` / `.update(...)` / `CodeWorkflowReconciliation` from Task 7; existing `ProjectWorkflowService` (`addWorkflow`, `delete(projectId, version, workflowId)`, `getProjectWorkflows(projectId, version)`), `WorkflowService.delete(String)`, `ProjectWorkflow.getWorkflowId()/getUuidAsString()`.
- Produces: `Optional<ProjectCodeWorkflow> fetchProjectCodeWorkflow(long projectId)` on `ProjectCodeWorkflowService`.

- [ ] **Step 1: Write failing tests**

`ProjectCodeWorkflowService` addition first (trivial): add to interface

```java
    Optional<ProjectCodeWorkflow> fetchProjectCodeWorkflow(long projectId);
```

impl:

```java
    @Override
    public Optional<ProjectCodeWorkflow> fetchProjectCodeWorkflow(long projectId) {
        return projectCodeWorkflowRepository.findFirstByProjectIdOrderByIdDesc(projectId);
    }
```

New `ProjectCodeWorkflowFacadeDraftTest` (reuse the loadable-JS-source helpers from `ProjectCodeWorkflowFacadeSourceTest` — it builds real polyglot sources that `loadProjectDefinition` can parse; mock `ProjectService`, `ProjectWorkflowService`, `WorkflowService`, `CodeWorkflowContainerFacade`, `ProjectCodeWorkflowService`, `CodeWorkflowContainerService`, `CodeWorkflowFileStorage`):

```java
    @Test
    void testEditorSaveUpdatesDraftContainerInPlace() {
        // project lastProjectVersion == 3; fetchProjectCodeWorkflow returns join row with projectVersion == 3
        // expect codeWorkflowContainerFacade.update(container, ...) called;
        //        codeWorkflowContainerFacade.create NEVER called;
        //        projectService.publishProject NEVER called (any args);
        //        projectCodeWorkflowService.create NEVER called
    }

    @Test
    void testEditorSaveAfterPublishMintsNewContainerReusingDraftWorkflowsByUuid() {
        // project lastProjectVersion == 2; join row projectVersion == 1 (published) pointing at container C1
        //   with workflowNameIds {"wf-a": "pub-id"}
        // getProjectWorkflows(projectId, 1) -> [ProjectWorkflow(workflowId "pub-id", uuid U)]
        // getProjectWorkflows(projectId, 2) -> [ProjectWorkflow(workflowId "draft-id", uuid U)]
        // expect create(..., reusableWorkflowNameIds == {"wf-a": "draft-id"});
        //        projectCodeWorkflowService.create called with the new container;
        //        projectService.publishProject NEVER called
    }

    @Test
    void testEditorSaveSyncsAddedAndRemovedProjectWorkflows() {
        // reconciliation returns added {"wf-new": "id-n"} and removed {"wf-old": "id-o"}
        // expect projectWorkflowService.addWorkflow(projectId, draftVersion, "id-n");
        //        projectWorkflowService.delete(projectId, draftVersion, "id-o");
        //        workflowService.delete("id-o")
    }

    @Test
    void testUploadSaveStillPublishes() {
        // facade.save(workspaceId, bytes, JS) -> deployInto path unchanged:
        // 6-arg container create + projectCodeWorkflowService.create + addWorkflow per workflow
        // + projectService.publishProject(projectId, null, false)
    }

    @Test
    void testCreateEmptyCodeWorkflowCreatesDraftWithoutPublishing() {
        // createEmptyCodeWorkflow(...) -> project created + draft save path;
        // projectService.publishProject NEVER called
    }
```

Also update `ProjectCodeWorkflowFacadeSourceTest`'s redeploy test (it currently asserts the deployInto shape on `updateCodeWorkflowSource`) to the new draft expectations.

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:test --tests "*ProjectCodeWorkflow*" > /tmp/t8.log 2>&1; grep -E "FAILED|error:" /tmp/t8.log | head`
Expected: FAIL/compile failure.

- [ ] **Step 3: Implement in `ProjectCodeWorkflowFacadeImpl`**

Add `WorkflowService workflowService` to the constructor (field + param, alphabetical placement with the others).

Replace the `deployInto` call in `updateCodeWorkflowSource` with `saveDraft(project, projectDefinition, bytes, language);`. Keep `deployInto` itself untouched for the upload path (`save`).

Change `createEmptyCodeWorkflow` to stop routing through `save` (which publishes):

```java
        String template = readTemplate(language).replace("__NAME__", name);

        byte[] bytes = template.getBytes(StandardCharsets.UTF_8);

        ProjectDefinition projectDefinition;

        try {
            projectDefinition = loadProjectDefinition(language, bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Project project = createProject(workspaceId, projectDefinition);

        saveDraft(project, projectDefinition, bytes, language);

        return project;
```

Add the two new private methods:

```java
    private void saveDraft(Project project, ProjectDefinition projectDefinition, byte[] bytes, Language language) {
        long projectId = Objects.requireNonNull(project.getId());
        int draftProjectVersion = project.getLastProjectVersion();

        ProjectCodeWorkflow latestProjectCodeWorkflow = projectCodeWorkflowService.fetchProjectCodeWorkflow(projectId)
            .orElse(null);

        CodeWorkflowContainerFacade.CodeWorkflowReconciliation reconciliation;

        if (latestProjectCodeWorkflow != null && latestProjectCodeWorkflow.getProjectVersion() == draftProjectVersion) {
            CodeWorkflowContainer codeWorkflowContainer = codeWorkflowContainerService.getCodeWorkflowContainer(
                latestProjectCodeWorkflow.getCodeWorkflowContainerId());

            reconciliation = codeWorkflowContainerFacade.update(
                codeWorkflowContainer, projectDefinition.getVersion(), projectDefinition.getWorkflows(), bytes,
                PlatformType.AUTOMATION);
        } else {
            Map<String, String> reusableWorkflowNameIds = latestProjectCodeWorkflow == null
                ? Map.of()
                : resolveDraftWorkflowNameIds(latestProjectCodeWorkflow, projectId, draftProjectVersion);

            reconciliation = codeWorkflowContainerFacade.create(
                projectDefinition.getName(), projectDefinition.getVersion(), projectDefinition.getWorkflows(),
                language, bytes, PlatformType.AUTOMATION, reusableWorkflowNameIds);

            projectCodeWorkflowService.create(reconciliation.codeWorkflowContainer(), project);
        }

        for (String workflowId : reconciliation.addedWorkflowNameIds()
            .values()) {

            projectWorkflowService.addWorkflow(projectId, draftProjectVersion, workflowId);
        }

        for (String workflowId : reconciliation.removedWorkflowNameIds()
            .values()) {

            projectWorkflowService.delete(projectId, draftProjectVersion, workflowId);
            workflowService.delete(workflowId);
        }
    }

    /**
     * Maps the published container's workflow names onto the current draft version's workflow ids. The facade publish
     * duplicates each workflow into the new draft version under a new workflow id but preserves the ProjectWorkflow
     * uuid across both rows, so the chain is: published container name -> published workflow id -> row uuid at the
     * container's (published) version -> draft-version row with the same uuid -> draft workflow id.
     */
    private Map<String, String> resolveDraftWorkflowNameIds(
        ProjectCodeWorkflow publishedProjectCodeWorkflow, long projectId, int draftProjectVersion) {

        CodeWorkflowContainer publishedCodeWorkflowContainer = codeWorkflowContainerService.getCodeWorkflowContainer(
            publishedProjectCodeWorkflow.getCodeWorkflowContainerId());

        Map<String, String> publishedWorkflowNameIds = publishedCodeWorkflowContainer.getWorkflowNameIds();

        Map<String, String> workflowIdUuids = projectWorkflowService.getProjectWorkflows(
            projectId, publishedProjectCodeWorkflow.getProjectVersion())
            .stream()
            .collect(Collectors.toMap(ProjectWorkflow::getWorkflowId, ProjectWorkflow::getUuidAsString));

        Map<String, String> uuidDraftWorkflowIds = projectWorkflowService.getProjectWorkflows(
            projectId, draftProjectVersion)
            .stream()
            .collect(Collectors.toMap(ProjectWorkflow::getUuidAsString, ProjectWorkflow::getWorkflowId));

        Map<String, String> draftWorkflowNameIds = new HashMap<>();

        for (Map.Entry<String, String> entry : publishedWorkflowNameIds.entrySet()) {
            String uuid = workflowIdUuids.get(entry.getValue());

            String draftWorkflowId = uuid == null ? null : uuidDraftWorkflowIds.get(uuid);

            if (draftWorkflowId != null) {
                draftWorkflowNameIds.put(entry.getKey(), draftWorkflowId);
            }
        }

        return draftWorkflowNameIds;
    }
```

(Imports to add: `ProjectWorkflow`, `HashMap`, `Collectors`, `WorkflowService`.)

Note the removed `projectService.publishProject(...)` call exists ONLY in `deployInto` — which the editor path no longer reaches. Do not modify `deployInto`.

- [ ] **Step 4: Run tests to verify they pass**

Run: same command, then the full module: `./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:test > /tmp/t8b.log 2>&1; grep -E "^> Task .* FAILED" /tmp/t8b.log`
Expected: PASS (fix any pre-existing test that pinned the old always-publish editor behavior).

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/automation server/ee/libs/platform/platform-code-workflow
git commit -m "Code workflow editor save updates draft without publishing"
```

---

### Task 9: Embedded mirror

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/.../facade/IntegrationCodeWorkflowFacadeImpl.java`
- Test: new `.../facade/IntegrationCodeWorkflowFacadeDraftTest.java`; update `.../facade/IntegrationCodeWorkflowFacadeSourceTest.java` redeploy expectations

**Interfaces:**
- Consumes: Task 7 facade API; existing `IntegrationCodeWorkflowService.fetchIntegrationCodeWorkflow(long)` (already `Optional`), `IntegrationWorkflowService` (`addWorkflow`, `delete(integrationId, version, workflowId)`, `getIntegrationWorkflows(integrationId, version)`), `IntegrationWorkflow.getWorkflowId()/getUuidAsString()`, `Integration.getLastIntegrationVersion()`.

- [ ] **Step 1: Write failing tests**

Mirror Task 8's five tests with embedded names: `testEditorSaveUpdatesDraftContainerInPlace`, `testEditorSaveAfterPublishMintsNewContainerReusingDraftWorkflowsByUuid`, `testEditorSaveSyncsAddedAndRemovedIntegrationWorkflows`, `testUploadSaveStillPublishes` (asserts `integrationService.publishIntegration(id, null)` still called on `save(bytes, language)`), `testCreateEmptyCodeWorkflowCreatesDraftWithoutPublishing` (asserts `publishIntegration` never called). Reuse source-building helpers from `IntegrationCodeWorkflowFacadeSourceTest`. Use `PlatformType.EMBEDDED` in the container facade expectations.

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests "*IntegrationCodeWorkflow*" > /tmp/t9.log 2>&1; grep -E "FAILED|error:" /tmp/t9.log | head`
Expected: FAIL.

- [ ] **Step 3: Implement**

In `IntegrationCodeWorkflowFacadeImpl`: add `WorkflowService` dependency; in `updateCodeWorkflowSource` replace the `deployInto(...)` call with `saveDraft(integration, integrationDefinition, workflowDefinitions, bytes, language);`; change `createEmptyCodeWorkflow` to `createIntegration` + `saveDraft` (mirroring Task 8's shape — load the definition first, create the integration, then draft-save; keep the existing guards). `save(byte[], Language)` keeps calling `deployInto` unchanged.

`saveDraft` and `resolveDraftWorkflowNameIds` are the Task 8 implementations transposed:
- `projectCodeWorkflowService.fetchProjectCodeWorkflow` → `integrationCodeWorkflowService.fetchIntegrationCodeWorkflow`
- `latest.getProjectVersion() == project.getLastProjectVersion()` → `latest.getIntegrationVersion() == integration.getLastIntegrationVersion()`
- `projectWorkflowService.addWorkflow/delete/getProjectWorkflows` → `integrationWorkflowService.addWorkflow/delete/getIntegrationWorkflows`
- `ProjectWorkflow::getWorkflowId/getUuidAsString` → `IntegrationWorkflow::getWorkflowId/getUuidAsString`
- `PlatformType.AUTOMATION` → `PlatformType.EMBEDDED`
- `projectCodeWorkflowService.create(container, project)` → `integrationCodeWorkflowService.create(container, integration)`

Write the transposed code out in full in the file (do not extract a shared abstraction across the two facades — they live in different modules with different domain types, and the repo keeps these mirrors as parallel code).

- [ ] **Step 4: Run tests to verify they pass**

Run: full embedded module test task, grep `^> Task .* FAILED`.

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/embedded
git commit -m "Embedded code workflow editor save updates draft without publishing"
```

---

## Part 4 — Verification

### Task 10: Full checks + spec cross-read

- [ ] **Step 1: Server-wide checks**

```bash
./gradlew spotlessApply > /tmp/v1.log 2>&1; echo $?
./gradlew compileJava compileTestJava --continue > /tmp/v2.log 2>&1; grep "^> Task .* FAILED" /tmp/v2.log
./gradlew :server:ee:libs:platform:platform-custom-component:platform-custom-component-configuration:platform-custom-component-configuration-service:test \
  :server:ee:libs:platform:platform-custom-component:platform-custom-component-handler:test \
  :server:ee:libs:platform:platform-code-workflow:platform-code-workflow-configuration:platform-code-workflow-configuration-service:test \
  :server:ee:libs:automation:automation-configuration:automation-configuration-service:test \
  :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test \
  --continue > /tmp/v3.log 2>&1; grep "^> Task .* FAILED" /tmp/v3.log
```

Expected: no FAILED lines. If spotless changed files, amend them into the relevant commits or add a formatting commit.

- [ ] **Step 2: Client checks**

```bash
cd client && npm run format && npm run check
```

Expected: exit 0.

- [ ] **Step 3: Spec coverage cross-read**

Re-read `docs/superpowers/specs/2026-08-05-draft-publish-editors-design.md` section by section and confirm each maps to a landed change: data model (Task 1), save semantics (Task 3), publish + audit (Tasks 2/5), runtime visibility (Task 4), upload exception (Task 3), client (Task 6), container reconciliation + identity preservation (Task 7), automation draft/publish split + createEmpty (Task 8), embedded mirror (Task 9), migration backfill (Task 1). Fix anything missing before declaring done.

- [ ] **Step 4: Final commit (if verification produced fixes)**

```bash
git add -A && git commit -m "Draft/publish editors: verification fixes"
```

---

## Self-review notes (already applied)

- **Publish direction pinned:** `ProjectFacadeImpl.publishProject` moves existing `ProjectWorkflow` rows FORWARD to the new draft version (with duplicated workflow ids) and pins snapshot rows to the old version, preserving `uuid` across both — `resolveDraftWorkflowNameIds` in Tasks 8/9 depends on exactly this and is documented inline.
- **No code-workflow client changes:** the editors' Save button and the project/integration header Publish already exist; only server semantics change. The GraphQL `updateCodeWorkflowSource: Boolean!` contract is unchanged.
- **Known legacy artifact (out of scope):** today's `deployInto` accumulated duplicate `ProjectWorkflow` rows in the draft version on every save; pre-upgrade duplicates are not cleaned up by this work.
- **`CustomComponentService.update` contract change** (full field copy) is load-bearing for Task 3 and covered by Task 2's `testUpdatePersistsComponentFileAndVersionAndStatus`.
