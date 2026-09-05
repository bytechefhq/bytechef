import {beforeEach, describe, expect, it} from 'vitest';

import useClusterFrameCollapsedStore from './useClusterFrameCollapsedStore';

const WORKFLOW_ID = 'workflow-1';
const OTHER_WORKFLOW_ID = 'workflow-2';
const ROOT_ID = 'aiAgent_1';

describe('useClusterFrameCollapsedStore', () => {
    beforeEach(() => {
        useClusterFrameCollapsedStore.setState({collapsedByWorkflowId: {}});
        window.localStorage.clear();
    });

    it('reports a root nobody has touched as expanded', () => {
        expect(useClusterFrameCollapsedStore.getState().collapsedByWorkflowId[WORKFLOW_ID]?.[ROOT_ID]).toBeUndefined();
    });

    it('collapses a root under its own workflow id', () => {
        useClusterFrameCollapsedStore.getState().setClusterFrameCollapsed(WORKFLOW_ID, ROOT_ID, true);

        expect(useClusterFrameCollapsedStore.getState().collapsedByWorkflowId).toEqual({
            [WORKFLOW_ID]: {[ROOT_ID]: true},
        });
    });

    it('deletes the entry when a root is expanded rather than storing false', () => {
        useClusterFrameCollapsedStore.getState().setClusterFrameCollapsed(WORKFLOW_ID, 'aiAgent_2', true);
        useClusterFrameCollapsedStore.getState().setClusterFrameCollapsed(WORKFLOW_ID, ROOT_ID, true);

        useClusterFrameCollapsedStore.getState().setClusterFrameCollapsed(WORKFLOW_ID, ROOT_ID, false);

        expect(useClusterFrameCollapsedStore.getState().collapsedByWorkflowId).toEqual({
            [WORKFLOW_ID]: {aiAgent_2: true},
        });
    });

    it('drops a workflow entirely once its last collapsed root expands', () => {
        useClusterFrameCollapsedStore.getState().setClusterFrameCollapsed(WORKFLOW_ID, ROOT_ID, true);

        useClusterFrameCollapsedStore.getState().setClusterFrameCollapsed(WORKFLOW_ID, ROOT_ID, false);

        expect(useClusterFrameCollapsedStore.getState().collapsedByWorkflowId).toEqual({});
    });

    it('leaves the same root name expanded in another workflow', () => {
        useClusterFrameCollapsedStore.getState().setClusterFrameCollapsed(WORKFLOW_ID, ROOT_ID, true);

        expect(
            useClusterFrameCollapsedStore.getState().collapsedByWorkflowId[OTHER_WORKFLOW_ID]?.[ROOT_ID]
        ).toBeUndefined();
    });

    it('persists under a stable storage key', () => {
        useClusterFrameCollapsedStore.getState().setClusterFrameCollapsed(WORKFLOW_ID, ROOT_ID, true);

        expect(JSON.parse(window.localStorage.getItem('bytechef.cluster-frame-collapsed')!).state).toEqual({
            collapsedByWorkflowId: {[WORKFLOW_ID]: {[ROOT_ID]: true}},
        });
    });
});
