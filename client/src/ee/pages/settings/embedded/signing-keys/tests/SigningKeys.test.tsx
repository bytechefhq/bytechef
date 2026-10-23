import SigningKeys from '@/ee/pages/settings/embedded/signing-keys/SigningKeys';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {act, render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    grants: {} as Record<number, string[]>,
    signingKeysQuery: vi.fn(),
    useRealScopeHook: false,
}));

vi.mock('@/shared/hooks/useHasEmbeddedScope', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/hooks/useHasEmbeddedScope')>();
    const {environmentStore: mockedEnvironmentStore} = await import('@/shared/stores/useEnvironmentStore');

    return {
        ...actual,
        useHasEmbeddedScope: (scope: string, environmentId?: number) =>
            hoisted.useRealScopeHook
                ? actual.useHasEmbeddedScope(scope as never, environmentId)
                : (
                      hoisted.grants[environmentId ?? mockedEnvironmentStore.getState().currentEnvironmentId] ?? []
                  ).includes(scope),
    };
});

vi.mock('@/ee/shared/queries/embedded/signingKeys.queries', () => ({
    SigningKeyKeys: {signingKeys: ['signingKeys']},
    useGeSigningKeysQuery: hoisted.signingKeysQuery,
}));

vi.mock('@/ee/pages/settings/embedded/signing-keys/components/SigningKeyDialog', () => ({
    default: ({triggerNode}: {triggerNode?: ReactNode}) => <>{triggerNode}</>,
}));

vi.mock('@/ee/pages/settings/embedded/signing-keys/components/SigningKeyDeleteDialog', () => ({
    default: () => null,
}));

vi.mock('@/shared/layout/LayoutContainer', () => ({
    default: ({children, header}: {children: ReactNode; header: ReactNode}) => (
        <div>
            {header}

            {children}
        </div>
    ),
}));

vi.mock('@/shared/layout/Header', () => ({
    default: ({right, title}: {right?: ReactNode; title?: ReactNode}) => (
        <div>
            <h1>{title}</h1>

            {right}
        </div>
    ),
}));

const DEVELOPMENT = 0;
const PRODUCTION = 2;
const NO_PERMISSION_TEXT = 'You do not have permission to manage the signing keys of this environment.';

const SIGNING_KEYS = [
    {
        createdBy: 'admin@localhost.com',
        createdDate: new Date(1_700_000_000_000),
        id: 4,
        keyId: 'key-id-4',
        lastUsedDate: new Date(1_700_000_000_000),
        name: 'SDK key',
    },
];

describe('embedded SigningKeys', () => {
    beforeEach(() => {
        windowResizeObserver();

        hoisted.grants = {};
        hoisted.signingKeysQuery.mockReset();
        hoisted.signingKeysQuery.mockImplementation((enabled?: boolean) => ({
            data: enabled === false ? undefined : SIGNING_KEYS,
            error: null,
            isLoading: false,
        }));
        hoisted.useRealScopeHook = false;

        act(() => {
            environmentStore.setState({currentEnvironmentId: PRODUCTION});
        });
    });

    afterEach(() => {
        act(() => {
            authenticationStore.setState({account: undefined, authenticated: false});
            environmentStore.setState({currentEnvironmentId: DEVELOPMENT});
        });

        resetAll();
    });

    it('testSigningKeysNeedEmbeddedSettingsManage', () => {
        hoisted.grants = {
            [DEVELOPMENT]: ['EMBEDDED_SETTINGS_MANAGE'],
            [PRODUCTION]: ['API_KEY_CREATE', 'API_KEY_DELETE', 'API_KEY_VIEW', 'EMBEDDED_VIEW'],
        };

        const editorRender = render(<SigningKeys />);

        expect(screen.getByText(NO_PERMISSION_TEXT)).toBeInTheDocument();
        expect(screen.queryByText('SDK key')).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'New Signing Key'})).not.toBeInTheDocument();
        expect(hoisted.signingKeysQuery).toHaveBeenLastCalledWith(false);

        editorRender.unmount();

        hoisted.grants = {[PRODUCTION]: ['EMBEDDED_SETTINGS_MANAGE']};

        render(<SigningKeys />);

        expect(hoisted.signingKeysQuery).toHaveBeenLastCalledWith(true);
        expect(screen.queryByText(NO_PERMISSION_TEXT)).not.toBeInTheDocument();
        expect(screen.getByText('SDK key')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'New Signing Key'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Edit Signing Key'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Delete Signing Key'})).toBeInTheDocument();
    });

    it('testATenantAdminSeesEveryControl', () => {
        hoisted.useRealScopeHook = true;

        act(() => {
            authenticationStore.setState({
                account: {authorities: ['ROLE_ADMIN'], login: 'admin'} as never,
                authenticated: true,
            });
        });

        render(<SigningKeys />);

        expect(screen.getByText('SDK key')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'New Signing Key'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Edit Signing Key'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Delete Signing Key'})).toBeInTheDocument();
    });
});
