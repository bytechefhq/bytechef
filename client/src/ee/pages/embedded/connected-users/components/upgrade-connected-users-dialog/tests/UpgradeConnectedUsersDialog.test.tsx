import {ConnectedUser} from '@/ee/shared/middleware/embedded/connected-user';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import UpgradeConnectedUsersDialog from '../UpgradeConnectedUsersDialog';

const mocks = vi.hoisted(() => ({
    automationImpactVariables: [] as unknown[],
    automationMutateAsync: vi.fn(),
    automationProjectVersions: [1, 4],
    automationSkipped: [] as Array<{connectedUserId: string; reason: string; workflowLabels: string[]}>,
    configurationsFilters: [] as unknown[],
    impactError: false,
    impactLoading: false,
    integrationImpactArguments: [] as unknown[],
    integrationMutateAsync: vi.fn(),
    integrationSkipped: [] as Array<{connectedUserId: number; reason: string; workflowLabels: string[]}>,
}));

vi.mock('@/ee/shared/queries/embedded/integrations.queries', () => ({
    useGetIntegrationsQuery: () => ({
        data: [
            {componentName: 'slack', id: 5, name: 'Slack'},
            {componentName: 'github', id: 6, name: 'GitHub'},
            {componentName: 'jira', id: 7, name: 'Jira'},
        ],
    }),
}));

vi.mock('@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries', () => ({
    useGetIntegrationInstanceConfigurationUpgradeImpactQuery: (
        id: number,
        request: {connectedUserIds: number[]},
        enabled?: boolean
    ) => {
        mocks.integrationImpactArguments.push({enabled, id, request});

        return {
            data:
                enabled && !mocks.impactLoading && !mocks.impactError
                    ? {
                          mcpServerCount: 0,
                          skipped: mocks.integrationSkipped,
                          workflows: [],
                      }
                    : undefined,
            isError: mocks.impactError,
            isLoading: mocks.impactLoading,
        };
    },
    useGetIntegrationInstanceConfigurationsQuery: (filters: {integrationId?: number}, enabled?: boolean) => {
        mocks.configurationsFilters.push(filters);

        return {
            data: enabled
                ? [
                      {id: 100, integrationId: 5, integrationVersion: 1, name: 'Alpha'},
                      {id: 101, integrationId: 5, integrationVersion: 3, name: 'Beta'},
                  ]
                : undefined,
        };
    },
}));

vi.mock('@/ee/shared/mutations/embedded/integrationInstanceConfigurations.mutations', () => ({
    useUpgradeIntegrationInstanceConfigurationConnectedUsersMutation: () => ({
        mutateAsync: mocks.integrationMutateAsync,
    }),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useAutomationWorkflowProjectConfigurationUpgradeImpactQuery: (
        variables: {connectedUserIds: string[]; targetId: string},
        options?: {enabled?: boolean}
    ) => {
        mocks.automationImpactVariables.push({enabled: options?.enabled, variables});

        return {
            data:
                options?.enabled && !mocks.impactLoading && !mocks.impactError
                    ? {
                          automationWorkflowProjectConfigurationUpgradeImpact: {
                              skipped: mocks.automationSkipped,
                              workflows: [],
                          },
                      }
                    : undefined,
            isError: mocks.impactError,
            isLoading: mocks.impactLoading,
        };
    },
    useAutomationWorkflowProjectConfigurationsQuery: (
        variables: {environmentId: string; projectId: string},
        options?: {enabled?: boolean}
    ) => ({
        data: options?.enabled
            ? {
                  automationWorkflowProjectConfigurations: [
                      {
                          id: '200',
                          name: 'Gamma',
                          projectId: variables.projectId,
                          projectVersion: mocks.automationProjectVersions[0],
                      },
                      {
                          id: '201',
                          name: 'Delta',
                          projectId: variables.projectId,
                          projectVersion: mocks.automationProjectVersions[1],
                      },
                  ],
              }
            : undefined,
    }),
    useAutomationWorkflowProjectsQuery: () => ({
        data: {
            automationWorkflowProjects: [
                {configurationCount: 2, id: '30', name: 'Onboarding', published: true},
                {configurationCount: 1, id: '33', name: 'Billing', published: true},
                {configurationCount: 0, id: '31', name: 'Unconfigured', published: true},
                {configurationCount: 1, id: '32', name: 'Draft', published: false},
            ],
        },
    }),
    useUpgradeAutomationWorkflowProjectConfigurationConnectedUsersMutation: () => ({
        mutateAsync: mocks.automationMutateAsync,
    }),
}));

