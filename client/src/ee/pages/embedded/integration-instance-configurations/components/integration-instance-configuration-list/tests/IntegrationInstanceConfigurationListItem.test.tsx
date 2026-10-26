import {Collapsible} from '@/components/ui/collapsible';
import {TooltipProvider} from '@/components/ui/tooltip';
import IntegrationInstanceConfigurationListItem from '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-list/IntegrationInstanceConfigurationListItem';
import {IntegrationInstanceConfiguration} from '@/ee/shared/middleware/embedded/configuration';
import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    captureIntegrationInstanceConfigurationEnabledMock: vi.fn(),
}));

vi.mock('@/ee/shared/mutations/embedded/integrationInstanceConfigurations.mutations', () => ({
    useDeleteIntegrationInstanceConfigurationMutation: () => ({isPending: false, mutate: vi.fn()}),
    useEnableIntegrationInstanceConfigurationMutation: ({
        onSuccess,
    }: {
        onSuccess?: (result: void, variables: {enable: boolean; id: number}) => void;
    }) => ({
        isPending: false,
        mutate: (variables: {enable: boolean; id: number}) => onSuccess?.(undefined, variables),
    }),
    useUpdateIntegrationInstanceConfigurationTagsMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/shared/hooks/useAnalytics', () => ({
    useAnalytics: () => ({
        captureIntegrationInstanceConfigurationEnabled: hoisted.captureIntegrationInstanceConfigurationEnabledMock,
    }),
}));

vi.mock(
    '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialog',
    () => ({
        default: () => <div data-testid="integration-instance-configuration-dialog" />,
    })
);

const integrationInstanceConfiguration = {
    enabled: true,
    id: 1,
    integrationInstanceConfigurationWorkflows: [{workflowId: 'workflow-1'}, {workflowId: 'workflow-2'}],
    integrationVersion: 1,
    name: 'My Instance Configuration',
    tags: [],
} as unknown as IntegrationInstanceConfiguration;

const renderItem = (overrides: Partial<IntegrationInstanceConfiguration> = {}) =>
    render(
        <TooltipProvider>
            <Collapsible>
                <IntegrationInstanceConfigurationListItem
                    integrationInstanceConfiguration={{...integrationInstanceConfiguration, ...overrides}}
                />
            </Collapsible>
        </TooltipProvider>
    );

// A pointer click on the menu trigger bubbles to the row in jsdom, because jsdom has no hit testing and ignores the
// pointer-events lock Radix puts on the body while a menu is open. The keyboard opens the menu without that click.
const openMenu = async () => {
    renderItem();

    screen
        .getAllByRole('button')
        .find((button) => button.getAttribute('aria-haspopup') === 'menu')!
        .focus();

    await userEvent.keyboard('{ArrowDown}');
};

const getWorkflowsTrigger = () => screen.getByRole('button', {hidden: true, name: /2 workflows/});

describe('IntegrationInstanceConfigurationListItem row click', () => {
    beforeEach(() => {
        windowResizeObserver();
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    it('should expand the workflows when the row is clicked', async () => {
        renderItem();

        await userEvent.click(screen.getByText('My Instance Configuration'));

        await waitFor(() => expect(getWorkflowsTrigger()).toHaveAttribute('aria-expanded', 'true'));
    });

    it('should keep the workflows collapsed when a menu row is clicked', async () => {
        await openMenu();

        expect(getWorkflowsTrigger()).toHaveAttribute('aria-expanded', 'false');

        await userEvent.click(screen.getByRole('menuitem', {name: 'Update Integration Version'}));

        expect(await screen.findByTestId('integration-instance-configuration-dialog')).toBeInTheDocument();
        expect(getWorkflowsTrigger()).toHaveAttribute('aria-expanded', 'false');
    });

    it('should keep the workflows collapsed when the menu itself is clicked', async () => {
        await openMenu();

        await userEvent.click(screen.getByRole('menu'));

        expect(getWorkflowsTrigger()).toHaveAttribute('aria-expanded', 'false');
    });
});

describe('IntegrationInstanceConfigurationListItem enable analytics', () => {
    beforeEach(() => {
        windowResizeObserver();
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    it('should capture the enabled event when the configuration is turned on', async () => {
        renderItem({enabled: false});

        await userEvent.click(screen.getByRole('switch'));

        expect(hoisted.captureIntegrationInstanceConfigurationEnabledMock).toHaveBeenCalledTimes(1);
    });

    it('should not capture the enabled event when the configuration is turned off', async () => {
        renderItem();

        await userEvent.click(screen.getByRole('switch'));

        expect(hoisted.captureIntegrationInstanceConfigurationEnabledMock).not.toHaveBeenCalled();
    });
});
