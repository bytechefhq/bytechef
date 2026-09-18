import {UpdateWorkflowMutationType} from '@/shared/types';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowDataStore from '../../stores/useWorkflowDataStore';
import removeWorkflowNodePosition from '../removeWorkflowNodePosition';
import saveWorkflowNodesPosition from '../saveWorkflowNodesPosition';

vi.mock('../workflowMutationGuard', async (importOriginal) => ({
    ...(await importOriginal<typeof import('../workflowMutationGuard')>()),
    isWorkflowMutating: vi.fn(() => true),
    setPendingDefinition: vi.fn(),
}));

interface StoredTriggerI {
    metadata?: {ui?: {nodePosition?: {x: number; y: number}}};
    name: string;
}

const updateWorkflowMutation = {mutate: vi.fn()} as unknown as UpdateWorkflowMutationType;

const setDefinition = (triggers: StoredTriggerI[]) =>
    useWorkflowDataStore.setState(
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        {nodes: [], workflow: {definition: JSON.stringify({tasks: [], triggers}), id: 'workflow-1'}} as any
    );

const storedTriggers = (): StoredTriggerI[] =>
    JSON.parse(useWorkflowDataStore.getState().workflow.definition!).triggers;

describe('trigger node positions', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('saves the dragged position on the matching trigger only', () => {
        setDefinition([{name: 'trigger_1'}, {name: 'trigger_2'}]);

        saveWorkflowNodesPosition({
            draggedNodeId: 'trigger_2',
            nodePositions: {trigger_2: {x: 320, y: 40}},
            updateWorkflowMutation,
        });

        const [firstTrigger, secondTrigger] = storedTriggers();

        expect(firstTrigger.metadata?.ui?.nodePosition).toBeUndefined();
        expect(secondTrigger.metadata?.ui?.nodePosition).toEqual({x: 320, y: 40});
    });

    it('clears the saved position of the matching trigger only', () => {
        setDefinition([
            {metadata: {ui: {nodePosition: {x: 10, y: 20}}}, name: 'trigger_1'},
            {metadata: {ui: {nodePosition: {x: 30, y: 40}}}, name: 'trigger_2'},
        ]);

        removeWorkflowNodePosition({
            incrementLayoutResetCounter: vi.fn(),
            invalidateWorkflowQueries: vi.fn(),
            nodeName: 'trigger_2',
            updateWorkflowMutation,
        });

        const [firstTrigger, secondTrigger] = storedTriggers();

        expect(firstTrigger.metadata?.ui?.nodePosition).toEqual({x: 10, y: 20});
        expect(secondTrigger.metadata?.ui?.nodePosition).toBeUndefined();
    });
});
