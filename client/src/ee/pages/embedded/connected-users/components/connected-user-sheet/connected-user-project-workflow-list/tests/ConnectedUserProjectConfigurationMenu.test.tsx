import {TooltipProvider} from '@/components/ui/tooltip';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {act, render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectedUserProjectConfigurationMenu from '../ConnectedUserProjectConfigurationMenu';

const hoisted = vi.hoisted(() => ({
    changeVersionDialogProps: [] as Array<Record<string, unknown>>,
    changeVersionMutateAsyncMock: vi.fn(),
    configurations: [] as Array<Record<string, unknown>> | undefined,
    configurationsError: false,
    dialogProps: [] as Array<Record<string, unknown>>,
    editDialogProps: [] as Array<Record<string, unknown>>,
    moveImpactQueryMock: vi.fn(),
    versions: [] as Array<{status: string; version: number}> | undefined,
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useAutomationProjectConfigurationMoveImpactQuery: (...args: unknown[]) => {
        hoisted.moveImpactQueryMock(...args);

        return {
            data: {
                automationProjectConfigurationMoveImpact: {
                    skipped: [],
                    workflows: [
                        {
                            affectedConnectedUserCount: 1,
                            label: 'Sync',
                            newRequiredConnections: [],
                            newRequiredInputs: [],
                        },
                    ],
                },
            },
            isError: false,
            isLoading: false,
        };
    },
    useAutomationProjectConfigurationsQuery: () => ({
        data: hoisted.configurations ? {automationProjectConfigurations: hoisted.configurations} : undefined,
        isError: hoisted.configurationsError,
    }),
    useAutomationProjectVersionsQuery: () => ({
        data: hoisted.versions ? {automationProjectVersions: hoisted.versions} : undefined,
    }),
    useChangeAutomationProjectConfigurationConnectedUserVersionMutation: () => ({
        isPending: false,
        mutateAsync: hoisted.changeVersionMutateAsyncMock,
    }),
}));

vi.mock(
    '@/ee/pages/embedded/connected-users/components/connected-user-sheet/change-version-dialog/ConnectedUserChangeVersionDialog',
    async (importOriginal) => ({
        ...(await importOriginal<
            typeof import('@/ee/pages/embedded/connected-users/components/connected-user-sheet/change-version-dialog/ConnectedUserChangeVersionDialog')
        >()),
        default: (props: Record<string, unknown>) => {
            hoisted.changeVersionDialogProps.push(props);

            return <div>change version dialog</div>;
        },
    })
);

vi.mock(
    '@/ee/pages/embedded/automations/components/automation-project-configuration-list/AutomationProjectConfigurationMoveUsersDialog',
    () => ({
        default: (props: Record<string, unknown>) => {
            hoisted.dialogProps.push(props);

            return <div>{`dialog ${props.title}`}</div>;
        },
    })
);

vi.mock(
    '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connected-user-project-workflow-list/ConnectedUserProjectWorkflowEditDialog',
    () => ({
        default: (props: Record<string, unknown>) => {
            hoisted.editDialogProps.push(props);

            return <div>{`edit dialog ${props.workflowLabel}`}</div>;
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

const connectedUserProjectWorkflows = [
    {automationUuid: 'wf-sync', id: '901', workflow: {label: 'Sync'}},
    {automationUuid: 'wf-rate', id: '902', workflow: {label: 'Rate Ticket'}},
];

const renderMenu = () =>
    render(
        <TooltipProvider>
            <ConnectedUserProjectConfigurationMenu
                connectedUser={{connectedUserId: '7', externalId: 'ext-7'}}
                connectedUserProjectWorkflows={connectedUserProjectWorkflows}
                currentConfiguration={currentConfiguration}
                projectId="10"
            />
        </TooltipProvider>
    );

describe('ConnectedUserProjectConfigurationMenu', () => {
    beforeEach(() => {
        windowResizeObserver();

        environmentStore.setState({currentEnvironmentId: 2});

        hoisted.changeVersionDialogProps = [];
        hoisted.configurations = [];
        hoisted.configurationsError = false;
        hoisted.dialogProps = [];
        hoisted.editDialogProps = [];
        hoisted.versions = [
            {status: 'PUBLISHED', version: 1},
            {status: 'PUBLISHED', version: 3},
            {status: 'PUBLISHED', version: 4},
            {status: 'DRAFT', version: 5},
        ];
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
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

    it('disables Change configuration with a reason and still offers Change version when there is no other configuration in the environment', async () => {
        hoisted.configurations = [createConfiguration('51'), createConfiguration('54', {environmentId: '1'})];

        const user = userEvent.setup();

        renderMenu();

        await user.click(screen.getByRole('button', {name: 'Configuration Actions'}));

        const changeConfigurationItem = await screen.findByRole('menuitem', {name: 'Change configuration'});

        expect(changeConfigurationItem).toHaveAttribute('aria-disabled', 'true');
        expect(screen.getByRole('menuitem', {name: 'Change version'})).not.toHaveAttribute('aria-disabled', 'true');

        await user.hover(changeConfigurationItem.parentElement as HTMLElement);

        await waitFor(async () =>
            expect((await screen.findAllByText('No other configuration in this environment')).length).toBeGreaterThan(0)
        );
    });

    it('opens the change version dialog with the published versions and the configurations of the environment', async () => {
        const changeResult = {movedConnectedUserIds: ['7'], needsAttentionConnectedUserIds: [], skipped: []};

        hoisted.changeVersionMutateAsyncMock.mockResolvedValue({
            changeAutomationProjectConfigurationConnectedUserVersion: changeResult,
        });
        hoisted.configurations = [
            createConfiguration('51', {projectVersion: 3}),
            createConfiguration('52', {name: 'Default', projectVersion: 4}),
            createConfiguration('54', {environmentId: '1', projectVersion: 4}),
        ];

        const user = userEvent.setup();

        renderMenu();

        await user.click(screen.getByRole('button', {name: 'Configuration Actions'}));
        await user.click(await screen.findByRole('menuitem', {name: 'Change version'}));

        expect(await screen.findByText('change version dialog')).toBeInTheDocument();

        const props = hoisted.changeVersionDialogProps.at(-1);

        expect(props).toMatchObject({
            configurations: [{enabled: true, id: '52', name: 'Default', version: 4}],
            currentConfigurationName: 'Default',
            currentVersion: 3,
            versions: [1, 3, 4],
        });

        await expect(
            (props?.onChange as (version: number, targetId?: string) => Promise<unknown>)(4, '52')
        ).resolves.toBe(changeResult);

        expect(hoisted.changeVersionMutateAsyncMock).toHaveBeenCalledWith({
            connectedUserId: '7',
            projectVersion: 4,
            sourceId: '51',
            targetId: '52',
        });
    });

    it('loads the move-impact preview for the target the change version dialog reports', async () => {
        hoisted.configurations = [
            createConfiguration('51', {projectVersion: 3}),
            createConfiguration('52', {projectVersion: 4}),
        ];

        const user = userEvent.setup();

        renderMenu();

        await user.click(screen.getByRole('button', {name: 'Configuration Actions'}));
        await user.click(await screen.findByRole('menuitem', {name: 'Change version'}));

        expect(await screen.findByText('change version dialog')).toBeInTheDocument();

        act(() => (hoisted.changeVersionDialogProps.at(-1)?.onTargetChange as (targetId?: string) => void)('52'));

        expect(hoisted.moveImpactQueryMock).toHaveBeenLastCalledWith(
            {connectedUserIds: ['7'], sourceId: '51', targetId: '52'},
            {enabled: true}
        );
        expect(hoisted.changeVersionDialogProps.at(-1)?.impact).toEqual([
            expect.objectContaining({affectedConnectedUserCount: 1, label: 'Sync'}),
        ]);
    });

    it('disables both actions with a reason when the configurations fail to load', async () => {
        hoisted.configurations = undefined;
        hoisted.configurationsError = true;

        const user = userEvent.setup();

        renderMenu();

        await user.click(screen.getByRole('button', {name: 'Configuration Actions'}));

        const changeVersionItem = await screen.findByRole('menuitem', {name: 'Change version'});

        expect(changeVersionItem).toHaveAttribute('aria-disabled', 'true');
        expect(screen.getByRole('menuitem', {name: 'Change configuration'})).toHaveAttribute('aria-disabled', 'true');

        await user.hover(changeVersionItem.parentElement as HTMLElement);

        await waitFor(async () =>
            expect(
                (await screen.findAllByText("Couldn't load the configurations. Try again later.")).length
            ).toBeGreaterThan(0)
        );
    });

    it('disables Change version with a reason when no other version is published', async () => {
        hoisted.configurations = [createConfiguration('51'), createConfiguration('52')];
        hoisted.versions = [
            {status: 'PUBLISHED', version: 3},
            {status: 'DRAFT', version: 4},
        ];

        const user = userEvent.setup();

        renderMenu();

        await user.click(screen.getByRole('button', {name: 'Configuration Actions'}));

        const changeVersionItem = await screen.findByRole('menuitem', {name: 'Change version'});

        expect(changeVersionItem).toHaveAttribute('aria-disabled', 'true');

        await user.hover(changeVersionItem.parentElement as HTMLElement);

        await waitFor(async () =>
            expect((await screen.findAllByText('No other published version')).length).toBeGreaterThan(0)
        );
    });

    it('disables both actions while the configurations load', async () => {
        hoisted.configurations = undefined;

        const user = userEvent.setup();

        renderMenu();

        await user.click(screen.getByRole('button', {name: 'Configuration Actions'}));

        expect(await screen.findByRole('menuitem', {name: 'Change configuration'})).toHaveAttribute(
            'aria-disabled',
            'true'
        );
        expect(screen.getByRole('menuitem', {name: 'Change version'})).toHaveAttribute('aria-disabled', 'true');
    });

    const openChangeConfiguration = async () => {
        hoisted.configurations = [createConfiguration('51'), createConfiguration('52')];

        const user = userEvent.setup();

        renderMenu();

        await user.click(screen.getByRole('button', {name: 'Configuration Actions'}));
        await user.click(await screen.findByRole('menuitem', {name: 'Change configuration'}));

        await screen.findByText('dialog Change configuration');

        return hoisted.dialogProps.at(-1)?.onMoved as (moved: Record<string, unknown>) => void;
    };

    const impactWorkflows = [
        {newRequiredConnections: [], newRequiredInputs: [], workflowUuid: 'wf-sync'},
        {newRequiredConnections: [], newRequiredInputs: ['contactPageUrl'], workflowUuid: 'wf-rate'},
    ];

    it('opens the edit dialog on the Inputs tab for the workflow that needs new inputs after the user moves', async () => {
        const onMoved = await openChangeConfiguration();

        act(() =>
            onMoved({
                impactWorkflows,
                result: {movedConnectedUserIds: ['7'], needsAttentionConnectedUserIds: ['7'], skipped: []},
                targetId: '52',
            })
        );

        expect(await screen.findByText('edit dialog Rate Ticket')).toBeInTheDocument();
        expect(screen.queryByText('dialog Change configuration')).not.toBeInTheDocument();
        expect(hoisted.editDialogProps.at(-1)).toMatchObject({
            configurationId: '52',
            connectedUserId: 7,
            connectedUserProjectWorkflowId: '902',
            defaultTab: 'inputs',
            projectId: '10',
            workflowUuid: 'wf-rate',
        });
    });

    it('keeps the move result open when the moved user needs no attention', async () => {
        const onMoved = await openChangeConfiguration();

        act(() =>
            onMoved({
                impactWorkflows,
                result: {movedConnectedUserIds: ['7'], needsAttentionConnectedUserIds: [], skipped: []},
                targetId: '52',
            })
        );

        expect(screen.getByText('dialog Change configuration')).toBeInTheDocument();
        expect(hoisted.editDialogProps).toHaveLength(0);
    });
});
