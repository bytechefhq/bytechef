import {ApiCollection, ResponseError} from '@/ee/shared/middleware/automation/api-platform';
import {ProjectStatus} from '@/shared/middleware/automation/configuration';
import {act, render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ApiCollectionProjectVersionDialog from '../ApiCollectionProjectVersionDialog';

interface MutationCallbacksI {
    onError?: (error: Error) => void;
    onSuccess?: (result: unknown) => void;
}

const hoisted = vi.hoisted(() => ({
    callbacks: {} as MutationCallbacksI,
    mutate: vi.fn(),
}));

vi.mock('@/ee/shared/mutations/automation/apiCollections.mutations', () => ({
    useUpdateApiCollectionProjectVersionMutation: (callbacks: MutationCallbacksI) => {
        hoisted.callbacks = callbacks;

        return {isPending: false, mutate: hoisted.mutate};
    },
}));

vi.mock('@/ee/shared/mutations/automation/apiCollections.queries', () => ({
    ApiCollectionKeys: {apiCollections: ['apiCollections']},
}));

vi.mock('@/shared/queries/automation/projectVersions.queries', () => ({
    useGetProjectVersionsQuery: () => ({
        data: [
            {status: ProjectStatus.Published, version: 1},
            {status: ProjectStatus.Published, version: 2},
            {status: ProjectStatus.Published, version: 3},
            {status: ProjectStatus.Draft, version: 4},
            {status: ProjectStatus.Published, version: 5},
        ],
        isPending: false,
    }),
}));

const apiCollection = {
    collectionVersion: 1,
    contextPath: 'orders',
    enabled: true,
    id: 3,
    name: 'Orders API',
    projectId: 7,
    projectVersion: 2,
} as unknown as ApiCollection;

const onClose = vi.fn();

const selectVersionAndSubmit = async (versionLabel: string) => {
    await userEvent.click(screen.getByLabelText(versionLabel));
    await userEvent.click(screen.getByRole('button', {name: 'Update'}));
};

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('ApiCollectionProjectVersionDialog', () => {
    it('should offer only published versions newer than the current one', () => {
        render(<ApiCollectionProjectVersionDialog apiCollection={apiCollection} onClose={onClose} />);

        expect(screen.getByLabelText('V3')).toBeInTheDocument();
        expect(screen.getByLabelText('V5')).toBeInTheDocument();
        expect(screen.queryByLabelText('V1')).not.toBeInTheDocument();
        expect(screen.queryByLabelText('V2')).not.toBeInTheDocument();
        expect(screen.queryByLabelText('V4')).not.toBeInTheDocument();
    });

    it('should send the selected version without removing endpoints', async () => {
        render(<ApiCollectionProjectVersionDialog apiCollection={apiCollection} onClose={onClose} />);

        await selectVersionAndSubmit('V3');

        expect(hoisted.mutate).toHaveBeenCalledWith({id: 3, projectVersion: 3, removeMissingEndpoints: false});
    });

    it('should resend with removeMissingEndpoints after the removal is confirmed', async () => {
        render(<ApiCollectionProjectVersionDialog apiCollection={apiCollection} onClose={onClose} />);

        await selectVersionAndSubmit('V3');

        const response = new Response(
            JSON.stringify({
                entityClass: 'ApiCollection',
                errorKey: 105,
                inputParameters: {endpointNames: ['createOrder']},
            }),
            {status: 400}
        );

        await act(async () => {
            hoisted.callbacks.onError?.(new ResponseError(response, 'Response returned an error code'));
        });

        await waitFor(() => expect(screen.getByText('createOrder')).toBeInTheDocument());

        await userEvent.click(screen.getByRole('button', {name: 'Remove these endpoints'}));

        expect(hoisted.mutate).toHaveBeenLastCalledWith({id: 3, projectVersion: 3, removeMissingEndpoints: true});
    });

    it('should list the missing connections after a successful update', async () => {
        render(<ApiCollectionProjectVersionDialog apiCollection={apiCollection} onClose={onClose} />);

        await selectVersionAndSubmit('V3');

        await act(async () => {
            hoisted.callbacks.onSuccess?.({
                apiCollection: {...apiCollection, projectVersion: 3},
                missingConnections: [
                    {componentName: 'gmail', endpointName: 'listOrders', key: 'gmail', workflowNodeName: 'sendEmail'},
                ],
                removedEndpointNames: [],
            });
        });

        expect(screen.getByText(/listOrders/)).toBeInTheDocument();
        expect(screen.getByText(/gmail/)).toBeInTheDocument();
    });

    it('should list the disabled endpoints and their missing inputs after a successful update', async () => {
        render(<ApiCollectionProjectVersionDialog apiCollection={apiCollection} onClose={onClose} />);

        await selectVersionAndSubmit('V3');

        await act(async () => {
            hoisted.callbacks.onSuccess?.({
                apiCollection: {...apiCollection, projectVersion: 3},
                disabledEndpointNames: ['createOrder'],
                missingConnections: [],
                missingInputs: [{endpointName: 'createOrder', inputName: 'tenant'}],
                removedEndpointNames: [],
            });
        });

        expect(screen.getByText('Disabled endpoints: createOrder')).toBeInTheDocument();
        expect(screen.getByText('createOrder: tenant')).toBeInTheDocument();
    });
});
