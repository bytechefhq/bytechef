import {TooltipProvider} from '@/components/Tooltip/Tooltip';
import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import IntegrationInstanceConfigurationListItemDropdownMenu from '../IntegrationInstanceConfigurationListItemDropdownMenu';

const hoisted = vi.hoisted(() => ({
    edition: 'EE',
    environmentsResult: {
        data: {
            environments: [
                {id: '0', name: 'Development'},
                {id: '1', name: 'Staging'},
            ],
        },
    } as {data: {environments: {id: string; name: string}[]} | undefined},
    promotionDialogProps: [] as Record<string, unknown>[],
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useEnvironmentsQuery: () => hoisted.environmentsResult,
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: Record<string, unknown>) => unknown) =>
        selector({application: {edition: hoisted.edition}}),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: Record<string, unknown>) => unknown) => selector({currentEnvironmentId: 0}),
}));

vi.mock('@/ee/shared/components/environment-promotion/EnvironmentPromotionDialog', () => ({
    default: (props: Record<string, unknown>) => {
        hoisted.promotionDialogProps.push(props);

        return <div data-testid="environment-promotion-dialog" />;
    },
}));

const handlers = {
    onDeleteClick: vi.fn(),
    onEditClick: vi.fn(),
    onMoveUsersClick: vi.fn(),
    onUpdateIntegrationVersionClick: vi.fn(),
};

const openMenu = async (moveUsersDisabledReason?: string) => {
    render(
        <TooltipProvider>
            <IntegrationInstanceConfigurationListItemDropdownMenu
                {...handlers}
                integrationInstanceConfigurationId={7}
                integrationInstanceConfigurationName="Acme"
                moveUsersDisabledReason={moveUsersDisabledReason}
            />
        </TooltipProvider>
    );

    await userEvent.click(screen.getByRole('button'));
};

describe('IntegrationInstanceConfigurationListItemDropdownMenu', () => {
    beforeEach(() => {
        windowResizeObserver();
        hoisted.edition = 'EE';
        hoisted.promotionDialogProps = [];
        hoisted.environmentsResult = {
            data: {
                environments: [
                    {id: '0', name: 'Development'},
                    {id: '1', name: 'Staging'},
                ],
            },
        };
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    it('should render with the design-system DropdownMenu', async () => {
        await openMenu();

        expect(screen.getByRole('menu')).toHaveClass('bg-surface-neutral-primary');
        expect(screen.getByRole('menuitem', {name: 'Edit'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Change Integration Version'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Delete'})).toHaveClass('text-content-destructive-primary');
    });

    it.each([
        ['Edit', 'onEditClick'],
        ['Change Integration Version', 'onUpdateIntegrationVersionClick'],
        ['Move Users', 'onMoveUsersClick'],
        ['Delete', 'onDeleteClick'],
    ] as const)('should call the handler when %s is clicked', async (rowName, handlerName) => {
        await openMenu();

        await userEvent.click(screen.getByRole('menuitem', {name: rowName}));

        expect(handlers[handlerName]).toHaveBeenCalledTimes(1);
    });

    it('menu shows Move Users…', async () => {
        await openMenu();

        expect(screen.getAllByRole('menuitem').map((item) => item.textContent)).toEqual([
            'Edit',
            'Change Integration Version',
            'Move Users…',
            'Promote to environment…',
            'Delete',
        ]);
        expect(screen.getByRole('menuitem', {name: 'Move Users'})).not.toHaveAttribute('aria-disabled', 'true');
        expect(screen.getByRole('separator')).toBeInTheDocument();
    });

    it('should disable Move Users with the given reason', async () => {
        await openMenu('No other configuration in this environment');

        const item = screen.getByRole('menuitem', {name: 'Move Users'});

        expect(item).toHaveAttribute('aria-disabled', 'true');

        await userEvent.hover(item.parentElement as HTMLElement);

        await waitFor(async () =>
            expect((await screen.findAllByText('No other configuration in this environment')).length).toBeGreaterThan(0)
        );
    });

    it('should hide Promote to environment when there is only one environment', async () => {
        hoisted.environmentsResult = {data: {environments: [{id: '0', name: 'Development'}]}};

        await openMenu();

        expect(screen.queryByRole('menuitem', {name: 'Promote to environment…'})).not.toBeInTheDocument();
    });

    it('should open the promotion dialog for the integration instance configuration', async () => {
        await openMenu();

        expect(screen.queryByTestId('environment-promotion-dialog')).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Promote to environment…'}));

        expect(await screen.findByTestId('environment-promotion-dialog')).toBeInTheDocument();

        expect(hoisted.promotionDialogProps[0]).toMatchObject({
            resourceType: 'INTEGRATION_INSTANCE_CONFIGURATION',
            sourceEnvironmentId: 0,
            sourceId: '7',
            sourceName: 'Acme',
            targetListPath: '/embedded/configurations',
        });
    });
});
