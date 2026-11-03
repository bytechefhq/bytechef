import DevelopmentAutomationWorkflowConfigurationsRedirect from '@/ee/pages/embedded/automation-workflow-configurations/DevelopmentAutomationWorkflowConfigurationsRedirect';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {render, screen} from '@/shared/util/test-utils';
import {MemoryRouter, Route, Routes, useLocation} from 'react-router-dom';
import {beforeEach, describe, expect, it} from 'vitest';

const AutomationWorkflowsLocation = () => {
    const location = useLocation();

    return <div>{`${location.pathname}${location.search}`}</div>;
};

const renderRedirect = (initialEntry: string) =>
    render(
        <MemoryRouter initialEntries={[initialEntry]}>
            <Routes>
                <Route
                    element={
                        <DevelopmentAutomationWorkflowConfigurationsRedirect>
                            <div>Automation configurations page</div>
                        </DevelopmentAutomationWorkflowConfigurationsRedirect>
                    }
                    path="/embedded/automation-configurations"
                />

                <Route element={<AutomationWorkflowsLocation />} path="/embedded/automation-workflows" />
            </Routes>
        </MemoryRouter>
    );

describe('DevelopmentAutomationWorkflowConfigurationsRedirect', () => {
    beforeEach(() => {
        environmentStore.setState({currentEnvironmentId: 0});
    });

    it('redirects to the project configurations tab in development', () => {
        renderRedirect('/embedded/automation-configurations?projectId=7');

        expect(screen.getByText('/embedded/automation-workflows?projectId=7&tab=configurations')).toBeInTheDocument();
    });

    it('redirects to automations when no project is given', () => {
        renderRedirect('/embedded/automation-configurations');

        expect(screen.getByText('/embedded/automation-workflows')).toBeInTheDocument();
    });

    it('renders the automation configurations page outside development', () => {
        environmentStore.setState({currentEnvironmentId: 2});

        renderRedirect('/embedded/automation-configurations?projectId=7');

        expect(screen.getByText('Automation configurations page')).toBeInTheDocument();
    });
});
