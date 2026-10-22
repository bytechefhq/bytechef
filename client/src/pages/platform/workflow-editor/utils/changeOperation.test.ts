import {ComponentDefinition} from '@/shared/middleware/platform/configuration';
import {NodeDataType} from '@/shared/types';
import {QueryClient} from '@tanstack/react-query';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import changeOperation from './changeOperation';
import saveClusterElementFieldChange from './saveClusterElementFieldChange';
import saveTaskDispatcherSubtaskFieldChange from './saveTaskDispatcherSubtaskFieldChange';
import saveWorkflowDefinition from './saveWorkflowDefinition';

const mockSetCurrentNode = vi.fn();
const mockSetOperationChangeInProgress = vi.fn();

let mockCurrentNode: NodeDataType | undefined;
let mockOperationChangeInProgress = false;

vi.mock('../stores/useWorkflowNodeDetailsPanelStore', () => ({
    default: {
        getState: () => ({
            currentNode: mockCurrentNode,
            operationChangeInProgress: mockOperationChangeInProgress,
            setCurrentNode: mockSetCurrentNode,
            setOperationChangeInProgress: mockSetOperationChangeInProgress,
        }),
    },
}));

vi.mock('./saveWorkflowDefinition', () => ({default: vi.fn()}));
vi.mock('./invalidateOperationQueries', () => ({default: vi.fn()}));
vi.mock('./saveClusterElementFieldChange', () => ({default: vi.fn()}));
vi.mock('./saveTaskDispatcherSubtaskFieldChange', () => ({default: vi.fn()}));

const componentDefinition = {name: 'dataStorage', version: 1} as ComponentDefinition;

const appendValueToListDefinition = {
    name: 'appendValueToList',
    properties: [
        {name: 'key', type: 'STRING'},
        {defaultValue: 'CURRENT_EXECUTION', name: 'scope', type: 'STRING'},
    ],
} as never;

const buildNode = (overrides: Partial<NodeDataType> = {}): NodeDataType => ({
    componentName: 'dataStorage',
    label: 'Data Storage',
    name: 'dataStorage_1',
    operationName: 'atomicIncrement',
    parameters: {key: 'counter'},
    type: 'dataStorage/v1/atomicIncrement',
    workflowNodeName: 'dataStorage_1',
    ...overrides,
});

const buildDependencies = (overrides: Record<string, unknown> = {}) => ({
    currentComponentDefinition: componentDefinition,
    currentOperationName: 'atomicIncrement',
    deleteWorkflowNodeTestOutputMutation: {mutateAsync: vi.fn().mockResolvedValue(undefined)},
    environmentId: 1,
    fetchActionDefinition: vi.fn().mockResolvedValue(appendValueToListDefinition),
    fetchClusterElementDefinition: vi.fn().mockResolvedValue(appendValueToListDefinition),
    fetchTriggerDefinition: vi.fn().mockResolvedValue(appendValueToListDefinition),
    isClusterElement: false,
    newOperationName: 'appendValueToList',
    queryClient: {invalidateQueries: vi.fn()} as unknown as QueryClient,
    setCurrentOperationName: vi.fn(),
    updateWorkflowMutation: {} as never,
    workflowId: 'workflow-1',
    ...overrides,
});

