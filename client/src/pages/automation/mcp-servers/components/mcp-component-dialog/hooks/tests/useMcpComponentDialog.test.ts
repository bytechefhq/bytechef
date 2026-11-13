import {McpComponent} from '@/shared/middleware/graphql';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {act, renderHook} from '@testing-library/react';
import {useForm} from 'react-hook-form';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useMcpComponentDialog from '../useMcpComponentDialog';

const hoisted = vi.hoisted(() => ({
    authorities: undefined as undefined | string[],
    createMutate: vi.fn(),
    createOnSuccess: undefined as undefined | (() => void),
    existingTools: [] as Array<{name: string; parameters: Record<string, unknown> | null}>,
    invalidateQueries: vi.fn(),
    seededValuesByTool: {} as Record<string, Record<string, string>>,
    seedingParams: [] as Array<{
        componentName: string;
        componentVersion: number;
        connectionRequired?: boolean;
        newToolNames: string[];
    }>,
    updateMutate: vi.fn(),
    updateOnSuccess: undefined as undefined | (() => void),
    waitForSeeding: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAuthoritiesQuery: () => ({
        data: hoisted.authorities ? {authorities: hoisted.authorities} : undefined,
        isLoading: false,
    }),
    useCreateMcpComponentWithToolsMutation: ({onSuccess}: {onSuccess: () => void}) => {
        hoisted.createOnSuccess = onSuccess;

        return {isPending: false, mutate: hoisted.createMutate};
    },
    useMcpToolsByComponentIdQuery: () => ({
        data: hoisted.existingTools.length ? {mcpToolsByComponentId: hoisted.existingTools} : undefined,
    }),
    useUpdateMcpComponentWithToolsMutation: ({onSuccess}: {onSuccess: () => void}) => {
        hoisted.updateOnSuccess = onSuccess;

        return {isPending: false, mutate: hoisted.updateMutate};
    },
}));

vi.mock('@/pages/platform/mcp-servers/hooks/useMcpToolSeeding', () => ({
    default: (params: {
        componentName: string;
        componentVersion: number;
        connectionRequired?: boolean;
        newToolNames: string[];
    }) => {
        hoisted.seedingParams.push(params);

        return {aiValuesByTool: {}, getFailedSeedKeys: () => [], waitForSeeding: hoisted.waitForSeeding};
    },
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    useGetComponentDefinitionQuery: () => ({data: {connectionRequired: true}}),
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: () => ({invalidateQueries: hoisted.invalidateQueries}),
}));

const existingMcpComponent = {
    componentName: 'slack',
    componentVersion: 1,
    id: '5',
    requiredAuthorities: ['ROLE_EDITOR'],
    title: 'Slack',
    version: 3,
} as unknown as McpComponent;

const selectableComponent = {name: 'github', title: 'GitHub', version: 2} as ComponentDefinitionBasic;

const runMutationSuccess = (mutate: ReturnType<typeof vi.fn>) => {
    const [, options] = mutate.mock.calls[mutate.mock.calls.length - 1] as [unknown, {onSuccess: () => void}];

    act(() => options.onSuccess());
};

const invalidatedKeys = () => hoisted.invalidateQueries.mock.calls.map(([argument]) => argument.queryKey[0]);

