import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    lastWorkflowNodesTabsProps: {} as Record<string, unknown>,
    mockFilterResult: {
        componentsWithActions: [
            {actionsCount: 3, name: 'gmail', triggersCount: 1, version: 1},
            {actionsCount: 2, name: 'slack', triggersCount: 0, version: 1},
        ],
        filter: '',
        isSearchFetching: false,
        setFilter: vi.fn(),
        trimmedFilter: '',
    },
}));

vi.mock('../hooks/useFilteredComponentDefinitions', () => ({
    useFilteredComponentDefinitions: () => hoisted.mockFilterResult,
}));

vi.mock('../stores/useWorkflowDataStore', () => ({
    default: Object.assign(
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        (selector: any) =>
            selector({
                componentDefinitions: [
                    {actionsCount: 3, name: 'gmail', triggersCount: 1, version: 1},
                    {actionsCount: 2, name: 'slack', triggersCount: 0, version: 1},
                ],
                nodes: [],
                taskDispatcherDefinitions: [],
            }),
        {getState: vi.fn()}
    ),
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

describe('WorkflowNodesPopoverMenuComponentList', () => {
    beforeEach(() => {
        windowResizeObserver();

        hoisted.mockFilterResult.isSearchFetching = false;
        hoisted.mockFilterResult.filter = '';
        hoisted.mockFilterResult.trimmedFilter = '';
        hoisted.lastWorkflowNodesTabsProps = {};
    });

    afterEach(() => {
        resetAll();
    });

    it('should render filter input', async () => {
        const {default: WorkflowNodesPopoverMenuComponentList} =
            await import('../components/WorkflowNodesPopoverMenuComponentList');

        render(<WorkflowNodesPopoverMenuComponentList actionPanelOpen={false} />);

        expect(screen.getByPlaceholderText('Filter components')).toBeInTheDocument();
    });

    it('should show loading spinner when search is fetching', async () => {
        hoisted.mockFilterResult.isSearchFetching = true;

        const {default: WorkflowNodesPopoverMenuComponentList} =
            await import('../components/WorkflowNodesPopoverMenuComponentList');

        render(<WorkflowNodesPopoverMenuComponentList actionPanelOpen={false} />);

        expect(screen.getByRole('status')).toBeInTheDocument();
    });

    it('should not show loading spinner when search is not fetching', async () => {
        hoisted.mockFilterResult.isSearchFetching = false;

        const {default: WorkflowNodesPopoverMenuComponentList} =
            await import('../components/WorkflowNodesPopoverMenuComponentList');

        render(<WorkflowNodesPopoverMenuComponentList actionPanelOpen={false} />);

        expect(screen.queryByRole('status')).not.toBeInTheDocument();
    });

    it('should show search match counts in tabs while searching', async () => {
        hoisted.mockFilterResult.filter = 'ai text';
        hoisted.mockFilterResult.trimmedFilter = 'ai text';

        const {default: WorkflowNodesPopoverMenuComponentList} =
            await import('../components/WorkflowNodesPopoverMenuComponentList');

        render(<WorkflowNodesPopoverMenuComponentList actionPanelOpen={false} />);

        expect(hoisted.lastWorkflowNodesTabsProps.showSearchMatchCounts).toBe(true);
    });

    it('should not show search match counts in tabs while search results are loading', async () => {
        hoisted.mockFilterResult.filter = 'ai text';
        hoisted.mockFilterResult.isSearchFetching = true;
        hoisted.mockFilterResult.trimmedFilter = 'ai text';

        const {default: WorkflowNodesPopoverMenuComponentList} =
            await import('../components/WorkflowNodesPopoverMenuComponentList');

        render(<WorkflowNodesPopoverMenuComponentList actionPanelOpen={false} />);

        expect(hoisted.lastWorkflowNodesTabsProps.showSearchMatchCounts).toBe(false);
    });

    it('should not show search match counts in tabs when not searching', async () => {
        const {default: WorkflowNodesPopoverMenuComponentList} =
            await import('../components/WorkflowNodesPopoverMenuComponentList');

        render(<WorkflowNodesPopoverMenuComponentList actionPanelOpen={false} />);

        expect(hoisted.lastWorkflowNodesTabsProps.showSearchMatchCounts).toBe(false);
    });
});
