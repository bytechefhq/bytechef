import {McpComponent} from '@/shared/middleware/graphql';
import {act, renderHook} from '@testing-library/react';
import {useForm} from 'react-hook-form';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useMcpComponentDialog from '../useMcpComponentDialog';

const hoisted = vi.hoisted(() => ({
    createMutate: vi.fn(),
    createMutationOptions: undefined as {onSuccess: () => void} | undefined,
    invalidateQueries: vi.fn(),
    seededValuesByTool: {} as Record<string, Record<string, string>>,
    seedingParams: [] as Array<{
        componentName: string;
        componentVersion: number;
        connectionRequired?: boolean;
        newToolNames: string[];
    }>,
    updateMutate: vi.fn(),
    updateMutationOptions: undefined as {onSuccess: () => void} | undefined,
    useEmbeddedMcpToolsByComponentIdQuery: vi.fn(),
    waitForSeeding: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useCreateEmbeddedMcpComponentMutation: (options: {onSuccess: () => void}) => {
        hoisted.createMutationOptions = options;

        return {isPending: false, mutate: hoisted.createMutate};
    },
    useEmbeddedMcpToolsByComponentIdQuery: hoisted.useEmbeddedMcpToolsByComponentIdQuery,
    useUpdateEmbeddedMcpComponentMutation: (options: {onSuccess: () => void}) => {
        hoisted.updateMutationOptions = options;

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

        return {aiValuesByTool: {}, waitForSeeding: hoisted.waitForSeeding};
    },
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    useGetComponentDefinitionQuery: () => ({data: {connectionRequired: true}}),
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: () => ({invalidateQueries: hoisted.invalidateQueries}),
}));

const mcpComponent = {
    componentName: 'gmail',
    componentVersion: 1,
    id: '7',
    integrationId: '5',
    title: 'Gmail',
    version: 2,
} as McpComponent;

const unboundMcpComponent = {...mcpComponent, integrationId: null} as McpComponent;