const connectedUsers = [
    {
        externalId: 'ext-1',
        id: 1,
        integrationInstances: [{integrationId: 5, integrationVersion: 2}],
        name: 'Jane',
    },
    {
        externalId: 'ext-2',
        id: 2,
        integrationInstances: [
            {integrationId: 5, integrationVersion: 2},
            {integrationId: 6, integrationVersion: 1},
        ],
        name: 'Bob',
    },
] as unknown as ConnectedUser[];

const renderDialog = (kind: 'automation' | 'integration', onUpgraded = vi.fn(), onClose = vi.fn()) =>
    render(
        <UpgradeConnectedUsersDialog
            connectedUsers={connectedUsers}
            kind={kind}
            onClose={onClose}
            onUpgraded={onUpgraded}
        />
    );

const chooseOption = async (selectName: string, optionName: string | RegExp) => {
    await userEvent.click(screen.getByRole('combobox', {name: selectName}));
    await userEvent.click(await screen.findByRole('option', {name: optionName}));
};

beforeEach(() => {
    windowResizeObserver();

    Element.prototype.hasPointerCapture = vi.fn().mockReturnValue(false);
    Element.prototype.releasePointerCapture = vi.fn();
    Element.prototype.scrollIntoView = vi.fn();
    Element.prototype.setPointerCapture = vi.fn();

    environmentStore.setState({currentEnvironmentId: 1});

    mocks.automationImpactVariables = [];
    mocks.automationProjectVersions = [1, 4];
    mocks.automationSkipped = [{connectedUserId: '2', reason: 'NOT_ON_SOURCE', workflowLabels: []}];
    mocks.configurationsFilters = [];
    mocks.integrationImpactArguments = [];
    mocks.integrationSkipped = [{connectedUserId: 2, reason: 'ALREADY_ON_TARGET', workflowLabels: []}];
    mocks.impactError = false;
    mocks.impactLoading = false;
    mocks.automationMutateAsync.mockReset();
    mocks.integrationMutateAsync.mockReset();
});

afterEach(() => {
    resetAll();
});

