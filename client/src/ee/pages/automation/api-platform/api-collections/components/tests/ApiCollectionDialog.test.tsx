import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ApiCollectionDialog from '../ApiCollectionDialog';

const existingApiCollection = {
    contextPath: 'orders',
    enabled: true,
    id: 1,
    name: 'Existing',
    projectId: 2,
    projectVersion: 3,
    workspaceId: 1,
};

const hoisted = vi.hoisted(() => ({
    createMutate: vi.fn(),
    updateMutate: vi.fn(),
}));

vi.mock('@/ee/shared/mutations/automation/apiCollections.mutations', () => ({
    useCreateApiCollectionMutation: () => ({mutate: hoisted.createMutate, reset: vi.fn()}),
    useUpdateApiCollectionMutation: () => ({mutate: hoisted.updateMutate, reset: vi.fn()}),
}));

vi.mock('@/ee/shared/mutations/automation/apiCollections.queries', () => ({
    ApiCollectionKeys: {apiCollections: ['apiCollections']},
}));

vi.mock('@/ee/shared/mutations/automation/apiCollectionTags.queries', () => ({
    ApiCollectionTagKeys: {apiCollectionTags: ['apiCollectionTags']},
}));

vi.mock('@/shared/queries/automation/projects.queries', () => ({
    useGetWorkspaceProjectsQuery: () => ({data: []}),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: Record<string, unknown>) => unknown) => selector({currentWorkspaceId: 1}),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: Record<string, unknown>) => unknown) => selector({currentEnvironmentId: 1}),
}));

vi.mock('@/ee/pages/automation/api-platform/api-collections/components/ApiCollectionDialogTagsSelect', () => ({
    default: () => <div data-testid="tags-select" />,
}));

vi.mock(
    '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogBasicStepProjectsComboBox',
    () => ({
        default: () => <div data-testid="projects-combo-box" />,
    })
);

vi.mock(
    '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogBasicStepProjectVersionsSelect',
    () => ({
        default: () => <div data-testid="project-versions-select" />,
    })
);

const onClose = vi.fn();

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('ApiCollectionDialog', () => {
    describe('create mode', () => {
        it('should render the create title and the description', () => {
            render(<ApiCollectionDialog onClose={onClose} />);

            expect(screen.getByText('Create API Collection')).toBeInTheDocument();
            expect(screen.getByText('Create new API collection and connect it with a project.')).toBeInTheDocument();
        });

        it('should render the project picker', () => {
            render(<ApiCollectionDialog onClose={onClose} />);

            expect(screen.getByTestId('projects-combo-box')).toBeInTheDocument();
        });
    });

    describe('edit mode', () => {
        it('should render the edit title', () => {
            render(<ApiCollectionDialog apiCollection={existingApiCollection} onClose={onClose} />);

            expect(screen.getByText('Edit API Collection')).toBeInTheDocument();
        });

        it('should prefill the name and hide the project picker', () => {
            render(<ApiCollectionDialog apiCollection={existingApiCollection} onClose={onClose} />);

            expect(screen.getByLabelText('Name')).toHaveValue('Existing');
            expect(screen.queryByTestId('projects-combo-box')).not.toBeInTheDocument();
        });
    });

    describe('dialog chrome', () => {
        it('should render the close, cancel and save controls', () => {
            render(<ApiCollectionDialog onClose={onClose} />);

            expect(screen.getByRole('button', {name: 'Close'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Save'})).toBeInTheDocument();
        });

        it('should keep the cancel button out of the form submission', () => {
            render(<ApiCollectionDialog onClose={onClose} />);

            expect(screen.getByRole('button', {name: 'Cancel'})).toHaveAttribute('type', 'button');
        });

        it('should render the name, description and context path fields', () => {
            render(<ApiCollectionDialog onClose={onClose} />);

            expect(screen.getByLabelText('Name')).toBeInTheDocument();
            expect(screen.getByLabelText('Description')).toBeInTheDocument();
            expect(screen.getByLabelText('Context Path')).toBeInTheDocument();
        });
    });

    describe('interactions', () => {
        it('should call onClose when cancel is clicked', async () => {
            const user = userEvent.setup();

            render(<ApiCollectionDialog onClose={onClose} />);

            await user.click(screen.getByRole('button', {name: 'Cancel'}));

            expect(onClose).toHaveBeenCalledTimes(1);
        });

        it('should call onClose when the close button is clicked', async () => {
            const user = userEvent.setup();

            render(<ApiCollectionDialog onClose={onClose} />);

            await user.click(screen.getByRole('button', {name: 'Close'}));

            expect(onClose).toHaveBeenCalledTimes(1);
        });
    });
});
