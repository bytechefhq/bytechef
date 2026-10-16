import {TooltipProvider} from '@/components/ui/tooltip';
import WorkflowBuilderHeader from '@/ee/pages/embedded/workflow-builder/components/workflow-builder-header/WorkflowBuilderHeader';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {QueryClient, QueryClientProvider, onlineManager} from '@tanstack/react-query';
import {act, render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const {isMutatingMock} = vi.hoisted(() => ({
    isMutatingMock: vi.fn(),
}));

vi.mock('@tanstack/react-query', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@tanstack/react-query')>();

    return {
        ...actual,
        useIsMutating: isMutatingMock,
    };
});

vi.mock(
    '@/ee/pages/embedded/workflow-builder/components/workflow-builder-header/hooks/useWorkflowBuilderHeader',
    () => ({
        useWorkflowBuilderHeader: () => ({
            handlePublishProjectSubmit: vi.fn(),
            handleRunClick: vi.fn(),
            handleShowOutputClick: vi.fn(),
            handleStopClick: vi.fn(),
            publishProjectMutationIsPending: false,
        }),
    })
);

vi.mock('@/ee/pages/embedded/workflow-builder/components/workflow-builder-header/components/PublishPopover', () => ({
    default: () => null,
}));

vi.mock(
    '@/ee/pages/embedded/workflow-builder/components/workflow-builder-header/components/WorkflowActionsButton',
    () => ({
        default: () => null,
    })
);

vi.mock('@/shared/components/workflow/WorkflowDialog', () => ({
    default: () => null,
}));

const renderHeader = () =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <TooltipProvider>
                <WorkflowBuilderHeader
                    bottomResizablePanelRef={{current: null}}
                    projectId={1}
                    runDisabled={false}
                    updateWorkflowMutation={{} as never}
                    workflowVersion={1}
                />
            </TooltipProvider>
        </QueryClientProvider>
    );

describe('WorkflowBuilderHeader', () => {
    beforeEach(() => {
        useWorkflowDataStore.setState({workflow: {id: 'workflow-1', label: 'Daily standup reminder'}} as never);

        useWorkflowEditorStore.setState({showEditWorkflowDialog: false});

        isMutatingMock.mockReturnValue(0);

        onlineManager.setOnline(true);
    });

    afterEach(() => {
        onlineManager.setOnline(true);

        vi.clearAllMocks();
    });

    it('hides the status indicator while everything is saved', () => {
        renderHeader();

        expect(screen.getByText('Daily standup reminder')).toBeInTheDocument();
        expect(screen.queryByLabelText('Loading indicator')).not.toBeInTheDocument();
    });

    it('shows the status indicator while a mutation is saving', () => {
        isMutatingMock.mockReturnValue(1);

        renderHeader();

        expect(screen.getByLabelText('Loading indicator')).toBeInTheDocument();
    });

    it('shows the status indicator as soon as the app goes offline', () => {
        renderHeader();

        expect(screen.queryByLabelText('Loading indicator')).not.toBeInTheDocument();

        act(() => {
            onlineManager.setOnline(false);
        });

        expect(screen.getByLabelText('Loading indicator')).toBeInTheDocument();

        act(() => {
            onlineManager.setOnline(true);
        });

        expect(screen.queryByLabelText('Loading indicator')).not.toBeInTheDocument();
    });

    it('opens the workflow edit dialog from the settings menu', async () => {
        renderHeader();

        expect(screen.queryByRole('button', {name: 'Edit workflow'})).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Settings'}));

        await userEvent.click(await screen.findByRole('menuitem', {name: 'Edit'}));

        await waitFor(() => {
            expect(useWorkflowEditorStore.getState().showEditWorkflowDialog).toBe(true);
        });
    });
});