describe('UpgradeConnectedUsersDialog', () => {
    describe('integration kind', () => {
        it('lists only the integrations a selected user has an instance of', async () => {
            renderDialog('integration');

            await userEvent.click(screen.getByRole('combobox', {name: 'Integration'}));

            expect(await screen.findByRole('option', {name: 'Slack'})).toBeInTheDocument();
            expect(screen.getByRole('option', {name: 'GitHub'})).toBeInTheDocument();
            expect(screen.queryByRole('option', {name: 'Jira'})).not.toBeInTheDocument();
        });

        it('explains when none of the selected users has an integration', () => {
            render(
                <UpgradeConnectedUsersDialog
                    connectedUsers={[{externalId: 'ext-3', id: 3, name: 'Eve'} as unknown as ConnectedUser]}
                    kind="integration"
                    onClose={vi.fn()}
                    onUpgraded={vi.fn()}
                />
            );

            expect(
                screen.getByText('None of the selected users has an integration in this environment.')
            ).toBeInTheDocument();
        });

        it('lists the configurations of the integration and marks the newer versions', async () => {
            renderDialog('integration');

            await chooseOption('Integration', 'Slack');

            await userEvent.click(screen.getByRole('combobox', {name: 'Configuration'}));

            expect(await screen.findByRole('option', {name: /^Alpha · V1$/})).toBeInTheDocument();
            expect(screen.getByRole('option', {name: /^Beta · V3\s*Newer version$/})).toBeInTheDocument();
            expect(mocks.configurationsFilters).toContainEqual({environmentId: 1, integrationId: 5});
        });

        it('disables Upgrade until a configuration is chosen and the impact has loaded', async () => {
            renderDialog('integration');

            expect(screen.getByRole('button', {name: 'Upgrade'})).toBeDisabled();

            await chooseOption('Integration', 'Slack');

            expect(screen.getByRole('button', {name: 'Upgrade'})).toBeDisabled();

            await chooseOption('Configuration', /Beta/);

            await waitFor(() => expect(screen.getByRole('button', {name: 'Upgrade'})).toBeEnabled());
        });

        it('requests the impact, upgrades and reports moved and skipped users', async () => {
            const onUpgraded = vi.fn();
            const onClose = vi.fn();

            mocks.integrationMutateAsync.mockResolvedValue({
                movedConnectedUserIds: [1],
                needsAttentionConnectedUserIds: [],
                skipped: [{connectedUserId: 2, reason: 'ALREADY_ON_TARGET', workflowLabels: []}],
            });

            renderDialog('integration', onUpgraded, onClose);

            await chooseOption('Integration', 'Slack');
            await chooseOption('Configuration', /Beta/);

            await waitFor(() =>
                expect(mocks.integrationImpactArguments).toContainEqual({
                    enabled: true,
                    id: 101,
                    request: {connectedUserIds: [1, 2]},
                })
            );

            expect(await screen.findByText('Bob: Already on this configuration')).toBeInTheDocument();

            await userEvent.click(screen.getByRole('button', {name: 'Upgrade'}));

            await waitFor(() =>
                expect(mocks.integrationMutateAsync).toHaveBeenCalledWith({
                    configurationUpgradeRequest: {connectedUserIds: [1, 2]},
                    id: 101,
                })
            );

            expect(await screen.findByText('1 moved, 0 need attention')).toBeInTheDocument();
            expect(screen.getByText('Bob: Already on this configuration')).toBeInTheDocument();

            await userEvent.click(screen.getAllByRole('button', {name: 'Close'}).at(-1)!);

            expect(onUpgraded).toHaveBeenCalledTimes(1);
            expect(onClose).toHaveBeenCalledTimes(1);
        });

        it('keeps Upgrade disabled when none of the selected users would move', async () => {
            mocks.integrationSkipped = [
                {connectedUserId: 1, reason: 'NOT_ON_SOURCE', workflowLabels: []},
                {connectedUserId: 2, reason: 'ALREADY_ON_TARGET', workflowLabels: []},
            ];

            renderDialog('integration');

            await chooseOption('Integration', 'Slack');
            await chooseOption('Configuration', /Beta/);

            expect(
                await screen.findByText('None of the selected users would move to this configuration.')
            ).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Upgrade'})).toBeDisabled();
        });

        it('keeps Upgrade enabled when a skipped user could still move from another configuration', async () => {
            mocks.integrationSkipped = [
                {connectedUserId: 1, reason: 'TARGET_LACKS_REFERENCED_WORKFLOWS', workflowLabels: ['Sync']},
                {connectedUserId: 2, reason: 'ALREADY_ON_TARGET', workflowLabels: []},
            ];

            renderDialog('integration');

            await chooseOption('Integration', 'Slack');
            await chooseOption('Configuration', /Beta/);

            await waitFor(() => expect(screen.getByRole('button', {name: 'Upgrade'})).toBeEnabled());
            expect(
                screen.queryByText('None of the selected users would move to this configuration.')
            ).not.toBeInTheDocument();
        });

        it('keeps Upgrade disabled while the impact is loading', async () => {
            mocks.impactLoading = true;

            renderDialog('integration');

            await chooseOption('Integration', 'Slack');
            await chooseOption('Configuration', /Beta/);

            expect(screen.getByRole('button', {name: 'Upgrade'})).toBeDisabled();
        });

        it('keeps Upgrade disabled when the impact failed to load', async () => {
            mocks.impactError = true;

            renderDialog('integration');

            await chooseOption('Integration', 'Slack');
            await chooseOption('Configuration', /Beta/);

            expect(screen.getByRole('button', {name: 'Upgrade'})).toBeDisabled();
        });

        it('clears the chosen configuration when the integration changes', async () => {
            renderDialog('integration');

            await chooseOption('Integration', 'Slack');
            await chooseOption('Configuration', /Beta/);

            expect(screen.getByRole('combobox', {name: 'Configuration'})).toHaveTextContent('Beta');

            await chooseOption('Integration', 'GitHub');

            expect(screen.getByRole('combobox', {name: 'Configuration'})).not.toHaveTextContent('Beta');
            expect(screen.getByRole('button', {name: 'Upgrade'})).toBeDisabled();
        });

        it('stays open while the upgrade is running', async () => {
            const onClose = vi.fn();

            let resolveUpgrade: (value: unknown) => void = () => undefined;

            mocks.integrationMutateAsync.mockReturnValue(
                new Promise((resolve) => {
                    resolveUpgrade = resolve;
                })
            );

            renderDialog('integration', vi.fn(), onClose);

            await chooseOption('Integration', 'Slack');
            await chooseOption('Configuration', /Beta/);

            await waitFor(() => expect(screen.getByRole('button', {name: 'Upgrade'})).toBeEnabled());

            await userEvent.click(screen.getByRole('button', {name: 'Upgrade'}));

            expect(await screen.findByRole('button', {name: 'Upgrading...'})).toBeDisabled();
            expect(screen.getByRole('button', {name: 'Cancel'})).toBeDisabled();

            await userEvent.keyboard('{Escape}');

            expect(onClose).not.toHaveBeenCalled();
            expect(screen.getByText('Upgrade Integration')).toBeInTheDocument();

            resolveUpgrade({movedConnectedUserIds: [1], needsAttentionConnectedUserIds: [], skipped: []});

            expect(await screen.findByText('1 moved, 0 need attention')).toBeInTheDocument();
        });

        it('calls onUpgraded when closed with Escape after a result', async () => {
            const onUpgraded = vi.fn();
            const onClose = vi.fn();

            mocks.integrationMutateAsync.mockResolvedValue({
                movedConnectedUserIds: [1, 2],
                needsAttentionConnectedUserIds: [],
                skipped: [],
            });

            renderDialog('integration', onUpgraded, onClose);

            await chooseOption('Integration', 'Slack');
            await chooseOption('Configuration', /Beta/);

            await waitFor(() => expect(screen.getByRole('button', {name: 'Upgrade'})).toBeEnabled());

            await userEvent.click(screen.getByRole('button', {name: 'Upgrade'}));

            expect(await screen.findByText('2 moved, 0 need attention')).toBeInTheDocument();

            await userEvent.keyboard('{Escape}');

            expect(onUpgraded).toHaveBeenCalledTimes(1);
            expect(onClose).toHaveBeenCalledTimes(1);
        });

        it('stays open and does not call onUpgraded when the upgrade fails', async () => {
            const onUpgraded = vi.fn();
            const onClose = vi.fn();

            mocks.integrationMutateAsync.mockRejectedValue(new Error('failed'));

            renderDialog('integration', onUpgraded, onClose);

            await chooseOption('Integration', 'Slack');
            await chooseOption('Configuration', /Beta/);

            await waitFor(() => expect(screen.getByRole('button', {name: 'Upgrade'})).toBeEnabled());

            await userEvent.click(screen.getByRole('button', {name: 'Upgrade'}));

            await waitFor(() => expect(mocks.integrationMutateAsync).toHaveBeenCalledTimes(1));
            await waitFor(() => expect(screen.getByRole('button', {name: 'Upgrade'})).toBeEnabled());

            expect(onUpgraded).not.toHaveBeenCalled();
            expect(onClose).not.toHaveBeenCalled();
            expect(screen.getByText('Upgrade Integration')).toBeInTheDocument();
        });
    });

    describe('automation kind', () => {
        it('lists only the published projects configured in the environment', async () => {
            renderDialog('automation');

            await userEvent.click(screen.getByRole('combobox', {name: 'Project'}));

            expect(await screen.findByRole('option', {name: 'Onboarding'})).toBeInTheDocument();
            expect(screen.queryByRole('option', {name: 'Unconfigured'})).not.toBeInTheDocument();
            expect(screen.queryByRole('option', {name: 'Draft'})).not.toBeInTheDocument();
        });

        it('marks the configuration with the highest project version as the latest', async () => {
            renderDialog('automation');

            await chooseOption('Project', 'Onboarding');

            await userEvent.click(screen.getByRole('combobox', {name: 'Configuration'}));

            expect(await screen.findByRole('option', {name: /^Gamma · V1$/})).toBeInTheDocument();
            expect(screen.getByRole('option', {name: /^Delta · V4\s*Latest version$/})).toBeInTheDocument();
        });

        it('marks no configuration as the latest when every version is the same', async () => {
            mocks.automationProjectVersions = [2, 2];

            renderDialog('automation');

            await chooseOption('Project', 'Onboarding');

            await userEvent.click(screen.getByRole('combobox', {name: 'Configuration'}));

            expect(await screen.findByRole('option', {name: /^Delta · V2$/})).toBeInTheDocument();
            expect(screen.queryByText('Latest version')).not.toBeInTheDocument();
        });

        it('disables Upgrade until a configuration is chosen and the impact has loaded', async () => {
            renderDialog('automation');

            expect(screen.getByRole('button', {name: 'Upgrade'})).toBeDisabled();

            await chooseOption('Project', 'Onboarding');

            expect(screen.getByRole('button', {name: 'Upgrade'})).toBeDisabled();

            await chooseOption('Configuration', /Delta/);

            await waitFor(() => expect(screen.getByRole('button', {name: 'Upgrade'})).toBeEnabled());
        });

        it('requests the impact, upgrades and reports skipped users', async () => {
            const onUpgraded = vi.fn();

            mocks.automationMutateAsync.mockResolvedValue({
                upgradeAutomationWorkflowProjectConfigurationConnectedUsers: {
                    movedConnectedUserIds: ['1'],
                    needsAttentionConnectedUserIds: [],
                    skipped: [{connectedUserId: '2', reason: 'NOT_ON_SOURCE', workflowLabels: []}],
                },
            });

            renderDialog('automation', onUpgraded);

            await chooseOption('Project', 'Onboarding');
            await chooseOption('Configuration', /Delta/);

            await waitFor(() =>
                expect(mocks.automationImpactVariables).toContainEqual({
                    enabled: true,
                    variables: {connectedUserIds: ['1', '2'], targetId: '201'},
                })
            );

            await userEvent.click(screen.getByRole('button', {name: 'Upgrade'}));

            await waitFor(() =>
                expect(mocks.automationMutateAsync).toHaveBeenCalledWith({
                    connectedUserIds: ['1', '2'],
                    targetId: '201',
                })
            );

            expect(await screen.findByText('1 moved, 0 need attention')).toBeInTheDocument();
            expect(screen.getByText("Bob: Doesn't have this project")).toBeInTheDocument();

            await userEvent.click(screen.getAllByRole('button', {name: 'Close'}).at(-1)!);

            expect(onUpgraded).toHaveBeenCalledTimes(1);
        });

        it('keeps Upgrade disabled when none of the selected users would move', async () => {
            mocks.automationSkipped = [
                {connectedUserId: '1', reason: 'NOT_ON_SOURCE', workflowLabels: []},
                {connectedUserId: '2', reason: 'NOT_ON_SOURCE', workflowLabels: []},
            ];

            renderDialog('automation');

            await chooseOption('Project', 'Onboarding');
            await chooseOption('Configuration', /Delta/);

            expect(
                await screen.findByText('None of the selected users would move to this configuration.')
            ).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Upgrade'})).toBeDisabled();
        });

        it('keeps Upgrade disabled while the impact is loading', async () => {
            mocks.impactLoading = true;

            renderDialog('automation');

            await chooseOption('Project', 'Onboarding');
            await chooseOption('Configuration', /Delta/);

            expect(screen.getByRole('button', {name: 'Upgrade'})).toBeDisabled();
        });

        it('keeps Upgrade disabled when the impact failed to load', async () => {
            mocks.impactError = true;

            renderDialog('automation');

            await chooseOption('Project', 'Onboarding');
            await chooseOption('Configuration', /Delta/);

            expect(screen.getByRole('button', {name: 'Upgrade'})).toBeDisabled();
        });

        it('clears the chosen configuration when the project changes', async () => {
            renderDialog('automation');

            await chooseOption('Project', 'Onboarding');
            await chooseOption('Configuration', /Delta/);

            expect(screen.getByRole('combobox', {name: 'Configuration'})).toHaveTextContent('Delta');

            await chooseOption('Project', 'Billing');

            expect(screen.getByRole('combobox', {name: 'Configuration'})).not.toHaveTextContent('Delta');
            expect(screen.getByRole('button', {name: 'Upgrade'})).toBeDisabled();
        });
    });
});
