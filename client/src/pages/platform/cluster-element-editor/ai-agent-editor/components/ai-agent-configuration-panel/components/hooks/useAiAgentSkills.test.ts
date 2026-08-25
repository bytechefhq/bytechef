import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {
    saveWorkflowDefinitionMock,
    setRootClusterElementNodeDataMock,
    useWorkflowDataStoreMock,
    useWorkflowEditorStoreMock,
} = vi.hoisted(() => ({
    saveWorkflowDefinitionMock: vi.fn(),
    setRootClusterElementNodeDataMock: vi.fn(),
    useWorkflowDataStoreMock: vi.fn(),
    useWorkflowEditorStoreMock: vi.fn(),
}));

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({updateWorkflowMutation: {mutate: vi.fn()}}),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowDataStore', () => ({
    default: useWorkflowDataStoreMock,
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowEditorStore', () => ({
    default: useWorkflowEditorStoreMock,
}));

vi.mock('@/pages/platform/workflow-editor/utils/saveWorkflowDefinition', () => ({
    default: saveWorkflowDefinitionMock,
}));

import useAiAgentSkills from './useAiAgentSkills';

const modelElement = {
    label: 'Claude',
    name: 'anthropic_1',
    parameters: {model: 'claude-opus-5'},
    type: 'anthropic/v1/model',
};

const sheetsTool = {
    label: 'Sheets',
    name: 'googleSheets_1',
    parameters: {spreadsheetId: 'abc'},
    type: 'googleSheets/v1/insertRow',
};

const skillsTool = {
    label: 'Skills',
    name: 'skillsTool_1',
    parameters: {skills: [7]},
    type: 'aiAgentUtils/v1/skillsTool',
};

const definitionClusterElements = {
    model: modelElement,
    tools: [sheetsTool, skillsTool],
};

const buildDefinition = () =>
    JSON.stringify({
        tasks: [
            {
                clusterElements: definitionClusterElements,
                name: 'aiAgent_1',
                parameters: {systemPrompt: 'Be helpful'},
                type: 'aiAgent/v1/chat',
            },
        ],
    });

// Seeded once when the AI Agent editor opened, before the tools above were added.
const staleRootClusterElementNodeData = {
    clusterElements: {tools: []},
    componentName: 'aiAgent',
    name: 'aiAgent_1',
    operationName: 'chat',
    type: 'aiAgent/v1/chat',
    workflowNodeName: 'aiAgent_1',
};

const mockStores = (rootClusterElementNodeData: unknown = staleRootClusterElementNodeData) => {
    useWorkflowEditorStoreMock.mockImplementation((selector: (state: unknown) => unknown) =>
        selector({
            rootClusterElementNodeData,
            setRootClusterElementNodeData: setRootClusterElementNodeDataMock,
        })
    );

    useWorkflowDataStoreMock.mockImplementation((selector: (state: unknown) => unknown) =>
        selector({workflow: {definition: buildDefinition(), id: 'workflow-1'}})
    );
};

describe('useAiAgentSkills', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        mockStores();
    });

    it('reads the skill ids off the definition rather than the stale seeded snapshot', () => {
        const {result} = renderHook(() => useAiAgentSkills());

        expect(result.current.skillIds).toEqual([7]);
    });

    it('keeps the model and the other tools when the skill ids change', () => {
        const {result} = renderHook(() => useAiAgentSkills());

        result.current.updateSkillIds([7, 9]);

        const [{nodeData}] = saveWorkflowDefinitionMock.mock.calls[0];

        expect(nodeData.clusterElements.model).toEqual(modelElement);
        expect(nodeData.clusterElements.tools).toContainEqual(sheetsTool);
        expect(nodeData.clusterElements.tools).toContainEqual({...skillsTool, parameters: {skills: [7, 9]}});
    });

    it('keeps the model and the other tools when every skill is removed', () => {
        const {result} = renderHook(() => useAiAgentSkills());

        result.current.updateSkillIds([]);

        const [{nodeData}] = saveWorkflowDefinitionMock.mock.calls[0];

        expect(nodeData.clusterElements.model).toEqual(modelElement);
        expect(nodeData.clusterElements.tools).toEqual([sheetsTool]);
    });

    it('keeps the definition cluster elements when the seeded root node data carries none at all', () => {
        mockStores({
            componentName: 'aiAgent',
            name: 'aiAgent_1',
            operationName: 'chat',
            type: 'aiAgent/v1/chat',
            workflowNodeName: 'aiAgent_1',
        });

        const {result} = renderHook(() => useAiAgentSkills());

        result.current.updateSkillIds([9]);

        const [{nodeData}] = saveWorkflowDefinitionMock.mock.calls[0];

        expect(nodeData.clusterElements.model).toEqual(modelElement);
        expect(nodeData.clusterElements.tools).toContainEqual(sheetsTool);
    });
});
