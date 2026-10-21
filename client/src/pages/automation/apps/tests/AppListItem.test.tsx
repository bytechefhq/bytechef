import {TooltipProvider} from '@/components/ui/tooltip';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AppListItem from '../components/app-list/AppListItem';

const hoisted = vi.hoisted(() => ({
    updateAppTagsMutate: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async () => ({
    ...(await vi.importActual<Record<string, unknown>>('@/shared/middleware/graphql')),
    useUpdateAppTagsMutation: vi.fn(() => ({isPending: false, mutate: hoisted.updateAppTagsMutate})),
}));

interface MockAppDialogPropsI {
    canShare?: boolean;
}

vi.mock('@/pages/automation/apps/components/AppDialog', () => ({
    default: ({canShare}: MockAppDialogPropsI) => <div data-testid="app-dialog">{`canShare=${String(canShare)}`}</div>,
}));

vi.mock('@/pages/automation/apps/components/AppStatusSwitch', () => ({
    default: () => null,
}));

vi.mock('@/pages/automation/apps/components/AppDeleteDialog', () => ({
    default: () => null,
}));

const app = {
    description: null,
    foreignInstances: [],
    id: '5',
    instances: [],
    name: 'group-crm',
    tagIds: ['5'],
    workspaceId: '7',
};

const appTags = [
    {id: '5', name: 'crm'},
    {id: '6', name: 'internal'},
];

const renderItem = (canManage = true) =>
    render(
        <TooltipProvider>
            <MemoryRouter>
                <AppListItem app={app as never} canManage={canManage} ownerName="Owner" tags={appTags} />
            </MemoryRouter>
        </TooltipProvider>
    );

describe('AppListItem', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('shows the tags of the app in its row', () => {
        renderItem();

        expect(screen.getByText('crm')).toBeInTheDocument();
        expect(screen.queryByText('internal')).not.toBeInTheDocument();
    });

    it('adds a typed tag from the row through updateAppTags', async () => {
        renderItem();

        await userEvent.click(screen.getByRole('button', {name: 'Add tag'}));
        await userEvent.type(screen.getByRole('combobox'), 'beta{Enter}');

        expect(hoisted.updateAppTagsMutate).toHaveBeenCalledWith({
            id: '5',
            tags: [{id: '5', name: 'crm'}, {name: 'beta'}],
        });
    });

    it('removes a tag from the row through updateAppTags', async () => {
        renderItem();

        await userEvent.click(screen.getByRole('button', {name: 'Remove tag'}));

        expect(hoisted.updateAppTagsMutate).toHaveBeenCalledWith({id: '5', tags: []});
    });

    it('shows the tags read-only without APP_MANAGE', () => {
        renderItem(false);

        expect(screen.getByText('crm')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Add tag'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Remove tag'})).not.toBeInTheDocument();
    });

    it('lets a manager share the app from the edit dialog opened in the list', async () => {
        renderItem();

        await userEvent.click(screen.getByRole('button', {name: 'Actions for group-crm'}));
        await userEvent.click(await screen.findByRole('menuitem', {name: /Edit/}));

        expect(await screen.findByTestId('app-dialog')).toHaveTextContent('canShare=true');
    });
});
