import {applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AccountProfileDeleteOrganization from '../AccountProfileDeleteOrganization';

const hoisted = vi.hoisted(() => ({
    analyticsResetMock: vi.fn(),
    clearAuthenticationMock: vi.fn(),
    logoutMock: vi.fn(() => Promise.resolve()),
    toastMock: vi.fn(),
}));

vi.mock('sonner', () => ({
    toast: Object.assign(hoisted.toastMock, {error: vi.fn()}),
}));

vi.mock('@/shared/hooks/useAnalytics', () => ({
    useAnalytics: () => ({reset: hoisted.analyticsResetMock}),
}));

const PURGE_AFTER = '2026-11-18T10:00:00Z';

let authProvider = 'LOCAL';
let deleteResponse: {json?: () => Promise<unknown>; ok: boolean} = {
    json: () => Promise.resolve({purgeAfter: PURGE_AFTER}),
    ok: true,
};

const fetchMock = vi.fn<(url: string, init?: RequestInit) => Promise<unknown>>((url) => {
    if (url === '/api/account/organization/delete') {
        return Promise.resolve(deleteResponse);
    }

    return Promise.resolve({json: () => Promise.resolve({authProvider, hasPassword: true}), ok: true});
});

const deleteRequests = () => fetchMock.mock.calls.filter(([url]) => url === '/api/account/organization/delete');

const openDialog = async () => {
    await userEvent.click(screen.getByRole('button', {name: 'Delete organization'}));

    return screen.getByRole('dialog');
};

const getConfirmButton = () => screen.getAllByRole('button', {name: 'Delete organization'}).at(-1)!;

beforeEach(() => {
    windowResizeObserver();

    authProvider = 'LOCAL';
    deleteResponse = {json: () => Promise.resolve({purgeAfter: PURGE_AFTER}), ok: true};

    vi.stubGlobal('fetch', fetchMock);

    applicationInfoStore.setState({tenant: {deletionGracePeriodDays: 30, mode: 'multi'}});

    authenticationStore.setState({
        account: {authorities: ['ROLE_ADMIN'], email: 'admin@localhost.com', login: 'admin@localhost.com'},
        authenticated: true,
        clearAuthentication: hoisted.clearAuthenticationMock,
        logout: hoisted.logoutMock,
    });
});

afterEach(() => {
    resetAll();
    vi.unstubAllGlobals();
    vi.clearAllMocks();
    applicationInfoStore.setState({tenant: {deletionGracePeriodDays: 30, mode: 'single'}});
});

describe('AccountProfileDeleteOrganization', () => {
    it('is hidden for a user who is not a tenant admin', () => {
        authenticationStore.setState({
            account: {authorities: ['ROLE_USER'], email: 'user@localhost.com', login: 'user@localhost.com'},
        });

        render(<AccountProfileDeleteOrganization />);

        expect(screen.queryByText('Delete organization')).not.toBeInTheDocument();
    });

    it('is hidden in single-tenant mode', () => {
        applicationInfoStore.setState({tenant: {deletionGracePeriodDays: 30, mode: 'single'}});

        render(<AccountProfileDeleteOrganization />);

        expect(screen.queryByText('Delete organization')).not.toBeInTheDocument();
    });

    it('shows the grace period in the section text', () => {
        render(<AccountProfileDeleteOrganization />);

        expect(screen.getByText(/After 30 days the data is deleted for good\./)).toBeInTheDocument();
    });

    it('keeps the delete button disabled until the email and password are entered', async () => {
        render(<AccountProfileDeleteOrganization />);

        await openDialog();

        expect(getConfirmButton()).toBeDisabled();

        await userEvent.type(screen.getByLabelText(/to confirm/), 'admin@localhost.com');

        expect(getConfirmButton()).toBeDisabled();

        await userEvent.type(screen.getByLabelText('Password'), 'secret');

        expect(getConfirmButton()).toBeEnabled();
    });

    it('does not ask for a password when the admin signs in through an identity provider', async () => {
        authProvider = 'github';

        render(<AccountProfileDeleteOrganization />);

        await waitFor(() => expect(fetchMock).toHaveBeenCalledWith('/api/account/linked-accounts'));

        await openDialog();

        await waitFor(() => expect(screen.queryByLabelText('Password')).not.toBeInTheDocument());

        await userEvent.type(screen.getByLabelText(/to confirm/), 'ADMIN@localhost.com');

        expect(getConfirmButton()).toBeEnabled();
    });

    it('requests deletion and logs out', async () => {
        render(<AccountProfileDeleteOrganization />);

        await openDialog();

        await userEvent.type(screen.getByLabelText(/to confirm/), 'admin@localhost.com');
        await userEvent.type(screen.getByLabelText('Password'), 'secret');

        await userEvent.click(getConfirmButton());

        await waitFor(() => expect(hoisted.clearAuthenticationMock).toHaveBeenCalled());

        expect(deleteRequests()).toHaveLength(1);
        expect(JSON.parse(String(deleteRequests()[0][1]?.body))).toEqual({
            confirmation: 'admin@localhost.com',
            password: 'secret',
        });
        expect(hoisted.logoutMock).toHaveBeenCalled();
        expect(hoisted.analyticsResetMock).toHaveBeenCalled();
        expect(hoisted.toastMock).toHaveBeenCalledWith(
            `Your organization will be deleted on ${new Date(PURGE_AFTER).toLocaleDateString()}.`
        );
    });

    it('stays logged in when the server rejects the deletion', async () => {
        deleteResponse = {ok: false};

        render(<AccountProfileDeleteOrganization />);

        await openDialog();

        await userEvent.type(screen.getByLabelText(/to confirm/), 'admin@localhost.com');
        await userEvent.type(screen.getByLabelText('Password'), 'wrong');

        await userEvent.click(getConfirmButton());

        await waitFor(() => expect(getConfirmButton()).toBeEnabled());

        expect(deleteRequests()).toHaveLength(1);
        expect(hoisted.logoutMock).not.toHaveBeenCalled();
        expect(hoisted.clearAuthenticationMock).not.toHaveBeenCalled();
    });
});
