import {useAutomationHubStore} from '@/ee/pages/embedded/automation-hub/stores/useAutomationHubStore';
import {render, screen, waitFor} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationHubLayout from '../AutomationHubLayout';

const {navigateMock} = vi.hoisted(() => ({navigateMock: vi.fn()}));

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');

    return {...actual, useNavigate: () => navigateMock};
});

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
        navigateMock.mockReset();
        window.sessionStorage.clear();

        useAutomationHubStore.setState({
            connectionDialogAllowed: true,
            includeComponents: undefined,
            initialized: true,
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

    it('never renders a page title, because that belongs to the host application', () => {
        wrap();

        expect(screen.queryByText('Automation Hub')).not.toBeInTheDocument();

        expect(screen.getByRole('tablist')).toBeInTheDocument();
    });

    it('drops the tab strip while a workflow is open in the builder', () => {
        render(
            <MemoryRouter initialEntries={['/embedded/hub/builder/wf-1']}>
                <AutomationHubLayout />
            </MemoryRouter>
        );

        expect(screen.queryByRole('tablist')).not.toBeInTheDocument();
    });

    it('gives the routed view a height box to fill, not just a top margin', () => {
        const {container} = wrap();

        const main = container.querySelector('main');

        expect(main?.className).toContain('min-h-0');
        expect(main?.className).toContain('flex-1');

        const outletWrapper = main?.lastElementChild;

        expect(outletWrapper?.className).toContain('min-h-0');
        expect(outletWrapper?.className).toContain('flex-1');
    });

    it('shows a loading indicator before the store is initialized', () => {
        useAutomationHubStore.setState({initialized: false});

        wrap();

        expect(screen.getByTestId('automation-hub-loading')).toBeInTheDocument();
        expect(screen.queryByRole('tablist')).not.toBeInTheDocument();
        expect(screen.queryByRole('tab')).not.toBeInTheDocument();
    });

    it('returns the viewer to the route they were on when the host page is refreshed', async () => {
        window.sessionStorage.setItem('automationHub.route', '/embedded/hub/builder/wf-1');

        wrap();

        await waitFor(() => expect(navigateMock).toHaveBeenCalledWith('/embedded/hub/builder/wf-1', {replace: true}));
    });

    it('still restores the route when the hub renders before the handshake lands', async () => {
        useAutomationHubStore.setState({initialized: false});
        window.sessionStorage.setItem('automationHub.route', '/embedded/hub/builder/wf-1');

        const {rerender} = wrap();

        expect(navigateMock).not.toHaveBeenCalled();

        useAutomationHubStore.setState({initialized: true});

        rerender(
            <MemoryRouter initialEntries={['/embedded/hub']}>
                <AutomationHubLayout />
            </MemoryRouter>
        );

        await waitFor(() => expect(navigateMock).toHaveBeenCalledWith('/embedded/hub/builder/wf-1', {replace: true}));
    });

    it('ignores a stored route that does not belong to the hub', () => {
        window.sessionStorage.setItem('automationHub.route', '/somewhere/else');

        wrap();

        expect(navigateMock).not.toHaveBeenCalled();
    });

    it('remembers the route it is on', () => {
        wrap();

        expect(window.sessionStorage.getItem('automationHub.route')).toBe('/embedded/hub');
    });
});
