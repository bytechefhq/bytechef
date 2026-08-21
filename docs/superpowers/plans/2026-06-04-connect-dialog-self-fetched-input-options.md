# ConnectDialog Self-Fetched Workflow Input Options & Group-Member Persistence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the embedded `ConnectDialog` fetch component-defined input options itself (regular + MCP workflows, via the existing public endpoint) and persist group-member selections, so the dynamic dropdowns populate and save.

**Architecture:** A new internal hook (`useWorkflowInputOptions`) owns option fetching/caching/in-flight-dedup; `ConnectDialog` calls it and reuses its existing render plumbing. Group-member value persistence is added to the `useConnectDialog` hook beside the existing input-save logic, reusing the per-workflow `PUT` endpoints. No server changes — the options endpoint already resolves by `(integrationInstanceId, workflowUuid)` for any workflow.

**Tech Stack:** React 19, TypeScript, Vitest + @testing-library/react, the SDK package `sdks/frontend/embedded/library/react`.

**Spec:** `docs/superpowers/specs/2026-06-04-connect-dialog-self-fetched-input-options-design.md`

**Working directory for all commands:** `sdks/frontend/embedded/library/react`

---

## File Structure

| File | Responsibility |
|------|----------------|
| `src/components/connect-dialog/useWorkflowInputOptions.ts` | **New.** Fetch + cache + in-flight dedup + reset for dynamic options. |
| `src/components/connect-dialog/useWorkflowInputOptions.test.ts` | **New.** Unit tests for the hook. |
| `src/components/connect-dialog/types.ts` | Add the shared `ApiFetch` type. |
| `src/components/connect-dialog/ConnectDialog.tsx` | Drop options data props; add `apiFetch`/`integrationInstanceId`/`handleMcpWorkflowGroupInputChange`; call the hook; thread `input.name`; real group handler for the tools container; reset effect. |
| `src/components/connect-dialog/ConnectDialog.dynamic.test.tsx` | Switch from injected options props to a mocked `apiFetch`; cover regular + MCP. |
| `src/components/connect-dialog/index.tsx` | Pass transport/context + group-change handlers; widen override types; group-aware merge defaulting. |
| `src/components/connect-dialog/index.test.tsx` | Cover group-member persistence (regular + MCP). |

---

## Task 1: `useWorkflowInputOptions` hook

**Files:**
- Create: `src/components/connect-dialog/useWorkflowInputOptions.ts`
- Create: `src/components/connect-dialog/useWorkflowInputOptions.test.ts`
- Modify: `src/components/connect-dialog/types.ts`

- [ ] **Step 1: Add the `ApiFetch` type to `types.ts`**

Append to the end of `src/components/connect-dialog/types.ts` (after the `CodePayloadI` interface):

```ts
export type ApiFetch = <T>(
    endpoint: string,
    options?: {
        method?: 'GET' | 'POST' | 'PUT' | 'DELETE';
        body?: object;
        headers?: Record<string, string>;
    }
) => Promise<T>;
```

- [ ] **Step 2: Write the failing test**

Create `src/components/connect-dialog/useWorkflowInputOptions.test.ts`:

```ts
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import useWorkflowInputOptions from './useWorkflowInputOptions';
import {optionsCacheKey} from './utils';

describe('useWorkflowInputOptions', () => {
    beforeEach(() => {
        vi.spyOn(console, 'error').mockImplementation(() => {});
    });

    it('does not fetch when integrationInstanceId is missing', () => {
        const apiFetch = vi.fn().mockResolvedValue([]);

        const {result} = renderHook(() => useWorkflowInputOptions(apiFetch, undefined));

        act(() => result.current.loadOptions('wf-1', 'channel', 'channelId', {}));

        expect(apiFetch).not.toHaveBeenCalled();
    });

    it('posts the option request and stores the result under the cache key', async () => {
        const options = [
            {label: 'General', value: 'C1'},
            {label: 'Random', value: 'C2'},
        ];
        const apiFetch = vi.fn().mockResolvedValue(options);

        const {result} = renderHook(() => useWorkflowInputOptions(apiFetch, 7));

        await act(async () => {
            result.current.loadOptions('wf-1', 'channel', 'channelId', {workspace: 'W1'});
        });

        expect(apiFetch).toHaveBeenCalledWith('/api/embedded/v1/integration-instances/7/workflows/wf-1/options', {
            body: {inputName: 'channel', lookupDependsOnValues: {workspace: 'W1'}, propertyName: 'channelId'},
            method: 'POST',
        });

        const cacheKey = optionsCacheKey('wf-1', 'channel', 'channelId', {workspace: 'W1'});

        expect(result.current.optionsByKey[cacheKey]).toEqual(options);
    });

    it('does not fetch again for an already cached key', async () => {
        const apiFetch = vi.fn().mockResolvedValue([{label: 'General', value: 'C1'}]);

        const {result} = renderHook(() => useWorkflowInputOptions(apiFetch, 7));

        await act(async () => {
            result.current.loadOptions('wf-1', 'channel', 'channelId', {});
        });

        await act(async () => {
            result.current.loadOptions('wf-1', 'channel', 'channelId', {});
        });

        expect(apiFetch).toHaveBeenCalledTimes(1);
    });

    it('deduplicates concurrent in-flight requests for the same key', () => {
        let resolveFetch: (value: unknown) => void = () => {};
        const apiFetch = vi.fn().mockReturnValue(
            new Promise((resolve) => {
                resolveFetch = resolve;
            })
        );

        const {result} = renderHook(() => useWorkflowInputOptions(apiFetch, 7));

        act(() => {
            result.current.loadOptions('wf-1', 'channel', 'channelId', {});
            result.current.loadOptions('wf-1', 'channel', 'channelId', {});
        });

        expect(apiFetch).toHaveBeenCalledTimes(1);

        resolveFetch([]);
    });

    it('clears the cache on resetOptions', async () => {
        const apiFetch = vi.fn().mockResolvedValue([{label: 'General', value: 'C1'}]);

        const {result} = renderHook(() => useWorkflowInputOptions(apiFetch, 7));

        await act(async () => {
            result.current.loadOptions('wf-1', 'channel', 'channelId', {});
        });

        act(() => result.current.resetOptions());

        expect(result.current.optionsByKey).toEqual({});
    });
});
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `npx vitest run src/components/connect-dialog/useWorkflowInputOptions.test.ts`
Expected: FAIL — `Failed to resolve import "./useWorkflowInputOptions"`.

- [ ] **Step 4: Implement the hook**

Create `src/components/connect-dialog/useWorkflowInputOptions.ts`:

```ts
import {useCallback, useRef, useState} from 'react';
import {ApiFetch, OptionType} from './types';
import {optionsCacheKey} from './utils';

