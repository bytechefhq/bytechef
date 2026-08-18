import {useAutomationHubStore} from '@/ee/pages/embedded/automation-hub/stores/useAutomationHubStore';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationHubLayout from '../AutomationHubLayout';

vi.mock('@/shared/providers/theme-provider', () => ({
    useTheme: () => ({setTheme: vi.fn(), theme: 'light'}),
}));

const DEFAULT_TABS = {automations: true, connections: true, newWorkflow: true};

const wrap = () =>
    render(
        <MemoryRouter initialEntries={['/embedded/hub']}>
            <AutomationHubLayout />
        </MemoryRouter>
    );

describe('AutomationHubLayout', () => {
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

    it('renders exactly the two route-backed tabs as links', () => {
        wrap();

        const tabs = screen.getAllByRole('tab');

        expect(tabs).toHaveLength(2);

        const automationsTab = screen.getByRole('tab', {name: 'Automations'});
        const connectionsTab = screen.getByRole('tab', {name: 'Connections'});

        expect(automationsTab.tagName).toBe('A');
        expect(automationsTab).toHaveAttribute('href', '/embedded/hub');
        expect(connectionsTab.tagName).toBe('A');
        expect(connectionsTab).toHaveAttribute('href', '/embedded/hub/connections');
    });

    it('hides the Automations tab when its section is disabled, leaving no tab strip', () => {
        useAutomationHubStore.setState({tabs: {...DEFAULT_TABS, automations: false}});

        wrap();

        expect(screen.queryByRole('tab', {name: 'Automations'})).not.toBeInTheDocument();
        expect(screen.queryByRole('tablist')).not.toBeInTheDocument();
    });

    it('hides the Connections tab when its section is disabled, leaving no tab strip', () => {
        useAutomationHubStore.setState({tabs: {...DEFAULT_TABS, connections: false}});

        wrap();

        expect(screen.queryByRole('tab', {name: 'Connections'})).not.toBeInTheDocument();
        expect(screen.queryByRole('tablist')).not.toBeInTheDocument();
    });

    it('shows a loading indicator before the store is initialized', () => {
        useAutomationHubStore.setState({initialized: false});

        wrap();

        expect(screen.getByTestId('automation-hub-loading')).toBeInTheDocument();
        expect(screen.queryByRole('tablist')).not.toBeInTheDocument();
        expect(screen.queryByRole('tab')).not.toBeInTheDocument();
    });
});
