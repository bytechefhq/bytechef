import {TooltipProvider} from '@/components/ui/tooltip';
import ProjectTabButtons from '@/pages/automation/project/components/project-header/components/settings-menu/components/ProjectTabButtons/ProjectTabButtons';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

// Mock the feature flags store
vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => (flag: string) => flag === 'ff-2482', // Enable ff_2482 for export functionality, disable others
}));

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
    hiddenFileInputRef: {current: null} as React.RefObject<HTMLInputElement>,
    onCloseDropdownMenuClick: vi.fn(),
    onDeleteProjectClick: vi.fn(),
    onDuplicateProjectClick: vi.fn(),
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
                    <ProjectTabButtons {...props} />
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

        const exportButton = screen.getByText('Export');
        expect(exportButton).toBeInTheDocument();
    });

    it('should have correct export URL when export button is clicked', async () => {
        renderProjectTabButtons();

        const exportButton = screen.getByText('Export');
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

        const exportButton = screen.getByText('Export');
        await userEvent.click(exportButton);

        expect(window.location.href).toBe('/api/automation/internal/projects/456/export');
    });

    it('should call onCloseDropdownMenuClick when export button is clicked', async () => {
        renderProjectTabButtons();

        const exportButton = screen.getByText('Export');
        await userEvent.click(exportButton);

        // The handleButtonClick function should trigger onCloseDropdownMenuClick
        expect(mockProps.onCloseDropdownMenuClick).toHaveBeenCalled();
    });

    it('should render other action buttons correctly', () => {
        renderProjectTabButtons();

        expect(screen.getByText('Edit')).toBeInTheDocument();
        expect(screen.getByText('Duplicate')).toBeInTheDocument();
        expect(screen.getByText('Export')).toBeInTheDocument();
        expect(screen.getByText('Project History')).toBeInTheDocument();
        expect(screen.getByText('Delete')).toBeInTheDocument();
    });

    it('should call appropriate handlers when buttons are clicked', async () => {
        renderProjectTabButtons();

        // Test Edit button
        await userEvent.click(screen.getByText('Edit'));
        expect(mockProps.onShowEditProjectDialogClick).toHaveBeenCalled();

        // Test Duplicate button
        await userEvent.click(screen.getByText('Duplicate'));
        expect(mockProps.onDuplicateProjectClick).toHaveBeenCalled();

        // Test Project History button
        await userEvent.click(screen.getByText('Project History'));
        expect(mockProps.onShowProjectVersionHistorySheet).toHaveBeenCalled();

        // Test Delete button
        await userEvent.click(screen.getByText('Delete'));
        expect(mockProps.onDeleteProjectClick).toHaveBeenCalled();
    });

    it('should not show Git-related buttons when feature flag is disabled', () => {
        renderProjectTabButtons();

        // These buttons should not be visible when ff_1039 is disabled
        expect(screen.queryByText('Pull Project from Git')).not.toBeInTheDocument();
        expect(screen.queryByText('Git Configuration')).not.toBeInTheDocument();
    });
});
