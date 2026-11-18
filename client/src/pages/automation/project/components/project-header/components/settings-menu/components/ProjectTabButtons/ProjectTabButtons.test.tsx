import {DropdownMenu, DropdownMenuContent} from '@/components/DropdownMenu/DropdownMenu';
import {TooltipProvider} from '@/components/ui/tooltip';
import ProjectTabButtons from '@/pages/automation/project/components/project-header/components/settings-menu/components/ProjectTabButtons/ProjectTabButtons';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const createTestQueryClient = () =>
    new QueryClient({
        defaultOptions: {
            queries: {
                retry: false,
            },
        },
    });

let queryClient: QueryClient;

beforeEach(() => {
    queryClient = createTestQueryClient();
});

afterEach(() => {
    queryClient.clear();
});

const mockProps = {
    importN8nWorkflowDisabled: false,
    onDeleteProjectClick: vi.fn(),
    onDuplicateProjectClick: vi.fn(),
    onImportN8nWorkflowClick: vi.fn(),
    onImportWorkflowClick: vi.fn(),
    onNewWorkflowClick: vi.fn(),
    onNewWorkflowFromTemplateClick: vi.fn(),
    onPullProjectFromGitClick: vi.fn(),
    onShareProject: vi.fn(),
    onShowEditProjectDialogClick: vi.fn(),
    onShowProjectGitConfigurationDialog: vi.fn(),
    onShowProjectVersionHistorySheet: vi.fn(),
    projectGitConfigurationEnabled: false,
    projectId: 123,
};

const renderProjectTabButtons = (props = mockProps) => {
    render(
        <MemoryRouter>
            <QueryClientProvider client={queryClient}>
                <TooltipProvider>
                    <DropdownMenu open>
                        <DropdownMenuContent>
                            <ProjectTabButtons {...props} />
                        </DropdownMenuContent>
                    </DropdownMenu>
                </TooltipProvider>
            </QueryClientProvider>
        </MemoryRouter>
    );
};

describe('ProjectTabButtons Export Functionality', () => {
    beforeEach(() => {
        // Mock window.location.href assignment
        Object.defineProperty(window, 'location', {
            value: {
                href: '',
            },
            writable: true,
        });

        // Clear all mocks
        vi.clearAllMocks();
    });

    it('should render export button', () => {
        renderProjectTabButtons();

        const exportButton = screen.getByRole('menuitem', {name: 'Export'});
        expect(exportButton).toBeInTheDocument();
    });

    it('should have correct export URL when export button is clicked', async () => {
        renderProjectTabButtons();

        const exportButton = screen.getByRole('menuitem', {name: 'Export'});
        expect(exportButton).toBeInTheDocument();

        // Click the export button
        await userEvent.click(exportButton);

        // Check that window.location.href was set to the correct URL
        expect(window.location.href).toBe('/api/automation/internal/projects/123/export');
    });

    it('should use correct project ID in export URL', async () => {
        const customProps = {
            ...mockProps,
            projectId: 456,
        };

        renderProjectTabButtons(customProps);

        const exportButton = screen.getByRole('menuitem', {name: 'Export'});
        await userEvent.click(exportButton);

        expect(window.location.href).toBe('/api/automation/internal/projects/456/export');
    });

    it('should render other action buttons correctly', () => {
        renderProjectTabButtons();

        expect(screen.getByRole('menuitem', {name: 'Edit'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Duplicate'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Export'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Project History'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Delete'})).toBeInTheDocument();
    });

    it('should call appropriate handlers when buttons are clicked', async () => {
        renderProjectTabButtons();

        // Test Edit button
        await userEvent.click(screen.getByRole('menuitem', {name: 'Edit'}));
        expect(mockProps.onShowEditProjectDialogClick).toHaveBeenCalled();

        // Test Duplicate button
        await userEvent.click(screen.getByRole('menuitem', {name: 'Duplicate'}));
        expect(mockProps.onDuplicateProjectClick).toHaveBeenCalled();

        // Test Project History button
        await userEvent.click(screen.getByRole('menuitem', {name: 'Project History'}));
        expect(mockProps.onShowProjectVersionHistorySheet).toHaveBeenCalled();

        // Test Delete button
        await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));
        expect(mockProps.onDeleteProjectClick).toHaveBeenCalled();
    });

    it('should call the workflow creation handlers when their buttons are clicked', async () => {
        renderProjectTabButtons();

        await userEvent.click(screen.getByRole('menuitem', {name: 'New Workflow'}));
        expect(mockProps.onNewWorkflowClick).toHaveBeenCalled();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Workflow from Template'}));
        expect(mockProps.onNewWorkflowFromTemplateClick).toHaveBeenCalled();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Import Workflow'}));
        expect(mockProps.onImportWorkflowClick).toHaveBeenCalled();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Import n8n Workflow'}));
        expect(mockProps.onImportN8nWorkflowClick).toHaveBeenCalled();
    });

    it('should disable Import n8n Workflow when no AI provider is enabled', () => {
        renderProjectTabButtons({...mockProps, importN8nWorkflowDisabled: true});

        expect(screen.getByRole('menuitem', {name: 'Import n8n Workflow'})).toHaveAttribute('aria-disabled', 'true');
    });

    it('should not show Git-related buttons when feature flag is disabled', () => {
        renderProjectTabButtons();

        // These buttons should not be visible when ff_1039 is disabled
        expect(screen.queryByText('Pull Project from Git')).not.toBeInTheDocument();
        expect(screen.queryByText('Git Configuration')).not.toBeInTheDocument();
    });

    it('should keep only the project actions that support code workflows', () => {
        renderProjectTabButtons({...mockProps, codeWorkflow: true} as typeof mockProps);

        for (const label of ['Edit', 'Share', 'Export', 'Project History', 'Delete']) {
            expect(screen.getByRole('menuitem', {name: label})).toBeInTheDocument();
        }

        for (const label of [
            'Duplicate',
            'New Workflow',
            'Workflow from Template',
            'Import Workflow',
            'Import n8n Workflow',
        ]) {
            expect(screen.queryByRole('menuitem', {name: label})).not.toBeInTheDocument();
        }
    });
});
