import DevelopmentIntegrationInstanceConfigurationsRedirect from '@/ee/pages/embedded/integration-instance-configurations/DevelopmentIntegrationInstanceConfigurationsRedirect';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {render, screen} from '@/shared/util/test-utils';
import {MemoryRouter, Route, Routes, useLocation} from 'react-router-dom';
import {beforeEach, describe, expect, it} from 'vitest';

const IntegrationsLocation = () => {
    const location = useLocation();

    return (
        <div>{`${location.pathname}${location.search} ${(location.state as {marker?: string} | null)?.marker ?? 'none'}`}</div>
    );
};

const renderRedirect = (initialEntry: string | {pathname: string; search: string; state: unknown}) =>
    render(
        <MemoryRouter initialEntries={[initialEntry]}>
            <Routes>
                <Route
                    element={
                        <DevelopmentIntegrationInstanceConfigurationsRedirect>
                            <div>Instance configurations page</div>
                        </DevelopmentIntegrationInstanceConfigurationsRedirect>
                    }
                    path="/embedded/configurations"
                />

                <Route element={<IntegrationsLocation />} path="/embedded/integrations" />
            </Routes>
        </MemoryRouter>
    );

describe('DevelopmentIntegrationInstanceConfigurationsRedirect', () => {
    beforeEach(() => {
        environmentStore.setState({currentEnvironmentId: 0});
    });

    it('redirects to the integration configurations tab in development', () => {
        renderRedirect({pathname: '/embedded/configurations', search: '?integrationId=7', state: {marker: 'kept'}});

        expect(screen.getByText('/embedded/integrations?integrationId=7&tab=configurations kept')).toBeInTheDocument();
    });

    it('redirects to integrations when no integration is given', () => {
        renderRedirect('/embedded/configurations');

        expect(screen.getByText('/embedded/integrations none')).toBeInTheDocument();
    });

    it('renders the instance configurations page outside development', () => {
        environmentStore.setState({currentEnvironmentId: 2});

        renderRedirect('/embedded/configurations?integrationId=7');

        expect(screen.getByText('Instance configurations page')).toBeInTheDocument();
    });
});
