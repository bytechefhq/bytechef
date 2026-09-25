import {render, screen} from '@/shared/util/test-utils';
import {MemoryRouter, Route, Routes, useLocation} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import EmbeddedIndexRedirect from './EmbeddedIndexRedirect';

const hoisted = vi.hoisted(() => ({
    isTenantAdmin: false,
}));

vi.mock('@/shared/hooks/useIsTenantAdmin', () => ({
    useIsTenantAdmin: () => hoisted.isTenantAdmin,
}));

const LocationPathname = () => <span data-testid="pathname">{useLocation().pathname}</span>;

const renderEmbeddedIndexRedirect = () =>
    render(
        <MemoryRouter initialEntries={['/embedded']}>
            <Routes>
                <Route path="/embedded">
                    <Route
                        element={<EmbeddedIndexRedirect fallbackHref="account" tenantAdminHref="integrations" />}
                        index
                    />
                </Route>

                <Route element={<LocationPathname />} path="*" />
            </Routes>
        </MemoryRouter>
    );

describe('EmbeddedIndexRedirect', () => {
    beforeEach(() => {
        hoisted.isTenantAdmin = false;
    });

    it('sends a tenant admin to the tenant admin page', () => {
        hoisted.isTenantAdmin = true;

        renderEmbeddedIndexRedirect();

        expect(screen.getByTestId('pathname')).toHaveTextContent('/embedded/integrations');
    });

    it('sends a non-admin to the fallback account page', () => {
        renderEmbeddedIndexRedirect();

        expect(screen.getByTestId('pathname')).toHaveTextContent('/embedded/account');
    });
});
