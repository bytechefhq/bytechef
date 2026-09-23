import IntegrationMarketplaceGate from '@/ee/pages/embedded/integration-marketplace/IntegrationMarketplaceGate';
import {useIntegrationMarketplaceStore} from '@/ee/pages/embedded/integration-marketplace/stores/useIntegrationMarketplaceStore';
import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {applyHubThemeMock, setThemeMock} = vi.hoisted(() => ({
    applyHubThemeMock: vi.fn(),
    setThemeMock: vi.fn(),
}));

vi.mock('@/ee/pages/embedded/automation-hub/theme/applyHubTheme', () => ({applyHubTheme: applyHubThemeMock}));

vi.mock('@/ee/pages/embedded/integration-marketplace/IntegrationMarketplaceView', () => ({
    default: () => <div data-testid="marketplace-view" />,
}));

vi.mock('@/shared/providers/theme-provider', () => ({useTheme: () => ({setTheme: setThemeMock})}));

describe('IntegrationMarketplaceGate', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        applyHubThemeMock.mockReturnValue('dark');

        useIntegrationMarketplaceStore.setState({initialized: false, theme: {}});
    });

    it('waits for the host page to initialize the marketplace', () => {
        render(<IntegrationMarketplaceGate />);

        expect(screen.getByTestId('marketplace-gate-loading')).toBeInTheDocument();
        expect(screen.queryByTestId('marketplace-view')).not.toBeInTheDocument();
        expect(setThemeMock).not.toHaveBeenCalled();
    });

    it('applies the host theme and shows the marketplace once initialized', () => {
        const theme = {mode: 'dark' as const};

        useIntegrationMarketplaceStore.setState({initialized: true, theme});

        render(<IntegrationMarketplaceGate />);

        expect(screen.getByTestId('marketplace-view')).toBeInTheDocument();
        expect(applyHubThemeMock).toHaveBeenCalledWith(theme);
        expect(setThemeMock).toHaveBeenCalledWith('dark');
    });
});
