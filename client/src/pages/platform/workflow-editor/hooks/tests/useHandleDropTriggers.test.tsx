import {ClickedDefinitionType} from '@/shared/types';
import {renderHook, waitFor} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowDataStore from '../../stores/useWorkflowDataStore';
import saveWorkflowDefinition from '../../utils/saveWorkflowDefinition';
import useHandleDrop from '../useHandleDrop';

const hoisted = vi.hoisted(() => ({
    captureComponentUsed: vi.fn(),
    updateWorkflowMutation: {mutate: vi.fn()},
}));

vi.mock('@/shared/hooks/useAnalytics', () => ({
    useAnalytics: () => ({captureComponentUsed: hoisted.captureComponentUsed}),
}));

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({updateWorkflowMutation: hoisted.updateWorkflowMutation}),
}));

vi.mock('@tanstack/react-query', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@tanstack/react-query')>()),
    useQueryClient: () => ({}),
}));

vi.mock('../../utils/saveWorkflowDefinition', () => ({default: vi.fn()}));

const manualTrigger = {name: 'manual', title: 'Manual', trigger: true} as ClickedDefinitionType;

const savedNodeData = () => vi.mocked(saveWorkflowDefinition).mock.calls[0]?.[0]?.nodeData;

describe('useHandleDrop for triggers', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        useWorkflowDataStore.setState({
            nodes: [{data: {name: 'trigger_1', trigger: true}, id: 'trigger_1', position: {x: 0, y: 0}}],
            workflow: {definition: JSON.stringify({tasks: [], triggers: [{name: 'trigger_1'}]}), id: 'workflow-1'},
        } as unknown as Parameters<typeof useWorkflowDataStore.setState>[0]);
    });

    it('keeps the target trigger name when a trigger is dropped onto an existing trigger', async () => {
        const {result} = renderHook(() => useHandleDrop({taskDispatcherDefinitions: []}));

        const handleDropOnTriggerNode = result.current[2];

        handleDropOnTriggerNode(manualTrigger, 'trigger_1');

        await waitFor(() => expect(saveWorkflowDefinition).toHaveBeenCalledOnce());

        expect(savedNodeData()).toEqual(
            expect.objectContaining({name: 'trigger_1', trigger: true, workflowNodeName: 'trigger_1'})
        );
        expect(hoisted.captureComponentUsed).toHaveBeenCalledWith('manual', undefined, undefined);
    });

    it('gives a trigger dropped on the add-trigger slot a new name', async () => {
        const {result} = renderHook(() => useHandleDrop({taskDispatcherDefinitions: []}));

        const handleDropOnTriggerPlaceholder = result.current[3];

        handleDropOnTriggerPlaceholder(manualTrigger);

        await waitFor(() => expect(saveWorkflowDefinition).toHaveBeenCalledOnce());

        expect(savedNodeData()?.trigger).toBe(true);
        expect(savedNodeData()?.name).toMatch(/^trigger_\d+$/);
        expect(savedNodeData()?.name).not.toBe('trigger_1');
    });
});
