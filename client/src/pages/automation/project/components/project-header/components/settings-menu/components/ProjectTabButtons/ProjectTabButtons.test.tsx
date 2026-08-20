import {TooltipProvider} from '@/components/ui/tooltip';
import ProjectTabButtons from '@/pages/automation/project/components/project-header/components/settings-menu/components/ProjectTabButtons/ProjectTabButtons';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const {mockFeatureFlags, mockUseVisibilityFeatureEnabled} = vi.hoisted(() => ({
    mockFeatureFlags: {} as Record<string, boolean>,
    mockUseVisibilityFeatureEnabled: vi.fn(),
}));

vi.mock('@/shared/hooks/useVisibilityFeatureEnabled', () => ({
    useVisibilityFeatureEnabled: mockUseVisibilityFeatureEnabled,
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => (featureFlag: string) => mockFeatureFlags[featureFlag] ?? false,
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
    hiddenFileInputRef: {current: null} as React.RefObject<HTMLInputElement | null>,
    onCloseDropdownMenuClick: vi.fn(),
    onDeleteProjectClick: vi.fn(),
    onDuplicateProjectClick: vi.fn(),
    onMembersClick: vi.fn(),
    onPullProjectFromGitClick: vi.fn(),
    onShareProject: vi.fn(),
    onShowEditProjectDialogClick: vi.fn(),
    onShowErrorWorkflowDialog: vi.fn(),
    onShowProjectGitConfigurationDialog: vi.fn(),
    onShowProjectVersionHistorySheet: vi.fn(),
    onShowVisibilityDialog: vi.fn(),
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

        Object.keys(mockFeatureFlags).forEach((featureFlag) => delete mockFeatureFlags[featureFlag]);

        mockUseVisibilityFeatureEnabled.mockReturnValue({enabled: false, isAdmin: false, workspaceId: undefined});
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

        // These buttons should not be visible when gitIntegrationEnabled (ff-1039) is disabled
        expect(screen.queryByText('Pull Project from Git')).not.toBeInTheDocument();
        expect(screen.queryByText('Git Configuration')).not.toBeInTheDocument();
    });
});

describe('ProjectTabButtons Visibility Item', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        Object.keys(mockFeatureFlags).forEach((featureFlag) => delete mockFeatureFlags[featureFlag]);
    });

    it('should render the Visibility item alongside Share in EE and call its handler', async () => {
        mockUseVisibilityFeatureEnabled.mockReturnValue({enabled: true, isAdmin: false, workspaceId: 1});

        // ff-1042 gates the existing outward-publishing Share item; enabling it is what lets this test prove
        // the two affordances coexist rather than one having replaced the other.
        mockFeatureFlags['ff-1042'] = true;

        renderProjectTabButtons();

        // Withholding (Visibility) and publishing (Share) are distinct affordances - a later edit must not
        // collapse them into one item.
        expect(screen.getByLabelText('Share ProjectButton')).toBeInTheDocument();

        const visibilityButton = screen.getByLabelText('Project Visibility Button');

        expect(visibilityButton).toBeInTheDocument();

        await userEvent.click(visibilityButton);

        expect(mockProps.onShowVisibilityDialog).toHaveBeenCalled();
    });

    // There is deliberately no "without a workspace in context" case here. The component reads only `enabled`, and
    // VisibilityFeatureType makes {enabled: true, workspaceId: undefined} unrepresentable — so such a test would be
    // effect-identical to the CE one below while claiming to construct a state it cannot. The hook is what folds the
    // workspace into `enabled`, and useVisibilityFeatureEnabled.test.ts pins that ("returns enabled=false on EE when
    // no workspace is selected").
    it('should not render the Visibility item in CE', () => {
        // CE with a workspace: the edition half of the gate is what withholds the item here.
        mockUseVisibilityFeatureEnabled.mockReturnValue({enabled: false, isAdmin: false, workspaceId: 1});

        mockFeatureFlags['ff-1042'] = true;

        renderProjectTabButtons();

        // Anchor: the menu itself rendered, so the absence below is the edition gate and not a failed render.
        expect(screen.getByLabelText('Share ProjectButton')).toBeInTheDocument();
        expect(screen.queryByLabelText('Project Visibility Button')).not.toBeInTheDocument();
    });
});
