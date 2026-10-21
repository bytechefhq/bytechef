import {render, screen, stubMutation, userEvent, waitFor, within} from '@/shared/util/test-utils';
import {QueryClient} from '@tanstack/react-query';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AppApiCollectionsTab from '../components/AppApiCollectionsTab';

vi.mock('@/shared/middleware/graphql', async () => ({
    ...(await vi.importActual<Record<string, unknown>>('@/shared/middleware/graphql')),
    useAppApiCollectionsQuery: vi.fn(() => ({
        data: {
            appApiCollections: [
                {
                    apiCollectionId: '21',
                    appId: '3',
                    available: true,
                    collectionVersion: 2,
                    contextPath: 'crm',
                    id: '31',
                    name: 'CRM',
                    projectId: '5',
                    projectName: 'Sales',
                },
                {
                    apiCollectionId: '22',
                    appId: '3',
                    available: false,
                    collectionVersion: 0,
                    contextPath: null,
                    id: '32',
                    name: '#22',
                    projectId: null,
                    projectName: null,
                },
            ],
        },
        error: null,
        isLoading: false,
    })),
    useAppBindableApiCollectionsQuery: vi.fn(() => ({data: {appBindableApiCollections: []}})),
    useBindAppApiCollectionMutation: vi.fn(),
    useUnbindAppApiCollectionMutation: vi.fn(),
}));

const {useBindAppApiCollectionMutation, useUnbindAppApiCollectionMutation} =
    await import('@/shared/middleware/graphql');

stubMutation(vi.mocked(useBindAppApiCollectionMutation));

const unbindMutation = stubMutation(vi.mocked(useUnbindAppApiCollectionMutation));

describe('AppApiCollectionsTab', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders a row per binding with its name, context path and project', () => {
        render(<AppApiCollectionsTab app={{id: '3'} as never} canManage />);

        const crmRow = screen.getByRole('row', {name: /CRM/});

        expect(within(crmRow).getByText('/crm')).toBeInTheDocument();
        expect(within(crmRow).getByText('Sales')).toBeInTheDocument();
        expect(within(crmRow).queryByText('Unavailable')).not.toBeInTheDocument();
    });

    it('marks a binding that is not available with an Unavailable badge', () => {
        render(<AppApiCollectionsTab app={{id: '3'} as never} canManage />);

        const missingRow = screen.getByRole('row', {name: /#22/});

        expect(within(missingRow).getByText('Unavailable')).toBeInTheDocument();
    });

    it('hides the bind and unbind buttons when the user cannot manage the app', () => {
        render(<AppApiCollectionsTab app={{id: '3'} as never} canManage={false} />);

        expect(screen.queryByRole('button', {name: 'Bind API Collection'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: /^Unbind/})).not.toBeInTheDocument();
    });

    it('unbinds a collection after the confirmation and refreshes the bindings', async () => {
        const invalidateQueriesSpy = vi.spyOn(QueryClient.prototype, 'invalidateQueries');

        render(<AppApiCollectionsTab app={{id: '3'} as never} canManage />);

        await userEvent.click(screen.getByRole('button', {name: 'Unbind CRM'}));
        await userEvent.click(await screen.findByRole('button', {name: 'Unbind'}));

        await waitFor(() => {
            expect(unbindMutation.mutate).toHaveBeenCalledWith({apiCollectionId: '21', appId: '3'});
        });

        unbindMutation.triggerOnSuccess(true, {apiCollectionId: '21', appId: '3'});

        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['appApiCollections']});

        invalidateQueriesSpy.mockRestore();
    });
});
