import {render, screen, userEvent} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {describe, expect, it, vi} from 'vitest';

import AppListItem from '../components/app-list/AppListItem';

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
    workspaceId: '7',
};

describe('AppListItem', () => {
    it('lets a manager share the app from the edit dialog opened in the list', async () => {
        render(
            <MemoryRouter>
                <AppListItem app={app as never} canManage ownerName="Owner" />
            </MemoryRouter>
        );

        await userEvent.click(screen.getByRole('button', {name: 'Actions for group-crm'}));
        await userEvent.click(await screen.findByRole('menuitem', {name: /Edit/}));

        expect(await screen.findByTestId('app-dialog')).toHaveTextContent('canShare=true');
    });
});
