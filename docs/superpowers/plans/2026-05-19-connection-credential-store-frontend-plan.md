# Connection Credential Store Frontend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task.

**Goal:** Surface the connection credential-store choice in the UI: a conditional storage picker in the Connection Create dialog, a "Register existing credential" toggle/mode for read-only deployments, and a new REST endpoint behind it that calls the existing `ConnectionService.registerExisting` from PR 1.

**Architecture:** Single React component change (`ConnectionDialog.tsx`) driven by the existing GraphQL info query `useConnectionCredentialStoresQuery`. The submit flow stays REST-consistent — extends the existing `ConnectionApi.createConnection` (adds `credentialStoreType`) and adds a parallel `registerExistingConnection` REST endpoint. OpenAPI spec drives both server controller skeleton and TypeScript client.

**Tech Stack:** React 19, TypeScript 5.9, react-hook-form, shadcn/ui (Select, FormField, Switch, Alert), Lingui i18n, Vitest, Spring Boot 4 REST controllers, OpenAPI/swagger-codegen.

**Spec:** [docs/superpowers/specs/2026-05-19-connection-credential-store-frontend-design.md](../specs/2026-05-19-connection-credential-store-frontend-design.md)

**Issue:** [bytechefhq/bytechef#547](https://github.com/bytechefhq/bytechef/issues/547)

**Branch state at start:** `claude/amazing-brahmagupta-12e13d` at commit `af05491d0f6` (frontend spec REST correction).

---

## Branch hygiene — applies to every task

Every subagent dispatch MUST:
1. Run `git branch --show-current` and confirm `claude/amazing-brahmagupta-12e13d` before any change.
2. NEVER run `git checkout`, `git pull`, `git rebase`, `git fetch`.
3. If anything seems wrong with the branch state, STOP and report — don't try to fix with git operations.

---

## File Structure

**New files:**
| File | Responsibility |
|---|---|
| `client/src/shared/components/connection/connectionCredentialStoreLabels.ts` | Enum-to-friendly-name mapping |
| `client/src/shared/components/connection/ConnectionDialog.test.tsx` | 7 component tests |
| Backend REST controller test (new or extending existing) | MockMvc test for register-existing endpoint |

**Modified files:**
| File | Change |
|---|---|
| OpenAPI spec yaml (location confirmed by Task 1) | Add `credentialStoreType` field + new `/register-existing` path |
| Connection REST controller (location confirmed by Task 1) | New `POST /register-existing` endpoint method |
| `client/src/shared/middleware/automation/configuration/*` | Regenerated TS client |
| `client/src/shared/mutations/automation/connections.mutations.ts` | Add `useRegisterExistingConnectionMutation` |
| `client/src/shared/components/connection/ConnectionDialog.tsx` | Picker + toggle + register-existing mode + branched submit |
| `client/src/locales/en/messages.po` | 11 new translation keys |

---

### Task 1: Locate existing REST scaffolding

This is a discovery-only task. No code changes, no commit. Output: a brief report fed to subsequent tasks.

- [ ] **Step 1: Verify branch state**

```bash
git branch --show-current   # → claude/amazing-brahmagupta-12e13d
git log --oneline -1        # top is af05491d0f6 547 Switch frontend spec from GraphQL to REST for register-existing
```

- [ ] **Step 2: Find the connection REST controller**

```bash
find server/libs/automation server/libs/platform -name "ConnectionApiController.java" -type f
```

Expected: one match under `server/libs/automation/automation-configuration-rest/.../web/rest/`. Open the file, identify the existing `createConnection(...)` handler — note its `@PostMapping` annotation, request body type (likely a generated model class), and response shape.

- [ ] **Step 3: Find the OpenAPI spec**

```bash
find server/libs/automation -name "*.yaml" -path "*swagger*" -o -name "*.yml" -path "*openapi*" 2>/dev/null | head -5
grep -l "createConnection" server/libs/automation -r --include="*.yaml" --include="*.yml" 2>/dev/null
```

Expected: spec file under `server/libs/automation/automation-configuration-rest/automation-configuration-rest-impl/openapi.yaml` or similar (likely `automation-swagger` adjacent). Open it. Identify:
- The `ConnectionModel` schema (the request body type)
- The existing `POST /connections` path operation (or whatever the create path is)
- The codegen target — both the server stub package and the TypeScript client output path

- [ ] **Step 4: Confirm the client mutations file**

```bash
cat client/src/shared/mutations/automation/connections.mutations.ts | head -50
```

Note the exact pattern for `useCreateConnectionMutation` — react-query mutation, calls `new ConnectionApi().createConnection(...)`. The new `useRegisterExistingConnectionMutation` will mirror this shape.

- [ ] **Step 5: Confirm codegen invocation**

```bash
grep -n "openapi\|swagger\|generateClient" client/package.json client/codegen.ts 2>/dev/null | head -10
```

Identify whether the OpenAPI client is generated via gradle (`./gradlew generate...`) or npm (`npm run openapi`). Note the exact command — Task 2 will use it.

- [ ] **Step 6: Report findings (no commit)**

Output a brief summary covering:
- REST controller file path + method signature for `createConnection`
- OpenAPI spec file path + connection model name (e.g., `ConnectionModel`)
- TypeScript client output directory under `client/src/shared/middleware/`
- Codegen command (gradle task or npm script)

These feed Tasks 2 and 3.

---

### Task 2: OpenAPI spec additions

**Files (modify, location from Task 1):** The OpenAPI spec yaml + regenerated server stubs and TS client.

- [ ] **Step 1: Verify branch state**

```bash
git branch --show-current   # → claude/amazing-brahmagupta-12e13d
```

- [ ] **Step 2: Add credentialStoreType to the existing create body**

Open the OpenAPI spec yaml from Task 1. Find the `ConnectionModel` schema. Add a new property:

```yaml
        credentialStoreType:
          type: string
          enum:
            - DATABASE
            - AWS_SECRETS_MANAGER
            - HASHICORP_VAULT
          default: DATABASE
          description: Backend that stores the credential payload. Defaults to DATABASE.
```

Place alphabetically with sibling properties.

- [ ] **Step 3: Add the new /register-existing path**

In the `paths:` section, add (alongside the existing `/connections` POST):

```yaml
  /connections/register-existing:
    post:
      tags:
        - connection   # match the tag the existing endpoint uses
      summary: Register a connection backed by an externally-provisioned credential
      operationId: registerExistingConnection
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/RegisterExistingConnectionRequestModel'
      responses:
        '200':
          description: Connection registered
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ConnectionModel'
        '400':
          description: Read-only store rejected the write, or secret not found at credentialRef
```

Add the request schema under `components.schemas`:

```yaml
    RegisterExistingConnectionRequestModel:
      type: object
      required:
        - componentName
        - connectionVersion
        - credentialRef
        - credentialStoreType
        - environmentId
        - name
        - type
      properties:
        componentName:
          type: string
        connectionVersion:
          type: integer
          format: int32
        credentialRef:
          type: string
          description: External-store path or UUID where the secret already lives.
        credentialStoreType:
          type: string
          enum:
            - AWS_SECRETS_MANAGER
            - HASHICORP_VAULT
          description: Must be a non-DATABASE store. DATABASE values are rejected.
        environmentId:
          type: integer
          format: int64
        name:
          type: string
        type:
          $ref: '#/components/schemas/PlatformType'
        tags:
          type: array
          items:
            $ref: '#/components/schemas/TagModel'
```

(Match exact existing schema names like `TagModel` / `PlatformType` from the spec.)

- [ ] **Step 4: Run codegen**

Use the command from Task 1 Step 5. Examples (use the one that applies):
- Gradle: `./gradlew :server:libs:automation:automation-configuration-rest:automation-configuration-rest-impl:openApiGenerate` (or similar)
- npm: `cd client && npm run openapi` (if defined)

Verify generated changes:
- Server: new `RegisterExistingConnectionRequestModel.java` (or whatever the model class is named) + new method signature in the generated API interface
- Client: new `registerExistingConnection` method on `ConnectionApi` + new `RegisterExistingConnectionRequestModel` TS type

- [ ] **Step 5: Compile/check the server changes from codegen**

```bash
./gradlew :server:libs:automation:automation-configuration-rest:automation-configuration-rest-impl:compileJava
```
Expected: BUILD SUCCESSFUL (the generated interface adds an abstract method; the concrete controller doesn't yet implement it — compile fails at the controller, expected, fix in Task 3).

If gradle compile fails ONLY in the controller class (missing `registerExistingConnection` implementation), that's the expected state. Other failures need investigation.

- [ ] **Step 6: Stage but don't commit yet**

```bash
git status --short
```

Expected files modified/added: OpenAPI yaml + generated Java models + generated TS client files. Don't commit yet — combine with Task 3.

---

### Task 3: Backend controller endpoint + test

**Files:**
- Modify: the connection REST controller (location from Task 1)
- Create: REST controller test (or extend existing)

- [ ] **Step 1: Verify branch state**

```bash
git branch --show-current   # → claude/amazing-brahmagupta-12e13d
```

- [ ] **Step 2: Implement the controller method**

Open the controller. Add a method matching the generated interface signature. Pattern:

```java
@Override
public ResponseEntity<ConnectionModel> registerExistingConnection(
    RegisterExistingConnectionRequestModel requestModel) {

    Connection connection = new Connection();
    connection.setComponentName(requestModel.getComponentName());
    connection.setConnectionVersion(requestModel.getConnectionVersion());
    connection.setName(requestModel.getName());
    connection.setEnvironmentId(requestModel.getEnvironmentId().intValue());
    connection.setType(PlatformType.values()[requestModel.getType().ordinal()]);
    // Tags handled identically to createConnection (look at the existing handler for the pattern)

    ConnectionCredentialStoreType storeType = ConnectionCredentialStoreType.valueOf(
        requestModel.getCredentialStoreType().getValue());

    Connection registered = connectionService.registerExisting(
        connection, storeType, requestModel.getCredentialRef());

    return ResponseEntity.ok(toConnectionModel(registered));
}
```

`toConnectionModel(...)` is whatever conversion helper the existing `createConnection` handler uses. Match the existing pattern exactly.

The controller likely needs `ConnectionService` injected — if it's not already there, add it via constructor following the existing DI pattern.

- [ ] **Step 3: Compile**

```bash
./gradlew :server:libs:automation:automation-configuration-rest:automation-configuration-rest-impl:compileJava
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Write the controller test**

Locate the existing controller test (likely `ConnectionApiControllerTest.java` or `*IntTest.java`). If it exists, extend it. Otherwise, create a new test class following the project's MockMvc pattern.

Add two test methods:

```java
@Test
void testRegisterExistingConnectionHappyPath() throws Exception {
    Connection saved = new Connection();
    saved.setId(42L);

    when(connectionService.registerExisting(
        any(Connection.class),
        eq(ConnectionCredentialStoreType.AWS_SECRETS_MANAGER),
        eq("bytechef/connections/abc-uuid")))
        .thenReturn(saved);

    mockMvc.perform(
            post("/connections/register-existing")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "componentName": "acme-api",
                      "connectionVersion": 1,
                      "credentialStoreType": "AWS_SECRETS_MANAGER",
                      "credentialRef": "bytechef/connections/abc-uuid",
                      "environmentId": 1,
                      "name": "my prod connection",
                      "type": "AUTOMATION"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(42));
}

@Test
void testRegisterExistingConnectionRejectsReadOnlyStore() throws Exception {
    when(connectionService.registerExisting(
        any(Connection.class), any(), anyString()))
        .thenThrow(new ReadOnlyCredentialStoreException(ConnectionCredentialStoreType.HASHICORP_VAULT));

    mockMvc.perform(
            post("/connections/register-existing")
                .contentType(MediaType.APPLICATION_JSON)
                .content(/* same body but with HASHICORP_VAULT */ ""))
        .andExpect(status().is4xxClientError());
}
```

Adapt to whatever test fixtures the existing controller test uses (`MockMvc` setup, mocked `ConnectionService` bean, etc.).

- [ ] **Step 5: Run the controller test**

```bash
./gradlew :server:libs:automation:automation-configuration-rest:automation-configuration-rest-impl:test
```
Expected: BUILD SUCCESSFUL, both new tests pass.

- [ ] **Step 6: Module check**

```bash
./gradlew :server:libs:automation:automation-configuration-rest:automation-configuration-rest-impl:check
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit Tasks 2 + 3 together**

```bash
git add \
  server/libs/automation/automation-configuration-rest/ \
  client/src/shared/middleware/automation/configuration/
git commit -m "$(cat <<'EOF'
547 Add /connections/register-existing REST endpoint

Backend support for the upcoming frontend "Register existing
credential" flow. The endpoint constructs a Connection from the
request body and delegates to ConnectionService.registerExisting
(added in PR 1), which probes the external store and saves the
row with credentialRef populated.

The existing /connections create endpoint accepts an optional
credentialStoreType field via the regenerated OpenAPI client —
defaults to DATABASE for back-compat. Existing clients are
unaffected.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 4: Client mutation hook + labels file

**Files:**
- Modify: `client/src/shared/mutations/automation/connections.mutations.ts`
- Create: `client/src/shared/components/connection/connectionCredentialStoreLabels.ts`

- [ ] **Step 1: Verify branch state**

```bash
git branch --show-current   # → claude/amazing-brahmagupta-12e13d
```

- [ ] **Step 2: Inspect the existing mutation hook**

```bash
cat client/src/shared/mutations/automation/connections.mutations.ts
```

Note the shape of `useCreateConnectionMutation` — likely a function that takes `MutationOptions`, calls `useMutation` with `mutationFn: (connection) => new ConnectionApi().createConnection({connectionModel: connection})`.

- [ ] **Step 3: Add useRegisterExistingConnectionMutation**

Add a parallel hook right after `useCreateConnectionMutation`:

```typescript
export const useRegisterExistingConnectionMutation = (
    mutationProps: MutationOptionsType<Connection, RegisterExistingConnectionRequestModel>
) =>
    useMutation<Connection, ErrorType, RegisterExistingConnectionRequestModel>({
        mutationFn: (requestModel: RegisterExistingConnectionRequestModel) =>
            new ConnectionApi().registerExistingConnection({registerExistingConnectionRequestModel: requestModel}),
        ...mutationProps,
    });
```

Match the EXACT signature pattern of `useCreateConnectionMutation` (parameter names, generic type ordering, options spread). Look at it and copy verbatim, just substituting the operation.

Add the import:
```typescript
import {RegisterExistingConnectionRequestModel} from '@/shared/middleware/automation/configuration';
```

- [ ] **Step 4: Create the labels file**

Create `client/src/shared/components/connection/connectionCredentialStoreLabels.ts`:

```typescript
import {ConnectionCredentialStoreType} from '@/shared/middleware/graphql';

export const connectionCredentialStoreLabels: Record<ConnectionCredentialStoreType, string> = {
    [ConnectionCredentialStoreType.AwsSecretsManager]: 'AWS Secrets Manager',
    [ConnectionCredentialStoreType.Database]: 'Database',
    [ConnectionCredentialStoreType.HashicorpVault]: 'HashiCorp Vault',
};
```

Per CLAUDE.md ESLint `sort-keys` rule, the keys must be alphabetical: `AwsSecretsManager` < `Database` < `HashicorpVault`. ESLint `--fix` does NOT auto-fix sort-keys — verify manually.

- [ ] **Step 5: Run client check**

```bash
cd client && npm run check
cd ..
```
Expected: lint + typecheck + tests all pass.

- [ ] **Step 6: Commit**

```bash
git add \
  client/src/shared/mutations/automation/connections.mutations.ts \
  client/src/shared/components/connection/connectionCredentialStoreLabels.ts
git commit -m "$(cat <<'EOF'
547 client - Add useRegisterExistingConnectionMutation and labels

Parallel mutation hook to useCreateConnectionMutation, calling the new
REST endpoint added in the previous commit. Labels file maps the
ConnectionCredentialStoreType enum values to user-friendly strings;
ConnectionDialog.tsx will consume both in the next commit.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 5: ConnectionDialog picker + toggle + register mode

**File:** `client/src/shared/components/connection/ConnectionDialog.tsx`

- [ ] **Step 1: Verify branch state**

```bash
git branch --show-current   # → claude/amazing-brahmagupta-12e13d
```

- [ ] **Step 2: Read ConnectionDialog.tsx**

```bash
wc -l client/src/shared/components/connection/ConnectionDialog.tsx
```

The file is ~700 lines. Read it end to end before editing. Key landmarks to find:
- Form schema / `useForm` call (around line 115)
- Submit handler (`onSubmit` or `handleSave`, line ~369)
- Name field rendering (around line 545)
- Edit mode detection (likely a `connection?.id != null` check or an `isEdit` flag)

- [ ] **Step 3: Add the stores query + form state**

At the top of the component body (alongside other hooks), add:

```typescript
const {data: storesData, isLoading: storesLoading} = useConnectionCredentialStoresQuery();

const stores = storesData?.connectionCredentialStores ?? [];
const externalStore = stores.find(store => store.type !== ConnectionCredentialStoreType.Database);
const showPicker = stores.length > 1;
```

Add the imports:
```typescript
import {ConnectionCredentialStoreType, useConnectionCredentialStoresQuery} from '@/shared/middleware/graphql';
import {connectionCredentialStoreLabels} from './connectionCredentialStoreLabels';
```

The form's existing `useForm` schema needs two new optional fields. Find the form's type/schema and extend:

```typescript
type ConnectionDialogFormProps = {
    // ...existing fields
    credentialStoreType?: ConnectionCredentialStoreType;
    registeringExisting?: boolean;
    credentialRef?: string;
};
```

Default values: `credentialStoreType: ConnectionCredentialStoreType.Database`, `registeringExisting: false`, `credentialRef: ''`.

- [ ] **Step 4: Render the picker after the Name field**

Find the existing Name FormField and insert immediately after:

```tsx
{showPicker && !isEdit && (
    <FormField
        control={form.control}
        name="credentialStoreType"
        render={({field}) => (
            <FormItem>
                <FormLabel>{t`Credential storage`}</FormLabel>
                <Select
                    onValueChange={value => {
                        field.onChange(value);
                        // If external is read-only, lock the toggle on.
                        const selected = stores.find(store => store.type === value);
                        if (selected?.readOnly) {
                            form.setValue('registeringExisting', true);
                        }
                    }}
                    value={field.value}
                >
                    <FormControl>
                        <SelectTrigger>
                            <SelectValue />
                        </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                        {stores.map(store => (
                            <SelectItem key={store.type} value={store.type}>
                                {connectionCredentialStoreLabels[store.type]}
                            </SelectItem>
                        ))}
                    </SelectContent>
                </Select>
                <FormMessage />
            </FormItem>
        )}
    />
)}

{showPicker && isEdit && (
    <FormItem>
        <FormLabel>{t`Credential storage`}</FormLabel>
        <Input
            value={connectionCredentialStoreLabels[connection?.credentialStoreType ?? ConnectionCredentialStoreType.Database]}
            disabled
        />
        <p className="text-xs text-muted-foreground">
            {t`Credential storage cannot be changed after creation.`}
        </p>
    </FormItem>
)}
```

`t` is the Lingui template tag — add `import {t} from '@lingui/core/macro';` if not already imported.

- [ ] **Step 5: Render the register-existing toggle and read-only alert**

Add immediately after the picker:

```tsx
{showPicker && !isEdit && form.watch('credentialStoreType') !== ConnectionCredentialStoreType.Database && (
    <>
        {externalStore?.readOnly && (
            <Alert>
                <AlertDescription>
                    {t`This credential store is configured read-only by your administrator. Provision the secret externally, then reference it here.`}
                </AlertDescription>
            </Alert>
        )}
        <FormField
            control={form.control}
            name="registeringExisting"
            render={({field}) => (
                <FormItem className="flex items-center gap-2">
                    <FormControl>
                        <Switch
                            checked={field.value}
                            onCheckedChange={field.onChange}
                            disabled={externalStore?.readOnly}
                        />
                    </FormControl>
                    <FormLabel className="!mt-0">
                        {t`Register existing credential`}
                    </FormLabel>
                </FormItem>
            )}
        />
    </>
)}
```

Add the import: `import {Switch} from '@/components/ui/switch';` (verify the exact import path matches other Switch usages in the codebase).

- [ ] **Step 6: Render the credential-reference field when register-existing is on**

When `registeringExisting === true`, the dialog's normal credential fields (API key, OAuth client ID, etc.) should be hidden, replaced by a single reference field. Find where the normal credential fields render (likely a `Properties` component or similar dynamic rendering block). Wrap them:

```tsx
{form.watch('registeringExisting') ? (
    <FormField
        control={form.control}
        name="credentialRef"
        render={({field}) => (
            <FormItem>
                <FormLabel>{t`Credential reference`}</FormLabel>
                <FormControl>
                    <Input {...field} placeholder="bytechef/connections/..." />
                </FormControl>
                <p className="text-xs text-muted-foreground">
                    {t`The path or UUID where your secret lives in the external store. Format depends on your operator's path template configuration.`}
                </p>
                <FormMessage />
            </FormItem>
        )}
    />
) : (
    // ...existing credential / authorization properties rendering
)}
```

- [ ] **Step 7: Branch the submit handler**

Find `onSubmit` (or whatever the form submit function is named). Branch it based on `registeringExisting`:

```typescript
const onSubmit: SubmitHandler<ConnectionDialogFormProps> = (values) => {
    if (values.registeringExisting) {
        registerExistingMutation.mutate({
            componentName: values.componentName,
            connectionVersion: values.connectionVersion,
            credentialRef: values.credentialRef!,
            credentialStoreType: values.credentialStoreType! as 'AWS_SECRETS_MANAGER' | 'HASHICORP_VAULT',
            environmentId: currentEnvironmentId,
            name: values.name,
            tags: values.tags,
            type: platformType,
        });
        return;
    }

    // ...existing call to createConnection / updateConnection
    connectionMutation.mutateAsync(getNewConnection({
        ...values,
        credentialStoreType: values.credentialStoreType ?? 'DATABASE',
    }));
};
```

Add the new mutation hook at the top of the component:
```typescript
const registerExistingMutation = useRegisterExistingConnectionMutation({
    onSuccess: (data) => {
        // mirror the existing onSuccess behavior of useCreateConnectionMutation
        // (refetch connections list, close dialog, etc.)
    },
});
```

Look at how `useCreateConnectionMutation` is wired today and mirror its onSuccess/onError handlers.

- [ ] **Step 8: Run client check**

```bash
cd client && npm run check
cd ..
```
Expected: lint + typecheck + tests pass. Common pitfalls:
- **`sort-keys` violation**: object literals in the JSX must have keys alphabetically sorted.
- **Hook order**: per CLAUDE.md, `useState` → `useRef` → custom stores → other custom hooks → memos → effects → return. The new hooks go in their correct slot.
- **`@typescript-eslint/no-non-null-asserted-optional-chain`**: avoid `foo?.bar!` patterns.

- [ ] **Step 9: Commit**

```bash
git add client/src/shared/components/connection/ConnectionDialog.tsx
git commit -m "$(cat <<'EOF'
547 client - Wire credential store picker into ConnectionDialog

