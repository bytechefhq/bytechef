# Apps — Template (`create-bytechef-app`) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship `create-bytechef-app` — a non-interactive scaffolder with a vendored TanStack Start template that is
safe by default and built for AI coding agents — plus the `GET /api/app/v1/actions` endpoint and SDK `actions.list()`
that let an agent discover every allowed action with a faithful input schema.

**Architecture:** Server first: a recursive `ActionInputSchemaGenerator` in `platform-component-api`, a `getActions()`
listing in `AppExecutionFacadeImpl` mirroring `getTools()`, and the endpoint on `AppConnectionApiController`. Then the
SDK gains `actions.list()` from the regenerated contract. The template lives at
`sdks/frontend/automation/app/create/template/` (not an npm workspace member) and is produced by TanStack's own
scaffolder (`--add-ons shadcn --deployment nitro`) plus shadcn's four components, then edited: a dev-only overview and
"Try it" reached only through a dynamic import gated on `import.meta.env.DEV` (so production builds never compile or
register those server functions), a status script, a leak check, agent instructions, Docker and Vercel files. The
scaffolder copies the template with zero runtime dependencies. A CI harness packs both packages, scaffolds, builds and
runs the generated app against a mock ByteChef.

**Tech Stack:** Java 25 / Spring Boot (EE module `automation-app`, Jackson 3 `tools.jackson`), TypeScript 5.7.3, Node
≥ 22.12, TanStack Start 1.168.60 + Router 1.170.41, Nitro 3.0.260610-beta, Vite 8.3.3, React 19.3.0, Tailwind CSS
4.3.3, shadcn 4.21.4 (`cn` 0.4.0, `radix-ui` 1.7.0), vitest 4.1.7, `@bytechef/app` 0.1.0.

**Spec:** `docs/superpowers/specs/2026-10-07-apps-template-design.md`. Background: `.agents/apps.md`,
`docs/superpowers/specs/2026-10-06-apps-workflows-and-sdk-design.md` (B), the SDK at
`sdks/frontend/automation/app/library`.

## Deviations from the spec, settled by the facts gathered before writing this plan

1. **Two server modules, not one.** The spec's table puts `getAppOverview` and `tryAction` in `src/server/bytechef.ts`
   "declared only when `import.meta.env.DEV` is true". A `createServerFn` must be a module-level declaration, so the
   gate cannot wrap the declarations. Verified alternative: `src/server/bytechef.ts` holds only the middleware (no
   `createServerFn`); `src/server/dev.ts` declares the two server functions; the route reaches `dev.ts` only through
   `import.meta.env.DEV ? import('../server/dev') : …`. Start's compiler transforms modules on demand and builds the
   server-function manifest from transformed modules only, and a build (`vite build` and `vite build --mode
   development` alike) eliminates the gated import entirely — the module is never transformed and neither function is
   registered. Only `NODE_ENV=development vite build` flips `DEV` to true; `npm run check` fails on such a build
   (Global Constraints).
2. **No positive "RPC marker" in the leak check.** `_serverFn` is the runtime URL prefix and is present in every Start
   client bundle, even with no server functions. The template's `check-bundle.mjs` keeps only the negative checks plus
   a server-bundle check that no dev-only server function was compiled.
3. **shadcn's generated components import `cn` from the `cn` package** (shadcn 4.21.4 registry; the `components.json`
   aliases do not change this) and use the `radix-ui` umbrella package. The template keeps the components verbatim and
   adds `cn` and `radix-ui` as pinned dependencies; `src/lib/utils.ts` is removed because nothing uses it.
4. **Under npm the scaffolder's flags need `--`**: `npm create bytechef-app@latest my-app -- --url …`. CI runs the
   packed bin through `npm exec --package=<tgz> -- create-bytechef-app …`, which passes everything after the bin name
   through untouched (verified). `npx <absolute-path>.tgz` does not work; `npx --package=<tgz> create-bytechef-app` does.
5. **`AppApiKeyFilterChainTest` does not enumerate paths** (it serves three constants through a `PingController`);
   `AppApiKeySecurityConfigurer.PATH_PATTERN` is `^/api/app/v[0-9]+/.+`, which already covers `/actions`. The plan adds
   a one-line pattern test instead of a filter-chain case.
6. **`npm pack` strips `.gitignore` and `.npmrc`** even under `files`; `_gitignore`/`_dockerignore` survive (verified).

## Global Constraints

- Package `create-bytechef-app` `0.1.0` (lockstep with `@bytechef/app` `0.1.0`), `"type": "module"`, `bin`
  `create-bytechef-app` → `bin/create-bytechef-app.mjs`, `files: ["bin", "template"]`, `engines.node >=22.12.0`,
  `publishConfig.access: public`, zero runtime dependencies (`node:util` `parseArgs`, `node:fs`, `node:path`,
  `node:child_process`, `node:os`). Never publish to npmjs; Verdaccio only when a task says so.
- Template dependency pins (exact): `@tanstack/react-start` `1.168.60`, `@tanstack/react-router` `1.170.41`,
  `@tanstack/react-router-devtools` `1.167.2`, `@tanstack/react-devtools` `0.10.13`, `@tanstack/devtools-vite` `0.8.5`,
  `@tanstack/router-cli` `1.167.40`, `nitro` `3.0.260610-beta`, `react` `19.3.0`, `react-dom` `19.3.0`, `vite`
  `8.3.3`, `@vitejs/plugin-react` `6.1.2`, `tailwindcss` `4.3.3`, `@tailwindcss/vite` `4.3.3`, `typescript` `5.7.3`,
  `@types/react` `19.3.0`, `@types/react-dom` `19.3.0`, `@types/node` `22.20.5`, `class-variance-authority` `0.7.1`,
  `clsx` `2.1.1`, `tailwind-merge` `3.7.0`, `lucide-react` `0.577.0`, `tw-animate-css` `1.4.0`, `cn` `0.4.0`,
  `radix-ui` `1.7.0`, `@bytechef/app` `0.1.0` (the scaffolder rewrites only this one).
- Template scripts: `dev` = `vite dev --port 3000`; `build` = `vite build`; `start` = `node .output/server/index.mjs`;
  `check` = `tsc --noEmit && vite build && node scripts/check-bundle.mjs`; `bytechef:status` =
  `node --env-file-if-exists=.env.local scripts/bytechef-status.mjs`; `generate-routes` = `tsr generate`.
