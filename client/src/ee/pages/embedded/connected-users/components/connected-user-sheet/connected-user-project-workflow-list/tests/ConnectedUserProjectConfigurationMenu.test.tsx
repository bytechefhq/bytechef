import {TooltipProvider} from '@/components/ui/tooltip';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {render} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectedUserProjectConfigurationMenu from '../ConnectedUserProjectConfigurationMenu';

const hoisted = vi.hoisted(() => ({
    configurations: [] as Array<Record<string, unknown>>,
    dialogProps: [] as Array<Record<string, unknown>>,
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useAutomationWorkflowProjectConfigurationsQuery: () => ({
        data: {automationWorkflowProjectConfigurations: hoisted.configurations},
    }),
}));

vi.mock(
    '@/ee/pages/embedded/automation-workflows/components/automation-workflow-project-configuration-list/AutomationWorkflowProjectConfigurationMoveUsersDialog',
    () => ({
        default: (props: Record<string, unknown>) => {
            hoisted.dialogProps.push(props);

            return <div>{`dialog ${props.title}`}</div>;
        },
    })
);

const currentConfiguration = {
    configurationId: '51',
    configurationName: 'Default',
    configurationProjectVersion: 3,
};

const createConfiguration = (id: string, overrides: Record<string, unknown> = {}) => ({
    enabled: true,
    environmentId: '2',
    id,
    name: `Config ${id}`,
    projectVersion: 1,
    ...overrides,
});

const renderMenu = () =>
    render(
        <TooltipProvider>
            <ConnectedUserProjectConfigurationMenu
                connectedUser={{connectedUserId: '7', externalId: 'ext-7'}}
                currentConfiguration={currentConfiguration}
                projectId="10"
            />
        </TooltipProvider>
    );

describe('ConnectedUserProjectConfigurationMenu', () => {
    beforeEach(() => {
        windowResizeObserver();

        environmentStore.setState({currentEnvironmentId: 2});

        hoisted.configurations = [];
        hoisted.dialogProps = [];
    });

    afterEach(() => {
        resetAll();
    });

    it('opens the dialog for this user with the other enabled configurations of the environment', async () => {
        hoisted.configurations = [
            createConfiguration('51'),
            createConfiguration('52'),
            createConfiguration('53', {enabled: false}),
            createConfiguration('54', {environmentId: '1'}),
        ];

        const user = userEvent.setup();

        renderMenu();

        await user.click(screen.getByRole('button', {name: 'Configuration Actions'}));
        await user.click(await screen.findByRole('menuitem', {name: 'Change configuration'}));

        expect(await screen.findByText('dialog Change configuration')).toBeInTheDocument();

        const props = hoisted.dialogProps.at(-1);

        expect(props).toMatchObject({
            configuration: {id: '51', name: 'Default', projectVersion: 3},
            connectedUsers: [{connectedUserId: '7', externalId: 'ext-7'}],
            initialSelectedConnectedUserIds: ['7'],
            title: 'Change configuration',
        });
        expect((props?.targetConfigurations as Array<{id: string}>).map((target) => target.id)).toEqual(['52']);
    });

    it('disables Change configuration with a tooltip when there is no other configuration', async () => {
        hoisted.configurations = [createConfiguration('51'), createConfiguration('53', {enabled: false})];

        const user = userEvent.setup();

        renderMenu();

        await user.click(screen.getByRole('button', {name: 'Configuration Actions'}));

        const item = await screen.findByRole('menuitem', {name: 'Change configuration'});

        expect(item).toHaveAttribute('aria-disabled', 'true');

        await user.hover(item.parentElement as HTMLElement);

        await waitFor(async () =>
            expect((await screen.findAllByText('No other configuration in this environment')).length).toBeGreaterThan(0)
        );
    });
});
