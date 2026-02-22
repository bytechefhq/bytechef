import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
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
    default: () => <div data-testid="workflow-nodes-tabs">Tabs</div>,
}));

describe('WorkflowNodesPopoverMenuComponentList', () => {
    beforeEach(() => {
        windowResizeObserver();

        hoisted.mockFilterResult.isSearchFetching = false;
        hoisted.mockFilterResult.filter = '';
        hoisted.mockFilterResult.trimmedFilter = '';
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
});
