import {NodeDataType} from '@/shared/types';
import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it} from 'vitest';

import useWorkflowDataStore from '../../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../../stores/useWorkflowEditorStore';
import useWorkflowIssuesStore from '../../stores/useWorkflowIssuesStore';
import useWorkflowNodeDetailsPanelStore from '../../stores/useWorkflowNodeDetailsPanelStore';
import WorkflowIssuesSidebar from '../WorkflowIssuesSidebar';

const agentNodeData = {
    clusterRoot: true,
    componentName: 'aiAgent',
    name: 'aiAgent_1',
    workflowNodeName: 'aiAgent_1',
} as NodeDataType;

describe('WorkflowIssuesSidebar with the real stores', () => {
    beforeEach(() => {
        useWorkflowIssuesStore.getState().reset();
        useWorkflowEditorStore.setState({clusterElementsCanvasOpen: false});
        useWorkflowNodeDetailsPanelStore.setState({currentNode: undefined, workflowNodeDetailsPanelOpen: false});
        useWorkflowDataStore.setState({
            nodes: [{data: agentNodeData, id: 'aiAgent_1', position: {x: 0, y: 0}}],
            workflow: {
                definition: '{}',
                id: 'wf-1',
                nodeNames: ['aiAgent_1'],
                tasks: [
                    {
                        clusterElements: {model: {type: 'openAi/v1/model', workflowNodeName: 'openAi_1'}},
                        name: 'aiAgent_1',
                        type: 'aiAgent/v1/chat',
                    },
                ],
            },
        });
    });

    it('opens the cluster elements canvas for the root of a cluster element issue', () => {
        useWorkflowIssuesStore.getState().setValidatorIssues([
            {
                kind: 'MISSING_CONNECTION',
                message: 'Missing required connection: OpenAI',
                nodeName: 'openAi_1',
                severity: 'ERROR',
                source: 'VALIDATOR',
            },
        ]);

        render(<WorkflowIssuesSidebar visible />);

        fireEvent.click(screen.getByText('Missing required connection: OpenAI'));

        expect(useWorkflowNodeDetailsPanelStore.getState().currentNode?.name).toBe('aiAgent_1');
        expect(useWorkflowNodeDetailsPanelStore.getState().workflowNodeDetailsPanelOpen).toBe(true);
        expect(useWorkflowEditorStore.getState().clusterElementsCanvasOpen).toBe(true);
    });
});
