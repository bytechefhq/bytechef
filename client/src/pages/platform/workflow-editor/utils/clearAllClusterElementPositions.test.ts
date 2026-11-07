import {ClusterElementItemType, ClusterElementsType, NodeDataType} from '@/shared/types';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import clearAllClusterElementPositions, {clearClusterElementPositions} from './clearAllClusterElementPositions';

const {saveWorkflowDefinitionMock} = vi.hoisted(() => ({saveWorkflowDefinitionMock: vi.fn()}));

vi.mock('./saveWorkflowDefinition', () => ({default: saveWorkflowDefinitionMock}));

function makeClusterElement(
    name: string,
    nodePosition?: {x: number; y: number},
    nested?: ClusterElementsType
): ClusterElementItemType {
    return {
        clusterElements: nested,
        metadata: nodePosition ? {ui: {nodePosition}} : undefined,
        name,
        type: `test/${name}`,
    };
}

describe('clearClusterElementPositions', () => {
    it('should clear positions from array-based cluster elements', () => {
        const clusterElements: ClusterElementsType = {
            steps: [makeClusterElement('step_1', {x: 100, y: 200}), makeClusterElement('step_2', {x: 300, y: 400})],
        };

        const result = clearClusterElementPositions(clusterElements);
        const steps = result.steps as ClusterElementItemType[];

        expect(steps[0].metadata?.ui?.nodePosition).toBeUndefined();
        expect(steps[1].metadata?.ui?.nodePosition).toBeUndefined();
    });

    it('should clear positions from object-based (single) cluster elements', () => {
        const clusterElements: ClusterElementsType = {
            trigger: makeClusterElement('trigger_1', {x: 50, y: 60}),
        };

        const result = clearClusterElementPositions(clusterElements);
        const trigger = result.trigger as ClusterElementItemType;

        expect(trigger.metadata?.ui?.nodePosition).toBeUndefined();
    });

    it('should handle mixed array and object entries', () => {
        const clusterElements: ClusterElementsType = {
            source: makeClusterElement('source_1', {x: 10, y: 20}),
            steps: [makeClusterElement('step_1', {x: 100, y: 200})],
        };

        const result = clearClusterElementPositions(clusterElements);
        const source = result.source as ClusterElementItemType;
        const steps = result.steps as ClusterElementItemType[];

        expect(source.metadata?.ui?.nodePosition).toBeUndefined();
        expect(steps[0].metadata?.ui?.nodePosition).toBeUndefined();
    });

    it('should recursively clear nested cluster element positions', () => {
        const nestedClusterElements: ClusterElementsType = {
            innerSteps: [makeClusterElement('inner_step_1', {x: 500, y: 600})],
        };

        const clusterElements: ClusterElementsType = {
            steps: [makeClusterElement('step_1', {x: 100, y: 200}, nestedClusterElements)],
        };

        const result = clearClusterElementPositions(clusterElements);
        const steps = result.steps as ClusterElementItemType[];
        const innerSteps = steps[0].clusterElements?.innerSteps as ClusterElementItemType[];

        expect(steps[0].metadata?.ui?.nodePosition).toBeUndefined();
        expect(innerSteps[0].metadata?.ui?.nodePosition).toBeUndefined();
    });

    it('should recursively clear nested cluster elements in object entries', () => {
        const nestedClusterElements: ClusterElementsType = {
            innerSource: makeClusterElement('inner_source', {x: 500, y: 600}),
        };

        const clusterElements: ClusterElementsType = {
            source: makeClusterElement('source_1', {x: 100, y: 200}, nestedClusterElements),
        };

        const result = clearClusterElementPositions(clusterElements);
        const source = result.source as ClusterElementItemType;
        const innerSource = source.clusterElements?.innerSource as ClusterElementItemType;

        expect(source.metadata?.ui?.nodePosition).toBeUndefined();
        expect(innerSource.metadata?.ui?.nodePosition).toBeUndefined();
    });

    it('should handle elements without positions', () => {
        const clusterElements: ClusterElementsType = {
            steps: [makeClusterElement('step_1')],
        };

        const result = clearClusterElementPositions(clusterElements);
        const steps = result.steps as ClusterElementItemType[];

        expect(steps[0].metadata?.ui?.nodePosition).toBeUndefined();
        expect(steps[0].name).toBe('step_1');
    });

    it('should handle empty cluster elements', () => {
        const result = clearClusterElementPositions({});

        expect(result).toEqual({});
    });

    it('should handle null entries by skipping them', () => {
        const clusterElements: ClusterElementsType = {
            empty: null,
            steps: [makeClusterElement('step_1', {x: 100, y: 200})],
        };

        const result = clearClusterElementPositions(clusterElements);
        const steps = result.steps as ClusterElementItemType[];

        // null entry should be skipped (not in result)
        expect(result.empty).toBeUndefined();
        expect(steps[0].metadata?.ui?.nodePosition).toBeUndefined();
    });

    it('should preserve non-position metadata', () => {
        const element: ClusterElementItemType = {
            metadata: {
                ui: {
                    nodePosition: {x: 100, y: 200},
                    placeholderPositions: {ph_1: {x: 50, y: 60}},
                },
            },
            name: 'step_1',
            type: 'test/step_1',
        };

        const clusterElements: ClusterElementsType = {
            steps: [element],
        };

        const result = clearClusterElementPositions(clusterElements);
        const steps = result.steps as ClusterElementItemType[];

        expect(steps[0].metadata?.ui?.nodePosition).toBeUndefined();
        expect(steps[0].metadata?.ui?.placeholderPositions).toEqual({ph_1: {x: 50, y: 60}});
    });

    it('should handle deeply nested cluster elements (3 levels)', () => {
        const level3: ClusterElementsType = {
            deepStep: makeClusterElement('deep_step', {x: 999, y: 888}),
        };
        const level2: ClusterElementsType = {
            midStep: makeClusterElement('mid_step', {x: 500, y: 600}, level3),
        };
        const level1: ClusterElementsType = {
            topStep: makeClusterElement('top_step', {x: 100, y: 200}, level2),
        };

        const result = clearClusterElementPositions(level1);
        const topStep = result.topStep as ClusterElementItemType;
        const midStep = topStep.clusterElements?.midStep as ClusterElementItemType;
        const deepStep = midStep.clusterElements?.deepStep as ClusterElementItemType;

        expect(topStep.metadata?.ui?.nodePosition).toBeUndefined();
        expect(midStep.metadata?.ui?.nodePosition).toBeUndefined();
        expect(deepStep.metadata?.ui?.nodePosition).toBeUndefined();
    });
});

