import {TooltipProvider} from '@/components/ui/tooltip';
import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectedUserSheetPanelTriggerSubscriptionList from '../ConnectedUserSheetPanelTriggerSubscriptionList';

const hoisted = vi.hoisted(() => ({
    componentDefinitionsQueryMock: vi.fn(),
    connectedUserTriggerSubscriptionsQueryMock: vi.fn(),
    deleteMutateMock: vi.fn(),
    enableMutateMock: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useConnectedUserTriggerSubscriptionsQuery: hoisted.connectedUserTriggerSubscriptionsQueryMock,
    useDeleteTriggerSubscriptionMutation: () => ({isPending: false, mutate: hoisted.deleteMutateMock}),
    useEnableTriggerSubscriptionMutation: () => ({isPending: false, mutate: hoisted.enableMutateMock}),
}));

vi.mock('@/ee/shared/queries/embedded/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: hoisted.componentDefinitionsQueryMock,
}));

const triggerSubscriptions = [
    {
        componentName: 'github',
        componentVersion: 1,
        errorMessage: null,
        id: '11',
        lastDeliveryError: null,
        lastDeliveryStatus: 'DELIVERED',
        lastEventDate: Date.UTC(2026, 10, 10, 9, 30),
        name: 'Repo stars',
        status: 'ENABLED',
        triggerName: 'newStar',
    },
    {
        componentName: 'slack',
        componentVersion: 1,
        errorMessage: null,
        id: '12',
        lastDeliveryError: null,
        lastDeliveryStatus: null,
        lastEventDate: null,
        name: null,
        status: 'DISABLED',
        triggerName: 'newMessage',
    },
    {
        componentName: 'hubspot',
        componentVersion: 1,
        errorMessage: 'Token expired',
        id: '13',
        lastDeliveryError: 'HTTP 500',
        lastDeliveryStatus: 'FAILED',
        lastEventDate: null,
        name: null,
        status: 'CONNECTION_ERROR',
        triggerName: 'newContact',
    },
];

const renderList = () =>
    render(
        <TooltipProvider>
            <ConnectedUserSheetPanelTriggerSubscriptionList connectedUserId={1050} />
        </TooltipProvider>
    );

beforeEach(() => {
    windowResizeObserver();

    hoisted.componentDefinitionsQueryMock.mockReturnValue({
        data: [{icon: undefined, name: 'github', title: 'GitHub'}],
    });
    hoisted.connectedUserTriggerSubscriptionsQueryMock.mockReturnValue({
        data: {connectedUserTriggerSubscriptions: triggerSubscriptions},
        isLoading: false,
    });
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('ConnectedUserSheetPanelTriggerSubscriptionList', () => {
    it('renders subscriptions with status badges', () => {
        renderList();

        expect(hoisted.connectedUserTriggerSubscriptionsQueryMock).toHaveBeenCalledWith({connectedUserId: '1050'});
        expect(screen.getByRole('heading', {name: 'Triggers'})).toBeInTheDocument();
        expect(screen.getByText('newStar')).toBeInTheDocument();
        expect(screen.getByText('Repo stars')).toBeInTheDocument();
        expect(screen.getByText('newMessage')).toBeInTheDocument();
        expect(screen.getByText('newContact')).toBeInTheDocument();

        expect(screen.getByText('ENABLED')).toHaveClass('bg-surface-success-primary');
        expect(screen.getByText('DISABLED')).toHaveClass('bg-surface-neutral-secondary');
        expect(screen.getByText('CONNECTION_ERROR')).toHaveClass('bg-surface-destructive-primary');

        expect(screen.getAllByText('No events')).toHaveLength(2);
        expect(screen.getByText('DELIVERED')).toBeInTheDocument();
        expect(screen.getByText('HTTP 500')).toBeInTheDocument();
    });

    it('disables a subscription from the row menu', async () => {
        renderList();

        const [firstRowMenuButton] = screen.getAllByRole('button', {name: 'More actions'});

        await userEvent.click(firstRowMenuButton);

        expect(screen.queryByRole('menuitem', {name: 'Enable'})).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Disable'}));

        expect(hoisted.enableMutateMock).toHaveBeenCalledWith({enable: false, id: '11'});
    });

    it('deletes after confirmation', async () => {
        renderList();

        const rowMenuButtons = screen.getAllByRole('button', {name: 'More actions'});

        await userEvent.click(rowMenuButtons[1]);

        await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));

        expect(hoisted.deleteMutateMock).not.toHaveBeenCalled();

        await waitFor(() => {
            expect(screen.getByRole('alertdialog')).toBeInTheDocument();
        });

        await userEvent.click(screen.getByRole('button', {name: 'Delete'}));

        expect(hoisted.deleteMutateMock).toHaveBeenCalledWith({id: '12'});
    });

    it('renders empty state', () => {
        hoisted.connectedUserTriggerSubscriptionsQueryMock.mockReturnValue({
            data: {connectedUserTriggerSubscriptions: []},
            isLoading: false,
        });

        renderList();

        expect(screen.getByText('No trigger subscriptions. Create them with the Trigger API.')).toBeInTheDocument();
    });
});
