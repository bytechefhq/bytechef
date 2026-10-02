import {HttpMethod} from '@/ee/shared/middleware/automation/api-platform';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ApiCollectionEndpointDialog from '../ApiCollectionEndpointDialog';

const existingApiEndpoint = {
    enabled: true,
    httpMethod: HttpMethod.Get,
    id: 5,
    name: 'Existing',
    path: 'list',
    workflowUuid: 'uuid-1',
};

const hoisted = vi.hoisted(() => ({
    createMutate: vi.fn(),
    updateMutate: vi.fn(),
}));

vi.mock('@/ee/shared/mutations/automation/apiCollectionEndpoints.mutations', () => ({
    useCreateApiCollectionEndpointMutation: () => ({mutate: hoisted.createMutate, reset: vi.fn()}),
    useUpdateApiCollectionEndpointMutation: () => ({mutate: hoisted.updateMutate, reset: vi.fn()}),
}));

vi.mock('@/ee/shared/mutations/automation/apiCollections.queries', () => ({
    ApiCollectionKeys: {apiCollections: ['apiCollections']},
}));

vi.mock('@/shared/queries/automation/projectWorkflows.queries', () => ({
    useGetProjectVersionWorkflowsQuery: () => ({data: [{label: 'My Workflow', workflowUuid: 'uuid-1'}]}),
}));

const onClose = vi.fn();

const defaultProps = {
    apiCollectionId: 1,
    collectionVersion: 2,
    contextPath: 'orders',
    onClose,
    projectId: 3,
    projectVersion: 4,
};

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('ApiCollectionEndpointDialog', () => {
    describe('create mode', () => {
        it('should render the create title and the description', () => {
            render(<ApiCollectionEndpointDialog {...defaultProps} />);

            expect(screen.getByText('Create API Endpoint')).toBeInTheDocument();
            expect(screen.getByText('Create new API endpoint and connect it with a workflow.')).toBeInTheDocument();
        });

        it('should render the workflow, name, method and path fields', () => {
            render(<ApiCollectionEndpointDialog {...defaultProps} />);

            expect(screen.getByText('Workflow')).toBeInTheDocument();
            expect(screen.getByLabelText('Name')).toBeInTheDocument();
            expect(screen.getByText('HTTP Method')).toBeInTheDocument();
            expect(screen.getByText('Path')).toBeInTheDocument();
        });

        it('should show the path prefix built from the collection version and context path', () => {
            render(<ApiCollectionEndpointDialog {...defaultProps} />);

            expect(screen.getByText('/v2/orders/')).toBeInTheDocument();
        });
    });

    describe('edit mode', () => {
        it('should render the edit title', () => {
            render(<ApiCollectionEndpointDialog {...defaultProps} apiEndpoint={existingApiEndpoint} />);

            expect(screen.getByText('Edit API Endpoint')).toBeInTheDocument();
        });

        it('should prefill the name from the endpoint', () => {
            render(<ApiCollectionEndpointDialog {...defaultProps} apiEndpoint={existingApiEndpoint} />);

            expect(screen.getByLabelText('Name')).toHaveValue('Existing');
        });
    });

    describe('dialog chrome', () => {
        it('should render the close, cancel and save controls', () => {
            render(<ApiCollectionEndpointDialog {...defaultProps} />);

            expect(screen.getByRole('button', {name: 'Close'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Save'})).toBeInTheDocument();
        });

        it('should keep the cancel button out of the form submission', () => {
            render(<ApiCollectionEndpointDialog {...defaultProps} />);

            expect(screen.getByRole('button', {name: 'Cancel'})).toHaveAttribute('type', 'button');
        });
    });

    describe('interactions', () => {
        it('should call onClose when cancel is clicked', async () => {
            const user = userEvent.setup();

            render(<ApiCollectionEndpointDialog {...defaultProps} />);

            await user.click(screen.getByRole('button', {name: 'Cancel'}));

            expect(onClose).toHaveBeenCalledTimes(1);
        });

        it('should call onClose when the close button is clicked', async () => {
            const user = userEvent.setup();

            render(<ApiCollectionEndpointDialog {...defaultProps} />);

            await user.click(screen.getByRole('button', {name: 'Close'}));

            expect(onClose).toHaveBeenCalledTimes(1);
        });
    });
});
