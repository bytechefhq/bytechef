import {TooltipProvider} from '@/components/ui/tooltip';
import AutomationConfigurationsSheet from '@/ee/pages/embedded/automation/components/automation-editor/components/AutomationConfigurationsSheet';
import {AutomationProjectsQuery} from '@/shared/middleware/graphql';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

type AutomationProjectType = AutomationProjectsQuery['automationProjects'][number];

const hoisted = vi.hoisted(() => ({
    configurationDialogProps: vi.fn(),
    configurationsQuery: vi.fn(),
    listProps: vi.fn(),
    navigate: vi.fn(),
    publishDialogProps: vi.fn(),
    refetch: vi.fn(),
}));

vi.mock('react-router-dom', async (importOriginal) => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    useNavigate: () => hoisted.navigate,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAutomationProjectConfigurationsQuery: (variables: unknown, options: unknown) => {
        hoisted.configurationsQuery(variables, options);

        return {isFetching: false, refetch: hoisted.refetch};
    },
}));

vi.mock(
    '@/ee/pages/embedded/automations/components/automation-project-configuration-list/AutomationProjectConfigurationList',
    () => ({
        default: (props: {
            environmentId: number;
            onCreate: () => void;
            onPublishProject?: (projectId: string) => void;
            onUpdateVersion: (configuration: {id: string}) => void;
            project: AutomationProjectType;
        }) => {
            hoisted.listProps(props);

            return (
                <div>
                    <span>{`Configurations of ${props.project.name}`}</span>

                    <button onClick={props.onCreate} type="button">
                        Create Configuration
                    </button>

                    <button onClick={() => props.onUpdateVersion({id: 'configuration-1'})} type="button">
                        Change Project Version
                    </button>

                    {props.onPublishProject && (
                        <button onClick={() => props.onPublishProject?.(props.project.id)} type="button">
                            Publish Project
                        </button>
                    )}
                </div>
            );
        },
    })
);

vi.mock(
    '@/ee/pages/embedded/automations/components/automation-project-configuration-dialog/AutomationProjectConfigurationDialog',
    () => ({
        default: (props: Record<string, unknown>) => {
            hoisted.configurationDialogProps(props);

            return <div>Configuration Dialog</div>;
        },
    })
);

vi.mock(
    '@/ee/pages/embedded/automations/components/automation-project-publish-dialog/AutomationProjectPublishDialog',
    () => ({
        default: (props: {onClose: () => void; project: AutomationProjectType}) => {
            hoisted.publishDialogProps(props);

            return (
                <div>
                    <span>{`Publish ${props.project.name}`}</span>

                    <button onClick={props.onClose} type="button">
                        Close publish dialog
                    </button>
                </div>
            );
        },
    })
);

const onSheetOpenChange = vi.fn();

const project = {id: 'project-1', name: 'Mailing'} as AutomationProjectType;

const renderAutomationConfigurationsSheet = () =>
    render(
        <TooltipProvider>
            <AutomationConfigurationsSheet onSheetOpenChange={onSheetOpenChange} project={project} sheetOpen />
        </TooltipProvider>
    );

describe('AutomationConfigurationsSheet', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('shows the development configurations of the project', () => {
        renderAutomationConfigurationsSheet();

        expect(screen.getByText('Automation Configurations')).toBeInTheDocument();
        expect(screen.getByText('Configurations of Mailing')).toBeInTheDocument();
        expect(hoisted.listProps).toHaveBeenCalledWith(expect.objectContaining({environmentId: 0, project}));
        expect(hoisted.configurationsQuery).toHaveBeenCalledWith(
            {environmentId: '0', projectId: 'project-1'},
            {enabled: true}
        );
    });

    it('refetches the configurations from the refresh button', async () => {
        renderAutomationConfigurationsSheet();

        await userEvent.click(screen.getByRole('button', {name: 'Refresh automation configurations'}));

        expect(hoisted.refetch).toHaveBeenCalledTimes(1);
    });

    it('opens the configuration dialog for the project from the list', async () => {
        renderAutomationConfigurationsSheet();

        expect(screen.queryByText('Configuration Dialog')).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Change Project Version'}));

        expect(screen.getByText('Configuration Dialog')).toBeInTheDocument();
        expect(hoisted.configurationDialogProps).toHaveBeenCalledWith(
            expect.objectContaining({configuration: {id: 'configuration-1'}, project, updateProjectVersion: true})
        );
    });

    it('opens the publish dialog for the project from the list', async () => {
        renderAutomationConfigurationsSheet();

        expect(screen.queryByText('Publish Mailing')).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Publish Project'}));

        expect(screen.getByText('Publish Mailing')).toBeInTheDocument();
        expect(hoisted.publishDialogProps).toHaveBeenCalledWith(expect.objectContaining({project}));

        await userEvent.click(screen.getByRole('button', {name: 'Close publish dialog'}));

        expect(screen.queryByText('Publish Mailing')).not.toBeInTheDocument();
    });

    it('closes from the Close button', async () => {
        renderAutomationConfigurationsSheet();

        const footerCloseButton = screen
            .getAllByRole('button', {name: 'Close'})
            .find((button) => button.textContent === 'Close');

        await userEvent.click(footerCloseButton!);

        expect(onSheetOpenChange).toHaveBeenCalledWith(false);
        expect(hoisted.navigate).not.toHaveBeenCalled();
    });

    it('offers only Close in the footer, without a link to the configurations page', () => {
        renderAutomationConfigurationsSheet();

        expect(screen.queryByRole('button', {name: 'Open in Configurations'})).not.toBeInTheDocument();
    });
});
