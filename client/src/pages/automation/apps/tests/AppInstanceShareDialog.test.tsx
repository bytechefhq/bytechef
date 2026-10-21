import {TooltipProvider} from '@/components/ui/tooltip';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    mockScrollIntoView,
    render,
    screen,
    stubMutation,
    userEvent,
    waitFor,
    windowResizeObserver,
} from '@/shared/util/test-utils';
import {ReactElement} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AppInstanceShareDialog from '../components/AppInstanceShareDialog';

const renderDialog = (dialog: ReactElement) => render(<TooltipProvider>{dialog}</TooltipProvider>);

const hoisted = vi.hoisted(() => ({
    grantedUserIds: [] as string[],
}));

vi.mock('@/shared/middleware/graphql', async () => ({
    ...(await vi.importActual<Record<string, unknown>>('@/shared/middleware/graphql')),
    useAppInstanceGrantsQuery: vi.fn(() => ({data: {appInstanceGrants: hoisted.grantedUserIds}})),
    useGrantAppInstanceAccessMutation: vi.fn(),
    useRevokeAppInstanceAccessMutation: vi.fn(),
    useSetAppInstanceVisibilityMutation: vi.fn(),
    useWorkspaceUsersQuery: vi.fn(() => ({
        data: {
            workspaceUsers: [
                {user: {email: 'ana@example.com'}, userId: '5'},
                {user: {email: 'ivo@example.com'}, userId: '6'},
            ],
        },
    })),
}));

const {
    useAppInstanceGrantsQuery,
    useGrantAppInstanceAccessMutation,
    useRevokeAppInstanceAccessMutation,
    useSetAppInstanceVisibilityMutation,
    useWorkspaceUsersQuery,
} = await import('@/shared/middleware/graphql');

const grantMutation = stubMutation(vi.mocked(useGrantAppInstanceAccessMutation));
const revokeMutation = stubMutation(vi.mocked(useRevokeAppInstanceAccessMutation));
const setVisibilityMutation = stubMutation(vi.mocked(useSetAppInstanceVisibilityMutation));

const foreignWorkspaceInstance = {
    id: '11',
    name: 'crm-germany',
    visibility: 'WORKSPACE',
    workspaceId: '8',
};

describe('AppInstanceShareDialog', () => {
    beforeEach(() => {
        windowResizeObserver();
        mockScrollIntoView();

        if (!Element.prototype.hasPointerCapture) {
            Element.prototype.hasPointerCapture = vi.fn(() => false);
            Element.prototype.setPointerCapture = vi.fn();
            Element.prototype.releasePointerCapture = vi.fn();
        }

        vi.clearAllMocks();

        hoisted.grantedUserIds = [];

        useWorkspaceStore.setState({currentWorkspaceId: 7});
    });

    it('changes the visibility in the instance workspace through the sharing mutation', async () => {
        renderDialog(
            <AppInstanceShareDialog appInstance={foreignWorkspaceInstance} onOpenChange={vi.fn()} open={true} />
        );

        expect(screen.getByRole('radio', {name: 'Shared with workspace'})).toBeChecked();

        await userEvent.click(screen.getByRole('radio', {name: 'Private'}));

        await waitFor(() => {
            expect(setVisibilityMutation.mutate).toHaveBeenCalledWith({
                appInstanceId: '11',
                visibility: 'PRIVATE',
                workspaceId: '8',
            });
        });
    });

    it('lists members of the instance workspace and grants access to a chosen person', async () => {
        renderDialog(
            <AppInstanceShareDialog
                appInstance={{...foreignWorkspaceInstance, visibility: 'PRIVATE'}}
                onOpenChange={vi.fn()}
                open={true}
            />
        );

        expect(vi.mocked(useWorkspaceUsersQuery)).toHaveBeenCalledWith(
            {workspaceId: '8'},
            expect.objectContaining({enabled: true})
        );
        expect(vi.mocked(useAppInstanceGrantsQuery)).toHaveBeenCalledWith(
            {appInstanceId: '11', workspaceId: '8'},
            expect.objectContaining({enabled: true})
        );

        await userEvent.click(screen.getByRole('radio', {name: 'Specific people'}));
        await userEvent.click(screen.getByRole('combobox', {name: 'Add person'}));
        await userEvent.click(await screen.findByRole('option', {name: 'ivo@example.com'}));

        await waitFor(() => {
            expect(grantMutation.mutate).toHaveBeenCalledWith({appInstanceId: '11', userId: '6', workspaceId: '8'});
        });
    });

    it('revokes access when a granted person is removed', async () => {
        hoisted.grantedUserIds = ['5'];

        renderDialog(
            <AppInstanceShareDialog
                appInstance={{...foreignWorkspaceInstance, visibility: 'PRIVATE'}}
                onOpenChange={vi.fn()}
                open={true}
            />
        );

        await userEvent.click(screen.getByRole('button', {name: 'Remove ana@example.com'}));

        await waitFor(() => {
            expect(revokeMutation.mutate).toHaveBeenCalledWith({appInstanceId: '11', userId: '5', workspaceId: '8'});
        });

        expect(grantMutation.mutate).not.toHaveBeenCalled();
    });

    it('loads no members or grants while the dialog is closed', () => {
        renderDialog(
            <AppInstanceShareDialog
                appInstance={{...foreignWorkspaceInstance, visibility: 'PRIVATE'}}
                triggerNode={<button type="button">Share</button>}
            />
        );

        expect(vi.mocked(useWorkspaceUsersQuery)).toHaveBeenCalledWith(
            {workspaceId: '8'},
            expect.objectContaining({enabled: false})
        );
        expect(vi.mocked(useAppInstanceGrantsQuery)).toHaveBeenCalledWith(
            {appInstanceId: '11', workspaceId: '8'},
            expect.objectContaining({enabled: false})
        );
    });
});