Conditional Select (hidden when stores.length === 1), Register
existing credential toggle, alternate field-set for register mode,
and branched submit between useCreateConnectionMutation and
useRegisterExistingConnectionMutation.

Edit mode shows credential store as disabled read-only text with
an explanatory caption; users cannot change the storage backing
after creation (would require server-side migration).

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 6: ConnectionDialog component tests

**File (create):** `client/src/shared/components/connection/ConnectionDialog.test.tsx`

- [ ] **Step 1: Verify branch state**

```bash
git branch --show-current   # → claude/amazing-brahmagupta-12e13d
```

- [ ] **Step 2: Inspect an existing component test for the project's pattern**

```bash
cat client/src/shared/components/EnvironmentSelect/EnvironmentSelect.test.tsx
```

Note: imports, `vi.mock(...)` for `@/shared/middleware/graphql`, render helper (likely a wrapper that provides QueryClient), assertion library (RTL `screen.getByRole`, etc.).

- [ ] **Step 3: Write the test file**

Create `ConnectionDialog.test.tsx`:

```tsx
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {describe, expect, it, vi} from 'vitest';

import ConnectionDialog from './ConnectionDialog';

// 1. Mock the GraphQL hook based on what each test needs
const mockStoresQuery = vi.fn();

vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();
    return {
        ...actual,
        useConnectionCredentialStoresQuery: () => mockStoresQuery(),
    };
});

// 2. Mock the mutation hooks so tests can verify which one was called
const createMutation = vi.fn();
const registerMutation = vi.fn();

vi.mock('@/shared/mutations/automation/connections.mutations', () => ({
    useCreateConnectionMutation: ({onSuccess}: any) => ({
        mutate: createMutation,
        mutateAsync: createMutation,
        isPending: false,
    }),
    useRegisterExistingConnectionMutation: ({onSuccess}: any) => ({
        mutate: registerMutation,
        mutateAsync: registerMutation,
        isPending: false,
    }),
    useUpdateConnectionMutation: () => ({mutate: vi.fn(), isPending: false}),
}));

function renderDialog(props: Partial<React.ComponentProps<typeof ConnectionDialog>> = {}) {
    const client = new QueryClient({defaultOptions: {queries: {retry: false}}});
    const defaultProps: React.ComponentProps<typeof ConnectionDialog> = {
        // Fill in based on actual ConnectionDialog props signature.
        // Look at ConnectionDialog.tsx top-level props type and provide minimum required props.
    };
    return render(
        <QueryClientProvider client={client}>
            <ConnectionDialog {...defaultProps} {...props} />
        </QueryClientProvider>
    );
}

describe('ConnectionDialog credential store integration', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('hides the credential storage picker when only DATABASE is registered', () => {
        mockStoresQuery.mockReturnValue({
            data: {connectionCredentialStores: [{type: 'DATABASE', readOnly: false}]},
            isLoading: false,
        });

        renderDialog();

        expect(screen.queryByLabelText(/credential storage/i)).not.toBeInTheDocument();
    });

    it('shows the picker with both options when an external store is configured', () => {
        mockStoresQuery.mockReturnValue({
            data: {
                connectionCredentialStores: [
                    {type: 'DATABASE', readOnly: false},
                    {type: 'AWS_SECRETS_MANAGER', readOnly: false},
                ],
            },
            isLoading: false,
        });

        renderDialog();

        expect(screen.getByLabelText(/credential storage/i)).toBeInTheDocument();
        expect(screen.getByText('Database')).toBeInTheDocument();
        expect(screen.getByText('AWS Secrets Manager')).toBeInTheDocument();
    });

    it('shows the register-existing toggle when external store is selected (read-write)', async () => {
        const user = userEvent.setup();
        mockStoresQuery.mockReturnValue({
            data: {
                connectionCredentialStores: [
                    {type: 'DATABASE', readOnly: false},
                    {type: 'AWS_SECRETS_MANAGER', readOnly: false},
                ],
            },
            isLoading: false,
        });

        renderDialog();

        const picker = screen.getByLabelText(/credential storage/i);
        await user.click(picker);
        await user.click(screen.getByText('AWS Secrets Manager'));

        const toggle = await screen.findByLabelText(/register existing credential/i);
        expect(toggle).toBeEnabled();
        expect(toggle).not.toBeChecked();
    });

    it('locks the toggle on when external store is read-only', async () => {
        const user = userEvent.setup();
        mockStoresQuery.mockReturnValue({
            data: {
                connectionCredentialStores: [
                    {type: 'DATABASE', readOnly: false},
                    {type: 'HASHICORP_VAULT', readOnly: true},
                ],
            },
            isLoading: false,
        });

        renderDialog();

        await user.click(screen.getByLabelText(/credential storage/i));
        await user.click(screen.getByText('HashiCorp Vault'));

        await waitFor(() => {
            const toggle = screen.getByLabelText(/register existing credential/i);
            expect(toggle).toBeChecked();
            expect(toggle).toBeDisabled();
        });

        expect(screen.getByText(/configured read-only by your administrator/i)).toBeInTheDocument();
    });

    it('disables the picker on edit and hides the toggle', () => {
        mockStoresQuery.mockReturnValue({
            data: {
                connectionCredentialStores: [
                    {type: 'DATABASE', readOnly: false},
                    {type: 'AWS_SECRETS_MANAGER', readOnly: false},
                ],
            },
            isLoading: false,
        });

        // Pass a connection prop that represents an existing connection (with id set).
        renderDialog({/* connection: {id: 1, credentialStoreType: 'AWS_SECRETS_MANAGER', ...} */});

        const picker = screen.getByLabelText(/credential storage/i);
        expect(picker).toBeDisabled();
        expect(screen.queryByLabelText(/register existing credential/i)).not.toBeInTheDocument();
    });

    it('calls registerExistingConnection mutation when toggle is on', async () => {
        const user = userEvent.setup();
        mockStoresQuery.mockReturnValue({
            data: {
                connectionCredentialStores: [
                    {type: 'DATABASE', readOnly: false},
                    {type: 'HASHICORP_VAULT', readOnly: false},
                ],
            },
            isLoading: false,
        });

        renderDialog();

        await user.click(screen.getByLabelText(/credential storage/i));
        await user.click(screen.getByText('HashiCorp Vault'));
        await user.click(screen.getByLabelText(/register existing credential/i));

        // Fill the name and credentialRef
        await user.type(screen.getByLabelText(/name/i), 'my conn');
        await user.type(screen.getByLabelText(/credential reference/i), 'bytechef/connections/abc');

        // Submit
        await user.click(screen.getByRole('button', {name: /save|create/i}));

        await waitFor(() => {
            expect(registerMutation).toHaveBeenCalled();
            expect(createMutation).not.toHaveBeenCalled();
        });
    });

    it('falls back to DATABASE behavior when stores query errors', () => {
        mockStoresQuery.mockReturnValue({
            data: undefined,
            error: new Error('GraphQL error'),
            isLoading: false,
        });

        renderDialog();

        expect(screen.queryByLabelText(/credential storage/i)).not.toBeInTheDocument();
    });
});
```

