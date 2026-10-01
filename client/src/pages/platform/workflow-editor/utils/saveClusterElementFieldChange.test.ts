import {ComponentDefinition} from '@/shared/middleware/platform/configuration';
import {NodeDataType} from '@/shared/types';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import saveClusterElementFieldChange from './saveClusterElementFieldChange';

const {saveWorkflowDefinitionMock} = vi.hoisted(() => ({saveWorkflowDefinitionMock: vi.fn()}));

vi.mock('./saveWorkflowDefinition', () => ({default: saveWorkflowDefinitionMock}));

const DEFINITION = JSON.stringify({
    tasks: [
        {
            clusterElements: {
                tools: [{label: 'Agile CRM', name: 'agileCrm_1', parameters: {}, type: 'agileCrm/v1/createDeal'}],
            },
            name: 'aiAgent_1',
            type: 'aiAgent/v1/chat',
        },
    ],
});

describe('saveClusterElementFieldChange', () => {
    beforeEach(() => {
        saveWorkflowDefinitionMock.mockReset();
        useWorkflowDataStore.setState({workflow: {definition: DEFINITION, id: 'workflow-1'}} as Parameters<
            typeof useWorkflowDataStore.setState
        >[0]);
        useWorkflowEditorStore.setState({rootClusterElementNodeData: undefined});
        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {
                clusterElementType: 'tools',
                componentName: 'agileCrm',
                name: 'agileCrm_1',
                parentClusterRootId: 'aiAgent_1',
                topLevelClusterRootId: 'aiAgent_1',
                workflowNodeName: 'agileCrm_1',
            } as NodeDataType,
        });
    });

    it('saves a label change against the root resolved from the node when no root is in the store', () => {
        saveClusterElementFieldChange({
            currentComponentDefinition: {name: 'agileCrm', version: 1} as ComponentDefinition,
            fieldUpdate: {field: 'label', value: 'Deals'},
            updateWorkflowMutation: {mutate: vi.fn()} as never,
        });

        expect(saveWorkflowDefinitionMock).toHaveBeenCalledTimes(1);

        const {nodeData} = saveWorkflowDefinitionMock.mock.calls[0][0];

        expect(nodeData.workflowNodeName).toBe('aiAgent_1');
        expect(nodeData.componentName).toBe('aiAgent');
        expect(nodeData.clusterElements.tools[0].label).toBe('Deals');
    });
});