describe('useMcpComponentDialog', () => {
    beforeEach(() => {
        hoisted.authorities = undefined;
        hoisted.createMutate.mockReset();
        hoisted.existingTools = [];
        hoisted.invalidateQueries.mockClear();
        hoisted.seededValuesByTool = {};
        hoisted.seedingParams = [];
        hoisted.updateMutate.mockReset();
        hoisted.waitForSeeding.mockReset();
        hoisted.waitForSeeding.mockImplementation(() => Promise.resolve(hoisted.seededValuesByTool));
    });

    // The server list is served by useWorkspaceMcpServersQuery and the per-server component count is read off that
    // payload, so a save that only invalidated 'mcpServers' left the count stale until a page reload.
    it('invalidates the workspace servers query after creating a component', () => {
        renderHook(() => useMcpComponentDialog({mcpServerId: '1', open: true}));

        hoisted.createOnSuccess?.();

        expect(invalidatedKeys()).toContain('workspaceMcpServers');
    });

    it('invalidates the workspace servers query after updating a component', () => {
        renderHook(() => useMcpComponentDialog({mcpServerId: '1', open: true}));

        hoisted.updateOnSuccess?.();

        expect(invalidatedKeys()).toContain('workspaceMcpServers');
    });

    describe('tool parameters', () => {
        const createContactTool = {componentName: 'slack', componentVersion: 1, name: 'createContact'};
        const postMessageTool = {componentName: 'slack', componentVersion: 1, name: 'postMessage'};

        const savedFirstName = "=fromAi('firstName', 'STRING', {'required': true})";
        const seededText = "=fromAi('text', 'STRING')";

        const renderEditDialog = () =>
            renderHook(() => useMcpComponentDialog({mcpComponent: existingMcpComponent, mcpServerId: '1', open: true}));

        const savedTools = (mutate: ReturnType<typeof vi.fn>) =>
            (mutate.mock.calls[mutate.mock.calls.length - 1][0] as {input: {tools: unknown}}).input.tools;

        beforeEach(() => {
            hoisted.existingTools = [{name: 'createContact', parameters: {firstName: savedFirstName}}];
        });

        it('keeps saved parameters of kept tools on update', async () => {
            const {result} = renderEditDialog();

            act(() => result.current.setSelectedTools([createContactTool]));

            await act(async () => {
                await result.current.handleSave();
            });

            expect(savedTools(hoisted.updateMutate)).toEqual([
                {name: 'createContact', parameters: {firstName: savedFirstName}},
            ]);
        });

        it('seeds new tools with ai values', async () => {
            hoisted.seededValuesByTool = {postMessage: {text: seededText}};

            const {result} = renderEditDialog();

            act(() => result.current.setSelectedTools([createContactTool, postMessageTool]));

            await act(async () => {
                await result.current.handleSave();
            });

            expect(hoisted.seedingParams[hoisted.seedingParams.length - 1]).toEqual(
                expect.objectContaining({
                    componentName: 'slack',
                    componentVersion: 1,
                    connectionRequired: true,
                    newToolNames: ['postMessage'],
                })
            );
            expect(savedTools(hoisted.updateMutate)).toEqual([
                {name: 'createContact', parameters: {firstName: savedFirstName}},
                {name: 'postMessage', parameters: {text: seededText}},
            ]);
        });

        it('does not re-seed existing tools', async () => {
            hoisted.seededValuesByTool = {createContact: {lastName: "=fromAi('lastName', 'STRING')"}};

            const {result} = renderEditDialog();

            act(() => result.current.setSelectedTools([createContactTool]));

            await act(async () => {
                await result.current.handleSave();
            });

            expect(hoisted.seedingParams[hoisted.seedingParams.length - 1].newToolNames).toEqual([]);
            expect(savedTools(hoisted.updateMutate)).toEqual([
                {name: 'createContact', parameters: {firstName: savedFirstName}},
            ]);
        });

        it('saves seeded ai values when save is clicked before the form renders', async () => {
            let resolveSeeding: (values: Record<string, Record<string, string>>) => void = () => {};

            hoisted.waitForSeeding.mockImplementation(
                () =>
                    new Promise((resolve) => {
                        resolveSeeding = resolve;
                    })
            );

            const {result} = renderHook(() => useMcpComponentDialog({mcpServerId: '1', open: true}));

            act(() => result.current.handleComponentSelect(selectableComponent));

            act(() => result.current.setSelectedTools([postMessageTool]));

            let savePromise: Promise<void> = Promise.resolve();

            act(() => {
                savePromise = result.current.handleSave();
            });

            expect(result.current.isSavePending).toBe(true);
            expect(hoisted.createMutate).not.toHaveBeenCalled();

            await act(async () => {
                resolveSeeding({postMessage: {text: seededText}});

                await savePromise;
            });

            expect(result.current.isSavePending).toBe(false);
            expect(savedTools(hoisted.createMutate)).toEqual([{name: 'postMessage', parameters: {text: seededText}}]);
        });

        it('merges the edited fields of an existing tool over its saved parameters', async () => {
            const {result: formResult} = renderHook(() =>
                useForm<Record<string, unknown>>({defaultValues: {firstName: '', lastName: ''}})
            );

            const {result} = renderEditDialog();

            act(() => result.current.setSelectedTools([createContactTool]));

            act(() => {
                result.current.registerToolForm('createContact', {
                    form: formResult.current,
                    initialValues: formResult.current.getValues(),
                });

                formResult.current.register('lastName');

                formResult.current.setValue('lastName', 'Smith', {shouldDirty: true});
            });

            await act(async () => {
                await result.current.handleSave();
            });

            expect(savedTools(hoisted.updateMutate)).toEqual([
                {name: 'createContact', parameters: {firstName: savedFirstName, lastName: 'Smith'}},
            ]);
        });

        it('saves an existing tool whose array item was deleted', async () => {
            hoisted.existingTools = [
                {name: 'createContact', parameters: {firstName: savedFirstName, tags: ['a', 'b']}},
            ];

            const {result: formResult} = renderHook(() =>
                useForm<Record<string, unknown>>({defaultValues: {firstName: savedFirstName, tags: ['a', 'b']}})
            );

            const {result} = renderEditDialog();

            act(() => result.current.setSelectedTools([createContactTool]));

            act(() => {
                result.current.registerToolForm('createContact', {
                    form: formResult.current,
                    initialValues: formResult.current.getValues(),
                });

                formResult.current.setValue('tags', ['a']);
            });

            await act(async () => {
                await result.current.handleSave();
            });

            expect(savedTools(hoisted.updateMutate)).toEqual([
                {name: 'createContact', parameters: {firstName: savedFirstName, tags: ['a']}},
            ]);
        });

        it('does not re-seed a field the user cleared', async () => {
            hoisted.seededValuesByTool = {postMessage: {text: seededText}};

            const {result: formResult} = renderHook(() =>
                useForm<Record<string, unknown>>({defaultValues: {text: seededText}})
            );

            const {result} = renderEditDialog();

            act(() => result.current.setSelectedTools([postMessageTool]));

            act(() => {
                result.current.registerToolForm('postMessage', {
                    form: formResult.current,
                    initialValues: formResult.current.getValues(),
                });

                formResult.current.register('text');

                formResult.current.setValue('text', '', {shouldDirty: true});
            });

            await act(async () => {
                await result.current.handleSave();
            });

            expect(savedTools(hoisted.updateMutate)).toEqual([{name: 'postMessage', parameters: {text: null}}]);
        });

        it('save is not blocked by empty required fields', async () => {
            const {result: formResult} = renderHook(() =>
                useForm<Record<string, unknown>>({defaultValues: {channel: ''}})
            );

            const {result} = renderHook(() => useMcpComponentDialog({mcpServerId: '1', open: true}));

            act(() => result.current.handleComponentSelect(selectableComponent));

            act(() => result.current.setSelectedTools([postMessageTool]));

            act(() => {
                formResult.current.register('channel', {required: true});

                result.current.registerToolForm('postMessage', {
                    form: formResult.current,
                    initialValues: formResult.current.getValues(),
                });
            });

            await act(async () => {
                await result.current.handleSave();
            });

            expect(savedTools(hoisted.createMutate)).toEqual([{name: 'postMessage', parameters: {}}]);
        });

        it('stops sending the values of a form that was discarded', async () => {
            const {result: formResult} = renderHook(() =>
                useForm<Record<string, unknown>>({defaultValues: {text: ''}})
            );

            const {result} = renderEditDialog();

            act(() => result.current.setSelectedTools([postMessageTool]));

            act(() => {
                result.current.registerToolForm('postMessage', {
                    form: formResult.current,
                    initialValues: formResult.current.getValues(),
                });

                formResult.current.register('text');

                formResult.current.setValue('text', 'typed', {shouldDirty: true});

                result.current.registerToolForm('postMessage', null);
            });

            await act(async () => {
                await result.current.handleSave();
            });

            expect(savedTools(hoisted.updateMutate)).toEqual([{name: 'postMessage', parameters: {}}]);
        });
    });

    describe('authorityOptions', () => {
        it('merges the server authorities with the component ones, without duplicates, sorted by label', () => {
            hoisted.authorities = ['ROLE_VIEWER', 'ROLE_ADMIN', 'ROLE_EDITOR'];

            const {result} = renderHook(() =>
                useMcpComponentDialog({
                    mcpComponent: {
                        ...existingMcpComponent,
                        requiredAuthorities: ['ROLE_EDITOR', 'ROLE_AUDITOR'],
                    } as McpComponent,
                    mcpServerId: '1',
                    open: true,
                })
            );

            expect(result.current.authorityOptions).toEqual([
                {label: 'Admin', value: 'ROLE_ADMIN'},
                {label: 'Auditor', value: 'ROLE_AUDITOR'},
                {label: 'Editor', value: 'ROLE_EDITOR'},
                {label: 'Viewer', value: 'ROLE_VIEWER'},
            ]);
        });

        it('lists no authorities when neither the server nor the component provides any', () => {
            const {result} = renderHook(() => useMcpComponentDialog({mcpServerId: '1', open: true}));

            expect(result.current.authorityOptions).toEqual([]);
            expect(result.current.requiredAuthorities).toEqual([]);
        });
    });

    describe('requiredAuthorities reset', () => {
        it('restores the component authorities when the dialog is closed', () => {
            const onOpenChange = vi.fn();

            const {result} = renderHook(() =>
                useMcpComponentDialog({mcpComponent: existingMcpComponent, mcpServerId: '1', onOpenChange, open: true})
            );

            act(() => result.current.setRequiredAuthorities(['ROLE_ADMIN']));

            act(() => result.current.handleClose());

            expect(onOpenChange).toHaveBeenCalledWith(false);
            expect(result.current.requiredAuthorities).toEqual(['ROLE_EDITOR']);
        });

        it('clears the authorities of a new component when the dialog is closed', () => {
            const {result} = renderHook(() => useMcpComponentDialog({mcpServerId: '1', open: true}));

            act(() => result.current.setRequiredAuthorities(['ROLE_ADMIN']));

            act(() => result.current.handleClose());

            expect(result.current.requiredAuthorities).toEqual([]);
        });

        it('restores the component authorities when the dialog open state turns off', () => {
            const {result} = renderHook(() =>
                useMcpComponentDialog({mcpComponent: existingMcpComponent, mcpServerId: '1', open: true})
            );

            act(() => result.current.setRequiredAuthorities(['ROLE_ADMIN']));

            act(() => result.current.handleOpenChange(true));

            expect(result.current.requiredAuthorities).toEqual(['ROLE_ADMIN']);

            act(() => result.current.handleOpenChange(false));

            expect(result.current.requiredAuthorities).toEqual(['ROLE_EDITOR']);
        });

        it('clears the authorities of a new component when the dialog open state turns off', () => {
            const {result} = renderHook(() => useMcpComponentDialog({mcpServerId: '1', open: true}));

            act(() => result.current.setRequiredAuthorities(['ROLE_ADMIN']));

            act(() => result.current.handleOpenChange(false));

            expect(result.current.requiredAuthorities).toEqual([]);
        });
    });

    describe('handleSave', () => {
        it('updates an existing component with the chosen authorities and restores them after success', async () => {
            const {result} = renderHook(() =>
                useMcpComponentDialog({mcpComponent: existingMcpComponent, mcpServerId: '1', open: true})
            );

            act(() => result.current.setRequiredAuthorities(['ROLE_ADMIN']));

            await act(async () => {
                await result.current.handleSave();
            });

            expect(hoisted.updateMutate).toHaveBeenCalledWith(
                {
                    id: '5',
                    input: expect.objectContaining({
                        componentName: 'slack',
                        mcpServerId: '1',
                        requiredAuthorities: ['ROLE_ADMIN'],
                        version: 3,
                    }),
                },
                expect.anything()
            );

            runMutationSuccess(hoisted.updateMutate);

            expect(result.current.requiredAuthorities).toEqual(['ROLE_EDITOR']);
        });

        it('creates a new component with the chosen authorities and clears them after success', async () => {
            const {result} = renderHook(() => useMcpComponentDialog({mcpServerId: '1', open: true}));

            act(() => result.current.handleComponentSelect(selectableComponent));

            act(() => result.current.setRequiredAuthorities(['ROLE_ADMIN']));

            await act(async () => {
                await result.current.handleSave();
            });

            expect(hoisted.createMutate).toHaveBeenCalledWith(
                {
                    input: expect.objectContaining({
                        componentName: 'github',
                        mcpServerId: '1',
                        requiredAuthorities: ['ROLE_ADMIN'],
                    }),
                },
                expect.anything()
            );

            runMutationSuccess(hoisted.createMutate);

            expect(result.current.requiredAuthorities).toEqual([]);
            expect(result.current.selectedComponent).toBeNull();
        });
    });
});