describe('changeOperation', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        mockCurrentNode = buildNode();
        mockOperationChangeInProgress = false;

        vi.mocked(saveWorkflowDefinition).mockImplementation(async ({onSuccess}) => onSuccess?.());
    });

    it('should do nothing when the node already uses the picked operation', async () => {
        const dependencies = buildDependencies({newOperationName: 'atomicIncrement'});

        await changeOperation(dependencies);

        expect(dependencies.setCurrentOperationName).not.toHaveBeenCalled();
        expect(mockSetOperationChangeInProgress).not.toHaveBeenCalled();
    });

    it('should do nothing when no node is open in the panel', async () => {
        mockCurrentNode = undefined;

        const dependencies = buildDependencies();

        await changeOperation(dependencies);

        expect(dependencies.setCurrentOperationName).not.toHaveBeenCalled();
        expect(saveWorkflowDefinition).not.toHaveBeenCalled();
    });

    it('should ignore a pick while another operation change is saving', async () => {
        mockOperationChangeInProgress = true;

        const dependencies = buildDependencies();

        await changeOperation(dependencies);

        expect(dependencies.setCurrentOperationName).not.toHaveBeenCalled();
        expect(mockSetOperationChangeInProgress).not.toHaveBeenCalled();
        expect(dependencies.fetchActionDefinition).not.toHaveBeenCalled();
    });

    it('should show the picked operation and mark the change in progress before saving', async () => {
        const dependencies = buildDependencies();

        await changeOperation(dependencies);

        expect(dependencies.setCurrentOperationName).toHaveBeenCalledWith('appendValueToList');
        expect(mockSetOperationChangeInProgress).toHaveBeenNthCalledWith(1, true);
    });

    it('should reset the node test output before saving the new operation', async () => {
        const dependencies = buildDependencies();

        await changeOperation(dependencies);

        expect(dependencies.deleteWorkflowNodeTestOutputMutation.mutateAsync).toHaveBeenCalledWith({
            environmentId: 1,
            id: 'workflow-1',
            workflowNodeName: 'dataStorage_1',
        });
    });

    it('should save the new operation type with the defaults of its properties', async () => {
        await changeOperation(buildDependencies());

        expect(vi.mocked(saveWorkflowDefinition).mock.calls[0][0].nodeData).toMatchObject({
            name: 'dataStorage_1',
            operationName: 'appendValueToList',
            parameters: {scope: 'CURRENT_EXECUTION'},
            type: 'dataStorage/v1/appendValueToList',
        });
    });

    it('should update the open node and clear the in-progress flag once the workflow is saved', async () => {
        await changeOperation(buildDependencies());

        expect(mockSetCurrentNode).toHaveBeenCalledWith(
            expect.objectContaining({operationName: 'appendValueToList', type: 'dataStorage/v1/appendValueToList'})
        );
        expect(mockSetOperationChangeInProgress).toHaveBeenLastCalledWith(false);
    });

    it('should clear the in-progress flag when saving the workflow fails', async () => {
        vi.mocked(saveWorkflowDefinition).mockImplementation(async ({onError}) => onError?.());

        await changeOperation(buildDependencies());

        expect(mockSetCurrentNode).not.toHaveBeenCalled();
        expect(mockSetOperationChangeInProgress).toHaveBeenLastCalledWith(false);
    });

    it('should clear the in-progress flag when the picked operation has no definition', async () => {
        await changeOperation(buildDependencies({fetchActionDefinition: vi.fn().mockResolvedValue(undefined)}));

        expect(saveWorkflowDefinition).not.toHaveBeenCalled();
        expect(mockSetOperationChangeInProgress).toHaveBeenLastCalledWith(false);
    });

    it('should clear the in-progress flag when resetting the test output fails', async () => {
        vi.spyOn(console, 'error').mockImplementation(() => {});

        await changeOperation(
            buildDependencies({
                deleteWorkflowNodeTestOutputMutation: {mutateAsync: vi.fn().mockRejectedValue(new Error('Gone'))},
            })
        );

        expect(saveWorkflowDefinition).not.toHaveBeenCalled();
        expect(mockSetOperationChangeInProgress).toHaveBeenLastCalledWith(false);
    });

    it('should resolve a trigger node against the triggers of the component', async () => {
        mockCurrentNode = buildNode({trigger: true});

        const dependencies = buildDependencies();

        await changeOperation(dependencies);

        expect(dependencies.fetchTriggerDefinition).toHaveBeenCalledWith('appendValueToList');
        expect(dependencies.fetchActionDefinition).not.toHaveBeenCalled();
    });

    it('should route a cluster element through the cluster element save path', async () => {
        mockCurrentNode = buildNode({clusterElementType: 'tools'});

        const dependencies = buildDependencies({isClusterElement: true});

        await changeOperation(dependencies);

        expect(dependencies.fetchClusterElementDefinition).toHaveBeenCalledWith('appendValueToList');
        expect(vi.mocked(saveClusterElementFieldChange).mock.calls[0][0]).toMatchObject({
            fieldUpdate: {field: 'operation', value: 'appendValueToList'},
        });
        expect(saveWorkflowDefinition).not.toHaveBeenCalled();

        vi.mocked(saveClusterElementFieldChange).mock.calls[0][0].onError?.();

        expect(mockSetOperationChangeInProgress).toHaveBeenLastCalledWith(false);
    });

    it('should route a task dispatcher subtask through the subtask save path', async () => {
        mockCurrentNode = buildNode({conditionData: {conditionCase: 'caseTrue', conditionId: 'condition_1', index: 0}});

        await changeOperation(buildDependencies({currentNodeIndex: 2}));

        expect(vi.mocked(saveTaskDispatcherSubtaskFieldChange).mock.calls[0][0]).toMatchObject({
            currentNodeIndex: 2,
            fieldUpdate: {field: 'operation', value: 'appendValueToList'},
        });
        expect(saveWorkflowDefinition).not.toHaveBeenCalled();

        vi.mocked(saveTaskDispatcherSubtaskFieldChange).mock.calls[0][0].onError?.();

        expect(mockSetOperationChangeInProgress).toHaveBeenLastCalledWith(false);
    });
});