interface UseWorkflowInputOptionsReturnType {
    loadOptions: (
        workflowUuid: string,
        inputName: string,
        propertyName: string,
        lookupDependsOnValues: Record<string, unknown>
    ) => void;
    optionsByKey: Record<string, OptionType[]>;
    resetOptions: () => void;
}

export default function useWorkflowInputOptions(
    apiFetch: ApiFetch | undefined,
    integrationInstanceId: number | undefined
): UseWorkflowInputOptionsReturnType {
    const [optionsByKey, setOptionsByKey] = useState<Record<string, OptionType[]>>({});

    const optionsByKeyRef = useRef<Record<string, OptionType[]>>({});
    const inFlightKeysRef = useRef<Set<string>>(new Set());

    const loadOptions = useCallback(
        (
            workflowUuid: string,
            inputName: string,
            propertyName: string,
            lookupDependsOnValues: Record<string, unknown>
        ) => {
            if (!apiFetch || !integrationInstanceId) {
                return;
            }

            const cacheKey = optionsCacheKey(workflowUuid, inputName, propertyName, lookupDependsOnValues);

            if (optionsByKeyRef.current[cacheKey] !== undefined || inFlightKeysRef.current.has(cacheKey)) {
                return;
            }

            inFlightKeysRef.current.add(cacheKey);

            void apiFetch<OptionType[]>(
                `/api/embedded/v1/integration-instances/${integrationInstanceId}/workflows/${workflowUuid}/options`,
                {
                    body: {inputName, lookupDependsOnValues, propertyName},
                    method: 'POST',
                }
            )
                .then((options) => {
                    optionsByKeyRef.current = {...optionsByKeyRef.current, [cacheKey]: options ?? []};

                    setOptionsByKey(optionsByKeyRef.current);
                })
                .catch((error: unknown) => {
                    console.error('Failed to load workflow input options:', (error as Error).message);
                })
                .finally(() => {
                    inFlightKeysRef.current.delete(cacheKey);
                });
        },
        [apiFetch, integrationInstanceId]
    );

    const resetOptions = useCallback(() => {
        optionsByKeyRef.current = {};

        inFlightKeysRef.current.clear();

        setOptionsByKey({});
    }, []);

    return {
        loadOptions,
        optionsByKey,
        resetOptions,
    };
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `npx vitest run src/components/connect-dialog/useWorkflowInputOptions.test.ts`
Expected: PASS (5 tests).

- [ ] **Step 6: Commit**

```bash
git add src/components/connect-dialog/useWorkflowInputOptions.ts src/components/connect-dialog/useWorkflowInputOptions.test.ts src/components/connect-dialog/types.ts
git commit -m "732 client - Add useWorkflowInputOptions hook for ConnectDialog dynamic options"
```

---

## Task 2: ConnectDialog self-fetches options (regular + MCP), threads input.name, resets on change

**Files:**
- Modify: `src/components/connect-dialog/ConnectDialog.tsx`
- Test: `src/components/connect-dialog/ConnectDialog.dynamic.test.tsx`

- [ ] **Step 1: Rewrite the dynamic test to mock `apiFetch`**

Replace the entire contents of `src/components/connect-dialog/ConnectDialog.dynamic.test.tsx` with:

```tsx
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import ConnectDialog from './ConnectDialog';
import {MergedWorkflowType} from './types';

const baseProps = {
    apiFetch: vi.fn().mockResolvedValue([]),
    closeDialog: vi.fn(),
    handleClick: vi.fn(),
    handleMcpWorkflowGroupInputChange: vi.fn(),
    handleWorkflowToggle: vi.fn(),
    handleWorkflowInputChange: vi.fn(),
    handleWorkflowGroupInputChange: vi.fn(),
    integration: {id: 1, name: 'Test Integration'},
    integrationInstanceId: 1,
    isOpen: true,
    loading: false,
    mergedMcpTools: [],
    mergedMcpWorkflows: [],
    workflowsView: true,
};

describe('ConnectDialog dynamic inputs', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        vi.spyOn(console, 'error').mockImplementation(() => {});
    });

    it('fetches and renders options for a single-property dynamic group member', async () => {
        const workflowUuid = 'wf-1';
        const apiFetch = vi.fn().mockResolvedValue([
            {label: 'General', value: 'C1'},
            {label: 'Random', value: 'C2'},
        ]);
        const mergedWorkflows: MergedWorkflowType[] = [
            {
                enabled: true,
                inputs: [
                    {
                        componentReference: {
                            componentName: 'slack',
                            componentVersion: 1,
                            group: {
                                name: 'channel',
                                properties: [{dynamicOptions: true, label: 'Channel', name: 'channelId'}],
                            },
                            groupName: 'channel',
                        },
                        label: 'Channel',
                        name: 'channel',
                        type: 'object',
                    },
                ],
                label: 'Workflow 1',
                workflowUuid,
            },
        ];

        render(<ConnectDialog {...baseProps} apiFetch={apiFetch} mergedWorkflows={mergedWorkflows} />);

        expect(apiFetch).toHaveBeenCalledWith(
            '/api/embedded/v1/integration-instances/1/workflows/wf-1/options',
            {
                body: {inputName: 'channel', lookupDependsOnValues: {}, propertyName: 'channelId'},
                method: 'POST',
            }
        );

        expect(await screen.findByText('General')).toBeTruthy();
        expect(await screen.findByText('Random')).toBeTruthy();
    });

    it('renders the member fields of a property group and reports member changes', () => {
        const workflowUuid = 'wf-2';
        const mergedWorkflows: MergedWorkflowType[] = [
            {
                enabled: true,
                inputs: [
                    {
                        componentReference: {
                            componentName: 'googleSheets',
                            componentVersion: 1,
                            group: {
                                label: 'Spreadsheet location',
                                name: 'location',
                                properties: [
                                    {label: 'Spreadsheet', name: 'spreadsheetId'},
                                    {label: 'Sheet', name: 'sheetName'},
                                ],
                            },
                            groupName: 'location',
                        },
                        label: 'Location',
                        name: 'location',
                        type: 'object',
                    },
                ],
                label: 'Workflow Group',
                workflowUuid,
            },
        ];

        render(<ConnectDialog {...baseProps} mergedWorkflows={mergedWorkflows} />);

        expect(screen.getByText('Spreadsheet location')).toBeTruthy();
        expect(screen.getByLabelText('Spreadsheet')).toBeTruthy();
        expect(screen.getByLabelText('Sheet')).toBeTruthy();

        fireEvent.change(screen.getByLabelText('Spreadsheet'), {target: {value: 'spreadsheet-1'}});

        expect(baseProps.handleWorkflowGroupInputChange).toHaveBeenCalledWith(
            workflowUuid,
            'location',
            'spreadsheetId',
            'spreadsheet-1'
        );
    });

    it('disables a dependent member select and does not fetch until the dependency is present', () => {
        const apiFetch = vi.fn().mockResolvedValue([]);
        const mergedWorkflows: MergedWorkflowType[] = [
            {
                enabled: true,
                inputs: [
                    {
                        componentReference: {
                            componentName: 'slack',
                            componentVersion: 1,
                            group: {
                                name: 'channel',
                                properties: [
                                    {label: 'Workspace', name: 'workspace'},
                                    {
                                        dynamicOptions: true,
                                        label: 'Channel',
                                        name: 'channelId',
                                        optionsLookupDependsOn: ['workspace'],
                                    },
                                ],
                            },
                            groupName: 'channel',
                        },
                        label: 'Channel',
                        name: 'channel',
                        type: 'object',
                    },
                ],
                label: 'Dependent Workflow',
                workflowUuid: 'wf-3',
            },
        ];

        render(<ConnectDialog {...baseProps} apiFetch={apiFetch} mergedWorkflows={mergedWorkflows} />);

        const select = screen.getByLabelText('Channel') as HTMLSelectElement;

        expect(select.disabled).toBe(true);
        expect(screen.getByText('Select dependencies first')).toBeTruthy();
        expect(apiFetch).not.toHaveBeenCalled();
    });

    it('fetches options once a previously unsatisfied dependency becomes available', () => {
        const workflowUuid = 'wf-3';
        const apiFetch = vi.fn().mockResolvedValue([]);
        const buildWorkflows = (workspaceValue: string): MergedWorkflowType[] => [
            {
                enabled: true,
                inputs: [
                    {
                        componentReference: {
                            componentName: 'slack',
                            componentVersion: 1,
                            group: {
                                name: 'channel',
                                properties: [
                                    {label: 'Workspace', name: 'workspace'},
                                    {
                                        dynamicOptions: true,
                                        label: 'Channel',
                                        name: 'channelId',
                                        optionsLookupDependsOn: ['workspace'],
                                    },
                                ],
                            },
                            groupName: 'channel',
                        },
                        label: 'Channel',
                        name: 'channel',
                        type: 'object',
                        value: {workspace: workspaceValue},
                    },
                ],
                label: 'Dependent Workflow',
                workflowUuid,
            },
        ];

        const {rerender} = render(
            <ConnectDialog {...baseProps} apiFetch={apiFetch} mergedWorkflows={buildWorkflows('')} />
        );

        expect(apiFetch).not.toHaveBeenCalled();

        rerender(<ConnectDialog {...baseProps} apiFetch={apiFetch} mergedWorkflows={buildWorkflows('W1')} />);

        expect(apiFetch).toHaveBeenCalledWith(
            '/api/embedded/v1/integration-instances/1/workflows/wf-3/options',
            {
                body: {inputName: 'channel', lookupDependsOnValues: {workspace: 'W1'}, propertyName: 'channelId'},
                method: 'POST',
            }
        );
    });

    it('falls back to a plain text input when a component reference has no resolved group', () => {
        const mergedWorkflows: MergedWorkflowType[] = [
            {
                enabled: true,
                inputs: [
                    {
                        componentReference: {componentName: 'slack', componentVersion: 1, groupName: 'missing'},
                        label: 'Dangling',
                        name: 'dangling',
                        type: 'string',
                    },
                ],
                label: 'Dangling Workflow',
                workflowUuid: 'wf-4',
            },
        ];

        render(<ConnectDialog {...baseProps} mergedWorkflows={mergedWorkflows} />);

        const input = screen.getByLabelText('Dangling') as HTMLInputElement;

        expect(input.tagName).toBe('INPUT');
    });

    it('fetches MCP-workflow group-member options from the same workflows options endpoint', async () => {
        const workflowUuid = 'mcp-wf-1';
        const apiFetch = vi.fn().mockResolvedValue([{label: 'General', value: 'C1'}]);
        const mergedMcpWorkflows: MergedWorkflowType[] = [
            {
                enabled: true,
                inputs: [
                    {
                        componentReference: {
                            componentName: 'slack',
                            componentVersion: 1,
                            group: {
                                name: 'channel',
                                properties: [{dynamicOptions: true, label: 'Channel', name: 'channelId'}],
                            },
                            groupName: 'channel',
                        },
                        label: 'Channel',
                        name: 'channel',
                        type: 'object',
                    },
                ],
                label: 'MCP Workflow 1',
                workflowUuid,
            },
        ];

        render(
            <ConnectDialog
                {...baseProps}
                apiFetch={apiFetch}
                mergedWorkflows={[]}
                mergedMcpWorkflows={mergedMcpWorkflows}
            />
        );

        expect(apiFetch).toHaveBeenCalledWith(
            '/api/embedded/v1/integration-instances/1/workflows/mcp-wf-1/options',
            {
                body: {inputName: 'channel', lookupDependsOnValues: {}, propertyName: 'channelId'},
                method: 'POST',
            }
        );

        expect(await screen.findByText('General')).toBeTruthy();

        fireEvent.change(screen.getByLabelText('Channel'), {target: {value: 'C1'}});

        await waitFor(() =>
            expect(baseProps.handleMcpWorkflowGroupInputChange).toHaveBeenCalledWith(
                workflowUuid,
                'channel',
                'channelId',
                'C1'
            )
        );
    });
});

describe('optionsCacheKey', () => {
    it('produces distinct keys for distinct dependency values and a stable key for equal values', async () => {
        const {optionsCacheKey} = await import('./utils');

        const first = optionsCacheKey('wf', 'channel', 'channelId', {teamId: 'T1'});
        const second = optionsCacheKey('wf', 'channel', 'channelId', {teamId: 'T2'});
        const repeated = optionsCacheKey('wf', 'channel', 'channelId', {teamId: 'T1'});

        expect(first).not.toBe(second);
        expect(first).toBe(repeated);
    });

    it('does not collide for distinct inputs that share a property name', async () => {
        const {optionsCacheKey} = await import('./utils');

        const topLevel = optionsCacheKey('wf', 'channel', 'channelId', {});
        const groupMember = optionsCacheKey('wf', 'location', 'channelId', {});

        expect(topLevel).not.toBe(groupMember);
    });
});
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `npx vitest run src/components/connect-dialog/ConnectDialog.dynamic.test.tsx`
Expected: FAIL — `apiFetch` is not used by `ConnectDialog` yet (no calls recorded), and the MCP group handler prop is not wired.

- [ ] **Step 3: Update imports in `ConnectDialog.tsx`**

In `src/components/connect-dialog/ConnectDialog.tsx`, change the types import (currently starting `import {` at line 6) to add `ApiFetch`:

```ts
import {
    ApiFetch,
    ComponentPropertyGroupType,
    FormType,
    IntegrationType,
    MergedMcpToolType,
    MergedWorkflowType,
    OptionType,
    PropertyType,
    RegisterFormSubmitFunction,
    WorkflowInputType,
} from './types';
```

Then add this import immediately after the `import {optionsCacheKey} from './utils';` line:

```ts
import useWorkflowInputOptions from './useWorkflowInputOptions';
```

- [ ] **Step 4: Replace the `DialogProps` interface**

Replace the whole `interface DialogProps { ... }` block (currently lines 61-93) with:

```ts
interface DialogProps {
    apiFetch?: ApiFetch;
    closeDialog: () => void;
    workflowsView?: boolean;
    form?: FormType;
    handleClick: (event: React.MouseEvent<HTMLButtonElement>) => void;
    handleMcpToolToggle?: (mcpToolId: number, pressed: boolean) => void;
    handleMcpWorkflowToggle?: (workflowUuid: string, pressed: boolean) => void;
    handleMcpWorkflowInputChange?: (workflowUuid: string, inputName: string, value: string) => void;
    handleMcpWorkflowGroupInputChange?: HandleWorkflowGroupInputChangeFunction;
    handleWorkflowToggle: (workflowUuid: string, pressed: boolean) => void;
    handleWorkflowInputChange: (workflowUuid: string, inputName: string, value: string) => void;
    handleWorkflowGroupInputChange?: HandleWorkflowGroupInputChangeFunction;
    integration?: IntegrationType;
    integrationInstanceId?: number;
    isOAuth2?: boolean;
    isOpen: boolean;
    loading?: boolean;
    mergedMcpTools?: MergedMcpToolType[];
    mergedMcpWorkflows?: MergedWorkflowType[];
    mergedWorkflows: MergedWorkflowType[];
    properties?: PropertyType[];
    registerFormSubmit?: RegisterFormSubmitFunction;
}
```

- [ ] **Step 5: Replace the `ConnectDialog` destructuring + body up to the `if (!isOpen)` guard**

Replace the component signature and the start of its body (currently lines 95-136, from `const ConnectDialog = ({` through the `if (!isOpen) { return null; }` block) with:

```tsx
const ConnectDialog = ({
    apiFetch,
    closeDialog,
    workflowsView = false,
    form,
    handleClick,
    handleMcpToolToggle = () => {},
    handleMcpWorkflowToggle = () => {},
    handleMcpWorkflowInputChange = () => {},
    handleMcpWorkflowGroupInputChange = () => {},
    handleWorkflowToggle,
    handleWorkflowInputChange,
    handleWorkflowGroupInputChange = () => {},
    integration,
    integrationInstanceId,
    isOAuth2 = false,
    isOpen,
    loading = false,
    mergedMcpTools = [],
    mergedMcpWorkflows = [],
    mergedWorkflows,
    properties,
    registerFormSubmit,
}: DialogProps) => {
    const {loadOptions, optionsByKey, resetOptions} = useWorkflowInputOptions(apiFetch, integrationInstanceId);

    useEffect(() => {
        const handleKeyDown = (event: KeyboardEvent) => {
            if (event.key === 'Escape') {
                closeDialog();
            }
        };

        if (isOpen) {
            window.addEventListener('keydown', handleKeyDown);
        }

        return () => {
            window.removeEventListener('keydown', handleKeyDown);
        };
    }, [isOpen, closeDialog]);

    useEffect(() => {
        resetOptions();
    }, [integration?.id, integrationInstanceId, resetOptions]);

    if (!isOpen) {
        return null;
    }
```

- [ ] **Step 6: Pass the hook outputs + MCP group handler into `DialogContent`**

In the `ConnectDialog` return, replace the `<DialogContent ... />` element (currently lines 148-166) with:

```tsx
                    <DialogContent
                        closeDialog={closeDialog}
                        workflowsView={workflowsView}
                        form={form}
                        handleMcpToolToggle={handleMcpToolToggle}
                        handleMcpWorkflowToggle={handleMcpWorkflowToggle}
                        handleMcpWorkflowInputChange={handleMcpWorkflowInputChange}
                        handleMcpWorkflowGroupInputChange={handleMcpWorkflowGroupInputChange}
                        handleWorkflowToggle={handleWorkflowToggle}
                        handleWorkflowInputChange={handleWorkflowInputChange}
                        handleWorkflowGroupInputChange={handleWorkflowGroupInputChange}
                        integration={integration}
                        loadWorkflowInputOptions={loadOptions}
                        mergedMcpTools={mergedMcpTools}
                        mergedMcpWorkflows={mergedMcpWorkflows}
                        mergedWorkflows={mergedWorkflows}
                        properties={properties}
                        registerFormSubmit={registerFormSubmit}
                        workflowInputOptions={optionsByKey}
                    />
```

- [ ] **Step 7: Add `handleMcpWorkflowGroupInputChange` to `DialogContentProps` and `DialogContent`, and pass it to the tools container**

In `interface DialogContentProps` (currently lines 380-408), add this line after the `handleMcpWorkflowInputChange` line:

```ts
    handleMcpWorkflowGroupInputChange?: HandleWorkflowGroupInputChangeFunction;
```

In the `DialogContent` destructuring (currently lines 412-429), add `handleMcpWorkflowGroupInputChange = () => {},` after `handleMcpWorkflowInputChange,`.

Then replace the `<DialogToolsContainer ... />` element (currently lines 505-513) with:

```tsx
                <DialogToolsContainer
                    handleMcpToolToggle={handleMcpToolToggle}
                    handleMcpWorkflowToggle={handleMcpWorkflowToggle}
                    handleMcpWorkflowInputChange={handleMcpWorkflowInputChange}
                    handleWorkflowGroupInputChange={handleMcpWorkflowGroupInputChange}
                    loadWorkflowInputOptions={loadWorkflowInputOptions}
                    mergedMcpTools={mergedMcpTools}
                    mergedMcpWorkflows={mergedMcpWorkflows}
                    workflowInputOptions={workflowInputOptions}
                />
```

- [ ] **Step 8: Give `DialogToolsContainer` a real group-change handler**

In `interface DialogToolsContainerProps` (currently lines 283-291), add after the `handleMcpWorkflowInputChange` line:

```ts
    handleWorkflowGroupInputChange: HandleWorkflowGroupInputChangeFunction;
```

In the `DialogToolsContainer` destructuring (currently lines 293-301), add `handleWorkflowGroupInputChange,` after `handleMcpWorkflowInputChange,`.

In `DialogToolsContainer`, the MCP `renderWorkflowInput(...)` call (currently lines 357-364) passes `handleWorkflowGroupInputChange: () => {}`. Replace that call's options object with:

```tsx
                                                        {renderWorkflowInput({
                                                            handleInputChange: handleMcpWorkflowInputChange,
                                                            handleWorkflowGroupInputChange,
                                                            input,
                                                            loadWorkflowInputOptions,
                                                            workflowInputOptions,
                                                            workflowUuid,
                                                        })}
```

- [ ] **Step 9: Thread `input.name` into `DialogGroupField`**

In `interface DialogGroupFieldProps` (currently lines 627-634), add after `handleWorkflowGroupInputChange: HandleWorkflowGroupInputChangeFunction;`:

```ts
    inputName: string;
```

In the `DialogGroupField` destructuring (currently lines 636-643), add `inputName,` after `handleWorkflowGroupInputChange,`.

Inside `DialogGroupField`, replace the three uses of `group.name` that identify the workflow input — in the dynamic-member branch (the `loadWorkflowInputOptions(...)`, `optionsCacheKey(...)`, and `onChange` calls, currently lines 656-669) and in the plain-member branch (`handleWorkflowGroupInputChange(...)`, currently line 683) — so they use `inputName`. The full `group.properties?.map((member) => { ... })` body becomes:

```tsx
        {group.properties?.map((member) => {
            if (member.dynamicOptions) {
                const dependencyValues = collectDependencyValues(member.optionsLookupDependsOn, memberValues);

                return (
                    <DialogDynamicSelectField
                        key={member.name}
                        dependencyValues={dependencyValues}
                        label={member.label ?? member.name}
                        loadOptions={(dependencies) =>
                            loadWorkflowInputOptions(workflowUuid, inputName, member.name, dependencies)
                        }
                        name={`${group.name}.${member.name}`}
                        onChange={(value) =>
                            handleWorkflowGroupInputChange(workflowUuid, inputName, member.name, value)
                        }
                        options={
                            workflowInputOptions[
                                optionsCacheKey(
                                    workflowUuid,
                                    inputName,
                                    member.name,
                                    collectDependencyValues(member.optionsLookupDependsOn, memberValues)
                                )
                            ]
                        }
                        required={member.required}
                        value={memberValues[member.name] as string | undefined}
                    />
                );
            }

            return (
                <DialogInputField
                    key={member.name}
                    onChange={(event) =>
                        handleWorkflowGroupInputChange(workflowUuid, inputName, member.name, event.target.value)
                    }
                    label={member.label ?? member.name}
                    name={`${group.name}.${member.name}`}
                    options={member.options?.map((option) => option.value)}
                    required={member.required}
                    field={{value: memberValues[member.name] as string | undefined}}
                />
            );
        })}
```

(Note: the `name={...}` DOM id keeps `group.name` so the rendered field id is unchanged; only the identity passed to the handler/loader/cache-key uses `inputName`.)

- [ ] **Step 10: Pass `inputName={input.name}` from `renderWorkflowInput`**

In `renderWorkflowInput` (currently lines 718-752), replace the `<DialogGroupField ... />` element (currently lines 732-739) with:

```tsx
            <DialogGroupField
                group={group}
                handleWorkflowGroupInputChange={handleWorkflowGroupInputChange}
                inputName={input.name}
                loadWorkflowInputOptions={loadWorkflowInputOptions}
                memberValues={memberValues}
                workflowInputOptions={workflowInputOptions}
                workflowUuid={workflowUuid}
            />
```

- [ ] **Step 11: Run the dynamic test to verify it passes**

Run: `npx vitest run src/components/connect-dialog/ConnectDialog.dynamic.test.tsx`
Expected: PASS (8 tests).

- [ ] **Step 12: Run the full connect-dialog test folder to catch regressions**

Run: `npx vitest run src/components/connect-dialog`
Expected: PASS. If `ConnectDialog.test.tsx` references removed props, it will fail — it should not (it does not use options props), but if it does, remove only the now-deleted props from its usage.

- [ ] **Step 13: Typecheck**

Run: `npx tsc --noEmit`
Expected: no errors. (`index.tsx` still compiles: it never passed the removed props, and the new props are optional.)

- [ ] **Step 14: Commit**

```bash
git add src/components/connect-dialog/ConnectDialog.tsx src/components/connect-dialog/ConnectDialog.dynamic.test.tsx
git commit -m "732 client - ConnectDialog self-fetches dynamic input options for regular and MCP workflows"
```

---

## Task 3: Regular group-member persistence in `useConnectDialog`

**Files:**
- Modify: `src/components/connect-dialog/index.tsx`
- Test: `src/components/connect-dialog/index.test.tsx`

- [ ] **Step 1: Write the failing test**

Append this `describe` block to `src/components/connect-dialog/index.test.tsx` (after the existing blocks):

```tsx
describe('useConnectDialog - group-member persistence', () => {
    beforeEach(() => {
        vi.useFakeTimers();
        vi.spyOn(console, 'error').mockImplementation(() => {});
    });

    afterEach(() => {
        vi.runOnlyPendingTimers();
        vi.useRealTimers();
        vi.restoreAllMocks();
    });

    it('passes apiFetch, integrationInstanceId and a working group handler that PUTs a nested object', async () => {
        const workflowUuid = '11111111-1111-1111-1111-111111111111';

        global.fetch = vi.fn().mockResolvedValue({
            ok: true,
            headers: {get: () => '0'},
            json: vi.fn().mockResolvedValue({
                name: 'Test Integration',
                workflows: [{label: 'Workflow 1', workflowUuid}],
                integrationInstances: [{id: 55, workflows: [{enabled: true, workflowUuid}]}],
            }),
        });

        const renderMock = vi.fn();

        vi.mocked(createRoot).mockReturnValue({
            render: renderMock,
            unmount: vi.fn(),
        });

        const {result} = renderHook(() =>
            useConnectDialog({
                baseUrl: 'https://api.example.com',
                environment: 'DEVELOPMENT',
                integrationId: '1234',
                integrationInstanceId: '55',
                jwtToken: 'ey',
            })
        );

        await act(async () => result.current.openDialog());

        const props = renderMock.mock.calls[renderMock.mock.calls.length - 1][0].props;

        expect(props.integrationInstanceId).toBe(55);
        expect(typeof props.apiFetch).toBe('function');
        expect(typeof props.handleWorkflowGroupInputChange).toBe('function');

        vi.mocked(global.fetch).mockClear();

        act(() => props.handleWorkflowGroupInputChange(workflowUuid, 'channel', 'channelId', 'C1'));

        await act(async () => {
            vi.advanceTimersByTime(600);
        });

        expect(global.fetch).toHaveBeenCalledWith(
            `https://api.example.com/api/embedded/v1/integration-instances/55/workflows/${workflowUuid}`,
            expect.objectContaining({
                method: 'PUT',
                body: JSON.stringify({inputs: {channel: {channelId: 'C1'}}}),
            })
        );
    });
});
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `npx vitest run src/components/connect-dialog/index.test.tsx -t "group-member persistence"`
Expected: FAIL — `props.handleWorkflowGroupInputChange` is `undefined` (not yet passed) / `props.apiFetch` undefined.

- [ ] **Step 3: Widen the `inputOverrides` state type**

In `src/components/connect-dialog/index.tsx`, replace (currently line 123):

```ts
    const [inputOverrides, setInputOverrides] = useState<Record<string, Record<string, string>>>({});
```

with:

```ts
    const [inputOverrides, setInputOverrides] = useState<
        Record<string, Record<string, string | Record<string, string>>>
    >({});
```

- [ ] **Step 4: Make `mergedWorkflows` default group inputs to an object**

Replace the `inputs:` mapping inside the `mergedWorkflows` memo (currently lines 168-176) with:

```ts
                inputs: Array.isArray(workflow.inputs)
                    ? workflow.inputs.map((input: WorkflowInputType) => {
                          const isGroupInput = input.componentReference?.group != null;

                          const overrideValue = workflowInputOverrides?.[input.name];
                          const serverValue = (instanceWorkflow?.inputs as Record<string, unknown> | undefined)?.[
                              input.name
                          ];

                          return {
                              ...input,
                              value: overrideValue ?? serverValue ?? (isGroupInput ? {} : ''),
                          };
                      })
                    : [],
```

- [ ] **Step 5: Extract `scheduleWorkflowInputsSave` and add the group handler**

Replace the entire `handleWorkflowInputChange` `useCallback` block (currently lines 677-739) with:

```ts
    const scheduleWorkflowInputsSave = useCallback(
        (workflowUuid: string) => {
            if (!currentIntegrationInstanceIdRef.current || isNaN(currentIntegrationInstanceIdRef.current)) {
                console.error('Invalid integration instance ID');

                return;
            }

            const debouncedFetchKey = workflowUuid;

            if (!debouncedFetchesRef.current[debouncedFetchKey]) {
                debouncedFetchesRef.current[debouncedFetchKey] = debounce(() => {
                    const instanceId = currentIntegrationInstanceIdRef.current;

                    if (!instanceId) {
                        return;
                    }

                    const currentIntegration = integrationRef.current;
                    const currentInstance = currentIntegration?.integrationInstances?.find(
                        (instance: IntegrationInstanceType) => instance.id === instanceId
                    );
                    const serverInputs =
                        (currentInstance?.workflows?.find(
                            (workflow: IntegrationInstanceWorkflowType) => workflow.workflowUuid === workflowUuid
                        )?.inputs as Record<string, unknown> | undefined) || {};

                    const mergedInputs = {
                        ...serverInputs,
                        ...inputOverridesRef.current[workflowUuid],
                    };

                    void fetch(`/api/embedded/v1/integration-instances/${instanceId}/workflows/${workflowUuid}`, {
                        body: {
                            inputs: mergedInputs,
                        },
                        method: 'PUT',
                    }).catch((error) => console.error('Failed to save workflow inputs:', error));
                }, 600);
            }

            debouncedFetchesRef.current[debouncedFetchKey]();
        },
        [fetch]
    );

    const handleWorkflowInputChange = useCallback(
        (workflowUuid: string, inputName: string, value: string) => {
            setInputOverrides((previous) => {
                const updated = {
                    ...previous,
                    [workflowUuid]: {
                        ...previous[workflowUuid],
                        [inputName]: value,
                    },
                };

                inputOverridesRef.current = updated;

                return updated;
            });

            scheduleWorkflowInputsSave(workflowUuid);
        },
        [scheduleWorkflowInputsSave]
    );

    const handleWorkflowGroupInputChange = useCallback(
        (workflowUuid: string, inputName: string, memberName: string, value: string) => {
            setInputOverrides((previous) => {
                const existingGroupValue =
                    (previous[workflowUuid]?.[inputName] as Record<string, string> | undefined) ?? {};

                const updated = {
                    ...previous,
                    [workflowUuid]: {
                        ...previous[workflowUuid],
                        [inputName]: {
                            ...existingGroupValue,
                            [memberName]: value,
                        },
                    },
                };

                inputOverridesRef.current = updated;

                return updated;
            });

            scheduleWorkflowInputsSave(workflowUuid);
        },
        [scheduleWorkflowInputsSave]
    );
```

- [ ] **Step 6: Pass the new props to `<ConnectDialog>` and update effect deps**

In the `rootRef.current.render(<ConnectDialog ... />)` call (currently lines 854-875), add these three props (keep the rest as-is): `apiFetch={fetch}`, `handleWorkflowGroupInputChange={handleWorkflowGroupInputChange}`, and `integrationInstanceId={currentIntegrationInstanceId}`. The element becomes:

```tsx
            <ConnectDialog
                apiFetch={fetch}
                closeDialog={closeDialog}
                form={form}
                handleClick={handleClick}
                handleMcpToolToggle={handleMcpToolToggle}
                handleMcpWorkflowToggle={handleMcpWorkflowToggle}
                handleMcpWorkflowInputChange={handleMcpWorkflowInputChange}
                handleWorkflowToggle={handleWorkflowToggle}
                handleWorkflowInputChange={handleWorkflowInputChange}
                handleWorkflowGroupInputChange={handleWorkflowGroupInputChange}
                integration={integration}
                integrationInstanceId={currentIntegrationInstanceId}
                isOAuth2={isOAuth2}
                isOpen={isOpen}
                loading={isLoading}
                mergedMcpTools={mergedMcpTools}
                mergedMcpWorkflows={mergedMcpWorkflows}
                mergedWorkflows={mergedWorkflows}
                properties={integration?.connectionConfig?.inputs}
                registerFormSubmit={registerFormSubmit}
                workflowsView={workflowsView}
            />
```

In the dependency array of that `useEffect` (currently lines 876-895), add `fetch,` and `handleWorkflowGroupInputChange,` (alongside the existing entries).

- [ ] **Step 7: Run the test to verify it passes**

Run: `npx vitest run src/components/connect-dialog/index.test.tsx -t "group-member persistence"`
Expected: PASS.

- [ ] **Step 8: Run the whole connect-dialog folder + typecheck**

Run: `npx vitest run src/components/connect-dialog && npx tsc --noEmit`
Expected: PASS, no type errors.

- [ ] **Step 9: Commit**

```bash
git add src/components/connect-dialog/index.tsx src/components/connect-dialog/index.test.tsx
git commit -m "732 client - Persist regular workflow group-member input values from ConnectDialog"
```

---

## Task 4: MCP group-member persistence in `useConnectDialog`

**Files:**
- Modify: `src/components/connect-dialog/index.tsx`
- Test: `src/components/connect-dialog/index.test.tsx`

- [ ] **Step 1: Write the failing test**

Append this test to the `useConnectDialog - group-member persistence` describe block in `index.test.tsx` (inside the same block as Task 3's test):

```tsx
    it('passes a working MCP group handler that PUTs to the mcp-workflows endpoint', async () => {
        const workflowUuid = '22222222-2222-2222-2222-222222222222';

        global.fetch = vi.fn().mockResolvedValue({
            ok: true,
            headers: {get: () => '0'},
            json: vi.fn().mockResolvedValue({
                name: 'Test Integration',
                mcpWorkflows: [{label: 'MCP Workflow 1', workflowUuid}],
                integrationInstances: [{id: 55, mcpWorkflows: [{enabled: true, workflowUuid}], workflows: []}],
            }),
        });

        const renderMock = vi.fn();

        vi.mocked(createRoot).mockReturnValue({
            render: renderMock,
            unmount: vi.fn(),
        });

        const {result} = renderHook(() =>
            useConnectDialog({
                baseUrl: 'https://api.example.com',
                environment: 'DEVELOPMENT',
                integrationId: '1234',
                integrationInstanceId: '55',
                jwtToken: 'ey',
            })
        );

        await act(async () => result.current.openDialog());

        const props = renderMock.mock.calls[renderMock.mock.calls.length - 1][0].props;

        expect(typeof props.handleMcpWorkflowGroupInputChange).toBe('function');

        vi.mocked(global.fetch).mockClear();

        act(() => props.handleMcpWorkflowGroupInputChange(workflowUuid, 'channel', 'channelId', 'C1'));

        await act(async () => {
            vi.advanceTimersByTime(600);
        });

        expect(global.fetch).toHaveBeenCalledWith(
            `https://api.example.com/api/embedded/v1/integration-instances/55/mcp-workflows/${workflowUuid}`,
            expect.objectContaining({
                method: 'PUT',
                body: JSON.stringify({inputs: {channel: {channelId: 'C1'}}}),
            })
        );
    });
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `npx vitest run src/components/connect-dialog/index.test.tsx -t "MCP group handler"`
Expected: FAIL — `props.handleMcpWorkflowGroupInputChange` is `undefined`.

- [ ] **Step 3: Widen the `mcpWorkflowInputOverrides` state type**

Replace (currently lines 128-130):

```ts
    const [mcpWorkflowInputOverrides, setMcpWorkflowInputOverrides] = useState<
        Record<string, Record<string, string>>
    >({});
```

with:

```ts
    const [mcpWorkflowInputOverrides, setMcpWorkflowInputOverrides] = useState<
        Record<string, Record<string, string | Record<string, string>>>
    >({});
```

- [ ] **Step 4: Make `mergedMcpWorkflows` default group inputs to an object**

Replace the `inputs:` mapping inside the `mergedMcpWorkflows` memo (currently lines 230-238) with:

```ts
                inputs: Array.isArray(workflow.inputs)
                    ? workflow.inputs.map((input: WorkflowInputType) => {
                          const isGroupInput = input.componentReference?.group != null;

                          const overrideValue = workflowInputOverrides?.[input.name];
                          const serverValue = (instanceWorkflow?.inputs as Record<string, unknown> | undefined)?.[
                              input.name
                          ];

                          return {
                              ...input,
                              value: overrideValue ?? serverValue ?? (isGroupInput ? {} : ''),
                          };
                      })
                    : [],
```

- [ ] **Step 5: Extract `scheduleMcpWorkflowInputsSave` and add the MCP group handler**

Replace the entire `handleMcpWorkflowInputChange` `useCallback` block (currently lines 741-803) with:

```ts
    const scheduleMcpWorkflowInputsSave = useCallback(
        (workflowUuid: string) => {
            if (!currentIntegrationInstanceIdRef.current || isNaN(currentIntegrationInstanceIdRef.current)) {
                console.error('Invalid integration instance ID');

                return;
            }

            const debouncedFetchKey = `mcp-${workflowUuid}`;

            if (!debouncedFetchesRef.current[debouncedFetchKey]) {
                debouncedFetchesRef.current[debouncedFetchKey] = debounce(() => {
                    const instanceId = currentIntegrationInstanceIdRef.current;

                    if (!instanceId) {
                        return;
                    }

                    const currentIntegration = integrationRef.current;
                    const currentInstance = currentIntegration?.integrationInstances?.find(
                        (instance: IntegrationInstanceType) => instance.id === instanceId
                    );
                    const serverInputs =
                        (currentInstance?.mcpWorkflows?.find(
                            (workflow: IntegrationInstanceWorkflowType) => workflow.workflowUuid === workflowUuid
                        )?.inputs as Record<string, unknown> | undefined) || {};

                    const mergedInputs = {
                        ...serverInputs,
                        ...mcpWorkflowInputOverridesRef.current[workflowUuid],
                    };

                    void fetch(
                        `/api/embedded/v1/integration-instances/${instanceId}/mcp-workflows/${workflowUuid}`,
                        {
                            body: {
                                inputs: mergedInputs,
                            },
                            method: 'PUT',
                        }
                    ).catch((error) => console.error('Failed to save MCP workflow inputs:', error));
                }, 600);
            }

            debouncedFetchesRef.current[debouncedFetchKey]();
        },
        [fetch]
    );

    const handleMcpWorkflowInputChange = useCallback(
        (workflowUuid: string, inputName: string, value: string) => {
            setMcpWorkflowInputOverrides((previous) => {
                const updated = {
                    ...previous,
                    [workflowUuid]: {
                        ...previous[workflowUuid],
                        [inputName]: value,
                    },
                };

                mcpWorkflowInputOverridesRef.current = updated;

                return updated;
            });

            scheduleMcpWorkflowInputsSave(workflowUuid);
        },
        [scheduleMcpWorkflowInputsSave]
    );

    const handleMcpWorkflowGroupInputChange = useCallback(
        (workflowUuid: string, inputName: string, memberName: string, value: string) => {
            setMcpWorkflowInputOverrides((previous) => {
                const existingGroupValue =
                    (previous[workflowUuid]?.[inputName] as Record<string, string> | undefined) ?? {};

                const updated = {
                    ...previous,
                    [workflowUuid]: {
                        ...previous[workflowUuid],
                        [inputName]: {
                            ...existingGroupValue,
                            [memberName]: value,
                        },
                    },
                };

                mcpWorkflowInputOverridesRef.current = updated;

                return updated;
            });

            scheduleMcpWorkflowInputsSave(workflowUuid);
        },
        [scheduleMcpWorkflowInputsSave]
    );
```

- [ ] **Step 6: Pass the MCP group handler to `<ConnectDialog>` and update effect deps**

In the `<ConnectDialog ... />` element, add `handleMcpWorkflowGroupInputChange={handleMcpWorkflowGroupInputChange}` immediately after the `handleMcpWorkflowInputChange={handleMcpWorkflowInputChange}` line.

In that `useEffect`'s dependency array, add `handleMcpWorkflowGroupInputChange,`.

- [ ] **Step 7: Run the test to verify it passes**

Run: `npx vitest run src/components/connect-dialog/index.test.tsx -t "MCP group handler"`
Expected: PASS.

- [ ] **Step 8: Run the whole connect-dialog folder + typecheck**

Run: `npx vitest run src/components/connect-dialog && npx tsc --noEmit`
Expected: PASS, no type errors.

- [ ] **Step 9: Commit**

```bash
git add src/components/connect-dialog/index.tsx src/components/connect-dialog/index.test.tsx
git commit -m "732 client - Persist MCP workflow group-member input values from ConnectDialog"
```

---

## Task 5: Final verification

**Files:** none (verification only)

- [ ] **Step 1: Lint**

Run: `npm run lint`
Expected: no errors. Fix any import-sort / sort-keys issues manually (ESLint `--fix` does not auto-fix `sort-keys`).

- [ ] **Step 2: Typecheck**

Run: `npx tsc --noEmit`
Expected: no errors.

- [ ] **Step 3: Full test run**

Run: `npx vitest run`
Expected: all tests PASS.

- [ ] **Step 4: Format**

Run: `npm run format:fix`
Expected: files formatted; re-run `npm run lint` if anything changed.

- [ ] **Step 5: Commit any formatting/lint fixups (if any)**

```bash
git add -A
git commit -m "732 client - Lint and format ConnectDialog dynamic options changes"
```

---

## Notes for the implementer

- **Server read-path assumption (from the spec):** a component-defined input's value is persisted as a nested object keyed by member name (e.g. `{channel: {channelId: "C1"}}`). The client types and tests model it this way. Before considering the feature done, manually confirm the server reads this shape (trace `updateIntegrationInstanceWorkflow` storage and the workflow execution read path). If the server expects a different shape, stop and revise the spec — do not silently reshape on the client.
- **Why `inputName` not `group.name`:** the options endpoint and the update body key by the workflow input name (`input.name`). They usually equal the group name but are not guaranteed to; using `input.name` is the correct contract.
- **Reset on integration change:** `ConnectDialog` is never unmounted on close (it returns `null`), so the `resetOptions()` effect keyed on `integration?.id`/`integrationInstanceId` is what prevents one integration showing another's cached options.
- **Out of scope:** typeahead `searchText` (the endpoint supports it; the dialog does not send it yet).
