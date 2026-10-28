import DevelopmentProjectDeploymentsRedirect from '@/pages/automation/project-deployments/DevelopmentProjectDeploymentsRedirect';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {render, screen} from '@/shared/util/test-utils';
import {MemoryRouter, Route, Routes, useLocation} from 'react-router-dom';
import {beforeEach, describe, expect, it} from 'vitest';

const ProjectsLocation = () => {
    const location = useLocation();

    return (
        <div>{`${location.pathname}${location.search} ${(location.state as {projectDeploymentId?: number} | null)?.projectDeploymentId ?? 'none'}`}</div>
    );
};

const renderRedirect = (initialEntry: string | {pathname: string; search: string; state: unknown}) =>
    render(
        <MemoryRouter initialEntries={[initialEntry]}>
            <Routes>
                <Route
                    element={
                        <DevelopmentProjectDeploymentsRedirect>
                            <div>Deployments page</div>
                        </DevelopmentProjectDeploymentsRedirect>
                    }
                    path="/automation/deployments"
                />

                <Route element={<ProjectsLocation />} path="/automation/projects" />
            </Routes>
        </MemoryRouter>
    );

describe('DevelopmentProjectDeploymentsRedirect', () => {
    beforeEach(() => {
        environmentStore.setState({currentEnvironmentId: 0});
    });

    it('redirects to the project deployments tab in development', () => {
        renderRedirect({pathname: '/automation/deployments', search: '?projectId=7', state: {projectDeploymentId: 3}});

        expect(screen.getByText('/automation/projects?projectId=7&tab=deployments 3')).toBeInTheDocument();
    });

    it('redirects to projects when no project is given', () => {
        renderRedirect('/automation/deployments');

        expect(screen.getByText('/automation/projects none')).toBeInTheDocument();
    });

    it('renders the deployments page outside development', () => {
        environmentStore.setState({currentEnvironmentId: 2});

        renderRedirect('/automation/deployments?projectId=7');

        expect(screen.getByText('Deployments page')).toBeInTheDocument();
    });
});
