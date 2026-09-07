import {ComponentDefinition} from '@/shared/middleware/platform/configuration';
import {NodeDataType} from '@/shared/types';
import {QueryClient} from '@tanstack/react-query';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import changeComponentVersion from './changeComponentVersion';
import saveClusterElementFieldChange from './saveClusterElementFieldChange';
import saveTaskDispatcherSubtaskFieldChange from './saveTaskDispatcherSubtaskFieldChange';
import saveWorkflowDefinition from './saveWorkflowDefinition';

const mockGetComponentDefinition = vi.fn();
const mockSetCurrentNode = vi.fn();
const mockSetOperationChangeInProgress = vi.fn();

let mockCurrentNode: NodeDataType | undefined;

vi.mock('../stores/useWorkflowNodeDetailsPanelStore', () => ({
    default: {
        getState: () => ({
            currentNode: mockCurrentNode,
            setCurrentNode: mockSetCurrentNode,
            setOperationChangeInProgress: mockSetOperationChangeInProgress,
        }),
    },
}));

vi.mock('@/shared/middleware/platform/configuration', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/platform/configuration')>()),
    ComponentDefinitionApi: class {
        getComponentDefinition = mockGetComponentDefinition;
    },
}));

vi.mock('./saveWorkflowDefinition', () => ({default: vi.fn()}));
vi.mock('./invalidateOperationQueries', () => ({default: vi.fn()}));
vi.mock('./saveClusterElementFieldChange', () => ({default: vi.fn()}));
vi.mock('./saveTaskDispatcherSubtaskFieldChange', () => ({default: vi.fn()}));

const versionOneDefinition = {name: 'slack', version: 1} as ComponentDefinition;

const versionTwoDefinition = {
    actions: [{name: 'sendMessage'}, {name: 'sendDirectMessage'}],
    clusterElements: [{name: 'sendMessage', type: 'MODEL'}],
    name: 'slack',
    triggers: [{name: 'newMessage'}],
    version: 2,
} as ComponentDefinition;

const sendMessageDefinition = {
    name: 'sendMessage',
    properties: [
        {name: 'channel', type: 'STRING'},
        {defaultValue: 'markdown', name: 'format', type: 'STRING'},
    ],
} as never;

const buildNode = (overrides: Partial<NodeDataType> = {}): NodeDataType => ({
    componentName: 'slack',
    label: 'Send message',
    metadata: {ui: {nodePosition: {x: 10, y: 20}}},
    name: 'slack_1',
    operationName: 'sendMessage',
    parameters: {channel: '#general'},
    type: 'slack/v1/sendMessage',
    version: 1,
    workflowNodeName: 'slack_1',
    ...overrides,
});

const buildQueryClient = (
    fetchQuery = vi.fn(({queryFn}: {queryFn: () => Promise<ComponentDefinition>}) => queryFn())
) => ({fetchQuery, invalidateQueries: vi.fn()}) as unknown as QueryClient;

const buildDependencies = (overrides: Record<string, unknown> = {}) => ({
    currentComponentDefinition: versionOneDefinition,
    currentOperationName: 'sendMessage',
    deleteWorkflowNodeTestOutputMutation: {mutateAsync: vi.fn().mockResolvedValue(undefined)},
    environmentId: 1,
    fetchActionDefinition: vi.fn().mockResolvedValue(sendMessageDefinition),
    fetchClusterElementDefinition: vi.fn().mockResolvedValue(sendMessageDefinition),
    fetchTriggerDefinition: vi.fn().mockResolvedValue(sendMessageDefinition),
    newComponentVersion: 2,
    queryClient: buildQueryClient(),
    setCurrentOperationName: vi.fn(),
    updateWorkflowMutation: {} as never,
    workflowId: 'workflow-1',
    ...overrides,
});