describe('useMcpComponentDialog', () => {
    beforeEach(() => {
        hoisted.createMutate.mockClear();
        hoisted.invalidateQueries.mockClear();
        hoisted.updateMutate.mockClear();
        hoisted.useEmbeddedMcpToolsByComponentIdQuery.mockReset();
        hoisted.useEmbeddedMcpToolsByComponentIdQuery.mockReturnValue({data: undefined});
        hoisted.seededValuesByTool = {};
        hoisted.seedingParams = [];
        hoisted.waitForSeeding.mockReset();
        hoisted.waitForSeeding.mockImplementation(() => Promise.resolve(hoisted.seededValuesByTool));
    });

    it('creates the component through the embedded mutation', async () => {
        const {result} = renderHook(() => useMcpComponentDialog({mcpServerId: '1', open: true}));

        act(() =>
            result.current.handleComponentSelect({integrationId: '5', name: 'gmail', title: 'Gmail', version: 1})
        );

        await act(async () => {
            await result.current.handleSave();
        });

        expect(hoisted.createMutate).toHaveBeenCalledWith(
            {
                input: {componentName: 'gmail', componentVersion: 1, mcpServerId: '1', tools: []},
                integrationId: '5',
            },
            expect.objectContaining({onSuccess: expect.any(Function)})
        );
        expect(hoisted.updateMutate).not.toHaveBeenCalled();
    });

    it('updates the component through the embedded mutation', async () => {
        const {result} = renderHook(() => useMcpComponentDialog({mcpComponent, mcpServerId: '1', open: true}));

        await act(async () => {
            await result.current.handleSave();
        });

        expect(hoisted.updateMutate).toHaveBeenCalledWith(
            {
                id: '7',
                input: {componentName: 'gmail', componentVersion: 1, mcpServerId: '1', tools: [], version: 2},
                integrationId: undefined,
            },
            expect.objectContaining({onSuccess: expect.any(Function)})
        );
        expect(hoisted.createMutate).not.toHaveBeenCalled();
    });

    describe('tool parameters', () => {
        const readEmailsTool = {componentName: 'gmail', componentVersion: 1, name: 'readEmails'};
        const sendEmailTool = {componentName: 'gmail', componentVersion: 1, name: 'sendEmail'};

        const savedTo = "=fromAi('to', 'STRING', {'required': true})";
        const seededQuery = "=fromAi('query', 'STRING')";

        const savedTools = (mutate: ReturnType<typeof vi.fn>) =>
            (mutate.mock.calls[mutate.mock.calls.length - 1][0] as {input: {tools: unknown}}).input.tools;

        beforeEach(() => {
            hoisted.useEmbeddedMcpToolsByComponentIdQuery.mockReturnValue({
                data: {embeddedMcpToolsByComponentId: [{name: 'sendEmail', parameters: {to: savedTo}}]},
            });
        });

        it('keeps saved parameters of kept tools on update', async () => {
            const {result} = renderHook(() => useMcpComponentDialog({mcpComponent, mcpServerId: '1', open: true}));

            act(() => result.current.setSelectedTools([sendEmailTool]));

            await act(async () => {
                await result.current.handleSave();
            });

            expect(savedTools(hoisted.updateMutate)).toEqual([{name: 'sendEmail', parameters: {to: savedTo}}]);
        });

        it('seeds new tools with ai values', async () => {
            hoisted.seededValuesByTool = {readEmails: {query: seededQuery}};

            const {result} = renderHook(() => useMcpComponentDialog({mcpComponent, mcpServerId: '1', open: true}));

            act(() => result.current.setSelectedTools([sendEmailTool, readEmailsTool]));

            await act(async () => {
                await result.current.handleSave();
            });

            expect(hoisted.seedingParams[hoisted.seedingParams.length - 1]).toEqual(
                expect.objectContaining({
                    componentName: 'gmail',
                    componentVersion: 1,
                    connectionRequired: true,
                    newToolNames: ['readEmails'],
                })
            );
            expect(savedTools(hoisted.updateMutate)).toEqual([
                {name: 'sendEmail', parameters: {to: savedTo}},
                {name: 'readEmails', parameters: {query: seededQuery}},
            ]);
        });

        it('does not re-seed existing tools', async () => {
            hoisted.seededValuesByTool = {sendEmail: {subject: "=fromAi('subject', 'STRING')"}};

            const {result} = renderHook(() => useMcpComponentDialog({mcpComponent, mcpServerId: '1', open: true}));

            act(() => result.current.setSelectedTools([sendEmailTool]));

            await act(async () => {
                await result.current.handleSave();
            });

            expect(hoisted.seedingParams[hoisted.seedingParams.length - 1].newToolNames).toEqual([]);
            expect(savedTools(hoisted.updateMutate)).toEqual([{name: 'sendEmail', parameters: {to: savedTo}}]);
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

            act(() =>
                result.current.handleComponentSelect({integrationId: '5', name: 'gmail', title: 'Gmail', version: 1})
            );

            act(() => result.current.setSelectedTools([readEmailsTool]));

            let savePromise: Promise<void> = Promise.resolve();

            act(() => {
                savePromise = result.current.handleSave();
            });

            expect(result.current.isSavePending).toBe(true);
            expect(hoisted.createMutate).not.toHaveBeenCalled();

            await act(async () => {
                resolveSeeding({readEmails: {query: seededQuery}});

                await savePromise;
            });

            expect(result.current.isSavePending).toBe(false);
            expect(savedTools(hoisted.createMutate)).toEqual([{name: 'readEmails', parameters: {query: seededQuery}}]);
        });

        it('saves an existing tool whose array item was deleted', async () => {
            hoisted.useEmbeddedMcpToolsByComponentIdQuery.mockReturnValue({
                data: {embeddedMcpToolsByComponentId: [{name: 'sendEmail', parameters: {cc: ['a', 'b'], to: savedTo}}]},
            });

            const {result: formResult} = renderHook(() =>
                useForm<Record<string, unknown>>({defaultValues: {cc: ['a', 'b'], to: savedTo}})
            );

            const {result} = renderHook(() => useMcpComponentDialog({mcpComponent, mcpServerId: '1', open: true}));

            act(() => result.current.setSelectedTools([sendEmailTool]));

            act(() => {
                result.current.registerToolForm('sendEmail', {
                    form: formResult.current,
                    initialValues: formResult.current.getValues(),
                });

                formResult.current.setValue('cc', ['a']);
            });

            await act(async () => {
                await result.current.handleSave();
            });

            expect(savedTools(hoisted.updateMutate)).toEqual([
                {name: 'sendEmail', parameters: {cc: ['a'], to: savedTo}},
            ]);
        });

        it('does not re-seed a field the user cleared', async () => {
            hoisted.seededValuesByTool = {readEmails: {query: seededQuery}};

            const {result: formResult} = renderHook(() =>
                useForm<Record<string, unknown>>({defaultValues: {query: seededQuery}})
            );

            const {result} = renderHook(() => useMcpComponentDialog({mcpComponent, mcpServerId: '1', open: true}));

            act(() => result.current.setSelectedTools([readEmailsTool]));

            act(() => {
                result.current.registerToolForm('readEmails', {
                    form: formResult.current,
                    initialValues: formResult.current.getValues(),
                });

                formResult.current.register('query');

                formResult.current.setValue('query', '', {shouldDirty: true});
            });

            await act(async () => {
                await result.current.handleSave();
            });

            expect(savedTools(hoisted.updateMutate)).toEqual([{name: 'readEmails', parameters: {query: null}}]);
        });

        it('save is not blocked by empty required fields', async () => {
            const {result: formResult} = renderHook(() =>
                useForm<Record<string, unknown>>({defaultValues: {label: ''}})
            );

            const {result} = renderHook(() => useMcpComponentDialog({mcpServerId: '1', open: true}));

            act(() =>
                result.current.handleComponentSelect({integrationId: '5', name: 'gmail', title: 'Gmail', version: 1})
            );

            act(() => result.current.setSelectedTools([readEmailsTool]));

            act(() => {
                formResult.current.register('label', {required: true});

                result.current.registerToolForm('readEmails', {
                    form: formResult.current,
                    initialValues: formResult.current.getValues(),
                });
            });

            await act(async () => {
                await result.current.handleSave();
            });

            expect(savedTools(hoisted.createMutate)).toEqual([{name: 'readEmails', parameters: {}}]);
        });
    });

    it('does not create a component before an integration is selected', async () => {
        const {result} = renderHook(() => useMcpComponentDialog({mcpServerId: '1', open: true}));

        act(() => result.current.handleComponentSelect({name: 'gmail', title: 'Gmail', version: 1}));

        await act(async () => {
            await result.current.handleSave();
        });

        expect(hoisted.createMutate).not.toHaveBeenCalled();
    });

    it('opens a component without an integration on the integration step', () => {
        const {result} = renderHook(() =>
            useMcpComponentDialog({mcpComponent: unboundMcpComponent, mcpServerId: '1', open: true})
        );

        expect(result.current.currentStep).toBe('components');
        expect(result.current.selectedComponent).toBeNull();
    });

    it('binds a component without an integration to the selected integration when it is updated', async () => {
        const {result} = renderHook(() =>
            useMcpComponentDialog({mcpComponent: unboundMcpComponent, mcpServerId: '1', open: true})
        );

        act(() =>
            result.current.handleComponentSelect({integrationId: '6', name: 'gmail', title: 'Gmail Code', version: 1})
        );

        expect(result.current.currentStep).toBe('tools');

        await act(async () => {
            await result.current.handleSave();
        });

        expect(hoisted.updateMutate).toHaveBeenCalledWith(
            {
                id: '7',
                input: {componentName: 'gmail', componentVersion: 1, mcpServerId: '1', tools: [], version: 2},
                integrationId: '6',
            },
            expect.objectContaining({onSuccess: expect.any(Function)})
        );
    });

    it('opens a component bound to an integration on the tool step with its integration selected', () => {
        const {result} = renderHook(() => useMcpComponentDialog({mcpComponent, mcpServerId: '1', open: true}));

        expect(result.current.currentStep).toBe('tools');
        expect(result.current.selectedComponent).toEqual({
            integrationId: '5',
            name: 'gmail',
            title: 'Gmail',
            version: 1,
        });
    });

    it('closes the dialog when going back from a component bound to an integration', () => {
        const onOpenChange = vi.fn();

        const {result} = renderHook(() =>
            useMcpComponentDialog({mcpComponent, mcpServerId: '1', onOpenChange, open: true})
        );

        act(() => result.current.handleBack());

        expect(onOpenChange).toHaveBeenCalledWith(false);
        expect(result.current.currentStep).toBe('tools');
        expect(result.current.selectedComponent?.integrationId).toBe('5');
    });

    it('returns to the integration step when going back from a component without an integration', () => {
        const onOpenChange = vi.fn();

        const {result} = renderHook(() =>
            useMcpComponentDialog({mcpComponent: unboundMcpComponent, mcpServerId: '1', onOpenChange, open: true})
        );

        act(() =>
            result.current.handleComponentSelect({integrationId: '6', name: 'gmail', title: 'Gmail Code', version: 1})
        );

        act(() => result.current.handleBack());

        expect(onOpenChange).not.toHaveBeenCalled();
        expect(result.current.currentStep).toBe('components');
        expect(result.current.selectedComponent).toBeNull();
    });

    it('forgets an unsaved integration choice when a component without an integration is closed', () => {
        const onOpenChange = vi.fn();

        const {result} = renderHook(() =>
            useMcpComponentDialog({mcpComponent: unboundMcpComponent, mcpServerId: '1', onOpenChange, open: true})
        );

        act(() =>
            result.current.handleComponentSelect({integrationId: '6', name: 'gmail', title: 'Gmail Code', version: 1})
        );

        act(() => result.current.handleOpenChange(false));

        expect(onOpenChange).toHaveBeenCalledWith(false);
        expect(result.current.currentStep).toBe('components');
        expect(result.current.selectedComponent).toBeNull();

        act(() =>
            result.current.handleComponentSelect({integrationId: '6', name: 'gmail', title: 'Gmail Code', version: 1})
        );

        act(() => result.current.handleClose());

        expect(result.current.currentStep).toBe('components');
        expect(result.current.selectedComponent).toBeNull();
    });

    it('keeps the tool step after a component bound to an integration is saved', async () => {
        const onOpenChange = vi.fn();

        const {result} = renderHook(() =>
            useMcpComponentDialog({mcpComponent, mcpServerId: '1', onOpenChange, open: true})
        );

        await act(async () => {
            await result.current.handleSave();
        });

        const saveOptions = hoisted.updateMutate.mock.calls[0][1];

        act(() => saveOptions.onSuccess());

        expect(onOpenChange).toHaveBeenCalledWith(false);
        expect(result.current.currentStep).toBe('tools');
        expect(result.current.selectedComponent?.integrationId).toBe('5');
    });

    it('opens a component whose integration is unavailable on the integration step', () => {
        const {result} = renderHook(() =>
            useMcpComponentDialog({integrationUnavailable: true, mcpComponent, mcpServerId: '1', open: true})
        );

        expect(result.current.currentStep).toBe('components');
        expect(result.current.selectedComponent).toBeNull();
    });

    it('rebinds a component whose integration is unavailable to the selected integration', async () => {
        const {result} = renderHook(() =>
            useMcpComponentDialog({integrationUnavailable: true, mcpComponent, mcpServerId: '1', open: true})
        );

        act(() =>
            result.current.handleComponentSelect({integrationId: '6', name: 'gmail', title: 'Gmail Code', version: 1})
        );

        await act(async () => {
            await result.current.handleSave();
        });

        expect(hoisted.updateMutate).toHaveBeenCalledWith(
            {
                id: '7',
                input: {componentName: 'gmail', componentVersion: 1, mcpServerId: '1', tools: [], version: 2},
                integrationId: '6',
            },
            expect.objectContaining({onSuccess: expect.any(Function)})
        );
    });

    it('returns to the integration step when going back from a component whose integration is unavailable', () => {
        const onOpenChange = vi.fn();

        const {result} = renderHook(() =>
            useMcpComponentDialog({
                integrationUnavailable: true,
                mcpComponent,
                mcpServerId: '1',
                onOpenChange,
                open: true,
            })
        );

        act(() =>
            result.current.handleComponentSelect({integrationId: '6', name: 'gmail', title: 'Gmail Code', version: 1})
        );

        act(() => result.current.handleBack());

        expect(onOpenChange).not.toHaveBeenCalled();
        expect(result.current.currentStep).toBe('components');
    });

    it('loads the existing embedded tools only for a saved component while the dialog is open', () => {
        const existingTools = {embeddedMcpToolsByComponentId: [{name: 'sendEmail'}]};

        hoisted.useEmbeddedMcpToolsByComponentIdQuery.mockReturnValue({data: existingTools});

        const {result} = renderHook(() => useMcpComponentDialog({mcpComponent, mcpServerId: '1', open: true}));

        expect(hoisted.useEmbeddedMcpToolsByComponentIdQuery).toHaveBeenCalledWith(
            {mcpComponentId: '7'},
            {enabled: true}
        );
        expect(result.current.existingTools).toBe(existingTools);

        renderHook(() => useMcpComponentDialog({mcpComponent, mcpServerId: '1', open: false}));

        expect(hoisted.useEmbeddedMcpToolsByComponentIdQuery).toHaveBeenLastCalledWith(
            {mcpComponentId: '7'},
            {enabled: false}
        );

        renderHook(() => useMcpComponentDialog({mcpServerId: '1', open: true}));

        expect(hoisted.useEmbeddedMcpToolsByComponentIdQuery).toHaveBeenLastCalledWith(
            {mcpComponentId: ''},
            {enabled: false}
        );
    });

    it('refreshes the embedded components and servers after a component is created', () => {
        renderHook(() => useMcpComponentDialog({mcpServerId: '1', open: true}));

        act(() => hoisted.createMutationOptions!.onSuccess());

        expect(hoisted.invalidateQueries).toHaveBeenCalledWith({queryKey: ['embeddedMcpComponentsByServerId']});
        expect(hoisted.invalidateQueries).toHaveBeenCalledWith({queryKey: ['embeddedMcpServers']});
        expect(hoisted.invalidateQueries).not.toHaveBeenCalledWith({queryKey: ['mcpComponentsByServerId']});
    });

    it('refreshes the embedded components and servers after a component is updated', () => {
        renderHook(() => useMcpComponentDialog({mcpComponent, mcpServerId: '1', open: true}));

        act(() => hoisted.updateMutationOptions!.onSuccess());

        expect(hoisted.invalidateQueries).toHaveBeenCalledWith({queryKey: ['embeddedMcpComponentsByServerId']});
        expect(hoisted.invalidateQueries).toHaveBeenCalledWith({queryKey: ['embeddedMcpServers']});
    });

    it('closes the dialog and resets to the component step after a new component is saved', async () => {
        const onOpenChange = vi.fn();

        const {result} = renderHook(() => useMcpComponentDialog({mcpServerId: '1', onOpenChange, open: true}));

        act(() =>
            result.current.handleComponentSelect({integrationId: '5', name: 'gmail', title: 'Gmail', version: 1})
        );

        expect(result.current.currentStep).toBe('tools');

        await act(async () => {
            await result.current.handleSave();
        });

        const saveOptions = hoisted.createMutate.mock.calls[0][1];

        act(() => saveOptions.onSuccess());

        expect(onOpenChange).toHaveBeenCalledWith(false);
        expect(result.current.currentStep).toBe('components');
        expect(result.current.selectedComponent).toBeNull();
    });
});
