import Button from '@/components/Button/Button';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {render, screen, stubMutation, userEvent, waitFor} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AppDialog from '../components/AppDialog';

vi.mock('@/shared/middleware/graphql', async () => ({
    ...(await vi.importActual<Record<string, unknown>>('@/shared/middleware/graphql')),
    useCreateAppMutation: vi.fn(),
    useUpdateAppMutation: vi.fn(),
}));

const {useCreateAppMutation, useUpdateAppMutation} = await import('@/shared/middleware/graphql');

const createMutation = stubMutation(vi.mocked(useCreateAppMutation));
const updateMutation = stubMutation(vi.mocked(useUpdateAppMutation));

describe('AppDialog', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        environmentStore.setState({currentEnvironmentId: 2});
        useWorkspaceStore.setState({currentWorkspaceId: 7});
    });

    it('rejects an empty name', async () => {
        render(<AppDialog onOpenChange={vi.fn()} open={true} />);

        await userEvent.click(screen.getByRole('button', {name: 'Create App'}));

        expect(await screen.findByText('Name is required')).toBeInTheDocument();
        expect(createMutation.mutate).not.toHaveBeenCalled();
    });

    it('rejects a url that is not a URL', async () => {
        render(<AppDialog onOpenChange={vi.fn()} open={true} />);

        await userEvent.type(screen.getByLabelText('Name'), 'crm');
        await userEvent.type(screen.getByLabelText('URL'), 'not a url');
        await userEvent.click(screen.getByRole('button', {name: 'Create App'}));

        expect(await screen.findByText('URL must be a valid URL')).toBeInTheDocument();
        expect(createMutation.mutate).not.toHaveBeenCalled();
    });

    it('creates an app and then shows the secret key once', async () => {
        render(<AppDialog onOpenChange={vi.fn()} open={true} />);

        await userEvent.type(screen.getByLabelText('Name'), 'crm-dashboard');
        await userEvent.click(screen.getByRole('button', {name: 'Create App'}));

        await waitFor(() => {
            expect(createMutation.mutate).toHaveBeenCalledWith({
                environmentId: '2',
                input: {description: undefined, hostingTarget: 'VERCEL', name: 'crm-dashboard', url: undefined},
                workspaceId: '7',
            });
        });

        createMutation.triggerOnSuccess({createApp: {app: {id: '1'}, secretKey: 'abcdefghijklmnop'}}, undefined);

        expect(await screen.findByDisplayValue('abcdefghijklmnop')).toBeInTheDocument();
        expect(screen.getByText(/BYTECHEF_APP_KEY=abcdefghijklmnop/)).toBeInTheDocument();
        expect(screen.getByText(/ByteChef cannot show it again/)).toBeInTheDocument();
    });

    it('updates an existing app, clearing optional fields with empty strings', async () => {
        render(
            <AppDialog
                app={
                    {
                        description: 'old',
                        hostingTarget: 'VERCEL',
                        id: '5',
                        name: 'crm',
                        url: 'https://old.example.com',
                    } as never
                }
                onOpenChange={vi.fn()}
                open={true}
            />
        );

        await userEvent.clear(screen.getByLabelText('Description'));
        await userEvent.clear(screen.getByLabelText('URL'));
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => {
            expect(updateMutation.mutate).toHaveBeenCalledWith({
                id: '5',
                input: {description: '', hostingTarget: 'VERCEL', name: 'crm', url: ''},
            });
        });
    });

    it('shows the current app values when the edit dialog is reopened after a save', async () => {
        const app = {description: '', hostingTarget: 'VERCEL', id: '5', name: 'crm', url: ''} as never;

        const {rerender} = render(<AppDialog app={app} triggerNode={<Button label="Edit" />} />);

        await userEvent.click(screen.getByRole('button', {name: 'Edit'}));
        await userEvent.clear(screen.getByLabelText('Name'));
        await userEvent.type(screen.getByLabelText('Name'), 'crm-renamed');
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        updateMutation.triggerOnSuccess(undefined, undefined);

        rerender(
            <AppDialog
                app={{description: '', hostingTarget: 'VERCEL', id: '5', name: 'crm-renamed', url: ''} as never}
                triggerNode={<Button label="Edit" />}
            />
        );

        await userEvent.click(await screen.findByRole('button', {name: 'Edit'}));

        expect(await screen.findByLabelText('Name')).toHaveValue('crm-renamed');
    });
});
