import {TooltipProvider} from '@/components/ui/tooltip';
import {render, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectionReassignmentDialog from '../ConnectionReassignmentDialog';

const hoisted = vi.hoisted(() => ({
    usersPages: [] as Array<Array<{activated: boolean; email: string; id: string; login: string}>>,
    workspaceMemberIds: [] as string[],
}));

vi.mock('@/shared/middleware/graphqlFetcher', () => ({
    fetcher: (_document: unknown, variables: {pageNumber: number}) => () =>
        Promise.resolve({
            users: {
                content: hoisted.usersPages[variables.pageNumber] ?? [],
                number: variables.pageNumber,
                totalPages: hoisted.usersPages.length,
            },
        }),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    UsersDocument: 'UsersDocument',
    useAffectedWorkflowsQuery: () => ({data: {affectedWorkflows: []}}),
    useReassignAllConnectionsMutation: () => ({isPending: false, mutate: vi.fn()}),
    useUnresolvedConnectionsQuery: () => ({
        data: {unresolvedConnections: [{connectionId: '10', connectionName: 'Slack', visibility: 'WORKSPACE'}]},
        isLoading: false,
    }),
    useWorkspaceUsersQuery: () => ({
        data: {workspaceUsers: hoisted.workspaceMemberIds.map((workspaceMemberId) => ({userId: workspaceMemberId}))},
    }),
}));

describe('ConnectionReassignmentDialog', () => {
    beforeEach(() => {
        hoisted.usersPages = [
            [
                {activated: true, email: 'member@example.com', id: '1', login: 'member@example.com'},
                {activated: true, email: 'outsider@example.com', id: '2', login: 'outsider@example.com'},
            ],
        ];
        hoisted.workspaceMemberIds = ['1'];

        windowResizeObserver();

        Element.prototype.hasPointerCapture = Element.prototype.hasPointerCapture || vi.fn();
        Element.prototype.releasePointerCapture = Element.prototype.releasePointerCapture || vi.fn();
        Element.prototype.scrollIntoView = Element.prototype.scrollIntoView || vi.fn();
    });

    it('offers only members of the connection workspace as the new owner', async () => {
        render(
            <TooltipProvider>
                <ConnectionReassignmentDialog
                    onClose={vi.fn()}
                    open
                    userLogin="departing@example.com"
                    workspaceId={7}
                />
            </TooltipProvider>
        );

        await userEvent.click(screen.getByRole('combobox'));

        expect(await screen.findByRole('option', {name: /member@example.com/})).toBeInTheDocument();
        expect(screen.queryByRole('option', {name: /outsider@example.com/})).not.toBeInTheDocument();
    });

    it('offers a workspace member who is beyond the first page of organization users', async () => {
        hoisted.usersPages = [
            [{activated: true, email: 'outsider@example.com', id: '2', login: 'outsider@example.com'}],
            [{activated: true, email: 'late-member@example.com', id: '3', login: 'late-member@example.com'}],
        ];
        hoisted.workspaceMemberIds = ['3'];

        render(
            <TooltipProvider>
                <ConnectionReassignmentDialog
                    onClose={vi.fn()}
                    open
                    userLogin="departing@example.com"
                    workspaceId={7}
                />
            </TooltipProvider>
        );

        await userEvent.click(screen.getByRole('combobox'));

        expect(await screen.findByRole('option', {name: /late-member@example.com/})).toBeInTheDocument();
    });
});
