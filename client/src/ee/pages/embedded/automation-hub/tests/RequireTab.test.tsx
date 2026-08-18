import {useAutomationHubStore} from '@/ee/pages/embedded/automation-hub/stores/useAutomationHubStore';
import {render, screen} from '@testing-library/react';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {beforeEach, describe, expect, it} from 'vitest';

import RequireTab from '../RequireTab';

const DEFAULT_TABS = {automations: true, connections: true, newWorkflow: true};

describe('RequireTab', () => {
    beforeEach(() => {
        useAutomationHubStore.setState({
            connectionDialogAllowed: true,
            includeComponents: undefined,
            initialized: true,
            sharedConnectionIds: [],
            tabs: DEFAULT_TABS,
            theme: {},
        });
    });

    it('renders its children when the tab is enabled', () => {
        render(
            <MemoryRouter initialEntries={['/embedded/hub/connections']}>
                <Routes>
                    <Route
                        element={
                            <RequireTab tab="connections">
                                <div>Connections content</div>
                            </RequireTab>
                        }
                        path="/embedded/hub/connections"
                    />
                </Routes>
            </MemoryRouter>
        );

        expect(screen.getByText('Connections content')).toBeInTheDocument();
    });

    it('redirects to the first enabled tab when the requested tab is disabled', () => {
        useAutomationHubStore.setState({tabs: {...DEFAULT_TABS, connections: false}});

        render(
            <MemoryRouter initialEntries={['/embedded/hub/connections']}>
                <Routes>
                    <Route element={<div>Automations content</div>} path="/embedded/hub" />

                    <Route
                        element={
                            <RequireTab tab="connections">
                                <div>Connections content</div>
                            </RequireTab>
                        }
                        path="/embedded/hub/connections"
                    />
                </Routes>
            </MemoryRouter>
        );

        expect(screen.queryByText('Connections content')).not.toBeInTheDocument();
        expect(screen.getByText('Automations content')).toBeInTheDocument();
    });

    it('renders an empty state instead of redirecting when every route-backed tab is disabled', () => {
        useAutomationHubStore.setState({
            tabs: {automations: false, connections: false, newWorkflow: true},
        });

        render(
            <MemoryRouter initialEntries={['/embedded/hub']}>
                <Routes>
                    <Route
                        element={
                            <RequireTab tab="automations">
                                <div>Automations content</div>
                            </RequireTab>
                        }
                        path="/embedded/hub"
                    />
                </Routes>
            </MemoryRouter>
        );

        expect(screen.queryByText('Automations content')).not.toBeInTheDocument();
        expect(screen.getByTestId('require-tab-empty-state')).toBeInTheDocument();
        expect(screen.getByText('No sections are enabled for this hub.')).toBeInTheDocument();
    });
});
