import Button from '@/components/Button/Button';
import {
    mockScrollIntoView,
    render,
    screen,
    stubMutation,
    userEvent,
    waitFor,
    windowResizeObserver,
} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AppApiCollectionBindingDialog from '../components/AppApiCollectionBindingDialog';

vi.mock('@/shared/middleware/graphql', async () => ({
    ...(await vi.importActual<Record<string, unknown>>('@/shared/middleware/graphql')),
    useAppBindableApiCollectionsQuery: vi.fn(() => ({
        data: {
            appBindableApiCollections: [
                {
                    apiCollectionId: '21',
                    available: true,
                    collectionVersion: 2,
                    contextPath: 'crm',
                    name: 'CRM',
                    projectId: '5',
                    projectName: 'Sales',
                },
                {
                    apiCollectionId: '22',
                    available: false,
                    collectionVersion: 1,
                    contextPath: 'billing',
                    name: 'Billing',
                    projectId: '6',
                    projectName: 'Finance',
                },
            ],
        },
    })),
    useBindAppApiCollectionMutation: vi.fn(),
}));

const {useBindAppApiCollectionMutation} = await import('@/shared/middleware/graphql');

const bindMutation = stubMutation(vi.mocked(useBindAppApiCollectionMutation));

describe('AppApiCollectionBindingDialog', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        windowResizeObserver();
        mockScrollIntoView();
    });

    it('lists the bindable collections and marks a disabled one', async () => {
        render(
            <AppApiCollectionBindingDialog
                app={{id: '3'} as never}
                triggerNode={<Button label="Bind API Collection" />}
            />
        );

        await userEvent.click(screen.getByRole('button', {name: 'Bind API Collection'}));
        await userEvent.click(screen.getByRole('combobox', {name: 'API collection'}));

        expect(await screen.findByText('CRM — Sales (/crm v2)')).toBeInTheDocument();
        expect(screen.getByText('Billing — Finance (/billing v1, disabled)')).toBeInTheDocument();
    });

    it('hides the collections already bound to the app', async () => {
        render(
            <AppApiCollectionBindingDialog
                app={{id: '3'} as never}
                boundApiCollectionIds={['21']}
                triggerNode={<Button label="Bind API Collection" />}
            />
        );

        await userEvent.click(screen.getByRole('button', {name: 'Bind API Collection'}));
        await userEvent.click(screen.getByRole('combobox', {name: 'API collection'}));

        expect(await screen.findByText('Billing — Finance (/billing v1, disabled)')).toBeInTheDocument();
        expect(screen.queryByText('CRM — Sales (/crm v2)')).not.toBeInTheDocument();
    });

    it('binds the selected collection', async () => {
        render(
            <AppApiCollectionBindingDialog
                app={{id: '3'} as never}
                triggerNode={<Button label="Bind API Collection" />}
            />
        );

        await userEvent.click(screen.getByRole('button', {name: 'Bind API Collection'}));
        await userEvent.click(screen.getByRole('combobox', {name: 'API collection'}));
        await userEvent.click(await screen.findByText('CRM — Sales (/crm v2)'));
        await userEvent.click(screen.getByRole('button', {name: 'Bind'}));

        await waitFor(() => {
            expect(bindMutation.mutate).toHaveBeenCalledWith({apiCollectionId: '21', appId: '3'});
        });
    });

    it('does not bind without a selected collection', async () => {
        render(
            <AppApiCollectionBindingDialog
                app={{id: '3'} as never}
                triggerNode={<Button label="Bind API Collection" />}
            />
        );

        await userEvent.click(screen.getByRole('button', {name: 'Bind API Collection'}));

        expect(screen.getByRole('button', {name: 'Bind'})).toBeDisabled();
        expect(bindMutation.mutate).not.toHaveBeenCalled();
    });
});