**IMPORTANT:** The test file will likely need adaptation to match the EXACT props/structure of `ConnectionDialog`. You must read the component once before finalizing each `renderDialog` call's props. The above is a structural template — the labels, prop names, and selectors need to match what the component actually renders.

- [ ] **Step 4: Run the tests**

```bash
cd client && npm run test -- ConnectionDialog.test
cd ..
```
Expected: 7 tests pass.

Common failure modes:
- **Labels don't match**: If picker label is "Credential storage" but test uses `/credential storage/i` and the actual rendered text is "Credential storage:", adjust the regex.
- **`renderDialog` props missing**: The component may require props for component definitions, environment, etc. — feed them as mocks.
- **Mutation hook destructuring mismatch**: The mock returns `{mutate, mutateAsync, isPending}` — if the component destructures other properties, add them.

If tests fail due to ConnectionDialog requiring complex context (e.g., MSW handlers for component definitions), simplify by mocking those queries too at the top of the test file. Don't fight the existing component structure.

- [ ] **Step 5: Commit**

```bash
git add client/src/shared/components/connection/ConnectionDialog.test.tsx
git commit -m "$(cat <<'EOF'
547 client - Test ConnectionDialog credential store integration

Seven scenarios: hidden-on-single-store, visible-on-two-store,
toggle-shown-on-external-rw, toggle-locked-on-external-readonly,
disabled-on-edit, submit-branches-to-register-mutation, falls-back-on-query-error.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 7: i18n + npm run check + spotless sweep

- [ ] **Step 1: Verify branch state**

```bash
git branch --show-current   # → claude/amazing-brahmagupta-12e13d
```

- [ ] **Step 2: Add i18n entries**

Open `client/src/locales/en/messages.po`. The file uses gettext PO format. Add new entries (place at the end of the file or grouped near related connection strings):

```
msgid "Credential storage"
msgstr ""

