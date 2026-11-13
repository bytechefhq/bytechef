import {TooltipProvider} from '@/components/ui/tooltip';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {render} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectedUserProjectConfigurationMenu from '../ConnectedUserProjectConfigurationMenu';

const hoisted = vi.hoisted(() => ({
    configurations: [] as Array<Record<string, unknown>> | undefined,
    dialogProps: [] as Array<Record<string, unknown>>,
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useAutomationProjectConfigurationsQuery: () => ({
        data: hoisted.configurations ? {automationProjectConfigurations: hoisted.configurations} : undefined,
    }),
}));

vi.mock(
    '@/ee/pages/embedded/automations/components/automation-project-configuration-list/AutomationProjectConfigurationMoveUsersDialog',
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

    it("opens the dialog for this user with the other configurations of the current configuration's environment", async () => {
        environmentStore.setState({currentEnvironmentId: 1});

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
        expect((props?.targetConfigurations as Array<{id: string}>).map((target) => target.id)).toEqual(['52', '53']);
    });

    it('offers a disabled sibling and stays enabled when it is the only one', async () => {
        hoisted.configurations = [createConfiguration('51'), createConfiguration('53', {enabled: false})];

        const user = userEvent.setup();

        renderMenu();

        await user.click(screen.getByRole('button', {name: 'Configuration Actions'}));

        const item = await screen.findByRole('menuitem', {name: 'Change configuration'});

        expect(item).not.toHaveAttribute('aria-disabled', 'true');

        await user.click(item);

        expect(await screen.findByText('dialog Change configuration')).toBeInTheDocument();
        expect(hoisted.dialogProps.at(-1)?.targetConfigurations).toEqual([
            expect.objectContaining({enabled: false, id: '53'}),
        ]);
    });

    it('shows no configuration actions when there is no other configuration in the environment', () => {
        hoisted.configurations = [createConfiguration('51'), createConfiguration('54', {environmentId: '1'})];

        renderMenu();

        expect(screen.queryByRole('button', {name: 'Configuration Actions'})).not.toBeInTheDocument();
    });

    it('shows no configuration actions while the configurations load', () => {
        hoisted.configurations = undefined;

        renderMenu();

        expect(screen.queryByRole('button', {name: 'Configuration Actions'})).not.toBeInTheDocument();
    });
});
