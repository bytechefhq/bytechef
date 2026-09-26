import {CLUSTER_ELEMENT_TYPE_TOOLS} from '@/shared/constants';
import {describe, expect, it} from 'vitest';

import resolveShowOutputTab from '../resolveShowOutputTab';

describe('resolveShowOutputTab', () => {
    it('shows the tab for an action that declares no output, so it can still be tested', () => {
        expect(resolveShowOutputTab({operationDefinition: {outputDefined: false}})).toBe(true);
    });

    it('shows the tab for an action that declares an output', () => {
        expect(resolveShowOutputTab({operationDefinition: {outputDefined: true}})).toBe(true);
    });

    it('shows the tab before the operation definition has loaded', () => {
        expect(resolveShowOutputTab({})).toBe(true);
    });

    it('hides the tab for a task dispatcher that declares no output', () => {
        expect(resolveShowOutputTab({operationDefinition: {outputDefined: false}, taskDispatcher: true})).toBe(false);
    });

    it('shows the tab for a task dispatcher that declares an output', () => {
        expect(resolveShowOutputTab({operationDefinition: {outputDefined: true}, taskDispatcher: true})).toBe(true);
    });

    it('shows the tab for a tool without a declared output inside a cluster root', () => {
        expect(
            resolveShowOutputTab({
                clusterElementType: CLUSTER_ELEMENT_TYPE_TOOLS,
                clusterRootWorkflowNodeName: 'aiAgent_1',
                operationDefinition: {outputDefined: false},
            })
        ).toBe(true);
    });

    it('hides the tab for a tool when no cluster root is resolved', () => {
        expect(resolveShowOutputTab({clusterElementType: CLUSTER_ELEMENT_TYPE_TOOLS})).toBe(false);
    });

    it('hides the tab for a cluster element that is not a tool', () => {
        expect(
            resolveShowOutputTab({
                clusterElementType: 'model',
                clusterRootWorkflowNodeName: 'aiAgent_1',
                operationDefinition: {outputDefined: true},
            })
        ).toBe(false);
    });
});
