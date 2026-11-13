import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useMcpWorkflowToolForms, {SavedToolParametersByWorkflowIdType} from '../useMcpWorkflowToolForms';

const hoisted = vi.hoisted(() => ({
    failedSeedKeys: [] as string[],
    seededValuesByWorkflowId: {} as Record<string, Record<string, string>>,
    toastWarning: vi.fn(),
    useMcpWorkflowToolSeeding: vi.fn(),
}));

vi.mock('sonner', () => ({toast: {warning: hoisted.toastWarning}}));

vi.mock('@/pages/platform/mcp-servers/hooks/useMcpWorkflowToolSeeding', () => ({
    default: (params: unknown) => {
        hoisted.useMcpWorkflowToolSeeding(params);

        return {
            aiValuesByWorkflowId: {},
            getFailedSeedKeys: () => hoisted.failedSeedKeys,
            waitForSeeding: async () => hoisted.seededValuesByWorkflowId,
        };
    },
}));

const loadWorkflowProperties = vi.fn(async () => []);

const savedParametersByWorkflowId: SavedToolParametersByWorkflowIdType = new Map([
    ['existing-workflow', {message: 'Hi', toolName: 'existing_tool'}],
]);

type HookPropsType = Parameters<typeof useMcpWorkflowToolForms>[0];

const baseProps: HookPropsType = {
    loadWorkflowProperties,
    savedParametersByWorkflowId,
    seedKey: 'seed',
    selectedWorkflowIds: ['existing-workflow', 'new-workflow'],
};

const renderToolForms = (props: HookPropsType = baseProps) =>
    renderHook((hookProps: HookPropsType) => useMcpWorkflowToolForms(hookProps), {initialProps: props});

describe('useMcpWorkflowToolForms', () => {
    beforeEach(() => {
        hoisted.seededValuesByWorkflowId = {};
        hoisted.useMcpWorkflowToolSeeding.mockReset();
    });

    it('seeds only the workflows that are not saved yet and keeps that list stable across renders', () => {
        const {rerender} = renderToolForms();

        const firstNewWorkflowIds = hoisted.useMcpWorkflowToolSeeding.mock.lastCall![0].newWorkflowIds;

        expect(firstNewWorkflowIds).toEqual(['new-workflow']);

        rerender(baseProps);

        expect(hoisted.useMcpWorkflowToolSeeding.mock.lastCall![0].newWorkflowIds).toBe(firstNewWorkflowIds);
    });

    it('sends the seeded values of a new workflow and omits an unchanged existing workflow', async () => {
        hoisted.seededValuesByWorkflowId = {'new-workflow': {message: '=fromAi(message)'}};

        const {result} = renderToolForms();

        let toolParameters: Awaited<ReturnType<typeof result.current.buildToolParameters>> = [];

        await act(async () => {
            toolParameters = await result.current.buildToolParameters(['existing-workflow', 'new-workflow']);
        });

        expect(toolParameters).toEqual([{parameters: {message: '=fromAi(message)'}, workflowId: 'new-workflow'}]);
    });

    it('warns when the tool settings of a new workflow could not be loaded', async () => {
        hoisted.failedSeedKeys = ['new-workflow', 'existing-workflow'];

        const {result} = renderToolForms();

        await act(async () => {
            await result.current.buildToolParameters(['existing-workflow', 'new-workflow']);
        });

        expect(hoisted.toastWarning).toHaveBeenCalledTimes(1);
        expect(hoisted.toastWarning).toHaveBeenCalledWith(
            "Couldn't load the settings of 1 workflow tool",
            expect.objectContaining({description: expect.stringContaining('without AI values')})
        );

        hoisted.failedSeedKeys = [];
        hoisted.toastWarning.mockReset();
    });

    it('sends the changed values of an existing workflow on top of its saved parameters', async () => {
        const {result} = renderToolForms();

        const toolForm = result.current.getToolForm('existing-workflow');

        act(() => {
            toolForm.formControl.reset({message: 'Hi', toolName: 'existing_tool'});

            result.current.setToolFormInitialValues('existing-workflow', {message: 'Hi', toolName: 'existing_tool'});
        });

        act(() => toolForm.formControl.setValue('message', '', {shouldDirty: true}));

        let toolParameters: Awaited<ReturnType<typeof result.current.buildToolParameters>> = [];

        await act(async () => {
            toolParameters = await result.current.buildToolParameters(['existing-workflow']);
        });

        expect(toolParameters).toEqual([
            {parameters: {message: null, toolName: 'existing_tool'}, workflowId: 'existing-workflow'},
        ]);
    });

    it('reports new and existing workflows', () => {
        const {result} = renderToolForms();

        expect(result.current.isNewWorkflow('new-workflow')).toBe(true);
        expect(result.current.isNewWorkflow('existing-workflow')).toBe(false);
    });

    it('drops the tool form of a deselected workflow', () => {
        const {rerender, result} = renderToolForms();

        const toolForm = result.current.getToolForm('new-workflow');

        expect(result.current.getToolForm('new-workflow')).toBe(toolForm);

        rerender({...baseProps, selectedWorkflowIds: ['existing-workflow']});

        expect(result.current.getToolForm('new-workflow')).not.toBe(toolForm);
    });
});
