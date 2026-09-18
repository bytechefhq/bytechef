import {render, resetAll, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowNodesSidebar from '../components/WorkflowNodesSidebar';

const hoisted = vi.hoisted(() => ({
    lastWorkflowNodesTabsProps: {} as Record<string, unknown>,
    mockFilterResult: {
        componentsWithActions: [{actionsCount: 3, name: 'gmail', triggersCount: 1, version: 1}],
        filter: '',
        isSearchFetching: false,
        setFilter: vi.fn(),
        trimmedFilter: '',
    },
}));

vi.mock('../hooks/useFilteredComponentDefinitions', () => ({
    useFilteredComponentDefinitions: () => hoisted.mockFilterResult,
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => () => false,
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: () => false,
}));

vi.mock('../components/workflow-nodes-tabs/WorkflowNodesTabs', () => ({
    default: (props: Record<string, unknown>) => {
        hoisted.lastWorkflowNodesTabsProps = props;

        return <div data-testid="workflow-nodes-tabs">Tabs</div>;
    },
}));

const renderWorkflowNodesSidebar = () =>
    render(
        <WorkflowNodesSidebar
            data={{
                componentDefinitions: [{actionsCount: 3, name: 'gmail', triggersCount: 1, version: 1}],
                taskDispatcherDefinitions: [],
            }}
            visible
        />
    );

describe('WorkflowNodesSidebar', () => {
    beforeEach(() => {
        windowResizeObserver();

        hoisted.lastWorkflowNodesTabsProps = {};
        hoisted.mockFilterResult.filter = '';
        hoisted.mockFilterResult.isSearchFetching = false;
        hoisted.mockFilterResult.trimmedFilter = '';
    });

    afterEach(() => {
        resetAll();
    });

    it('should show search match counts in tabs while searching', () => {
        hoisted.mockFilterResult.filter = 'gmail';
        hoisted.mockFilterResult.trimmedFilter = 'gmail';

        renderWorkflowNodesSidebar();

        expect(hoisted.lastWorkflowNodesTabsProps.showSearchMatchCounts).toBe(true);
    });

    it('should not show search match counts in tabs while search results are loading', () => {
        hoisted.mockFilterResult.filter = 'gmail';
        hoisted.mockFilterResult.isSearchFetching = true;
        hoisted.mockFilterResult.trimmedFilter = 'gmail';

        renderWorkflowNodesSidebar();

        expect(hoisted.lastWorkflowNodesTabsProps.showSearchMatchCounts).toBe(false);
    });

    it('should not show search match counts in tabs when not searching', () => {
        renderWorkflowNodesSidebar();

        expect(hoisted.lastWorkflowNodesTabsProps.showSearchMatchCounts).toBe(false);
    });
});