msgid "Database"
msgstr ""

msgid "AWS Secrets Manager"
msgstr ""

msgid "HashiCorp Vault"
msgstr ""

msgid "Register existing credential"
msgstr ""

msgid "My credential already exists in {storeName}; I'll provide the reference."
msgstr ""

msgid "This credential store is configured read-only by your administrator. Provision the secret externally, then reference it here."
msgstr ""

msgid "Credential reference"
msgstr ""

msgid "The path or UUID where your secret lives in the external store. Format depends on your operator's path template configuration."
msgstr ""

msgid "No secret found at that reference. Check the path with your administrator."
msgstr ""

msgid "Credential storage cannot be changed after creation."
msgstr ""
```

`msgstr` empty is correct — Lingui's English strings come from the `msgid` itself. Translators fill in `msgstr` for other locales.

If the project has a Lingui CLI command to extract strings (`lingui extract`), run it instead of manually editing the `.po` file — but inspect what changed and only commit the intended additions.

- [ ] **Step 3: Compile messages**

```bash
cd client && npx lingui compile && cd ..
```

This regenerates `client/src/locales/en/messages.ts` (the compiled message catalog). If the command isn't `lingui compile`, find the right command in `client/package.json` scripts.

- [ ] **Step 4: Run full client check**

```bash
cd client && npm run check
cd ..
```
Expected: lint + typecheck + tests all pass.

- [ ] **Step 5: Backend spotless + check sweep**

```bash
./gradlew \
  :server:libs:automation:automation-configuration-rest:automation-configuration-rest-impl:spotlessApply \
  :server:libs:automation:automation-configuration-rest:automation-configuration-rest-impl:check
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: If spotless or codegen changed files, commit**

