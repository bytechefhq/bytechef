import {renderHook, waitFor} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => {
    const panelStoreState = {
        currentNode: {} as Record<string, unknown>,
        setFocusedInput: vi.fn(),
        workflowNodeDetailsPanelOpen: true,
    };

    const dataStoreState = {
        dataPills: [] as Array<{id: string; value: string}>,
        workflow: {
            definition: JSON.stringify({tasks: []}),
            id: 'workflow-1',
            tasks: [] as Array<Record<string, unknown>>,
            triggers: [] as Array<Record<string, unknown>>,
        },
    };

    return {dataStoreState, panelStoreState};
});

vi.mock('../../../../utils/saveProperty', () => ({default: vi.fn()}));

vi.mock('../../../../utils/deleteProperty', () => ({default: vi.fn()}));

vi.mock('../../../../stores/useWorkflowNodeDetailsPanelStore', () => ({
    default: Object.assign(
        (selector: (state: typeof hoisted.panelStoreState) => unknown) => selector(hoisted.panelStoreState),
        {getState: () => hoisted.panelStoreState}
    ),
}));

vi.mock('../../../../stores/useWorkflowDataStore', () => ({
    default: Object.assign(
        (selector: (state: typeof hoisted.dataStoreState) => unknown) => selector(hoisted.dataStoreState),
        {getState: () => hoisted.dataStoreState}
    ),
}));

vi.mock('../../../../stores/useDataPillPanelStore', () => {
    const dataPillPanelStoreState = {dataPillPanelHasContent: true, setDataPillPanelOpen: vi.fn()};

    return {
        default: Object.assign(
            (selector: (state: typeof dataPillPanelStoreState) => unknown) => selector(dataPillPanelStoreState),
            {getState: () => dataPillPanelStoreState}
        ),
    };
});

vi.mock('../../../../stores/useWorkflowEditorStore', () => ({
    default: (selector: (state: {rootClusterElementNodeData: undefined}) => unknown) =>
        selector({rootClusterElementNodeData: undefined}),
}));

vi.mock('../../../../providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({
        deleteClusterElementParameterMutation: undefined,
        deleteWorkflowNodeParameterMutation: {mutateAsync: vi.fn()},
        updateClusterElementParameterMutation: undefined,
        updateWorkflowNodeParameterMutation: {mutateAsync: vi.fn()},
    }),
}));

/* eslint-disable @typescript-eslint/no-explicit-any */
const renderProperty = async (property: Record<string, unknown>) => {
    const {useProperty} = await import('../useProperty');

    return renderHook(() => useProperty({property: property as any} as any));
};

describe('useProperty under a trigger cluster root', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        hoisted.panelStoreState.currentNode = {
            clusterElementType: 'voiceAgent',
            componentName: 'openAi',
            metadata: {ui: {}},
            name: 'openAiVoiceAgent_1',
            operationName: 'voiceAgent',
            parameters: {},
            topLevelClusterRootId: 'trigger_1',
            workflowNodeName: 'openAiVoiceAgent_1',
        };

        hoisted.dataStoreState.workflow = {
            definition: JSON.stringify({
                tasks: [],
                triggers: [
                    {
                        clusterElements: {
                            voiceAgent: {
                                name: 'openAiVoiceAgent_1',
                                parameters: {voice: 'verse'},
                                type: 'openAi/v1/voiceAgent',
                            },
                        },
                        name: 'trigger_1',
                        type: 'browser/v1/voiceSession',
                    },
                ],
            }),
            id: 'workflow-1',
            tasks: [],
            triggers: [],
        };
    });

    it('resolves a Voice Agent field from the trigger that owns the element', async () => {
        const {result} = await renderProperty({controlType: 'SELECT', name: 'voice', type: 'STRING'});

        await waitFor(
            () => {
                expect(result.current.selectValue).toBe('verse');
            },
            {interval: 5, timeout: 5000}
        );
    });

    it('resolves nothing for an element whose trigger root does not hold it', async () => {
        hoisted.panelStoreState.currentNode = {
            ...hoisted.panelStoreState.currentNode,
            topLevelClusterRootId: 'trigger_2',
        };

        const {result} = await renderProperty({controlType: 'SELECT', name: 'voice', type: 'STRING'});

        expect(result.current.selectValue).toBe('');
    });
});
