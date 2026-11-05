import {ClusterElementItemType, ClusterElementsType} from '@/shared/types';
import {describe, expect, it} from 'vitest';

import {removeClusterElementPosition} from './removeClusterElementPosition';

function makeClusterElement(
    name: string,
    nodePosition?: {x: number; y: number},
    nested?: ClusterElementsType
): ClusterElementItemType {
    return {
        clusterElements: nested,
        metadata: nodePosition ? {ui: {nodePosition}} : undefined,
        name,
        type: `test/v1/${name}`,
    };
}

describe('removeClusterElementPosition', () => {
    it('clears the position of the clicked element in an array and keeps the others', () => {
        const clusterElements: ClusterElementsType = {
            tools: [makeClusterElement('tool_1', {x: 1, y: 1}), makeClusterElement('tool_2', {x: 2, y: 2})],
        };

        const result = removeClusterElementPosition(clusterElements, 'tool_1');
        const tools = result.tools as ClusterElementItemType[];

        expect(tools[0].metadata?.ui?.nodePosition).toBeUndefined();
        expect(tools[1].metadata?.ui?.nodePosition).toEqual({x: 2, y: 2});
    });

    it('clears the position of a clicked single element', () => {
        const clusterElements: ClusterElementsType = {model: makeClusterElement('model_1', {x: 4, y: 4})};

        const result = removeClusterElementPosition(clusterElements, 'model_1');

        expect((result.model as ClusterElementItemType).metadata?.ui?.nodePosition).toBeUndefined();
    });

    it('keeps other ui metadata on the clicked element', () => {
        const clusterElements: ClusterElementsType = {
            model: {
                metadata: {ui: {nodePosition: {x: 4, y: 4}, placeholderPositions: {tools: {x: 9, y: 9}}}},
                name: 'model_1',
                type: 'test/v1/model',
            },
        };

        const result = removeClusterElementPosition(clusterElements, 'model_1');

        expect((result.model as ClusterElementItemType).metadata?.ui?.placeholderPositions).toEqual({
            tools: {x: 9, y: 9},
        });
    });

    it('clears positions of clicked elements nested in single and array elements', () => {
        const clusterElements: ClusterElementsType = {
            model: makeClusterElement('agent_1', {x: 0, y: 0}, {tools: [makeClusterElement('target', {x: 5, y: 5})]}),
            tools: [makeClusterElement('agent_2', {x: 0, y: 0}, {model: makeClusterElement('target', {x: 6, y: 6})})],
        };

        const result = removeClusterElementPosition(clusterElements, 'target');

        const singleParent = result.model as ClusterElementItemType;
        const arrayParent = (result.tools as ClusterElementItemType[])[0];

        expect(singleParent.metadata?.ui?.nodePosition).toEqual({x: 0, y: 0});
        expect(
            (singleParent.clusterElements?.tools as ClusterElementItemType[])[0].metadata?.ui?.nodePosition
        ).toBeUndefined();
        expect(arrayParent.metadata?.ui?.nodePosition).toEqual({x: 0, y: 0});
        expect(
            (arrayParent.clusterElements?.model as ClusterElementItemType).metadata?.ui?.nodePosition
        ).toBeUndefined();
    });

    it('does not mutate the input', () => {
        const clusterElements: ClusterElementsType = {model: makeClusterElement('model_1', {x: 4, y: 4})};

        const snapshot = structuredClone(clusterElements);

        removeClusterElementPosition(clusterElements, 'model_1');

        expect(clusterElements).toEqual(snapshot);
    });

    it('drops null entries', () => {
        expect(removeClusterElementPosition({model: null}, 'model_1')).toEqual({});
    });
});