describe('clearAllClusterElementPositions', () => {
    beforeEach(() => {
        saveWorkflowDefinitionMock.mockReset();
        useWorkflowDataStore.setState({
            workflow: {
                definition: JSON.stringify({
                    tasks: [],
                    triggers: [
                        {
                            clusterElements: {
                                voiceAgent: {
                                    metadata: {ui: {nodePosition: {x: 40, y: 20}}},
                                    name: 'voiceAgent_1',
                                    parameters: {},
                                    type: 'deepgram/v1/voiceAgent',
                                },
                            },
                            name: 'trigger_1',
                            parameters: {},
                            type: 'browser/v1/voiceSession',
                        },
                    ],
                }),
                id: 'workflow-1',
            },
        } as Parameters<typeof useWorkflowDataStore.setState>[0]);
        useWorkflowEditorStore.setState({
            rootClusterElementNodeData: {componentName: 'browser', workflowNodeName: 'trigger_1'} as NodeDataType,
        });
    });

    it('clears the element positions of a trigger cluster root', () => {
        clearAllClusterElementPositions({updateWorkflowMutation: {mutate: vi.fn()} as never});

        expect(saveWorkflowDefinitionMock).toHaveBeenCalledTimes(1);

        const {nodeData} = saveWorkflowDefinitionMock.mock.calls[0][0];

        expect(nodeData.workflowNodeName).toBe('trigger_1');
        expect(nodeData.clusterElements.voiceAgent.metadata.ui.nodePosition).toBeUndefined();
    });
});
