import {ClusterElementItemType, ClusterElementsType} from '@/shared/types';
import {describe, expect, it} from 'vitest';

import updateClusterElementsPositions from './updateClusterElementsPositions';

function makeClusterElement(
    name: string,
    nodePosition?: {x: number; y: number},
    nested?: ClusterElementsType
): ClusterElementItemType {
    return {
        clusterElements: nested,
        metadata: nodePosition ? {ui: {nodePosition, placeholderPositions: {tools: {x: 1, y: 2}}}} : undefined,
        name,
        type: `test/v1/${name}`,
    };
}

describe('updateClusterElementsPositions', () => {
    it('sets the position of the moved element inside an array', () => {
        const clusterElements: ClusterElementsType = {
            tools: [makeClusterElement('tool_1', {x: 0, y: 0}), makeClusterElement('tool_2', {x: 5, y: 5})],
        };

        const result = updateClusterElementsPositions({
            clusterElements,
            movedClusterElementId: 'tool_2',
            nodePositions: {tool_1: {x: 99, y: 99}, tool_2: {x: 10, y: 20}},
        });

        const tools = result.tools as ClusterElementItemType[];

        expect(tools[0].metadata?.ui?.nodePosition).toEqual({x: 0, y: 0});
        expect(tools[1].metadata?.ui).toEqual({
            nodePosition: {x: 10, y: 20},
            placeholderPositions: {tools: {x: 1, y: 2}},
        });
    });

    it('sets the position of the moved single element and creates metadata when absent', () => {
        const clusterElements: ClusterElementsType = {model: makeClusterElement('model_1')};

        const result = updateClusterElementsPositions({
            clusterElements,
            movedClusterElementId: 'model_1',
            nodePositions: {model_1: {x: 3, y: 4}},
        });

        expect((result.model as ClusterElementItemType).metadata).toEqual({ui: {nodePosition: {x: 3, y: 4}}});
    });

    it('leaves the moved element unchanged when no position is recorded for it', () => {
        const clusterElements: ClusterElementsType = {model: makeClusterElement('model_1', {x: 1, y: 1})};

        const result = updateClusterElementsPositions({
            clusterElements,
            movedClusterElementId: 'model_1',
            nodePositions: {},
        });

        expect(result.model).toEqual(clusterElements.model);
    });

    it('updates a moved element nested inside a single element', () => {
        const clusterElements: ClusterElementsType = {
            tools: makeClusterElement('agent_1', {x: 0, y: 0}, {model: makeClusterElement('model_2', {x: 0, y: 0})}),
        };

        const result = updateClusterElementsPositions({
            clusterElements,
            movedClusterElementId: 'model_2',
            nodePositions: {model_2: {x: 7, y: 8}},
        });

        const agent = result.tools as ClusterElementItemType;

        expect(agent.metadata?.ui?.nodePosition).toEqual({x: 0, y: 0});
        expect((agent.clusterElements?.model as ClusterElementItemType).metadata?.ui?.nodePosition).toEqual({
            x: 7,
            y: 8,
        });
    });

    it('updates a moved element nested inside an array element', () => {
        const clusterElements: ClusterElementsType = {
            tools: [makeClusterElement('agent_1', undefined, {tools: [makeClusterElement('tool_9', {x: 0, y: 0})]})],
        };

        const result = updateClusterElementsPositions({
            clusterElements,
            movedClusterElementId: 'tool_9',
            nodePositions: {tool_9: {x: 11, y: 12}},
        });

        const agent = (result.tools as ClusterElementItemType[])[0];
        const nestedTool = (agent.clusterElements?.tools as ClusterElementItemType[])[0];

        expect(nestedTool.metadata?.ui?.nodePosition).toEqual({x: 11, y: 12});
    });

    it('does not mutate the input', () => {
        const clusterElements: ClusterElementsType = {
            tools: [makeClusterElement('tool_1', {x: 0, y: 0})],
        };

        const snapshot = structuredClone(clusterElements);

        updateClusterElementsPositions({
            clusterElements,
            movedClusterElementId: 'tool_1',
            nodePositions: {tool_1: {x: 50, y: 50}},
        });

        expect(clusterElements).toEqual(snapshot);
    });

    it('drops null entries', () => {
        const result = updateClusterElementsPositions({
            clusterElements: {model: null, tools: []},
            movedClusterElementId: 'tool_1',
            nodePositions: {},
        });

        expect(result).toEqual({tools: []});
    });
});