```bash
git status --short
git add -u
git commit -m "$(cat <<'EOF'
547 Apply formatting / i18n compilation

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

If nothing changed, skip this commit.

- [ ] **Step 7: Verify commit graph**

```bash
git log --oneline af05491d0f6..HEAD
```

Expected (~5-6 new commits):
```
<hash> 547 Apply formatting / i18n compilation              (only if Step 6 was needed)
<hash> 547 client - Test ConnectionDialog credential store integration
<hash> 547 client - Wire credential store picker into ConnectionDialog
<hash> 547 client - Add useRegisterExistingConnectionMutation and labels
<hash> 547 Add /connections/register-existing REST endpoint
```

Frontend PR complete.

---

## Self-Review Notes

**Spec coverage:**
- ✓ Conditional picker (hidden when stores.length === 1) → Task 5
- ✓ Register-existing toggle inside same dialog → Task 5
- ✓ Read-only mode locks toggle on → Task 5
- ✓ Edit mode disables picker, hides toggle → Task 5
- ✓ Branched submit (createConnection vs registerExisting) → Task 5
- ✓ Loading + error fallback to no-picker DATABASE → Task 5
- ✓ Backend REST endpoint + tests → Tasks 2, 3
- ✓ Client mutation hook + labels file → Task 4
- ✓ 7 component tests → Task 6
- ✓ i18n + checks → Task 7

**Out-of-scope items (confirmed not in plan):**
- Settings page (deferred per spec)
- Connection list page changes (existing credential-status badge sufficient)
- Bulk import / migration UI
- Provider-specific config UI

**Risks:**
- Task 1's discovery determines the exact controller / spec / codegen command paths. The subsequent tasks reference them by placeholder — engineer fills in concrete paths after Task 1.
- Task 6's test selectors depend on the rendered DOM, which depends on `ConnectionDialog`'s existing prop signature. The engineer reads the component and adapts the `renderDialog` helper.
- Some labels may already be extracted in messages.po (e.g., "Database" might exist for environment names). Check and reuse instead of duplicating.
- The OpenAPI spec file may be split into multiple yamls (per-platform: automation vs embedded). Confirm during Task 1 whether to update one spec or both.