describe('changeComponentVersion', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        mockCurrentNode = buildNode();

        mockGetComponentDefinition.mockResolvedValue(versionTwoDefinition);

        vi.mocked(saveWorkflowDefinition).mockImplementation(async ({onSuccess}) => onSuccess?.());
    });

    it('should do nothing when the node already uses the picked version', async () => {
        const dependencies = buildDependencies({newComponentVersion: 1});

        await changeComponentVersion(dependencies);

        expect(dependencies.queryClient.fetchQuery).not.toHaveBeenCalled();
        expect(mockSetOperationChangeInProgress).not.toHaveBeenCalled();
    });

    it('should do nothing when the picked version is not a number', async () => {
        const dependencies = buildDependencies({newComponentVersion: Number('not a version')});

        await changeComponentVersion(dependencies);

        expect(dependencies.queryClient.fetchQuery).not.toHaveBeenCalled();
    });

    it('should do nothing when no node is open in the panel', async () => {
        mockCurrentNode = undefined;

        const dependencies = buildDependencies();

        await changeComponentVersion(dependencies);

        expect(dependencies.queryClient.fetchQuery).not.toHaveBeenCalled();
    });

    it('should write the picked version into the node type and version', async () => {
        await changeComponentVersion(buildDependencies());

        expect(vi.mocked(saveWorkflowDefinition).mock.calls[0][0].nodeData).toMatchObject({
            operationName: 'sendMessage',
            type: 'slack/v2/sendMessage',
            version: 2,
        });

        expect(mockSetCurrentNode).toHaveBeenCalledWith(
            expect.objectContaining({type: 'slack/v2/sendMessage', version: 2})
        );
    });

    it('should carry configured parameters over and seed the defaults of new properties', async () => {
        await changeComponentVersion(buildDependencies());

        expect(vi.mocked(saveWorkflowDefinition).mock.calls[0][0].nodeData?.parameters).toEqual({
            channel: '#general',
            format: 'markdown',
        });
    });

    it('should keep the position of the node', async () => {
        await changeComponentVersion(buildDependencies());

        expect(vi.mocked(saveWorkflowDefinition).mock.calls[0][0].nodeData?.metadata).toEqual({
            ui: {nodePosition: {x: 10, y: 20}},
        });
    });

    it('should clear the in-progress flag once the workflow is saved', async () => {
        await changeComponentVersion(buildDependencies());

        expect(mockSetOperationChangeInProgress).toHaveBeenNthCalledWith(1, true);
        expect(mockSetOperationChangeInProgress).toHaveBeenLastCalledWith(false);
    });

    it('should fall back to the first action when the picked version dropped the current one', async () => {
        const dependencies = buildDependencies({currentOperationName: 'removedAction'});

        await changeComponentVersion(dependencies);

        expect(dependencies.fetchActionDefinition).toHaveBeenCalledWith('sendMessage', 2);
        expect(dependencies.setCurrentOperationName).toHaveBeenCalledWith('sendMessage');
    });

    it('should resolve a trigger node against the triggers of the picked version', async () => {
        mockCurrentNode = buildNode({operationName: 'newMessage', trigger: true});

        const dependencies = buildDependencies({currentOperationName: 'newMessage'});

        await changeComponentVersion(dependencies);

        expect(dependencies.fetchTriggerDefinition).toHaveBeenCalledWith('newMessage', 2);
        expect(dependencies.fetchActionDefinition).not.toHaveBeenCalled();
    });

    it('should route a cluster element through the cluster element save path', async () => {
        mockCurrentNode = buildNode({clusterElementType: 'model'});

        const dependencies = buildDependencies();

        await changeComponentVersion(dependencies);

        expect(dependencies.fetchClusterElementDefinition).toHaveBeenCalledWith('sendMessage', 2);
        expect(vi.mocked(saveClusterElementFieldChange).mock.calls[0][0]).toMatchObject({
            currentComponentDefinition: versionTwoDefinition,
            fieldUpdate: {field: 'operation', value: 'sendMessage'},
            parameters: {channel: '#general', format: 'markdown'},
        });
        expect(saveWorkflowDefinition).not.toHaveBeenCalled();
    });

    it('should route a task dispatcher subtask through the subtask save path', async () => {
        mockCurrentNode = buildNode({conditionData: {conditionCase: 'caseTrue', conditionId: 'condition_1', index: 0}});

        await changeComponentVersion(buildDependencies());

        expect(vi.mocked(saveTaskDispatcherSubtaskFieldChange).mock.calls[0][0]).toMatchObject({
            currentComponentDefinition: versionTwoDefinition,
            parameters: {channel: '#general', format: 'markdown'},
        });
        expect(saveWorkflowDefinition).not.toHaveBeenCalled();
    });

    it('should clear the in-progress flag when the picked version has no usable operation', async () => {
        mockGetComponentDefinition.mockResolvedValue({name: 'slack', version: 2});

        await changeComponentVersion(buildDependencies());

        expect(mockSetOperationChangeInProgress).toHaveBeenLastCalledWith(false);
        expect(saveWorkflowDefinition).not.toHaveBeenCalled();
    });

    it('should clear the in-progress flag when the operation definition cannot be resolved', async () => {
        const dependencies = buildDependencies({fetchActionDefinition: vi.fn().mockResolvedValue(undefined)});

        await changeComponentVersion(dependencies);

        expect(mockSetOperationChangeInProgress).toHaveBeenLastCalledWith(false);
        expect(saveWorkflowDefinition).not.toHaveBeenCalled();
    });

    it('should clear the in-progress flag when the component definition cannot be fetched', async () => {
        mockGetComponentDefinition.mockRejectedValue(new Error('network down'));

        await changeComponentVersion(buildDependencies());

        expect(mockSetOperationChangeInProgress).toHaveBeenLastCalledWith(false);
        expect(saveWorkflowDefinition).not.toHaveBeenCalled();
    });

    it('should clear the in-progress flag when the test output cannot be deleted', async () => {
        const dependencies = buildDependencies({
            deleteWorkflowNodeTestOutputMutation: {
                mutateAsync: vi.fn().mockRejectedValue(new Error('request failed')),
            },
        });

        await changeComponentVersion(dependencies);

        expect(mockSetOperationChangeInProgress).toHaveBeenLastCalledWith(false);
        expect(saveWorkflowDefinition).not.toHaveBeenCalled();
    });

    it('should clear the in-progress flag when saving the workflow fails', async () => {
        vi.mocked(saveWorkflowDefinition).mockRejectedValue(new Error('save failed'));

        await changeComponentVersion(buildDependencies());

        expect(mockSetOperationChangeInProgress).toHaveBeenLastCalledWith(false);
    });
});