- Template environment: `BYTECHEF_URL`, `BYTECHEF_APP_KEY`; the shell environment wins over `.env.local` (Node's
  `--env-file-if-exists` and Vite's env loading both behave so). Nitro's server honours `PORT` (or `NITRO_PORT`, default
  3000) and `HOST` (or `NITRO_HOST`).
- `check-bundle.mjs` (template): scans `.output/public` (fallback `dist/client`) files matching
  `/\.(js|mjs|html|css|json|map)$/`; fails on `BYTECHEF_APP_KEY`, `/api/app/v1`, or the key value read from
  `process.env.BYTECHEF_APP_KEY` and from `.env.local`; fails if any file under `.output/server` contains
  `getAppOverview_createServerFn_handler` or `tryAction_createServerFn_handler` (a dev-mode build) or
  `BYTECHEF_APP_KEY=` followed by a non-empty value.
- Server: new class `com.bytechef.platform.component.util.ActionInputSchemaGenerator` (platform-component-api, Jackson 3
  `tools.jackson.databind`), `AppActionDTO(String bindingKey, String componentName, String actionName,
  @Nullable String description, Map<String, Object> inputSchema)` in `automation-app-api`, `List<AppActionDTO>
  getActions()` on `AppExecutionFacade`, `GET /actions` under tag `app-connection`, operationId `getActions`, model
  `AppAction` with `required: [bindingKey, componentName, actionName, inputSchema]`, `description` `nullable: true`,
  `inputSchema` `type: object`, `additionalProperties: true`.
- Schema rules (generator): root `{"type":"object","properties":…,"required":[…],"additionalProperties":false}`;
  `required` lists properties with `getRequired()` true AND `getDisplayCondition()` null; STRING → `string`,
  INTEGER → `integer`, NUMBER → `number`, BOOLEAN → `boolean`, DATE → `string` + `format: date`, DATE_TIME →
  `string` + `format: date-time`, TIME → `string` + `format: time`, NULL → `null`, OBJECT → `object` with nested
  `properties`/`required` (and `additionalProperties: false` only when it declares properties), ARRAY → `array` with
  `items` (one item type → that schema; several → `anyOf`; none → `{}`), FILE_ENTRY → `object` with its properties,
  DYNAMIC_PROPERTIES → `{"type":"object","additionalProperties":true}`; `description` copied when non-null;
  `enum` from `getOptions()` values when non-empty, except BOOLEAN (its default options are True/False) and except
  OBJECT/ARRAY; `default` from `getDefaultValue()` when non-null on STRING/INTEGER/NUMBER/BOOLEAN.
- SDK: `actions.list(options?: CallOptions): Promise<AppAction[]>`; `AppAction = Omit<components['schemas']['AppAction'],
  'inputSchema'> & {inputSchema: JsonSchema}`; exported from `@bytechef/app`.
- Commit subjects: `- <description>` for server, `- sdk - <description>` for anything under `sdks/`, `- docs - …` for
  docs/specs/deep-dive, `- Build …` style plain for CI. No `Co-Authored-By`, no "Generated with". Never amend; commit by
  path; never `git add -A`; never `git stash`. Run `spotlessApply` + `check` on touched Gradle modules; `prettier` +
  lint on touched JS/TS; vitest output pristine (no `Unhandled`). No rationale `//` comments; JSDoc/Javadoc contracts
  are fine. Checkstyle: test method names camelCase without `_`; no `TODO:`; blank line before control statements.
- Process: every npm/npx/gradle command foreground with Bash `timeout: 600000`, output to a file, `$?` on its own line.
  Background only an HTTP server, via the Bash tool's `run_in_background: true`, and stop it before the task ends. No
  `&`, no `sleep` (use `curl --retry … --retry-connrefused`). 600 s no-progress watchdog: write reports incrementally,
  commit per task. zsh: quote paths; `${VAR}` before `:`; `$x[` is a subscript — avoid `$d[^:]`-style regexes.
- `S` = the session scratchpad directory, stated in every dispatch that uses it; `W` =
  `sdks/frontend/automation/app`; `L` = `W/library`; `C` = `W/create`; `T` = `C/template`.

## Review Focus

1. **`npm create bytechef-app my-app --url X` without `--` under npm** — a person expects either success or a clear
   message; npm silently eats the flags. The scaffolder refuses a second positional and detects `npm_config_url` /
   `npm_config_install`, explaining the `--` rule — Task 5 (`args.test.mjs`).
2. **`NODE_ENV=development npm run build`** (a developer's shell exporting it) ships the overview and `tryAction` in a
   production image — `npm run check` must fail on that build — Task 4 Step 9 and Task 6 Step 5.
3. **A binding whose component is unavailable, or a server without `/actions`** — status must still list what the app
   is allowed and say why there is no schema, never hide the operation — Task 4 (`bytechef-status.mjs`) and Task 6
   (mock 404 variant).
4. **A stale `BYTECHEF_APP_KEY` exported in the shell** silently beats `.env.local` — the scaffolder says when it took
   the key from the environment, and README/AGENTS.md state the precedence — Task 5 (`env.test.mjs`) and Task 4 docs.
5. **A nested-object action input** (array of objects with options and a conditional field) — the generated schema
   must be faithful, or the agent sends wrong inputs while being told not to guess — Task 1 (`ActionInputSchemaGeneratorTest`).

---

## File structure

```
server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/util/ActionInputSchemaGenerator.java
server/libs/platform/platform-component/platform-component-api/src/test/java/com/bytechef/platform/component/util/ActionInputSchemaGeneratorTest.java
server/ee/libs/automation/automation-app/automation-app-api/src/main/java/com/bytechef/ee/automation/app/dto/AppActionDTO.java
server/ee/libs/automation/automation-app/automation-app-api/src/main/java/com/bytechef/ee/automation/app/facade/AppExecutionFacade.java   (+ getActions)
server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/facade/AppExecutionFacadeImpl.java (+ getActions)
server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/facade/AppExecutionFacadeTest.java (+ tests)
server/ee/libs/automation/automation-app/automation-app-public-rest/openapi.yaml (+ /actions, AppAction)
server/ee/libs/automation/automation-app/automation-app-public-rest/generated/... (regenerated)
server/ee/libs/automation/automation-app/automation-app-public-rest/src/main/java/.../AppConnectionApiController.java (+ getActions)
server/ee/libs/automation/automation-app/automation-app-public-rest/src/test/java/.../AppConnectionApiControllerTest.java (+ tests)
server/ee/libs/automation/automation-security-web/automation-security-web-impl/src/test/java/.../AppApiKeyPathPatternTest.java (new, one assertion)
sdks/frontend/automation/app/
  package.json                      (+ workspace "create", scripts check:template, test:create)
  library/src/generated/openapi.ts  (regenerated)
  library/src/core/types.ts         (+ AppAction)
  library/src/core/connections.ts   (+ actions.list)
  library/src/core/connections.test.ts
  library/src/index.ts              (+ type AppAction)
  library/README.md                 (+ actions.list)
  create/
    package.json  README.md  bin/create-bytechef-app.mjs  src/{args,scaffold,env,install}.mjs  test/*.test.mjs  vitest.config.mjs
    template/                       (the generated app; NOT an npm workspace)
      package.json  vite.config.ts  tsconfig.json  tsr.config.json  components.json  vercel.json  Dockerfile
      _gitignore  _dockerignore  .env.example  AGENTS.md  CLAUDE.md  README.md
      src/{router.tsx, routeTree.gen.ts, styles.css}
      src/routes/{__root.tsx, index.tsx}
      src/server/{bytechef.ts, overview.ts, dev.ts}
      src/components/overview.tsx
      src/components/ui/{button,card,table,badge}.tsx
      scripts/{bytechef-status.mjs, check-bundle.mjs}
  scripts/check-template.mjs        (CI harness)  scripts/mock-bytechef.mjs  scripts/fixtures/app.json
.github/workflows/build_pull_request.yml, build_push.yml   (+ check:template)
docs/superpowers/specs/*.md, .agents/apps.md                 (Task 7)
```

---

### Task 1: `ActionInputSchemaGenerator` (server, platform-component-api)

**Files:**
- Create: `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/util/ActionInputSchemaGenerator.java`
- Test: `server/libs/platform/platform-component/platform-component-api/src/test/java/com/bytechef/platform/component/util/ActionInputSchemaGeneratorTest.java`

**Interfaces:**
- Consumes: the platform property model in `com.bytechef.platform.component.domain` (`Property` with `getType()` →
  `com.bytechef.component.definition.Property.Type`; `BaseProperty` in `com.bytechef.platform.domain` with `String
  getName()`, `@Nullable String getDescription()`, `@Nullable String getDisplayCondition()`, `boolean getRequired()`;
  `ValueProperty<V>` with `@Nullable V getDefaultValue()`; `ObjectProperty.getProperties()`,
  `ArrayProperty.getItems()`, `FileEntryProperty.getProperties()`; `List<Option> getOptions()` on String/Integer/
  Number/Boolean/Date/DateTime/Time/Object/Array properties with `Option.getValue()`), Jackson 3
  (`tools.jackson.databind.json.JsonMapper`, `node.ObjectNode`, `node.ArrayNode`) and
  `org.springframework.ai.util.JacksonUtils.getDefaultJsonMapper()` as `JsonSchemaGeneratorUtils` does.
- Produces: `public final class ActionInputSchemaGenerator { public static Map<String, Object>
  generateInputSchema(List<? extends Property> properties); }` — a JSON-Schema object as a `Map` (converted from the
  node with `jsonMapper.convertValue(node, new TypeReference<Map<String, Object>>() {})`).

- [ ] **Step 1: Write the failing test** `ActionInputSchemaGeneratorTest.java` (same package as the generator;
  license header as the sibling test files in that module):

  ```java
  package com.bytechef.platform.component.util;

  import static com.bytechef.component.definition.ComponentDsl.array;
  import static com.bytechef.component.definition.ComponentDsl.bool;
  import static com.bytechef.component.definition.ComponentDsl.date;
  import static com.bytechef.component.definition.ComponentDsl.dynamicProperties;
  import static com.bytechef.component.definition.ComponentDsl.integer;
  import static com.bytechef.component.definition.ComponentDsl.object;
  import static com.bytechef.component.definition.ComponentDsl.option;
  import static com.bytechef.component.definition.ComponentDsl.string;
  import static org.assertj.core.api.Assertions.assertThat;

  import com.bytechef.platform.component.domain.Property;
  import java.util.List;
  import java.util.Map;
  import org.junit.jupiter.api.Test;

  class ActionInputSchemaGeneratorTest {

      @Test
      void testGeneratesNestedObjectsArraysOptionsAndConditionalFields() {
          List<Property> properties = List.of(
              Property.toProperty(string("name").description("Full name").required(true)),
              Property.toProperty(
                  string("priority").options(option("Low", "low"), option("High", "high")).defaultValue("low")),
              Property.toProperty(integer("retries").required(true).displayCondition("priority == 'high'")),
              Property.toProperty(
                  array("lines").required(true)
                      .items(
                          object("line").properties(
                              string("sku").required(true),
                              integer("quantity").required(true).defaultValue(1),
                              bool("gift")))),
              Property.toProperty(date("dueDate")),
              Property.toProperty(dynamicProperties("extra")));

          Map<String, Object> schema = ActionInputSchemaGenerator.generateInputSchema(properties);

          assertThat(schema).containsEntry("type", "object");
          assertThat(schema).containsEntry("additionalProperties", false);
          assertThat(schema.get("required")).isEqualTo(List.of("name", "lines"));

          @SuppressWarnings("unchecked")
          Map<String, Object> props = (Map<String, Object>) schema.get("properties");

          assertThat(props.get("name")).isEqualTo(Map.of("type", "string", "description", "Full name"));
          assertThat(props.get("priority"))
              .isEqualTo(Map.of("type", "string", "enum", List.of("low", "high"), "default", "low"));
          assertThat(props.get("retries")).isEqualTo(Map.of("type", "integer"));
          assertThat(props.get("dueDate")).isEqualTo(Map.of("type", "string", "format", "date"));
          assertThat(props.get("extra")).isEqualTo(Map.of("type", "object", "additionalProperties", true));

          @SuppressWarnings("unchecked")
          Map<String, Object> lines = (Map<String, Object>) props.get("lines");

          assertThat(lines.get("type")).isEqualTo("array");

          @SuppressWarnings("unchecked")
          Map<String, Object> line = (Map<String, Object>) lines.get("items");

          assertThat(line.get("type")).isEqualTo("object");
          assertThat(line.get("required")).isEqualTo(List.of("sku", "quantity"));
          assertThat(line.get("additionalProperties")).isEqualTo(false);

          @SuppressWarnings("unchecked")
          Map<String, Object> lineProps = (Map<String, Object>) line.get("properties");

          assertThat(lineProps.get("quantity")).isEqualTo(Map.of("type", "integer", "default", 1L));
          assertThat(lineProps.get("gift")).isEqualTo(Map.of("type", "boolean"));
      }

      @Test
      void testArrayWithoutItemTypeAndWithSeveralItemTypes() {
          Map<String, Object> schema = ActionInputSchemaGenerator.generateInputSchema(List.of(
              Property.toProperty(array("tags")),
              Property.toProperty(array("mixed").items(string("text"), integer("count")))));

          @SuppressWarnings("unchecked")
          Map<String, Object> props = (Map<String, Object>) schema.get("properties");

          assertThat(props.get("tags")).isEqualTo(Map.of("type", "array", "items", Map.of()));
          assertThat(props.get("mixed")).isEqualTo(
              Map.of(
                  "type", "array",
                  "items", Map.of("anyOf", List.of(Map.of("type", "string"), Map.of("type", "integer")))));
      }

      @Test
      void testBooleanOptionsAreNotAnEnumAndEmptyInputHasNoRequired() {
          Map<String, Object> schema = ActionInputSchemaGenerator.generateInputSchema(
              List.of(Property.toProperty(bool("flag"))));

          @SuppressWarnings("unchecked")
          Map<String, Object> props = (Map<String, Object>) schema.get("properties");

          assertThat(props.get("flag")).isEqualTo(Map.of("type", "boolean"));
          assertThat(schema.get("required")).isEqualTo(List.of());
          assertThat(ActionInputSchemaGenerator.generateInputSchema(List.of()).get("properties"))
              .isEqualTo(Map.of());
      }
  }
  ```

  (`integer(...).defaultValue(1)` is stored as a `Long`, hence `1L`.)

- [ ] **Step 2: Run to verify it fails**

  ```bash
  ./gradlew ":server:libs:platform:platform-component:platform-component-api:test" --tests "*ActionInputSchemaGeneratorTest*" > "$S/t1-red.log" 2>&1; echo $?
  ```

  Expected: non-zero, compilation error naming `ActionInputSchemaGenerator`.

- [ ] **Step 3: Implement** `ActionInputSchemaGenerator.java` (Apache license header like `JsonSchemaGeneratorUtils`;
  same package):

  ```java
  package com.bytechef.platform.component.util;

  import com.bytechef.component.definition.Property.Type;
  import com.bytechef.platform.component.domain.ArrayProperty;
  import com.bytechef.platform.component.domain.BooleanProperty;
  import com.bytechef.platform.component.domain.FileEntryProperty;
  import com.bytechef.platform.component.domain.IntegerProperty;
  import com.bytechef.platform.component.domain.NumberProperty;
  import com.bytechef.platform.component.domain.ObjectProperty;
  import com.bytechef.platform.component.domain.Option;
  import com.bytechef.platform.component.domain.Property;
  import com.bytechef.platform.component.domain.StringProperty;
  import com.bytechef.platform.component.domain.ValueProperty;
  import java.util.List;
  import java.util.Map;
  import org.springframework.ai.util.JacksonUtils;
  import tools.jackson.core.type.TypeReference;
  import tools.jackson.databind.json.JsonMapper;
  import tools.jackson.databind.node.ArrayNode;
  import tools.jackson.databind.node.ObjectNode;

  /**
   * Builds a JSON Schema for an action's input from the full property tree: nested objects, array items, static
   * options as enums, defaults, and display-conditioned properties left out of {@code required}.
   *
   * @author Ivica Cardic
   */
  public final class ActionInputSchemaGenerator {

      private static final JsonMapper JSON_MAPPER = JacksonUtils.getDefaultJsonMapper();

      private ActionInputSchemaGenerator() {
      }

      public static Map<String, Object> generateInputSchema(List<? extends Property> properties) {
          ObjectNode schema = objectSchema(properties, true);

          return JSON_MAPPER.convertValue(schema, new TypeReference<>() {});
      }

      private static ObjectNode objectSchema(List<? extends Property> properties, boolean closed) {
          ObjectNode schema = JSON_MAPPER.createObjectNode();

          schema.put("type", "object");

          ObjectNode propertiesNode = schema.putObject("properties");
          ArrayNode requiredNode = JSON_MAPPER.createArrayNode();

          for (Property property : properties) {
              if (property.getName() == null) {
                  continue;
              }

              propertiesNode.set(property.getName(), propertySchema(property));

              if (property.getRequired() && property.getDisplayCondition() == null) {
                  requiredNode.add(property.getName());
              }
          }

          schema.set("required", requiredNode);

          if (closed) {
              schema.put("additionalProperties", false);
          }

          return schema;
      }

      private static ObjectNode propertySchema(Property property) {
          ObjectNode schema = switch (property.getType()) {
              case ARRAY -> arraySchema((ArrayProperty) property);
              case BOOLEAN -> typed("boolean");
              case DATE -> formatted("date");
              case DATE_TIME -> formatted("date-time");
              case DYNAMIC_PROPERTIES -> openObject();
              case FILE_ENTRY -> objectSchema(((FileEntryProperty) property).getProperties(), false);
              case INTEGER -> typed("integer");
              case NULL -> typed("null");
              case NUMBER -> typed("number");
              case OBJECT -> nestedObjectSchema((ObjectProperty) property);
              case STRING -> typed("string");
              case TIME -> formatted("time");
          };

          if (property.getDescription() != null) {
              schema.put("description", property.getDescription());
          }

          addEnum(property, schema);
          addDefault(property, schema);

          return schema;
      }

      private static ObjectNode nestedObjectSchema(ObjectProperty property) {
          List<? extends Property> properties = property.getProperties();

          if (properties.isEmpty()) {
              return openObject();
          }

          return objectSchema(properties, true);
      }

      private static ObjectNode arraySchema(ArrayProperty property) {
          ObjectNode schema = typed("array");
          List<? extends Property> items = property.getItems();

          if (items.isEmpty()) {
              schema.putObject("items");
          } else if (items.size() == 1) {
              schema.set("items", propertySchema(items.getFirst()));
          } else {
              ArrayNode anyOf = schema.putObject("items")
                  .putArray("anyOf");

              for (Property item : items) {
                  anyOf.add(propertySchema(item));
              }
          }

          return schema;
      }

      private static void addEnum(Property property, ObjectNode schema) {
          List<Option> options = switch (property) {
              case StringProperty stringProperty -> stringProperty.getOptions();
              case IntegerProperty integerProperty -> integerProperty.getOptions();
              case NumberProperty numberProperty -> numberProperty.getOptions();
              default -> List.of();
          };

          if (options.isEmpty()) {
              return;
          }

          ArrayNode enumNode = schema.putArray("enum");

          for (Option option : options) {
              enumNode.add(JSON_MAPPER.valueToTree(option.getValue()));
          }
      }

      private static void addDefault(Property property, ObjectNode schema) {
          boolean scalar = property instanceof StringProperty || property instanceof IntegerProperty ||
              property instanceof NumberProperty || property instanceof BooleanProperty;

          if (!scalar) {
              return;
          }

          Object defaultValue = ((ValueProperty<?>) property).getDefaultValue();

          if (defaultValue != null) {
              schema.set("default", JSON_MAPPER.valueToTree(defaultValue));
          }
      }

      private static ObjectNode typed(String type) {
          ObjectNode schema = JSON_MAPPER.createObjectNode();

          schema.put("type", type);

          return schema;
      }

      private static ObjectNode formatted(String format) {
          ObjectNode schema = typed("string");

          schema.put("format", format);

          return schema;
      }

      private static ObjectNode openObject() {
          ObjectNode schema = typed("object");

          schema.put("additionalProperties", true);

          return schema;
      }
  }
  ```

  If `Property.Type` has values beyond the twelve listed (the switch must be exhaustive), add the missing cases as
  `openObject()` and say so in the report. If `JacksonUtils.getDefaultJsonMapper()` is not importable in this module,
  use `JsonMapper.builder().build()` and say so. If `convertValue(node, TypeReference)` yields `LinkedHashMap` values
  with `Long`/`Integer` types that break the test's `1L` expectation, adjust the expectation to what Jackson produces
  and note it.

- [ ] **Step 4: Run to verify it passes**, then the module's checks:

  ```bash
  M=":server:libs:platform:platform-component:platform-component-api"; ./gradlew "${M}:spotlessApply" "${M}:test" --tests "*ActionInputSchemaGeneratorTest*" > "$S/t1-green.log" 2>&1; echo $?
  ./gradlew "${M}:check" --continue > "$S/t1-check.log" 2>&1; echo $?; grep -E "^> Task .* FAILED" "$S/t1-check.log"
  ```

  Expected: both `0`, no `FAILED` lines.

- [ ] **Step 5: Commit**

  ```bash
  git add server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/util/ActionInputSchemaGenerator.java \
    server/libs/platform/platform-component/platform-component-api/src/test/java/com/bytechef/platform/component/util/ActionInputSchemaGeneratorTest.java
  git commit -m "- Add a recursive JSON Schema generator for action inputs"
  ```

---

### Task 2: `GET /api/app/v1/actions` (facade, contract, controller)

**Files:**
- Modify: `server/ee/libs/automation/automation-app/automation-app-public-rest/openapi.yaml`
- Regenerate: `server/ee/libs/automation/automation-app/automation-app-public-rest/generated/**` (commit only files whose content changed beyond the `@Generated` date)
- Create: `server/ee/libs/automation/automation-app/automation-app-api/src/main/java/com/bytechef/ee/automation/app/dto/AppActionDTO.java`
- Modify: `.../automation-app-api/.../facade/AppExecutionFacade.java`, `.../automation-app-service/.../facade/AppExecutionFacadeImpl.java`, `.../automation-app-public-rest/.../AppConnectionApiController.java`
- Test: `.../automation-app-service/src/test/.../AppExecutionFacadeTest.java`, `.../automation-app-public-rest/src/test/.../AppConnectionApiControllerTest.java`, create `.../automation-security-web/automation-security-web-impl/src/test/java/com/bytechef/ee/automation/security/web/app/AppApiKeyPathPatternTest.java`

**Interfaces:**
- Consumes: `ActionInputSchemaGenerator.generateInputSchema(List<? extends Property>)` (Task 1);
  `ComponentDefinitionService.fetchComponentDefinition(String, @Nullable Integer)` → `Optional<ComponentDefinition>`;
  `ComponentDefinition.getActions()` → `List<ActionDefinition>` with `getName()`, `getDescription()` (never null),
  `getProperties()`; `AppConnection.getOperationNames(AppOperationType.ACTION)`; the `getTools()` pattern.
- Produces: `AppActionDTO`, `AppExecutionFacade#getActions()`, REST `GET /actions` → `AppAction[]`.

- [ ] **Step 1: Contract.** In `openapi.yaml`, after the `/connections` path block add:

  ```yaml
    /actions:
      get:
        description: "Lists the actions the calling app may execute, with the JSON Schema of each action's input."
        summary: "Lists the actions the calling app may execute"
        tags:
          - "app-connection"
        operationId: "getActions"
        security:
          - bearerAuth: []
        responses:
          "200":
            description: "The actions."
            content:
              application/json:
                schema:
                  type: "array"
                  items:
                    $ref: "#/components/schemas/AppAction"
          "401":
            $ref: "#/components/responses/unauthorized"
          "403":
            $ref: "#/components/responses/forbidden"
          "500":
            $ref: "#/components/responses/internalServerError"
  ```

  and before `AppTool:` under `components.schemas`:

  ```yaml
      AppAction:
        description: "An action the calling app may execute through a bound connection."
        type: "object"
        required:
          - "bindingKey"
          - "componentName"
          - "actionName"
          - "inputSchema"
        properties:
          bindingKey:
            type: "string"
          componentName:
            type: "string"
          actionName:
            type: "string"
          description:
            type: "string"
            nullable: true
          inputSchema:
            description: "JSON Schema of the action input, generated from the component's full property definition.\
              \ Properties whose shape depends on the connection appear as open objects."
            type: "object"
            additionalProperties: true
  ```

  Regenerate: `./gradlew ":server:ee:libs:automation:automation-app:automation-app-public-rest:generateOpenAPI" > "$S/t2-gen.log" 2>&1; echo $?`.
  Then revert every generated file whose only change is the `@Generated(... date = ...)` line:
  `for f in $(git diff --name-only -- '*/generated/*'); do git diff -- "$f" | grep '^[-+][^-+]' | grep -vq '@Generated' || git checkout -- "$f"; done`.
  Expected survivors: `AppConnectionApi.java` (new `getActions` default method) and a new `model/AppActionModel.java`
  (with `JsonNullable<String> description`, `Map<String, Object> inputSchema`, required-args ctor
  `AppActionModel(String bindingKey, String componentName, String actionName, Map<String, Object> inputSchema)`).

- [ ] **Step 2: DTO + interface.** `AppActionDTO.java` (EE license header, `@version ee`):

  ```java
  package com.bytechef.ee.automation.app.dto;

  import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
  import java.util.Map;
  import org.jspecify.annotations.Nullable;

  /**
   * An action the calling app may execute, with its input JSON Schema.
   *
   * @version ee
   *
   * @author Ivica Cardic
   */
  @SuppressFBWarnings("EI")
  public record AppActionDTO(
      String bindingKey, String componentName, String actionName, @Nullable String description,
      Map<String, Object> inputSchema) {
  }
  ```

  In `AppExecutionFacade` add, next to `getTools()`: `List<AppActionDTO> getActions();` with a one-line Javadoc
  ("The actions the calling app may execute across its bindings, with input schemas from the latest component
  version; bindings whose component cannot be resolved are skipped.").

- [ ] **Step 3: Failing facade tests.** In `AppExecutionFacadeTest` (imports: `AppActionDTO`,
  `com.bytechef.platform.component.domain.ActionDefinition`, `com.bytechef.component.definition.ComponentDsl`
  statics `action`, `string`, `integer`):

  ```java
  @Test
  void testActionsListUsesOnlyAllowedActionsAndGeneratesSchemas() {
      ActionDefinition create = new ActionDefinition(
          ComponentDsl.action("createRecord")
              .description("Creates a record")
              .properties(string("name").required(true), integer("age")),
          "salesforce", 3);
      ActionDefinition delete = new ActionDefinition(ComponentDsl.action("deleteRecord"), "salesforce", 3);
      ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

      when(componentDefinition.getVersion()).thenReturn(3);
      when(componentDefinition.getActions()).thenReturn(List.of(create, delete));
      when(componentDefinitionService.fetchComponentDefinition("salesforce", null))
          .thenReturn(Optional.of(componentDefinition));

      List<AppActionDTO> actions = facade.getActions();

      assertThat(actions).extracting(AppActionDTO::actionName)
          .containsExactly("createRecord");
      assertThat(actions.getFirst()
          .bindingKey()).isEqualTo("crm");
      assertThat(actions.getFirst()
          .componentName()).isEqualTo("salesforce");
      assertThat(actions.getFirst()
          .description()).isEqualTo("Creates a record");
      assertThat(actions.getFirst()
          .inputSchema())
              .isEqualTo(
                  ActionInputSchemaGenerator.generateInputSchema(
                      List.of(
                          Property.toProperty(string("name").required(true)),
                          Property.toProperty(integer("age")))));
      assertThat(actions.getFirst()
          .inputSchema()
          .get("required")).isEqualTo(List.of("name"));
  }

  @Test
  void testActionsListLeavesOutABindingToAnUnresolvableComponent() {
      when(componentDefinitionService.fetchComponentDefinition("salesforce", null)).thenReturn(Optional.empty());

      assertThat(facade.getActions()).isEmpty();
  }

  @Test
  void testActionsListLeavesOutAnAllowedNameMissingFromTheLatestVersion() {
      ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

      when(componentDefinition.getActions()).thenReturn(List.of());
      when(componentDefinitionService.fetchComponentDefinition("salesforce", null))
          .thenReturn(Optional.of(componentDefinition));

      assertThat(facade.getActions()).isEmpty();
  }

  @Test
  void testBindingWithoutActionsContributesNoActions() {
      binding.setOperations(List.of(), List.of("searchRecords"));

      assertThat(facade.getActions()).isEmpty();

      verify(componentDefinitionService, never()).fetchComponentDefinition(any(), any());
  }
  ```

  (`binding` in `beforeEach` allows actions `createRecord`, `updateRecord`.) If the platform `ActionDefinition`
  constructor signature differs from `(sdkActionDefinition, componentName, componentVersion)`, read the class and
  adapt; if it cannot be built from the DSL, fall back to `mock(ActionDefinition.class)` with stubbed `getName`,
  `getDescription`, `getProperties` and say so in the report.

- [ ] **Step 4: Run to verify they fail** (compile error on `getActions`):
  `./gradlew ":server:ee:libs:automation:automation-app:automation-app-service:test" --tests "*AppExecutionFacadeTest*" > "$S/t2-red.log" 2>&1; echo $?` → non-zero.

- [ ] **Step 5: Implement `getActions()`** in `AppExecutionFacadeImpl`, directly after `getTools()` (imports
  `AppActionDTO`, `ActionDefinition`, `ActionInputSchemaGenerator`):

  ```java
  @Override
  public List<AppActionDTO> getActions() {
      App app = resolveApp().app();

      List<AppActionDTO> actions = new ArrayList<>();

      for (AppConnection binding : appConnectionService.getBindings(app.getId())) {
          List<String> allowedActionNames = binding.getOperationNames(AppOperationType.ACTION);

          if (allowedActionNames.isEmpty()) {
              continue;
          }

          Connection connection = connectionService.getConnection(binding.getConnectionId());
          Optional<ComponentDefinition> componentDefinition = componentDefinitionService.fetchComponentDefinition(
              connection.getComponentName(), null);

          if (componentDefinition.isEmpty()) {
              continue;
          }

          for (ActionDefinition definition : componentDefinition.get()
              .getActions()) {

              if (!allowedActionNames.contains(definition.getName())) {
                  continue;
              }

              actions.add(
                  new AppActionDTO(
                      binding.getBindingKey(), connection.getComponentName(), definition.getName(),
                      definition.getDescription(),
                      ActionInputSchemaGenerator.generateInputSchema(definition.getProperties())));
          }
      }

      return actions;
  }
  ```

- [ ] **Step 6: Controller + test.** In `AppConnectionApiController` add (imports `AppActionDTO`, `AppActionModel`,
  `JsonNullable` already imported):

  ```java
  @Override
  public ResponseEntity<List<AppActionModel>> getActions() {
      return ResponseEntity.ok(
          appExecutionFacade.getActions()
              .stream()
              .map(AppConnectionApiController::toModel)
              .toList());
  }

  private static AppActionModel toModel(AppActionDTO action) {
      AppActionModel appActionModel = new AppActionModel(
          action.bindingKey(), action.componentName(), action.actionName(), action.inputSchema());

      appActionModel.setDescription(JsonNullable.of(action.description()));

      return appActionModel;
  }
  ```

  In `AppConnectionApiControllerTest` add:

  ```java
  @Test
  void testGetActions() throws Exception {
      when(appExecutionFacade.getActions()).thenReturn(
          List.of(
              new AppActionDTO(
                  "crm", "hubspot", "createContact", "Creates a contact",
                  Map.of("type", "object", "properties", Map.of("email", Map.of("type", "string")))),
              new AppActionDTO("crm", "hubspot", "ping", null, Map.of("type", "object"))));

      mockMvc.perform(get("/api/app/v1/actions"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(2))
          .andExpect(jsonPath("$[0].bindingKey").value("crm"))
          .andExpect(jsonPath("$[0].componentName").value("hubspot"))
          .andExpect(jsonPath("$[0].actionName").value("createContact"))
          .andExpect(jsonPath("$[0].description").value("Creates a contact"))
          .andExpect(jsonPath("$[0].inputSchema.properties.email.type").value("string"))
          .andExpect(jsonPath("$[1].description").value((Object) null))
          .andExpect(jsonPath("$[1].description").hasJsonPath());
  }

  @Test
  void testGetActionsAccessDeniedIsForbidden() throws Exception {
      when(appExecutionFacade.getActions()).thenThrow(new AccessDeniedException("Not an app request"));

      mockMvc.perform(get("/api/app/v1/actions"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
  }
  ```

  Create `AppApiKeyPathPatternTest` in `automation-security-web-impl` (same package as
  `AppApiKeySecurityConfigurer`; EE header):

  ```java
  class AppApiKeyPathPatternTest {

      @Test
      void testActionsListingIsOnTheAppKeyChain() {
          assertThat("/api/app/v1/actions").matches(AppApiKeySecurityConfigurer.PATH_PATTERN);
      }
  }
  ```

  (If `PATH_PATTERN` is not accessible from the test package, assert through the same mechanism
  `AppApiKeyFilterChainTest` uses and say so.)

- [ ] **Step 7: Verify** — spotless + `check` on the four modules:

  ```bash
  for m in automation-app-api automation-app-service automation-app-public-rest; do M=":server:ee:libs:automation:automation-app:${m}"; ./gradlew "${M}:spotlessApply" "${M}:check" --continue > "$S/t2-${m}.log" 2>&1; echo "${m} $?"; grep -E "^> Task .* FAILED" "$S/t2-${m}.log"; done
  M=":server:ee:libs:automation:automation-security-web:automation-security-web-impl"; ./gradlew "${M}:spotlessApply" "${M}:check" --continue > "$S/t2-sec.log" 2>&1; echo "sec $?"
  ```

  All `0`, no `FAILED`.

- [ ] **Step 8: Commit** (list the generated files that survived Step 1 explicitly):

  ```bash
  git add server/ee/libs/automation/automation-app/automation-app-public-rest/openapi.yaml \
    server/ee/libs/automation/automation-app/automation-app-public-rest/generated/src/main/java/com/bytechef/ee/automation/app/public_/web/rest/AppConnectionApi.java \
    server/ee/libs/automation/automation-app/automation-app-public-rest/generated/src/main/java/com/bytechef/ee/automation/app/public_/web/rest/model/AppActionModel.java \
    server/ee/libs/automation/automation-app/automation-app-api/src/main/java/com/bytechef/ee/automation/app/dto/AppActionDTO.java \
    server/ee/libs/automation/automation-app/automation-app-api/src/main/java/com/bytechef/ee/automation/app/facade/AppExecutionFacade.java \
    server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/facade/AppExecutionFacadeImpl.java \
    server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/facade/AppExecutionFacadeTest.java \
    server/ee/libs/automation/automation-app/automation-app-public-rest/src/main/java/com/bytechef/ee/automation/app/public_/web/rest/AppConnectionApiController.java \
    server/ee/libs/automation/automation-app/automation-app-public-rest/src/test/java/com/bytechef/ee/automation/app/public_/web/rest/AppConnectionApiControllerTest.java \
    server/ee/libs/automation/automation-security-web/automation-security-web-impl/src/test/java/com/bytechef/ee/automation/security/web/app/AppApiKeyPathPatternTest.java
  git commit -m "- List the actions an app may execute with their input schemas"
  ```

---

### Task 3: SDK `actions.list()`

**Files:**
- Regenerate: `L/src/generated/openapi.ts`
- Modify: `L/src/core/types.ts`, `L/src/core/connections.ts`, `L/src/index.ts`, `L/README.md`
- Test: `L/src/core/connections.test.ts`

**Interfaces:**
- Consumes: the regenerated `components['schemas']['AppAction']`; `Requester`, `CallOptions`, `JsonSchema`.
- Produces: `export type AppAction = Omit<components['schemas']['AppAction'], 'inputSchema'> & {inputSchema: JsonSchema}`
  (types.ts, exported from index); `createActionsApi(request)` gains `list(options?: CallOptions): Promise<AppAction[]>`.

- [ ] **Step 1:** From `L`: `npm run generate:api > "$S/t3-gen.log" 2>&1; echo $?` → 0; `grep -c "AppAction" src/generated/openapi.ts` ≥ 1.
- [ ] **Step 2: Failing test** in `connections.test.ts`, inside `describe('actions.execute', …)`'s sibling block:

  ```ts
  describe('actions.list', () => {
      it('GETs /actions and returns the actions with object schemas', async () => {
          const action = {actionName: 'createContact', bindingKey: 'crm', componentName: 'hubspot', description: null, inputSchema: {properties: {email: {type: 'string'}}, type: 'object'}};
          const {calls, request} = requesterWith(jsonResponse({body: [action]}));

          await expect(createActionsApi(request).list()).resolves.toEqual([action]);
          expect(lastRequest(calls).url).toBe('https://bc.example/api/app/v1/actions');
      });

      it('rejects with ABORTED and no request when the signal is already aborted', async () => {
          const controller = new AbortController();
          const {calls, request} = requesterWith(jsonResponse({body: []}));

          controller.abort();

          await expect(createActionsApi(request).list({signal: controller.signal})).rejects.toMatchObject({code: 'ABORTED'});
          expect(calls).toHaveLength(0);
      });
  });
  ```

- [ ] **Step 3:** `npx vitest run src/core/connections.test.ts > "$S/t3-red.log" 2>&1; echo $?` → non-zero (`list is not a function`).
- [ ] **Step 4: Implement.** `types.ts`:
  `export type AppAction = Omit<components['schemas']['AppAction'], 'inputSchema'> & {inputSchema: JsonSchema};`.
  `connections.ts`, in `createActionsApi`'s returned object (keys sorted: `execute` then `list`):

  ```ts
  async list(options: CallOptions = {}): Promise<AppAction[]> {
      const {data} = await request<AppAction[]>({method: 'GET', path: '/actions', signal: options.signal});

      return data;
  },
  ```

  `index.ts`: add `type AppAction` to the types export. `README.md`: in the core usage block add
  `const actions = await bytechef.actions.list(); // allowed actions with input JSON Schemas` after `connections.list`,
  and one sentence in the errors section: an older ByteChef without `/actions` answers 404 → `HTTP_ERROR` 404.

- [ ] **Step 5: Verify** from `L`: `npm run check > "$S/t3-check.log" 2>&1; echo $?` (runs check:api, lint, format, typecheck, test) → 0; `npm run build` → 0.
- [ ] **Step 6: Commit**

  ```bash
  git add sdks/frontend/automation/app/library/src/generated/openapi.ts sdks/frontend/automation/app/library/src/core/types.ts \
    sdks/frontend/automation/app/library/src/core/connections.ts sdks/frontend/automation/app/library/src/core/connections.test.ts \
    sdks/frontend/automation/app/library/src/index.ts sdks/frontend/automation/app/library/README.md
  git commit -m "- sdk - List allowed actions with their input schemas"
  ```

---

### Task 4: The template app

**Files:**
- Create: everything under `C/template/` (see File structure, incl. `src/server/overview.ts` and `src/components/overview.tsx`); `C/package.json` (minimal, so the workspace resolves — the bin comes in Task 5); modify `W/package.json` (`workspaces` + `"create"`).
- Verification uses a scratch scaffold under `$S/tpl/app` with `@bytechef/app` installed from a packed tarball.

**Interfaces:**
- Consumes: `@bytechef/app` (`createBytechefClient`, `BytechefAppError`, `isFinished`, `type AppAction`),
  `@bytechef/app/tanstack-start` (`bytechefMiddleware`, `executeActionInput`, `executeActionHandler`, `type
  JsonSerializable`, `type JsonValue`).
- Produces: the template files; the status JSON shape; `npm run check` semantics; `src/server/dev.ts` exporting
  `getAppOverview` and `tryAction` server functions; the exact UI ids `#overview`, `#placeholder`, `#binding-<key>`
  used by Task 6.

- [ ] **Step 1: Generate the base with TanStack's scaffolder** (into scratch, then copy):

  ```bash
  cd "$S" && rm -rf tplgen && mkdir tplgen && cd tplgen && npx -y @tanstack/cli@0.71.1 create app --add-ons shadcn --deployment nitro --no-install --no-git --non-interactive --no-toolchain --no-examples --no-intent --package-manager npm --framework React > gen.log 2>&1; echo $?
  cd app && sed -i '' 's|"typescript": "\^6.0.2"|"typescript": "5.7.3"|' package.json && npm install > install.log 2>&1; echo $?
  npx -y shadcn@4.21.4 add button card table badge --yes --overwrite > shadcn.log 2>&1; echo $?
  ls src/components/ui
  ```

  Expected: three `0`; `badge.tsx button.tsx card.tsx table.tsx`. Then copy into the repo:
  `mkdir -p "$T" && rsync -a --exclude node_modules --exclude .cta.json --exclude .cursorrules --exclude README.md --exclude src/lib --exclude .gitignore "$S/tplgen/app/" "$T/"`.

- [ ] **Step 2: `T/package.json`** — replace wholesale:

  ```json
  {
      "name": "bytechef-app",
      "private": true,
      "type": "module",
      "scripts": {
          "dev": "vite dev --port 3000",
          "build": "vite build",
          "start": "node .output/server/index.mjs",
          "check": "tsc --noEmit && vite build && node scripts/check-bundle.mjs",
          "bytechef:status": "node --env-file-if-exists=.env.local scripts/bytechef-status.mjs",
          "generate-routes": "tsr generate"
      },
      "dependencies": {
          "@bytechef/app": "0.1.0",
          "@tailwindcss/vite": "4.3.3",
          "@tanstack/react-devtools": "0.10.13",
          "@tanstack/react-router": "1.170.41",
          "@tanstack/react-router-devtools": "1.167.2",
          "@tanstack/react-start": "1.168.60",
          "class-variance-authority": "0.7.1",
          "clsx": "2.1.1",
          "cn": "0.4.0",
          "lucide-react": "0.577.0",
          "nitro": "3.0.260610-beta",
          "radix-ui": "1.7.0",
          "react": "19.3.0",
          "react-dom": "19.3.0",
          "tailwind-merge": "3.7.0",
          "tailwindcss": "4.3.3",
          "tw-animate-css": "1.4.0"
      },
      "devDependencies": {
          "@tanstack/devtools-vite": "0.8.5",
          "@tanstack/router-cli": "1.167.40",
          "@types/node": "22.20.5",
          "@types/react": "19.3.0",
          "@types/react-dom": "19.3.0",
          "@vitejs/plugin-react": "6.1.2",
          "typescript": "5.7.3",
          "vite": "8.3.3"
      },
      "engines": {
          "node": ">=22.12.0"
      }
  }
  ```

  (`@tailwindcss/typography` and the `pnpm`/`imports` blocks from the scaffold are dropped.) In `tsconfig.json` keep
  only the `@/*` path (drop `#/*`); in `components.json` change every `#/` alias to `@/`. Keep `vite.config.ts`,
  `tsr.config.json`, `src/router.tsx`, `src/styles.css` and `src/components/ui/*` as generated. Rename `.gitignore` →
  `_gitignore` with this content, and add `_dockerignore`:

  ```
  node_modules
  dist
  .output
  .nitro
  .tanstack
  .vercel
  .env*
  !.env.example
  .DS_Store
  ```

  ```
  node_modules
  dist
  .output
  .nitro
  .tanstack
  .vercel
  .git
  .env*
  !.env.example
  ```

  `.env.example`:

  ```
  BYTECHEF_URL=
  BYTECHEF_APP_KEY=
  ```

  `vercel.json`: `{"$schema": "https://openapi.vercel.sh/vercel.json", "framework": "tanstack-start"}`.

  `Dockerfile`:

  ```dockerfile
  FROM node:22-alpine AS build
  WORKDIR /app
  COPY package.json package-lock.json* ./
  RUN npm ci
  COPY . .
  RUN npm run build

  FROM node:22-alpine
  WORKDIR /app
  ENV NODE_ENV=production
  COPY --from=build /app/.output ./.output
  EXPOSE 3000
  CMD ["node", ".output/server/index.mjs"]
  ```

  (Nitro's server bundle is self-contained — `.output/server` carries its own `_libs`/`_chunks`; verify in Step 9 that
  `node .output/server/index.mjs` runs with `node_modules` absent, otherwise add
  `COPY --from=build /app/node_modules ./node_modules` and say so.)

- [ ] **Step 3: Server modules.** `src/server/bytechef.ts`:

  ```ts
  import {bytechefMiddleware} from '@bytechef/app/tanstack-start';

  export const bytechef = bytechefMiddleware();
  ```

  `src/server/overview.ts` (pure mapping, no server-function and no `@bytechef/app` runtime import, so it is unit-tested
  from the scaffolder's tests):

  ```ts
  import type {AppAction, AppBinding, AppTool, AppWorkflow} from '@bytechef/app';

  export interface BindingOverview {
      actions: Array<{description: string | null; inputSchema: Record<string, unknown> | null; name: string}>;
      bindingKey: string;
      componentName: string;
      componentVersion: number | null;
      proxy: {enabled: boolean; methods: string[]};
      tools: Array<{description: string | null; name: string; parameters: Record<string, unknown>; toolName: string}>;
  }

  export interface AppOverview {
      actionSchemas: boolean;
      bindings: BindingOverview[];
      workflows: AppWorkflow[];
  }

  export function buildOverview(bindings: readonly AppBinding[], actions: readonly AppAction[], actionSchemas: boolean, tools: readonly AppTool[], workflows: readonly AppWorkflow[]): AppOverview {
      return {
          actionSchemas,
          bindings: bindings.map((binding) => ({
              actions: binding.actionNames.map((name) => {
                  const action = actions.find((candidate) => candidate.bindingKey === binding.bindingKey && candidate.actionName === name);

                  return {description: action?.description ?? null, inputSchema: action?.inputSchema ?? null, name};
              }),
              bindingKey: binding.bindingKey,
              componentName: binding.componentName,
              componentVersion: binding.componentVersion ?? null,
              proxy: {enabled: binding.proxyEnabled, methods: [...binding.proxyAllowedMethods]},
              tools: tools
                  .filter((tool) => tool.bindingKey === binding.bindingKey)
                  .map((tool) => ({description: tool.description ?? null, name: tool.name, parameters: tool.parameters, toolName: tool.toolName})),
          })),
          workflows: [...workflows],
      };
  }
  ```

  `src/server/dev.ts` (development only; reached solely through the gated dynamic import in the route):

  ```ts
  import {BytechefAppError, type AppAction, type BytechefClient} from '@bytechef/app';
  import {executeActionHandler, executeActionInput, type JsonSerializable} from '@bytechef/app/tanstack-start';
  import {createServerFn} from '@tanstack/react-start';

  import {bytechef} from './bytechef';
  import {buildOverview, type AppOverview} from './overview';

  async function listActions(client: BytechefClient): Promise<{actionSchemas: boolean; actions: AppAction[]}> {
      try {
          return {actionSchemas: true, actions: await client.actions.list()};
      } catch (error) {
          if (error instanceof BytechefAppError && error.code === 'HTTP_ERROR' && error.status === 404) {
              return {actionSchemas: false, actions: []};
          }

          throw error;
      }
  }

  export const getAppOverview = createServerFn({method: 'GET'})
      .middleware([bytechef])
      .handler(async ({context}): Promise<JsonSerializable<AppOverview>> => {
          const client = context.bytechef;
          const [bindings, listed, tools, workflows] = await Promise.all([
              client.connections.list(),
              listActions(client),
              client.tools.list(),
              client.workflows.list(),
          ]);

          return buildOverview(bindings, listed.actions, listed.actionSchemas, tools, workflows) as JsonSerializable<AppOverview>;
      });

  export const tryAction = createServerFn({method: 'POST'})
      .middleware([bytechef])
      .validator(executeActionInput)
      .handler(executeActionHandler);
  ```

  If `JsonSerializable<AppOverview>` rejects `Record<string, unknown>` fields (`inputSchema`, `parameters`), type those
  two fields as `JsonValue` from `@bytechef/app/tanstack-start` in `overview.ts` and say so in the report.

- [ ] **Step 4: Routes.** `src/routes/__root.tsx`: keep the scaffold's, change the title to `ByteChef app`, and
  render the devtools only when `import.meta.env.DEV` (`{import.meta.env.DEV ? <TanStackDevtools … /> : null}`).
  `src/routes/index.tsx`:

  ```tsx
  import {createFileRoute} from '@tanstack/react-router';
  import {useState} from 'react';

  import {BindingCard, WorkflowsCard} from '@/components/overview';
  import type {AppOverview} from '@/server/overview';

  type LoaderData = {overview: AppOverview; placeholder: false} | {overview: null; placeholder: true};

  async function loadOverview(): Promise<LoaderData> {
      if (!import.meta.env.DEV) {
          return {overview: null, placeholder: true};
      }

      const {getAppOverview} = await import('@/server/dev');

      return {overview: (await getAppOverview()) as AppOverview, placeholder: false};
  }

  export const Route = createFileRoute('/')({
      component: Home,
      loader: loadOverview,
  });

  function Home() {
      const data = Route.useLoaderData();

      if (data.placeholder) {
          return (
              <main className="mx-auto max-w-2xl p-8" id="placeholder">
                  <h1 className="text-2xl font-semibold">This app is ready to be built</h1>
                  <p className="mt-2 text-muted-foreground">
                      Run it in development to see what it may call, then replace this page with your features.
                  </p>
              </main>
          );
      }

      return (
          <main className="mx-auto max-w-4xl space-y-6 p-8" id="overview">
              <h1 className="text-2xl font-semibold">What this app may call</h1>
              {!data.overview.actionSchemas && (
                  <p className="text-sm text-muted-foreground">This ByteChef server does not provide action input schemas.</p>
              )}
              {data.overview.bindings.map((binding) => (
                  <BindingCard binding={binding} key={binding.bindingKey} />
              ))}
              <WorkflowsCard workflows={data.overview.workflows} />
          </main>
      );
  }
  ```

  `src/components/overview.tsx`:

  ```tsx
  import {useState} from 'react';

  import {Badge} from '@/components/ui/badge';
  import {Button} from '@/components/ui/button';
  import {Card, CardContent, CardDescription, CardHeader, CardTitle} from '@/components/ui/card';
  import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
  import type {BindingOverview} from '@/server/overview';
  import type {AppWorkflow} from '@bytechef/app';

  function prefill(schema: Record<string, unknown> | null): string {
      const properties = schema && typeof schema.properties === 'object' && schema.properties !== null ? (schema.properties as Record<string, unknown>) : {};

      return JSON.stringify(Object.fromEntries(Object.keys(properties).map((key) => [key, ''])), null, 2);
  }

  function TryIt({action, bindingKey}: {action: BindingOverview['actions'][number]; bindingKey: string}) {
      const [text, setText] = useState(() => prefill(action.inputSchema));
      const [result, setResult] = useState<string | null>(null);
      const [running, setRunning] = useState(false);

      async function run() {
          setRunning(true);

          try {
              const {tryAction} = await import('@/server/dev');
              const output = await tryAction({data: {actionName: action.name, bindingKey, input: JSON.parse(text) as Record<string, unknown>}});

              setResult(JSON.stringify(output, null, 2));
          } catch (error) {
              setResult((error as Error).message);
          } finally {
              setRunning(false);
          }
      }

      return (
          <div className="space-y-2">
              <textarea className="w-full rounded-md border p-2 font-mono text-xs" onChange={(event) => setText(event.target.value)} rows={4} value={text} />
              <Button disabled={running} onClick={run} size="sm" type="button">
                  {running ? 'Running…' : 'Run'}
              </Button>
              {result !== null && <pre className="overflow-auto rounded-md bg-muted p-2 text-xs">{result}</pre>}
          </div>
      );
  }

  export function BindingCard({binding}: {binding: BindingOverview}) {
      return (
          <Card id={`binding-${binding.bindingKey}`}>
              <CardHeader>
                  <CardTitle>{binding.bindingKey}</CardTitle>
                  <CardDescription>
                      {binding.componentName}{' '}
                      {binding.componentVersion === null ? <Badge variant="destructive">component unavailable</Badge> : <Badge variant="secondary">v{binding.componentVersion}</Badge>}
                  </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                  <Table>
                      <TableHeader>
                          <TableRow>
                              <TableHead>Action</TableHead>
                              <TableHead>Description</TableHead>
                              <TableHead>Try it</TableHead>
                          </TableRow>
                      </TableHeader>
                      <TableBody>
                          {binding.actions.map((action) => (
                              <TableRow key={action.name}>
                                  <TableCell className="font-mono">{action.name}</TableCell>
                                  <TableCell>{action.description ?? ''}</TableCell>
                                  <TableCell>
                                      {binding.componentVersion === null ? <span className="text-muted-foreground">unavailable</span> : <TryIt action={action} bindingKey={binding.bindingKey} />}
                                  </TableCell>
                              </TableRow>
                          ))}
                      </TableBody>
                  </Table>
                  <div className="flex flex-wrap gap-2">
                      {binding.tools.map((tool) => (
                          <Badge key={tool.name} variant="outline">
                              {tool.name}
                          </Badge>
                      ))}
                  </div>
                  <p className="text-sm text-muted-foreground">{binding.proxy.enabled ? `Proxy: ${binding.proxy.methods.join(', ')}` : 'Proxy: off'}</p>
              </CardContent>
          </Card>
      );
  }

  export function WorkflowsCard({workflows}: {workflows: AppWorkflow[]}) {
      return (
          <Card id="workflows">
              <CardHeader>
                  <CardTitle>Workflows</CardTitle>
              </CardHeader>
              <CardContent>
                  {workflows.length === 0 ? (
                      <p className="text-sm text-muted-foreground">No workflows are bound to this app.</p>
                  ) : (
                      <ul className="space-y-1">
                          {workflows.map((workflow) => (
                              <li className="flex items-center gap-2" key={workflow.workflowKey}>
                                  <span className="font-mono">{workflow.workflowKey}</span>
                                  <span>{workflow.name}</span>
                                  {!workflow.available && <Badge variant="destructive">unavailable</Badge>}
                              </li>
                          ))}
                      </ul>
                  )}
              </CardContent>
          </Card>
      );
  }
  ```

  `index.tsx` imports `BindingCard` and `WorkflowsCard` from `@/components/overview` and the `AppOverview` type from
  `@/server/overview`; the `Badge`, `Button`, `Card`, `Table` imports move out of the route.

- [ ] **Step 5: `scripts/bytechef-status.mjs`:**

  ```js
  import {BytechefAppError, createBytechefClient} from '@bytechef/app';

  function fail(message) {
      process.stderr.write(`${message}\n`);
      process.exit(1);
  }

  const client = (() => {
      try {
          return createBytechefClient();
      } catch (error) {
          return fail(error.message);
      }
  })();

  async function listActions() {
      try {
          return {actionSchemas: true, actions: await client.actions.list()};
      } catch (error) {
          if (error instanceof BytechefAppError && error.code === 'HTTP_ERROR' && error.status === 404) {
              return {actionSchemas: false, actions: []};
          }

          throw error;
      }
  }

  try {
      const [bindings, listed, tools, workflows] = await Promise.all([
          client.connections.list(),
          listActions(),
          client.tools.list(),
          client.workflows.list(),
      ]);

      const status = {
          actionSchemas: listed.actionSchemas,
          bindings: bindings.map((binding) => ({
              actions: binding.actionNames.map((name) => {
                  const action = listed.actions.find((candidate) => candidate.bindingKey === binding.bindingKey && candidate.actionName === name);

                  return {description: action?.description ?? null, inputSchema: action?.inputSchema ?? null, name};
              }),
              bindingKey: binding.bindingKey,
              componentName: binding.componentName,
              componentVersion: binding.componentVersion ?? null,
              proxy: {enabled: binding.proxyEnabled, methods: [...binding.proxyAllowedMethods]},
              tools: tools
                  .filter((tool) => tool.bindingKey === binding.bindingKey)
                  .map((tool) => ({description: tool.description ?? null, name: tool.name, parameters: tool.parameters, toolName: tool.toolName})),
          })),
          bytechefUrl: process.env.BYTECHEF_URL,
          workflows,
      };

      process.stdout.write(`${JSON.stringify(status, null, 2)}\n`);
  } catch (error) {
      if (error instanceof BytechefAppError) {
          fail(`${error.code}: ${error.message}`);
      }

      throw error;
  }
  ```

  Note the duplication with `buildOverview` in `dev.ts`: the script cannot import TypeScript, so the mapping is
  repeated here; keep the two in step (Task 6 asserts both against the same fixtures).

- [ ] **Step 6: `scripts/check-bundle.mjs`:**

  ```js
  import {existsSync, readdirSync, readFileSync, statSync} from 'node:fs';
  import {join} from 'node:path';

  const CLIENT_DIRS = ['.output/public', 'dist/client'];
  const SERVER_DIR = '.output/server';
  const FORBIDDEN = ['BYTECHEF_APP_KEY', '/api/app/v1'];
  const DEV_ONLY_SERVER_FNS = ['getAppOverview_createServerFn_handler', 'tryAction_createServerFn_handler'];

  function readEnvLocalKey() {
      if (!existsSync('.env.local')) {
          return '';
      }

      const line = readFileSync('.env.local', 'utf8')
          .split('\n')
          .find((candidate) => candidate.startsWith('BYTECHEF_APP_KEY='));

      return line ? line.slice('BYTECHEF_APP_KEY='.length).trim().replace(/^["']|["']$/g, '') : '';
  }

  function walk(dir) {
      return readdirSync(dir).flatMap((name) => {
          const path = join(dir, name);

          return statSync(path).isDirectory() ? walk(path) : [path];
      });
  }

  const clientDir = CLIENT_DIRS.find((dir) => existsSync(dir) && statSync(dir).isDirectory());

  if (!clientDir) {
      console.error(`No client build found in ${CLIENT_DIRS.join(' or ')}; run npm run build first`);
      process.exit(1);
  }

  const keyValues = [process.env.BYTECHEF_APP_KEY ?? '', readEnvLocalKey()].filter((value) => value.length > 0);
  const problems = [];
  const clientFiles = walk(clientDir).filter((file) => /\.(js|mjs|html|css|json|map)$/.test(file));

  for (const file of clientFiles) {
      const text = readFileSync(file, 'utf8');

      for (const forbidden of FORBIDDEN) {
          if (text.includes(forbidden)) {
              problems.push(`${file}: contains ${forbidden}`);
          }
      }

      for (const value of keyValues) {
          if (text.includes(value)) {
              problems.push(`${file}: contains the app key value`);
          }
      }
  }

  if (existsSync(SERVER_DIR)) {
      for (const file of walk(SERVER_DIR).filter((candidate) => /\.(js|mjs)$/.test(candidate))) {
          const text = readFileSync(file, 'utf8');

          for (const name of DEV_ONLY_SERVER_FNS) {
              if (text.includes(name)) {
                  problems.push(`${file}: contains the development-only server function ${name}; build without NODE_ENV=development`);
              }
          }

          for (const value of keyValues) {
              if (text.includes(`BYTECHEF_APP_KEY=${value}`)) {
                  problems.push(`${file}: contains the app key value`);
              }
          }
      }
  }

  if (problems.length > 0) {
      console.error(problems.join('\n'));
      process.exit(1);
  }

  console.log(`Checked ${clientFiles.length} client files under ${clientDir} and the server build: nothing server-only, no development-only server functions`);
  ```

- [ ] **Step 7: Docs in the template.** `CLAUDE.md` is the single line `@AGENTS.md`. `README.md` (for humans):

  ```markdown
  # ByteChef app

  A TanStack Start app that calls ByteChef — actions, tools, workflows and HTTP through bound connections — with one
  app key. Third-party systems are reached only through ByteChef; this app holds no vendor credentials.

  ## Run

  1. Copy `.env.example` to `.env.local` and set `BYTECHEF_URL` and `BYTECHEF_APP_KEY` (the key comes from the app's
     page in ByteChef, or from `bytechef app create` once the ByteChef CLI supports apps). The shell environment wins
     over `.env.local`: an exported `BYTECHEF_APP_KEY` replaces the file's value.
  2. `npm run dev` — in development the home page lists what this app may call and lets you try an action.
  3. `npm run -s bytechef:status` — the same, as JSON (for scripts and coding agents).

  ## Check

  `npm run check` typechecks, builds, and verifies that no secret reached the browser bundle and that no
  development-only server function was compiled. Run it before every deploy.

  ## Deploy

  The production build shows a placeholder page until you replace `src/routes/index.tsx`; the development overview
  and "Try it" are never part of a production build.

  - **Vercel:** `vercel env add BYTECHEF_URL production`, `vercel env add BYTECHEF_APP_KEY production`, then
    `vercel deploy --prod`.
  - **Docker:** `docker build -t my-app .` then
    `docker run -e BYTECHEF_URL=https://… -e BYTECHEF_APP_KEY=… -p 3000:3000 my-app`.
  - After rotating the key in ByteChef, update the host's variable and redeploy.

  See `AGENTS.md` for how features are built.
  ```

  `AGENTS.md`:

  ````markdown
  # Working in this ByteChef app

  This app calls external systems only through ByteChef, using the app key in `BYTECHEF_APP_KEY`. An admin decides in
  ByteChef which connections this app may use and which actions, tools, proxy methods and workflows are allowed.

  ## Rules

  1. **Every third-party system goes through `@bytechef/app`.** Never add an API key, OAuth client or vendor SDK for a
     system this app reaches through a ByteChef binding, and never call such a system's API directly.
  2. **Discover before building.** Run `npm run -s bytechef:status` and use only the operations it lists. Action input
     schemas come from the component's full property definition; a field shown as an open object depends on the
     connection — ask the human what to send. `"inputSchema": null` means the schema is unavailable, not that the
     operation is forbidden.
  3. **Only `src/server/` imports `@bytechef/app`.** Components import the server functions declared there, never the
     client. Nothing from ByteChef goes under a `VITE_` variable.
  4. **Write purpose-built server functions** on `bytechef` from `src/server/bytechef.ts`, with `.validator(...)` and a
     concrete return type, exposing exactly what the page needs. The overview and "Try it" in `src/server/dev.ts` exist
     only in development builds; replace the placeholder page with real features. Do not use the ready-made server
     functions from `@bytechef/app/tanstack-start/server-fns` unless the human asks — each one is a public endpoint.
  5. **When something is not allowed, ask; do not work around it.** If an operation or binding is missing from the
     status output, or a call fails with `APP_OPERATION_NOT_ALLOWED`, stop and tell the human what to ask a ByteChef
     admin for: the binding key, the component, and the action or tool names. Never fall back to the proxy, a direct
     API call or a new credential. Use the proxy only where the status output shows it enabled, with the listed
     methods.
  6. **Do not retry actions automatically;** they can have side effects. Show `BytechefAppError` codes meaningfully.
  7. **Anything a page shows is visible to anyone who can open the app** (there is no sign-in yet). Show a workflow's
     approval link only to its intended approver; ask the human before putting it on a page.
  8. **If you add `src/start.ts`,** include `createCsrfMiddleware({filter: (ctx) => ctx.handlerType === 'serverFn'})`
     from `@tanstack/react-start`, or the server functions lose CSRF protection.
  9. **Finish with `npm run check`.** Typecheck, build and the leak check must pass.

  ## Recipes

  All server functions live in `src/server/`. `bytechef` is the middleware from `src/server/bytechef.ts`; it puts a
  client on `context.bytechef`.

  ### An action

  ```ts
  import {createServerFn} from '@tanstack/react-start';

  import {bytechef} from './bytechef';

  interface Contact {
      email: string;
      id: string;
  }

  function createContactInput(input: unknown): {email: string; name: string} {
      if (typeof input !== 'object' || input === null) {
          throw new TypeError('input must be an object');
      }

      const {email, name} = input as Record<string, unknown>;

      if (typeof email !== 'string' || typeof name !== 'string') {
          throw new TypeError('email and name must be strings');
      }

      return {email, name};
  }

  export const createContact = createServerFn({method: 'POST'})
      .middleware([bytechef])
      .validator(createContactInput)
      .handler(({context, data}) => context.bytechef.actions.execute<Contact>('crm', 'createContact', data));
  ```

  ### A tool

  ```ts
  import {createServerFn} from '@tanstack/react-start';

  import {bytechef} from './bytechef';

  export const searchContacts = createServerFn({method: 'POST'})
      .middleware([bytechef])
      .validator((input: unknown) => ({query: String((input as {query?: unknown}).query ?? '')}))
      .handler(({context, data}) => context.bytechef.tools.executeByName<{id: string; name: string}[]>('CRM__SEARCH_CONTACTS', data));
  ```

  ### A workflow run

  Start the run and return it; poll from the page until it is finished. A server function gets no abort signal, so do
  not wait inside it.

  ```ts
  import {createServerFn} from '@tanstack/react-start';
  import type {WorkflowRun} from '@bytechef/app';

  import {bytechef} from './bytechef';

  export const startOnboarding = createServerFn({method: 'POST'})
      .middleware([bytechef])
      .validator((input: unknown) => ({email: String((input as {email?: unknown}).email ?? '')}))
      .handler(({context, data}): Promise<WorkflowRun> => context.bytechef.workflows.execute('onboard-customer', data, {waitSeconds: 10}));

  export const getOnboarding = createServerFn({method: 'GET'})
      .middleware([bytechef])
      .validator((input: unknown) => ({runId: String((input as {runId?: unknown}).runId ?? '')}))
      .handler(({context, data}): Promise<WorkflowRun> => context.bytechef.workflows.getRun(data.runId));
  ```

  ```tsx
  import {isFinished} from '@bytechef/app';

  import {getOnboarding, startOnboarding} from '@/server/onboarding';

  export async function runOnboarding(email: string) {
      let run = await startOnboarding({data: {email}});

      while (!isFinished(run.status)) {
          if (run.status === 'APPROVAL_REQUIRED' && run.approvalFormUrl) {
              console.log('Waiting for approval; only the approver should see', run.approvalFormUrl);
          }

          await new Promise((resolve) => setTimeout(resolve, 3000));

          run = await getOnboarding({data: {runId: run.runId}});
      }

      return run;
  }
  ```

  ### The proxy

  Only where `bytechef:status` shows `proxy.enabled` and the method is listed.

  ```ts
  import {createServerFn} from '@tanstack/react-start';

  import {bytechef} from './bytechef';

  export const getContactRaw = createServerFn({method: 'GET'})
      .middleware([bytechef])
      .validator((input: unknown) => ({id: String((input as {id?: unknown}).id ?? '')}))
      .handler(async ({context, data}) => {
          const response = await context.bytechef.proxy('crm', {method: 'GET', path: `/crm/v3/objects/contacts/${encodeURIComponent(data.id)}`});

          return {body: response.body, bodyEncoding: response.bodyEncoding, status: response.status};
      });
  ```

  ### An AI chat with ByteChef tools

  Needs `ai`, `zod`, a provider package (for example `@ai-sdk/anthropic`) and a model API key. That key is the app's
  own secret, not governed by ByteChef — ask the human before adding it.

  ```ts
  import {anthropic} from '@ai-sdk/anthropic';
  import {toAiSdkTools} from '@bytechef/app/ai-sdk';
  import {createServerFn} from '@tanstack/react-start';
  import {generateText, stepCountIs} from 'ai';

  import {bytechef} from './bytechef';

  export const ask = createServerFn({method: 'POST'})
      .middleware([bytechef])
      .validator((input: unknown) => ({prompt: String((input as {prompt?: unknown}).prompt ?? '')}))
      .handler(async ({context, data}) => {
          const result = await generateText({
              abortSignal: AbortSignal.timeout(60_000),
              model: anthropic('claude-sonnet-5-5'),
              prompt: data.prompt,
              stopWhen: stepCountIs(5),
              tools: await toAiSdkTools(context.bytechef, {workflows: true}),
          });

          return result.text;
      });
  ```

  ## Deploy

  - **Vercel:** `vercel env add BYTECHEF_URL production` and `vercel env add BYTECHEF_APP_KEY production`, then
    `vercel deploy --prod`.
  - **Self-host:** `docker build -t my-app .`; run it with `BYTECHEF_URL` and `BYTECHEF_APP_KEY` set.
  - After the key is rotated in ByteChef, update the host's variable and redeploy.
  - If the `bytechef` CLI is installed and has `app` commands, prefer `bytechef app deploy`; otherwise ask the human.
  ````

  Every ```ts/```tsx block above must typecheck against the installed packages (Step 9 compiles them; `@ai-sdk/anthropic`
  is stubbed there with a `declare module`).

- [ ] **Step 8: Workspace wiring.** `C/package.json` (minimal for now; Task 5 completes it):

  ```json
  {
      "name": "create-bytechef-app",
      "version": "0.1.0",
      "description": "Scaffold a ByteChef app: a TanStack Start starter that calls ByteChef through @bytechef/app.",
      "type": "module",
      "private": false,
      "files": ["bin", "template"],
      "engines": {"node": ">=22.12.0"},
      "publishConfig": {"access": "public"},
      "license": "MIT"
  }
  ```

  In `W/package.json` add `"create"` to `workspaces`. Run `npm install` at `W`.
  The template directory is NOT a workspace member and must not be listed; confirm
  `npm ls --workspaces 2>/dev/null | grep -c bytechef-app` is 0 after install.

- [ ] **Step 9: Verify with the packed SDK.** From `L`: `npm run build` then `npm pack --pack-destination "$S/tpl"`.
  Scaffold by hand: `rm -rf "$S/tpl/app" && rsync -a "$T/" "$S/tpl/app/" && cd "$S/tpl/app" && mv _gitignore .gitignore && mv _dockerignore .dockerignore && sed -i '' 's|"@bytechef/app": "0.1.0"|"@bytechef/app": "file:../bytechef-app-0.1.0.tgz"|' package.json && npm install > install.log 2>&1; echo $?`.
  Then, with `BYTECHEF_URL=https://example.invalid BYTECHEF_APP_KEY=sk-canary-7f3a` exported for each command:
  1. `npm run check > check.log 2>&1; echo $?` → 0 (tsc, build, bundle check).
  2. `grep -rlE "getAppOverview|tryAction" .output/server | wc -l` → 0; `grep -c "placeholder" .output/server/index.mjs` ≥ 1 or in `_ssr/*` (the placeholder route is compiled).
  3. `NODE_ENV=development npm run build > devbuild.log 2>&1; node scripts/check-bundle.mjs; echo $?` → 1 naming `getAppOverview_createServerFn_handler` (Review Focus 2). Rebuild normally afterwards (`npm run build`).
  4. Start the production server (Bash `run_in_background: true`): `PORT=3995 node .output/server/index.mjs`; then `curl -sf --retry 30 --retry-delay 1 --retry-connrefused http://localhost:3995/ -o ssr.html; echo $?` → 0; `grep -c 'id="placeholder"' ssr.html` → 1; `grep -c 'id="overview"' ssr.html` → 0; `pkill -f 'index.mjs'`.
  5. Docker self-containment proxy: `mv node_modules node_modules.off; PORT=3994 node .output/server/index.mjs` in the background, `curl -sf --retry 10 --retry-delay 1 --retry-connrefused http://localhost:3994/ -o /dev/null; echo $?`; `pkill -f 'index.mjs'`; `mv node_modules.off node_modules`. If the server fails without `node_modules`, add the `COPY --from=build /app/node_modules` line to the Dockerfile (Step 2) and say so.
  6. AGENTS.md samples typecheck: extract each ```ts/```tsx block of `AGENTS.md` into `$S/tpl/app/src/agents-samples/sampleN.tsx` (declare any `request`/`model` placeholders with `declare const`), run `npx tsc --noEmit -p tsconfig.json` → 0, then delete the samples directory.
  7. `npx prettier --check "$T" --ignore-path /dev/null` is not required (the template follows the scaffold's style); but run `npx prettier --write` on the files YOU wrote (`src/server/*.ts`, `src/routes/*.tsx`, `src/components/overview.tsx`, `scripts/*.mjs`, `AGENTS.md`, `README.md`) using the workspace `.prettierrc`, and make sure `W`'s `.prettierignore` excludes `create/template/**` (add the line) so the workspace `format` check does not reformat scaffold output.

- [ ] **Step 10: Commit** (paths explicit; nothing from `$S`):

  ```bash
  git add sdks/frontend/automation/app/package.json sdks/frontend/automation/app/package-lock.json sdks/frontend/automation/app/.prettierignore \
    sdks/frontend/automation/app/create/package.json sdks/frontend/automation/app/create/template
  git commit -m "- sdk - Add the ByteChef app template"
  ```

  Before committing, `git status --short sdks/frontend/automation/app/create` must show no `node_modules`, `.output`,
  `.env.local` or `routeTree.gen.ts`-less state: `T/src/routeTree.gen.ts` IS committed (copy it from
  `$S/tpl/app/src/routeTree.gen.ts` after the build if the scaffold did not create it).

---

### Task 5: The scaffolder

**Files:**
- Create: `C/bin/create-bytechef-app.mjs`, `C/src/args.mjs`, `C/src/scaffold.mjs`, `C/src/env.mjs`, `C/src/install.mjs`, `C/vitest.config.mjs`, `C/README.md`
- Modify: `C/package.json` (bin, scripts, devDependencies), `W/package.json` (`test:create` script)
- Test: `C/test/args.test.mjs`, `C/test/scaffold.test.mjs`, `C/test/env.test.mjs`, `C/test/install.test.mjs`

**Interfaces:**
- Produces:
  ```js
  // args.mjs
  export function parseCliArgs(argv, env = process.env) // → {directory, url, install: boolean, packageManager}
  //   throws Error with messages: 'Only one directory may be given …' (names the `--` rule), 'Unknown option …'
  // env.mjs
  export function buildEnvLocal({url, env = process.env}) // → {content: string, keyFromEnvironment: boolean}
  export function detectPackageManager(env = process.env)   // 'npm' | 'pnpm' | 'yarn' | 'bun'
  export function validatePackageName(name)                 // returns name or throws Error(rule)
  // scaffold.mjs
  export function scaffold({templateDir, targetDir, packageName, appVersion, envLocal}) // copies, renames, rewrites package.json; throws if target non-empty
  // install.mjs
  export function installCommand(packageManager) // → {command, args, shell}
  export function runInstall(packageManager, cwd)  // spawnSync; returns exit code
  ```

- [ ] **Step 1: Failing tests.** `C/vitest.config.mjs`: `export default {test: {include: ['test/**/*.test.mjs']}}`.
  `test/args.test.mjs`:

  ```js
  import {describe, expect, it} from 'vitest';

  import {parseCliArgs} from '../src/args.mjs';

  describe('parseCliArgs', () => {
      it('defaults the directory, install and package manager', () => {
          expect(parseCliArgs([], {})).toEqual({directory: 'bytechef-app', install: true, packageManager: undefined, url: undefined});
      });

      it('reads the directory, --url, --no-install and --package-manager', () => {
          expect(parseCliArgs(['my-app', '--url', 'https://bc.example', '--no-install', '--package-manager', 'pnpm'], {})).toEqual({
              directory: 'my-app',
              install: false,
              packageManager: 'pnpm',
              url: 'https://bc.example',
          });
      });

      it('refuses a second positional and explains the npm -- rule', () => {
          expect(() => parseCliArgs(['my-app', 'https://bc.example'], {})).toThrowError(/npm create bytechef-app@latest my-app -- --url/);
      });

      it('explains when npm consumed the flags', () => {
          expect(() => parseCliArgs(['my-app'], {npm_config_url: 'true'})).toThrowError(/npm took --url as its own option/);
          expect(() => parseCliArgs(['my-app'], {npm_config_install: ''})).toThrowError(/npm took --no-install/);
      });

      it('rejects an unknown package manager and an unknown option', () => {
          expect(() => parseCliArgs(['--package-manager', 'deno'], {})).toThrowError(/package-manager/);
          expect(() => parseCliArgs(['--key', 'x'], {})).toThrowError(/Unknown option/);
      });
  });
  ```

  `test/env.test.mjs`:

  ```js
  import {describe, expect, it} from 'vitest';

  import {buildEnvLocal, detectPackageManager, validatePackageName} from '../src/env.mjs';

  describe('buildEnvLocal', () => {
      it('takes the url from the flag and the key from the environment', () => {
          expect(buildEnvLocal({env: {BYTECHEF_APP_KEY: 'sk-1'}, url: 'https://bc.example'})).toEqual({
              content: 'BYTECHEF_URL=https://bc.example\nBYTECHEF_APP_KEY=sk-1\n',
              keyFromEnvironment: true,
          });
      });

      it('falls back to the environment url and leaves both empty otherwise', () => {
          expect(buildEnvLocal({env: {BYTECHEF_URL: 'https://env.example'}})).toEqual({content: 'BYTECHEF_URL=https://env.example\nBYTECHEF_APP_KEY=\n', keyFromEnvironment: false});
          expect(buildEnvLocal({env: {}})).toEqual({content: 'BYTECHEF_URL=\nBYTECHEF_APP_KEY=\n', keyFromEnvironment: false});
      });
  });

  describe('detectPackageManager', () => {
      it.each([
          ['pnpm/9.0.0 node/v22.0.0', 'pnpm'],
          ['yarn/4.0.0 npm/? node/v22.0.0', 'yarn'],
          ['bun/1.1.0', 'bun'],
          ['npm/11.0.0 node/v22.0.0', 'npm'],
          [undefined, 'npm'],
      ])('%s → %s', (agent, expected) => {
          expect(detectPackageManager({npm_config_user_agent: agent})).toBe(expected);
      });
  });

  describe('validatePackageName', () => {
      it('accepts a valid name and rejects invalid ones with the rule', () => {
          expect(validatePackageName('my-app')).toBe('my-app');
          expect(() => validatePackageName('My App')).toThrowError(/lower-case/);
          expect(() => validatePackageName('.hidden')).toThrowError(/start/);
      });
  });
  ```

  `test/scaffold.test.mjs` (uses a temp template with `_gitignore`, `_dockerignore`, `package.json` containing
  `"name": "bytechef-app"` and `"@bytechef/app": "0.1.0"`, and `src/x.ts`):

  ```js
  import {mkdtempSync, mkdirSync, readFileSync, statSync, writeFileSync} from 'node:fs';
  import {tmpdir} from 'node:os';
  import {join} from 'node:path';
  import {describe, expect, it} from 'vitest';

  import {scaffold} from '../src/scaffold.mjs';

  function makeTemplate() {
      const dir = mkdtempSync(join(tmpdir(), 'cba-template-'));

      mkdirSync(join(dir, 'src'));
      writeFileSync(join(dir, '_gitignore'), 'node_modules\n');
      writeFileSync(join(dir, '_dockerignore'), 'node_modules\n');
      writeFileSync(join(dir, 'package.json'), JSON.stringify({dependencies: {'@bytechef/app': '0.1.0'}, name: 'bytechef-app'}, null, 4));
      writeFileSync(join(dir, 'src', 'x.ts'), 'export const x = 1;\n');

      return dir;
  }

  describe('scaffold', () => {
      it('copies the template, renames the dotfiles, rewrites package.json and writes .env.local with mode 0600', () => {
          const target = join(mkdtempSync(join(tmpdir(), 'cba-target-')), 'my-app');

          scaffold({appVersion: '0.1.0', envLocal: 'BYTECHEF_URL=\nBYTECHEF_APP_KEY=\n', packageName: 'my-app', targetDir: target, templateDir: makeTemplate()});

          expect(readFileSync(join(target, '.gitignore'), 'utf8')).toBe('node_modules\n');
          expect(readFileSync(join(target, '.dockerignore'), 'utf8')).toBe('node_modules\n');
          expect(statSync(join(target, '_gitignore'), {throwIfNoEntry: false})).toBeUndefined();
          expect(JSON.parse(readFileSync(join(target, 'package.json'), 'utf8'))).toEqual({dependencies: {'@bytechef/app': '0.1.0'}, name: 'my-app'});
          expect(readFileSync(join(target, '.env.local'), 'utf8')).toBe('BYTECHEF_URL=\nBYTECHEF_APP_KEY=\n');
          expect(statSync(join(target, '.env.local')).mode & 0o777).toBe(0o600);
          expect(readFileSync(join(target, 'src', 'x.ts'), 'utf8')).toBe('export const x = 1;\n');
      });

      it('refuses a non-empty target directory by name', () => {
          const target = mkdtempSync(join(tmpdir(), 'cba-busy-'));

          writeFileSync(join(target, 'keep.txt'), 'x');

          expect(() => scaffold({appVersion: '0.1.0', envLocal: '', packageName: 'busy', targetDir: target, templateDir: makeTemplate()})).toThrowError(new RegExp(`${target}.*not empty`));
      });
  });
  ```

  `test/install.test.mjs`:

  ```js
  import {describe, expect, it} from 'vitest';

  import {installCommand} from '../src/install.mjs';

  describe('installCommand', () => {
      it.each([
          ['npm', 'npm', ['install']],
          ['pnpm', 'pnpm', ['install']],
          ['yarn', 'yarn', ['install']],
          ['bun', 'bun', ['install']],
      ])('%s', (packageManager, command, args) => {
          expect(installCommand(packageManager, 'linux')).toEqual({args, command, shell: false});
          expect(installCommand(packageManager, 'win32')).toEqual({args, command, shell: true});
      });
  });
  ```

  (The `mode` assertion is skipped on Windows: wrap it in `if (process.platform !== 'win32')`.)

- [ ] **Step 2: Run to verify they fail**: from `C`, `npx vitest run > "$S/t5-red.log" 2>&1; echo $?` → non-zero (missing modules). `C/package.json` gains `"scripts": {"test": "vitest run", "test:watch": "vitest --watch"}` and `"devDependencies": {"vitest": "4.1.7"}`; run `npm install` at `W`.

- [ ] **Step 3: Implement.** `src/args.mjs`:

  ```js
  import {parseArgs} from 'node:util';

  const PACKAGE_MANAGERS = ['npm', 'pnpm', 'yarn', 'bun'];
  const NPM_HINT = 'Under npm, flags must follow "--": npm create bytechef-app@latest my-app -- --url https://… --no-install';

  export function parseCliArgs(argv, env = process.env) {
      if ('npm_config_url' in env) {
          throw new Error(`npm took --url as its own option. ${NPM_HINT}`);
      }

      if ('npm_config_install' in env) {
          throw new Error(`npm took --no-install as its own option. ${NPM_HINT}`);
      }

      let parsed;

      try {
          parsed = parseArgs({
              allowPositionals: true,
              args: argv,
              options: {
                  install: {default: true, type: 'boolean'},
                  'package-manager': {type: 'string'},
                  url: {type: 'string'},
              },
              strict: true,
          });
      } catch (error) {
          throw new Error(`${error.message}. ${NPM_HINT}`);
      }

      if (parsed.positionals.length > 1) {
          throw new Error(`Only one directory may be given, got: ${parsed.positionals.join(' ')}. ${NPM_HINT}`);
      }

      const packageManager = parsed.values['package-manager'];

      if (packageManager !== undefined && !PACKAGE_MANAGERS.includes(packageManager)) {
          throw new Error(`--package-manager must be one of ${PACKAGE_MANAGERS.join(', ')}`);
      }

      return {
          directory: parsed.positionals[0] ?? 'bytechef-app',
          install: parsed.values.install,
          packageManager,
          url: parsed.values.url,
      };
  }
  ```

  `parseArgs` with `strict: true` reports an unknown option as `Unknown option '--key'`; keep that text in the thrown
  message so the test's `/Unknown option/` matches. `src/env.mjs`:

  ```js
  const NAME_RULES = [
      [/^[a-z0-9]/, 'must start with a lower-case letter or digit'],
      [/^[a-z0-9][a-z0-9._-]*$/, 'may contain only lower-case letters, digits, ".", "_" and "-"'],
      [/^.{1,214}$/, 'must be at most 214 characters'],
  ];

  export function validatePackageName(name) {
      for (const [pattern, rule] of NAME_RULES) {
          if (!pattern.test(name)) {
              throw new Error(`"${name}" is not a valid package name: it ${rule}`);
          }
      }

      return name;
  }

  export function detectPackageManager(env = process.env) {
      const agent = env.npm_config_user_agent ?? '';

      for (const candidate of ['pnpm', 'yarn', 'bun']) {
          if (agent.startsWith(`${candidate}/`)) {
              return candidate;
          }
      }

      return 'npm';
  }

  export function buildEnvLocal({env = process.env, url}) {
      const key = env.BYTECHEF_APP_KEY ?? '';

      return {
          content: `BYTECHEF_URL=${url ?? env.BYTECHEF_URL ?? ''}\nBYTECHEF_APP_KEY=${key}\n`,
          keyFromEnvironment: key.length > 0,
      };
  }
  ```

  `src/scaffold.mjs`:

  ```js
  import {cpSync, existsSync, readdirSync, readFileSync, renameSync, writeFileSync} from 'node:fs';
  import {join} from 'node:path';

  const RENAMES = [['_gitignore', '.gitignore'], ['_dockerignore', '.dockerignore']];

  export function scaffold({appVersion, envLocal, packageName, targetDir, templateDir}) {
      if (existsSync(targetDir) && readdirSync(targetDir).length > 0) {
          throw new Error(`${targetDir} exists and is not empty`);
      }

      cpSync(templateDir, targetDir, {recursive: true});

      for (const [from, to] of RENAMES) {
          renameSync(join(targetDir, from), join(targetDir, to));
      }

      const packageJsonPath = join(targetDir, 'package.json');
      const packageJson = JSON.parse(readFileSync(packageJsonPath, 'utf8'));

      packageJson.name = packageName;
      packageJson.dependencies['@bytechef/app'] = appVersion;

      writeFileSync(packageJsonPath, `${JSON.stringify(packageJson, null, 4)}\n`);
      writeFileSync(join(targetDir, '.env.local'), envLocal, {mode: 0o600});
  }
  ```

  `src/install.mjs`:

  ```js
  import {spawnSync} from 'node:child_process';

  export function installCommand(packageManager, platform = process.platform) {
      return {args: ['install'], command: packageManager, shell: platform === 'win32'};
  }

  export function runInstall(packageManager, cwd) {
      const {args, command, shell} = installCommand(packageManager);
      const result = spawnSync(command, args, {cwd, shell, stdio: 'inherit'});

      return result.status ?? 1;
  }
  ```

  `bin/create-bytechef-app.mjs`:

  ```js
  #!/usr/bin/env node
  import {readFileSync} from 'node:fs';
  import {basename, dirname, resolve} from 'node:path';
  import {fileURLToPath} from 'node:url';

  import {parseCliArgs} from '../src/args.mjs';
  import {buildEnvLocal, detectPackageManager, validatePackageName} from '../src/env.mjs';
  import {installCommand, runInstall} from '../src/install.mjs';
  import {scaffold} from '../src/scaffold.mjs';

  const packageDir = resolve(dirname(fileURLToPath(import.meta.url)), '..');
  const {version} = JSON.parse(readFileSync(resolve(packageDir, 'package.json'), 'utf8'));

  function main() {
      let options;

      try {
          options = parseCliArgs(process.argv.slice(2));
      } catch (error) {
          console.error(error.message);
          process.exit(2);
      }

      const targetDir = resolve(process.cwd(), options.directory);
      const packageName = validatePackageName(basename(targetDir));
      const packageManager = options.packageManager ?? detectPackageManager();
      const envLocal = buildEnvLocal({url: options.url});

      scaffold({appVersion: version, envLocal: envLocal.content, packageName, targetDir, templateDir: resolve(packageDir, 'template')});

      console.log(`Created ${targetDir}`);

      if (envLocal.keyFromEnvironment) {
          console.log('BYTECHEF_APP_KEY taken from the environment and written to .env.local');
      }

      if (options.install) {
          const status = runInstall(packageManager, targetDir);

          if (status !== 0) {
              const {args, command} = installCommand(packageManager);

              console.error(`Installing dependencies failed; run "${command} ${args.join(' ')}" in ${targetDir}`);
              process.exit(status);
          }
      }

      console.log(`
  Next steps:
    cd ${options.directory}
    ${envLocal.keyFromEnvironment ? '' : 'Set BYTECHEF_APP_KEY in .env.local (from the app page in ByteChef)\n    '}${packageManager} run dev
    ${packageManager} run -s bytechef:status
    ${packageManager} run check
  `);
  }

  try {
      main();
  } catch (error) {
      console.error(error.message);
      process.exit(1);
  }
  ```

  `C/package.json` final form: add `"bin": {"create-bytechef-app": "bin/create-bytechef-app.mjs"}`,
  `"repository": {"type": "git", "url": "git+https://github.com/bytechefhq/bytechef.git", "directory": "sdks/frontend/automation/app/create"}`,
  keywords; `chmod +x bin/create-bytechef-app.mjs`. `C/README.md`: usage (both forms, the `--` rule), flags, what gets
  written, that no key flag exists, next steps. `W/package.json`: `"test:create": "cd create && npm test"` and add it
  to `check:all`.

- [ ] **Step 4: Run to verify they pass**: from `C`, `npx vitest run > "$S/t5-green.log" 2>&1; echo $?` → 0, pristine.
  Then an end-to-end dry run through the packed bin: `cd "$C" && npm pack --pack-destination "$S/tpl" > /dev/null; cd "$S/tpl" && rm -rf e2e && mkdir e2e && cd e2e && npm exec --package="$S/tpl/create-bytechef-app-0.1.0.tgz" -- create-bytechef-app my-app --url https://bc.example --no-install > out.log 2>&1; echo $?` → 0; `cat my-app/.env.local` shows the url and an empty key; `ls -a my-app | grep -c "^\.gitignore$"` → 1; `grep '"name"' my-app/package.json` → `my-app`.
  Run `npx prettier --write "$C/bin" "$C/src" "$C/test"` with the workspace config, then `npm run format` at `W` → 0.

- [ ] **Step 5: Commit**

  ```bash
  git add sdks/frontend/automation/app/package.json sdks/frontend/automation/app/package-lock.json \
    sdks/frontend/automation/app/create/package.json sdks/frontend/automation/app/create/README.md sdks/frontend/automation/app/create/vitest.config.mjs \
    sdks/frontend/automation/app/create/bin sdks/frontend/automation/app/create/src sdks/frontend/automation/app/create/test
  git commit -m "- sdk - Add the create-bytechef-app scaffolder"
  ```

---

### Task 6: The CI end-to-end check (`check:template`)

**Files:**
- Create: `W/scripts/check-template.mjs`, `W/scripts/mock-bytechef.mjs`, `W/scripts/fixtures/app.json`, `C/test/overview.test.mjs`
- Modify: `W/package.json` (`check:template`), `.github/workflows/build_pull_request.yml`, `.github/workflows/build_push.yml`

**Interfaces:**
- Consumes: the packed `@bytechef/app` and `create-bytechef-app`; the template's `#placeholder`/`#overview` ids; `buildOverview` from `T/src/server/dev.ts` (tested through a `.mjs` copy? no — see Step 2).
- Produces: `npm run check:template` at `W` exits 0 on success; `node scripts/mock-bytechef.mjs <fixture> [--no-actions]` prints `{"port": N}` on stdout when listening.

- [ ] **Step 1: Fixture + mock.** `scripts/fixtures/app.json`:

  ```json
  {
      "actions": [
          {"actionName": "createContact", "bindingKey": "crm", "componentName": "hubspot", "description": "Creates a contact", "inputSchema": {"additionalProperties": false, "properties": {"email": {"type": "string"}, "name": {"type": "string"}}, "required": ["email"], "type": "object"}}
      ],
      "connections": [
          {"actionNames": ["createContact"], "bindingKey": "crm", "componentName": "hubspot", "componentVersion": 3, "connectionVersion": 1, "proxyAllowedMethods": ["GET"], "proxyEnabled": true, "toolNames": ["searchContacts"]},
          {"actionNames": ["send"], "bindingKey": "mail", "componentName": "gone-component", "componentVersion": null, "connectionVersion": 1, "proxyAllowedMethods": [], "proxyEnabled": false, "toolNames": []}
      ],
      "tools": [
          {"bindingKey": "crm", "componentName": "hubspot", "description": "Search contacts", "name": "CRM__SEARCH_CONTACTS", "parameters": "{\"type\":\"object\",\"properties\":{\"q\":{\"type\":\"string\"}}}", "toolName": "searchContacts"}
      ],
      "workflows": [
          {"available": true, "description": null, "inputSchema": {"properties": {"email": {"type": "string"}}, "type": "object"}, "name": "Onboard customer", "outputSchema": null, "workflowKey": "onboard-customer"}
      ]
  }
  ```

  `scripts/mock-bytechef.mjs`:

  ```js
  import {readFileSync} from 'node:fs';
  import {createServer} from 'node:http';

  const [fixturePath, ...flags] = process.argv.slice(2);
  const fixture = JSON.parse(readFileSync(fixturePath, 'utf8'));
  const noActions = flags.includes('--no-actions');
  const requests = [];

  const server = createServer((request, response) => {
      requests.push({method: request.method, url: request.url});

      const authorization = request.headers.authorization ?? '';

      if (!authorization.startsWith('Bearer ') || authorization === 'Bearer ') {
          response.writeHead(401, {'content-type': 'application/json'});
          response.end(JSON.stringify({code: 'APP_UNAUTHORIZED', message: 'Missing app key'}));

          return;
      }

      if (request.method !== 'GET') {
          response.writeHead(405, {'content-type': 'application/json'});
          response.end(JSON.stringify({code: 'MOCK_UNEXPECTED_POST', message: `Unexpected ${request.method} ${request.url}`}));
          process.exitCode = 3;

          return;
      }

      const routes = {
          '/api/app/v1/actions': noActions ? null : fixture.actions,
          '/api/app/v1/connections': fixture.connections,
          '/api/app/v1/tools': fixture.tools,
          '/api/app/v1/workflows': fixture.workflows,
      };

      if (request.url === '/__requests') {
          response.writeHead(200, {'content-type': 'application/json'});
          response.end(JSON.stringify(requests));

          return;
      }

      if (!(request.url in routes) || routes[request.url] === null) {
          response.writeHead(404, {'content-type': 'text/plain'});
          response.end('Not found');

          return;
      }

      response.writeHead(200, {'content-type': 'application/json'});
      response.end(JSON.stringify(routes[request.url]));
  });

  server.listen(0, '127.0.0.1', () => {
      process.stdout.write(`${JSON.stringify({port: server.address().port})}\n`);
  });
  ```

- [ ] **Step 2: Unit test for `buildOverview`.** `T/src/server/overview.ts` is pure TypeScript with only type
  imports, so vitest in `C` can import it directly. In `C/vitest.config.mjs` set `test.include` to
  `['test/**/*.test.{mjs,ts}']`. `C/test/overview.test.ts`:

  ```ts
  import {readFileSync} from 'node:fs';
  import {describe, expect, it} from 'vitest';

  import {buildOverview} from '../template/src/server/overview';

  const fixture = JSON.parse(readFileSync(new URL('../../scripts/fixtures/app.json', import.meta.url), 'utf8'));

  describe('buildOverview', () => {
      it('lists allowed actions even when the component is unavailable and attaches schemas where known', () => {
          const overview = buildOverview(fixture.connections, fixture.actions, true, fixture.tools.map((tool: {parameters: string}) => ({...tool, parameters: JSON.parse(tool.parameters)})), fixture.workflows);

          expect(overview.bindings.map((binding) => binding.bindingKey)).toEqual(['crm', 'mail']);
          expect(overview.bindings[0]!.actions[0]).toMatchObject({inputSchema: {required: ['email']}, name: 'createContact'});
          expect(overview.bindings[1]).toMatchObject({actions: [{inputSchema: null, name: 'send'}], componentVersion: null});
          expect(overview.bindings[0]!.tools[0]!.name).toBe('CRM__SEARCH_CONTACTS');
      });

      it('marks actionSchemas false when the server has none', () => {
          expect(buildOverview(fixture.connections, [], false, [], []).actionSchemas).toBe(false);
      });
  });
  ```

  Run `npx vitest run` in `C` → green.

- [ ] **Step 3: The harness** `W/scripts/check-template.mjs` (no dependencies). Linux/macOS only (process groups);
  CI runs it on ubuntu:

  ```js
  import {spawn, spawnSync} from 'node:child_process';
  import {existsSync, mkdtempSync, readdirSync, readFileSync, rmSync, statSync, writeFileSync} from 'node:fs';
  import {createServer} from 'node:net';
  import {tmpdir} from 'node:os';
  import {join, resolve} from 'node:path';

  const W = resolve(import.meta.dirname, '..');
  const CANARY = 'sk-ci-canary-4b9e';
  const FIXTURE = join(W, 'scripts/fixtures/app.json');
  const MOCK = join(W, 'scripts/mock-bytechef.mjs');
  const baseEnv = {...process.env};

  delete baseEnv.BYTECHEF_URL;
  delete baseEnv.BYTECHEF_APP_KEY;
  delete baseEnv.NODE_ENV;

  function step(name) {
      console.log(`== ${name}`);
  }

  function assert(condition, message) {
      if (!condition) {
          throw new Error(`Assertion failed: ${message}`);
      }
  }

  function run(command, args, {cwd, env = {}, expectStatus = 0, label}) {
      const result = spawnSync(command, args, {cwd, encoding: 'utf8', env: {...baseEnv, ...env}});

      if (result.status !== expectStatus) {
          throw new Error(`${label}: exit ${result.status} (expected ${expectStatus})\n${result.stdout}\n${result.stderr}`);
      }

      return result;
  }

  function freePort() {
      return new Promise((resolvePort, reject) => {
          const probe = createServer();

          probe.once('error', reject);
          probe.listen(0, '127.0.0.1', () => {
              const {port} = probe.address();

              probe.close(() => resolvePort(port));
          });
      });
  }

  async function waitFor(url, timeoutMs) {
      const deadline = Date.now() + timeoutMs;

      while (Date.now() < deadline) {
          try {
              const response = await fetch(url);

              if (response.status < 500) {
                  return;
              }
          } catch {
              await new Promise((resolveDelay) => setTimeout(resolveDelay, 250));
          }
      }

      throw new Error(`${url} did not answer within ${timeoutMs} ms`);
  }

  function startMock(flags = []) {
      return new Promise((resolveMock, reject) => {
          const child = spawn('node', [MOCK, FIXTURE, ...flags], {cwd: W, detached: true, env: baseEnv, stdio: ['ignore', 'pipe', 'inherit']});
          let output = '';

          child.stdout.on('data', (chunk) => {
              output += chunk;

              const line = output.split('\n').find((candidate) => candidate.startsWith('{'));

              if (line) {
                  resolveMock({child, url: `http://127.0.0.1:${JSON.parse(line).port}`});
              }
          });
          child.once('exit', (code) => reject(new Error(`mock exited with ${code}`)));
      });
  }

  async function startApp(command, args, {cwd, env}) {
      const port = await freePort();
      const child = spawn(command, args, {cwd, detached: true, env: {...baseEnv, ...env, PORT: String(port)}, stdio: ['ignore', 'inherit', 'inherit']});

      await waitFor(`http://127.0.0.1:${port}/`, 60_000);

      return {child, url: `http://127.0.0.1:${port}`};
  }

  function stop(handle) {
      if (handle && handle.child.exitCode === null) {
          process.kill(-handle.child.pid, 'SIGTERM');
      }
  }

  function filesUnder(dir) {
      return readdirSync(dir).flatMap((name) => {
          const path = join(dir, name);

          return statSync(path).isDirectory() ? filesUnder(path) : [path];
      });
  }

  async function requestsSeen(mockUrl) {
      return (await fetch(`${mockUrl}/__requests`, {headers: {authorization: 'Bearer probe'}})).json();
  }

  const work = mkdtempSync(join(tmpdir(), 'bytechef-template-'));
  const appDir = join(work, 'app');
  let mock;
  let app;

  try {
      step('pack');
      run('npm', ['pack', '--pack-destination', work], {cwd: join(W, 'library'), label: 'pack library'});
      run('npm', ['pack', '--pack-destination', work], {cwd: join(W, 'create'), label: 'pack create'});

      step('scaffold');
      run('npm', ['exec', `--package=${join(work, 'create-bytechef-app-0.1.0.tgz')}`, '--', 'create-bytechef-app', 'app', '--url', 'http://127.0.0.1:9', '--no-install'], {cwd: work, label: 'scaffold'});
      assert(existsSync(join(appDir, '.gitignore')) && existsSync(join(appDir, '.dockerignore')), 'dotfiles renamed');

      step('install');
      const packageJson = JSON.parse(readFileSync(join(appDir, 'package.json'), 'utf8'));

      packageJson.dependencies['@bytechef/app'] = `file:${join(work, 'bytechef-app-0.1.0.tgz')}`;
      writeFileSync(join(appDir, 'package.json'), `${JSON.stringify(packageJson, null, 4)}\n`);
      run('npm', ['install'], {cwd: appDir, label: 'install'});

      step('check');
      writeFileSync(join(appDir, '.env.local'), `BYTECHEF_URL=https://example.invalid\nBYTECHEF_APP_KEY=${CANARY}\n`);
      run('npm', ['run', 'check'], {cwd: appDir, env: {BYTECHEF_APP_KEY: CANARY, BYTECHEF_URL: 'https://example.invalid'}, label: 'check'});

      step('dev-mode build fails the bundle check');
      run('npx', ['vite', 'build'], {cwd: appDir, env: {NODE_ENV: 'development'}, label: 'dev-mode build'});
      run('node', ['scripts/check-bundle.mjs'], {cwd: appDir, expectStatus: 1, label: 'bundle check on a dev-mode build'});
      run('npx', ['vite', 'build'], {cwd: appDir, label: 'rebuild'});
      assert(!filesUnder(join(appDir, '.output/server')).some((file) => readFileSync(file, 'utf8').includes('getAppOverview_createServerFn_handler')), 'no dev-only server function in the production build');

      step('status');
      mock = await startMock();
      const status = JSON.parse(run('node', ['scripts/bytechef-status.mjs'], {cwd: appDir, env: {BYTECHEF_APP_KEY: CANARY, BYTECHEF_URL: mock.url}, label: 'status'}).stdout);

      assert(status.actionSchemas === true, 'actionSchemas true');
      assert(status.bindings[0].actions[0].inputSchema.required[0] === 'email', 'action schema attached');
      assert(status.bindings[1].componentVersion === null && status.bindings[1].actions[0].inputSchema === null && status.bindings[1].actions[0].name === 'send', 'unavailable component still lists its actions');
      assert(status.bindings[0].tools[0].name === 'CRM__SEARCH_CONTACTS' && typeof status.bindings[0].tools[0].parameters === 'object', 'tools nested with parsed parameters');

      step('status without a key');
      const missing = run('node', ['scripts/bytechef-status.mjs'], {cwd: appDir, env: {BYTECHEF_APP_KEY: '', BYTECHEF_URL: mock.url}, expectStatus: 1, label: 'status without key'});

      assert(missing.stdout === '' && /BYTECHEF_APP_KEY/.test(missing.stderr), 'empty stdout and a message naming the variable');

      step('status against a server without /actions');
      stop(mock);
      mock = await startMock(['--no-actions']);
      const old = JSON.parse(run('node', ['scripts/bytechef-status.mjs'], {cwd: appDir, env: {BYTECHEF_APP_KEY: CANARY, BYTECHEF_URL: mock.url}, label: 'status old server'}).stdout);

      assert(old.actionSchemas === false && old.bindings[0].actions[0].inputSchema === null && old.bindings[0].actions[0].name === 'createContact', 'names without schemas');
      stop(mock);
      mock = await startMock();

      step('production server renders the placeholder');
      app = await startApp('node', ['.output/server/index.mjs'], {cwd: appDir, env: {BYTECHEF_APP_KEY: CANARY, BYTECHEF_URL: mock.url}});
      const html = await (await fetch(app.url)).text();

      assert(html.includes('id="placeholder"') && !html.includes('id="overview"'), 'placeholder page');
      assert(!/createContact|hubspot|binding-crm/.test(html), 'no fixture data in production');
      assert((await requestsSeen(mock.url)).length === 0, 'production page made no ByteChef request');
      stop(app);

      step('development server renders the overview');
      app = await startApp('npx', ['vite', 'dev', '--host', '127.0.0.1'], {cwd: appDir, env: {BYTECHEF_APP_KEY: CANARY, BYTECHEF_URL: mock.url}});
      const devHtml = await (await fetch(app.url)).text();

      assert(devHtml.includes('id="overview"') && devHtml.includes('id="binding-crm"') && devHtml.includes('createContact'), 'overview rendered');
      assert(devHtml.includes('component unavailable'), 'unavailable component flagged');
      const seen = await requestsSeen(mock.url);

      assert(seen.every((request) => request.method === 'GET'), 'no POST reached ByteChef');
      assert(['/api/app/v1/connections', '/api/app/v1/actions', '/api/app/v1/tools', '/api/app/v1/workflows'].every((path) => seen.some((request) => request.url === path)), 'overview fetched all four listings');

      console.log('check:template passed');
  } finally {
      stop(app);
      stop(mock);
      rmSync(work, {force: true, recursive: true});
  }
  ```

  The dev server honours `--port` not `PORT`; pass `'--port', String(port)` in `startApp` when the command is
  `npx vite dev` (give `startApp` a `portArgs` option: `portArgs: (port) => ['--port', String(port)]` for dev,
  env `PORT` for Nitro). The mock's `/__requests` route must be served before the method check and must not count
  itself: move the `requests.push` after the `/__requests` branch in `mock-bytechef.mjs`, and make `/__requests`
  skip the bearer check.

- [ ] **Step 4: Wire it.** `W/package.json`: `"check:template": "node scripts/check-template.mjs"`. In both workflow
  files' `sdk-app` job, append `npm run check:template` after `npm run check:test-app`. Validate YAML with
  `ruby -ryaml -e 'YAML.load_file(ARGV[0])' <file>` for both.

- [ ] **Step 5: Run it**: from `W`, `npm run check:template > "$S/t6.log" 2>&1; echo $?` → 0, with every step line
  printed. Then prove it fails: temporarily change the template's placeholder id to `placeholder2`, rerun → exit 1 at
  step 10; revert.

- [ ] **Step 6: Commit**

  ```bash
  git add sdks/frontend/automation/app/package.json sdks/frontend/automation/app/scripts sdks/frontend/automation/app/create/test/overview.test.ts \
    sdks/frontend/automation/app/create/vitest.config.mjs .github/workflows/build_pull_request.yml .github/workflows/build_push.yml
  git commit -m "- sdk - Prove the scaffolded app end to end against a mock ByteChef in CI"
  ```

  (If a template file had to change for the harness, add it explicitly and say why in the report.)

---

### Task 7: Docs and spec amendments

**Files:**
- Modify: `W/README.md`, `W/DEVELOPMENT.md`, `.agents/apps.md`, `docs/superpowers/specs/2026-10-06-apps-governed-ai-built-apps-design.md`, `docs/superpowers/specs/2026-10-06-apps-workflows-and-sdk-design.md`

- [ ] **Step 1:** `W/README.md`: a "create" section (what `create-bytechef-app` is, the `npm create … -- --url` usage,
  the `--` rule, `check:template`). `W/DEVELOPMENT.md`: how to work on the template (scaffold into a temp dir with the
  packed library — the Task 4 Step 9 recipe as a `template:scaffold` script if Task 6 added one, otherwise the manual
  commands), `test:create`, `check:template`, Verdaccio for both packages, and the lockstep version rule.
- [ ] **Step 2:** `.agents/apps.md`: a **Template** section — location, the two-module dev-only design and WHY a
  `createServerFn` cannot be gated inline (module-level only; dynamic import gated on `import.meta.env.DEV` is
  eliminated from builds; `NODE_ENV=development vite build` is the one way to ship dev code and `npm run check`
  catches it), the status JSON contract and the `/actions` 404 fallback, the leak check's rules, the `cn`/`radix-ui`
  gotcha, the exact-pin rule, the npm `--` rule, Nitro as the hosting layer. Amend the SDK section's bullet that the
  template imports the ready-made server functions (it does not), and add `actions.list()` + `AppAction` to the SDK
  surface list. Also add `GET /actions` and `ActionInputSchemaGenerator` to the server contract part (with the note
  that tools still use the flat `JsonSchemaGeneratorUtils`).
- [ ] **Step 3:** B spec (`2026-10-06-apps-workflows-and-sdk-design.md`): amend the spike-verdict sentence
  ("The template (C) therefore imports the ready-made server functions …") to say C writes its own server functions
  on `bytechefMiddleware()` and the ready-made ones stay opt-in; add `actions.list()` to the core API list and
  `AppAction` to the types. Apps spec (`2026-10-06-apps-governed-ai-built-apps-design.md`): replace the "Design — C.
  Template" body with a pointer to `2026-10-07-apps-template-design.md`; in Testing → "Template", describe the real
  check (`check:template`); in Testing → "End to end", reword the release check: the development overview renders
  the bound actions and "Try it" executes one; the deployed page is a placeholder; disabling the app makes
  `bytechef:status` fail within one request.
- [ ] **Step 4: Commit**

  ```bash
  git add sdks/frontend/automation/app/README.md sdks/frontend/automation/app/DEVELOPMENT.md
  git commit -m "- sdk - Document the app template and scaffolder"
  git add .agents/apps.md docs/superpowers/specs/2026-10-06-apps-governed-ai-built-apps-design.md docs/superpowers/specs/2026-10-06-apps-workflows-and-sdk-design.md
  git commit -m "- docs - Sync the Apps specs and deep-dive with the template"
  ```

- [ ] **Step 5: Final verification** (controller may run): from `W`, `npm ci`, then with
  `BYTECHEF_APP_KEY=sk-ci-canary-4b9e BYTECHEF_URL=https://example.invalid`: `npm run format`, `npm run check:all`,
  `npm run build:all`, `npm run check:test-app`, `npm run check:template` → all 0; the four Gradle modules' `check`
  → 0; `git status --short` clean.
