import {ComponentDefinition, Workflow} from '@/shared/middleware/platform/configuration';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {QueryClient} from '@tanstack/react-query';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowDataStore from '../../stores/useWorkflowDataStore';
import addClusterElementToClusterRoot from '../addClusterElementToClusterRoot';
import saveWorkflowDefinition from '../saveWorkflowDefinition';

vi.mock('../saveWorkflowDefinition', () => ({default: vi.fn()}));

const AI_AGENT_COMPONENT_DEFINITION = {
    clusterElementTypes: [
        {key: 'model', label: 'Model', multipleElements: false, name: 'MODEL', required: true},
        {key: 'tools', label: 'Tools', multipleElements: true, name: 'TOOLS', required: false},
    ],
    name: 'aiAgent',
} as unknown as ComponentDefinition;

const WORKFLOW = {
    definition: JSON.stringify({
        tasks: [
            {
                clusterElements: {tools: [{name: 'gmail_1', type: 'gmail/v1/getEmail'}]},
                name: 'aiAgent_1',
                type: 'aiAgent/v1/chat',
            },
        ],
    }),
    id: 'workflow-1',
} as Workflow;

const queryClient = {query: vi.fn()} as unknown as QueryClient;

const updateWorkflowMutation = {mutate: vi.fn()} as unknown as UpdateWorkflowMutationType;

const addOpenAiModel = (workflow: Workflow = WORKFLOW) =>
    addClusterElementToClusterRoot({
        clusterElementName: 'model',
        clusterElementType: 'model',
        clusterRootComponentDefinition: AI_AGENT_COMPONENT_DEFINITION,
        clusterRootId: 'aiAgent_1',
        componentDefinition: {clusterRoot: false, name: 'openAi', version: 1},
        isMultipleElements: false,
        label: 'OpenAI',
        queryClient,
        sourceNodeId: 'aiAgent_1',
        updateWorkflowMutation,
        workflow,
    });

describe('addClusterElementToClusterRoot', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        useWorkflowDataStore.setState({
            workflow: {...WORKFLOW, nodeNames: ['aiAgent_1', 'gmail_1'], version: 1},
        });

        vi.mocked(queryClient.query).mockResolvedValue({
            properties: [{defaultValue: 'gpt-5', name: 'model', type: 'STRING'}],
        });
    });

    it('saves the cluster root with the new element added beside its existing elements', async () => {
        const nestedClusterElements = await addOpenAiModel();

        expect(saveWorkflowDefinition).toHaveBeenCalledOnce();

        const {nodeData} = vi.mocked(saveWorkflowDefinition).mock.calls[0][0];

        expect(nodeData).toEqual(
            expect.objectContaining({componentName: 'aiAgent', name: 'aiAgent_1', workflowNodeName: 'aiAgent_1'})
        );
        expect(nodeData?.clusterElements).toEqual(nestedClusterElements);
        expect(nodeData?.clusterElements).toEqual(
            expect.objectContaining({
                model: expect.objectContaining({
                    label: 'OpenAI',
                    name: expect.stringMatching(/^openAi_\d+$/),
                    parameters: {model: 'gpt-5'},
                    type: 'openAi/v1/model',
                }),
                tools: [expect.objectContaining({name: 'gmail_1'})],
            })
        );
    });

    it('fetches the definition of the element it adds', async () => {
        await addOpenAiModel();

        expect(queryClient.query).toHaveBeenCalledWith(
            expect.objectContaining({
                queryKey: expect.arrayContaining(['openAi', 1, 'model', 'MODEL']),
            })
        );
    });

    it('saves nothing when the cluster root task is not in the workflow', async () => {
        const result = await addOpenAiModel({...WORKFLOW, definition: JSON.stringify({tasks: []})});

        expect(result).toBeUndefined();
        expect(saveWorkflowDefinition).not.toHaveBeenCalled();
    });
});
