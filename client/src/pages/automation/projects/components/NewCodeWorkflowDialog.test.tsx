import {CodeWorkflowLanguage} from '@/shared/middleware/graphql';
import {render, resetAll, screen, waitFor} from '@/shared/util/test-utils';
import userEvent from '@testing-library/user-event';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import NewCodeWorkflowDialog from './NewCodeWorkflowDialog';

const hoisted = vi.hoisted(() => ({
    mockGetProject: vi.fn(),
    mockMutate: vi.fn(),
    mockNavigate: vi.fn(),
    mockOnClose: vi.fn(),
    mockUseCreateCodeWorkflowMutation: vi.fn(),
}));

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual<Record<string, unknown>>('react-router-dom');

    return {
        ...actual,
        useNavigate: () => hoisted.mockNavigate,
    };
});

vi.mock('@/shared/middleware/graphql', async () => {
    const actual = await vi.importActual<Record<string, unknown>>('@/shared/middleware/graphql');

    return {
        ...actual,
        useCreateCodeWorkflowMutation: hoisted.mockUseCreateCodeWorkflowMutation,
    };
});

vi.mock('@/shared/middleware/automation/configuration', async () => {
    const actual = await vi.importActual<Record<string, unknown>>('@/shared/middleware/automation/configuration');

    class MockProjectApi {
        getProject = hoisted.mockGetProject;
    }

    return {
        ...actual,
        ProjectApi: MockProjectApi,
    };
});

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: vi.fn(() => 100),
}));

const renderDialog = () =>
    render(
        <MemoryRouter initialEntries={['/projects']}>
            <Routes>
                <Route element={<NewCodeWorkflowDialog onClose={hoisted.mockOnClose} />} path="*" />
            </Routes>
        </MemoryRouter>
    );

beforeEach(() => {
    hoisted.mockGetProject.mockResolvedValue({id: 42, projectWorkflowIds: [4200]});

    hoisted.mockUseCreateCodeWorkflowMutation.mockImplementation(
        (options?: {onSuccess?: (data: {createCodeWorkflow: string}) => void}) => ({
            isPending: false,
            mutate: (variables: {language: CodeWorkflowLanguage; name: string; workspaceId: number}) => {
                hoisted.mockMutate(variables);

                options?.onSuccess?.({createCodeWorkflow: '42'});
            },
        })
    );
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('NewCodeWorkflowDialog', () => {
    it('creates a JavaScript code workflow and navigates to the new project on success', async () => {
        const user = userEvent.setup();

        renderDialog();

        await user.type(screen.getByLabelText('Project Name'), 'my-code-workflow');

        await user.click(screen.getByRole('button', {name: /create/i}));

        expect(hoisted.mockMutate).toHaveBeenCalledWith({
            categoryId: undefined,
            description: undefined,
            language: CodeWorkflowLanguage.Javascript,
            name: 'my-code-workflow',
            tags: [],
            workspaceId: 100,
        });

        await waitFor(() => {
            expect(hoisted.mockNavigate).toHaveBeenCalledWith('/automation/projects/42/project-workflows/4200');
        });

        expect(hoisted.mockOnClose).toHaveBeenCalled();
    });

    it('keeps the dialog open when the mutation does not call onSuccess (e.g. a server-side error)', async () => {
        hoisted.mockUseCreateCodeWorkflowMutation.mockImplementation(() => ({
            isPending: false,
            mutate: (variables: {language: CodeWorkflowLanguage; name: string; workspaceId: number}) => {
                hoisted.mockMutate(variables);
            },
        }));

        const user = userEvent.setup();

        renderDialog();

        await user.type(screen.getByLabelText('Project Name'), 'duplicate-name');

        await user.click(screen.getByRole('button', {name: /create/i}));

        expect(hoisted.mockMutate).toHaveBeenCalledWith({
            categoryId: undefined,
            description: undefined,
            language: CodeWorkflowLanguage.Javascript,
            name: 'duplicate-name',
            tags: [],
            workspaceId: 100,
        });

        expect(hoisted.mockNavigate).not.toHaveBeenCalled();
        expect(hoisted.mockOnClose).not.toHaveBeenCalled();
        expect(screen.getByLabelText('Project Name')).toBeInTheDocument();
    });

    it('disables the Cancel button while the mutation is pending', () => {
        hoisted.mockUseCreateCodeWorkflowMutation.mockImplementation(() => ({
            isPending: true,
            mutate: hoisted.mockMutate,
        }));

        renderDialog();

        expect(screen.getByRole('button', {name: /cancel/i})).toBeDisabled();
    });
});
