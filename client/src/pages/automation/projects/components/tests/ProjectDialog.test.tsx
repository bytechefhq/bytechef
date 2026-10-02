import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ProjectDialog from '../ProjectDialog';

const hoisted = vi.hoisted(() => ({
    createMutate: vi.fn(),
    updateMutate: vi.fn(),
}));

vi.mock('@/shared/mutations/automation/projects.mutations', () => ({
    useCreateProjectMutation: () => ({mutate: hoisted.createMutate, reset: vi.fn()}),
    useUpdateProjectMutation: () => ({mutate: hoisted.updateMutate, reset: vi.fn()}),
}));

vi.mock('@/shared/queries/automation/projectCategories.queries', () => ({
    ProjectCategoryKeys: {projectCategories: () => ['projectCategories']},
    useGetProjectCategoriesQuery: () => ({data: [], error: null, isLoading: false}),
}));

vi.mock('@/shared/queries/automation/projectTags.queries', () => ({
    ProjectTagKeys: {projectTags: ['projectTags']},
    useGetProjectTagsQuery: () => ({data: [], error: null, isLoading: false}),
}));

vi.mock('@/shared/queries/automation/projects.queries', () => ({
    ProjectKeys: {project: (id: number) => ['project', id], projects: ['projects']},
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: Record<string, unknown>) => unknown) => selector({currentWorkspaceId: 1}),
}));

vi.mock('@/shared/hooks/useAnalytics', () => ({
    useAnalytics: () => ({captureProjectCreated: vi.fn()}),
}));

const onClose = vi.fn();

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('ProjectDialog', () => {
    describe('create mode', () => {
        it('should render the create title and description', () => {
            render(<ProjectDialog onClose={onClose} />);

            expect(screen.getByText('Create Project')).toBeInTheDocument();
            expect(
                screen.getByText('Use this to create your project which will contain workflows')
            ).toBeInTheDocument();
        });

        it('should render empty name and description fields', () => {
            render(<ProjectDialog onClose={onClose} />);

            expect(screen.getByLabelText('Name')).toHaveValue('');
            expect(screen.getByLabelText('Description')).toHaveValue('');
        });

        it('should create the project on save', async () => {
            const user = userEvent.setup();

            render(<ProjectDialog onClose={onClose} />);

            await user.type(screen.getByLabelText('Name'), 'New project');
            await user.click(screen.getByRole('button', {name: 'Save'}));

            expect(hoisted.createMutate).toHaveBeenCalledTimes(1);
            expect(hoisted.updateMutate).not.toHaveBeenCalled();
        });
    });

    describe('edit mode', () => {
        it('should render the edit title and description', () => {
            render(<ProjectDialog onClose={onClose} project={{id: 1, name: 'Existing', workspaceId: 1}} />);

            expect(screen.getByText('Edit Project')).toBeInTheDocument();
            expect(screen.getByText('Use this to edit your project which will contain workflows')).toBeInTheDocument();
        });

        it('should prefill the name from the project', () => {
            render(<ProjectDialog onClose={onClose} project={{id: 1, name: 'Existing', workspaceId: 1}} />);

            expect(screen.getByLabelText('Name')).toHaveValue('Existing');
        });

        it('should update the project on save', async () => {
            const user = userEvent.setup();

            render(<ProjectDialog onClose={onClose} project={{id: 1, name: 'Existing', workspaceId: 1}} />);

            await user.click(screen.getByRole('button', {name: 'Save'}));

            expect(hoisted.updateMutate).toHaveBeenCalledTimes(1);
            expect(hoisted.createMutate).not.toHaveBeenCalled();
        });
    });

    describe('dialog chrome', () => {
        it('should render the close, cancel and save controls', () => {
            render(<ProjectDialog onClose={onClose} />);

            expect(screen.getByRole('button', {name: 'Close'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Save'})).toBeInTheDocument();
        });

        it('should keep the cancel button out of the form submission', () => {
            render(<ProjectDialog onClose={onClose} />);

            expect(screen.getByRole('button', {name: 'Cancel'})).toHaveAttribute('type', 'button');
        });

        // The dialog takes its accessible name from the header title via
        // aria-labelledby, which wins over the aria-label on DialogContent.
        it('should name the dialog after its title', () => {
            render(<ProjectDialog onClose={onClose} />);

            expect(screen.getByRole('dialog', {name: 'Create Project'})).toBeInTheDocument();
        });
    });

    describe('interactions', () => {
        it('should call onClose when cancel is clicked', async () => {
            const user = userEvent.setup();

            render(<ProjectDialog onClose={onClose} />);

            await user.click(screen.getByRole('button', {name: 'Cancel'}));

            expect(onClose).toHaveBeenCalledTimes(1);
        });

        it('should call onClose when the close button is clicked', async () => {
            const user = userEvent.setup();

            render(<ProjectDialog onClose={onClose} />);

            await user.click(screen.getByRole('button', {name: 'Close'}));

            expect(onClose).toHaveBeenCalledTimes(1);
        });

        it('should not create the project without a name', async () => {
            const user = userEvent.setup();

            render(<ProjectDialog onClose={onClose} />);

            await user.click(screen.getByRole('button', {name: 'Save'}));

            expect(hoisted.createMutate).not.toHaveBeenCalled();
        });
    });
});
