import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import {beforeEach, describe, expect, it} from 'vitest';

import endChatTestSession from './endChatTestSession';

describe('endChatTestSession', () => {
    beforeEach(() => {
        useWorkflowTestChatStore.setState({chatTriggerName: undefined, workflowTestChatPanelOpen: false});
    });

    it('ends an active session and closes the chat panel', () => {
        useWorkflowTestChatStore.getState().startChatTestSession('trigger_2');

        expect(endChatTestSession()).toBe(true);
        expect(useWorkflowTestChatStore.getState().chatTriggerName).toBeUndefined();
        expect(useWorkflowTestChatStore.getState().workflowTestChatPanelOpen).toBe(false);
    });

    it('does nothing when no session is active', () => {
        useWorkflowTestChatStore.setState({workflowTestChatPanelOpen: true});

        expect(endChatTestSession()).toBe(false);
        expect(useWorkflowTestChatStore.getState().workflowTestChatPanelOpen).toBe(true);
    });

    it('ends a session only once', () => {
        useWorkflowTestChatStore.getState().startChatTestSession('trigger_1');

        expect(endChatTestSession()).toBe(true);
        expect(endChatTestSession()).toBe(false);
    });
});
