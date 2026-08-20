import {TooltipProvider} from '@/components/ui/tooltip';
import SettingsMenu from '@/pages/automation/project/components/project-header/components/settings-menu/SettingsMenu';
import {Project, Workflow} from '@/shared/middleware/automation/configuration';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {ReactNode} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {projectVisibilityDialogSpy} = vi.hoisted(() => ({
    projectVisibilityDialogSpy: vi.fn(),
}));

// The two children are stubbed so this test pins ONE thing: SettingsMenu's own state wiring - the handler it
// hands down, the state it flips, and the props it passes to the dialog. The real components are covered by
// their own test files.
vi.mock(
    '@/pages/automation/project/components/project-header/components/settings-menu/components/ProjectTabButtons/ProjectTabButtons',
    () => ({
        default: ({onShowVisibilityDialog}: {onShowVisibilityDialog: () => void}) => (
            <button onClick={onShowVisibilityDialog} type="button">
                stub visibility item
            </button>
        ),
    })
);

vi.mock('@/pages/automation/project/components/ProjectVisibilityDialog', () => ({
    default: (props: {projectId: number; visibility?: string}) => {
        projectVisibilityDialogSpy(props);

        return <div>stub visibility dialog</div>;
    },
}));

vi.mock('@/pages/automation/project/components/project-header/components/settings-menu/hooks/useSettingsMenu', () => ({
    useSettingsMenu: () => ({
        handleDeleteProjectAlertDialogClick: vi.fn(),
        handleDeleteWorkflowAlertDialogClick: vi.fn(),
        handleDuplicateProjectClick: vi.fn(),
        handleDuplicateWorkflowClick: vi.fn(),
        handlePullProjectFromGitClick: vi.fn(),
        handleUpdateProjectGitConfigurationSubmit: vi.fn(),
        projectGitConfiguration: undefined,
        projectVersions: [],
    }),
}));

const project = {
    id: 5,
    lastProjectVersion: 1,
    name: 'Alpha',
    visibility: 'PRIVATE',
    workspaceId: 7,
} as never as Project;

const workflow = {id: 'workflow-1', projectWorkflowId: 11} as never as Workflow;

let queryClient: QueryClient;

const Wrapper = ({children}: {children: ReactNode}) => (
    <MemoryRouter>
        <QueryClientProvider client={queryClient}>
            <TooltipProvider>{children}</TooltipProvider>
        </QueryClientProvider>
    </MemoryRouter>
);

beforeEach(() => {
    vi.clearAllMocks();

    queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});
});

describe('SettingsMenu visibility dialog wiring', () => {
    it('opens the visibility dialog for the current project only after the menu item is used', async () => {
        render(<SettingsMenu project={project} updateWorkflowMutation={undefined as never} workflow={workflow} />, {
            wrapper: Wrapper,
        });

        await userEvent.click(screen.getByLabelText('Settings'));
        await userEvent.click(screen.getByLabelText('Project tab'));

        expect(screen.queryByText('stub visibility dialog')).not.toBeInTheDocument();

        await userEvent.click(screen.getByText('stub visibility item'));

        expect(screen.getByText('stub visibility dialog')).toBeInTheDocument();
        expect(projectVisibilityDialogSpy).toHaveBeenCalledWith(
            expect.objectContaining({projectId: 5, visibility: 'PRIVATE'})
        );
    });
});
