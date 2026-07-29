import {render, resetAll, screen} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import Components from './Components';

const hoisted = vi.hoisted(() => ({
    mockIsFeatureFlagEnabled: vi.fn(),
    mockUseApiConnectorsQuery: vi.fn(),
    mockUseCustomComponentsQuery: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async () => {
    const actual = await vi.importActual<Record<string, unknown>>('@/shared/middleware/graphql');

    return {
        ...actual,
        useApiConnectorsQuery: hoisted.mockUseApiConnectorsQuery,
        useCustomComponentsQuery: hoisted.mockUseCustomComponentsQuery,
    };
});

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => hoisted.mockIsFeatureFlagEnabled,
}));

vi.mock('@/ee/pages/settings/platform/api-connectors/components/ApiConnectorEndpointDetailPanel', () => ({
    default: () => <div data-testid="endpoint-detail-panel" />,
}));

const renderComponents = (tab: 'api-connectors' | 'custom') =>
    render(
        <MemoryRouter initialEntries={[`/automation/settings/components/${tab}`]}>
            <Components tab={tab} />
        </MemoryRouter>
    );

beforeEach(() => {
    hoisted.mockIsFeatureFlagEnabled.mockReturnValue(true);
    hoisted.mockUseApiConnectorsQuery.mockReturnValue({data: {apiConnectors: []}, error: null, isLoading: false});
    hoisted.mockUseCustomComponentsQuery.mockReturnValue({data: {customComponents: []}, error: null, isLoading: false});
});

afterEach(() => {
    resetAll();
});

describe('Components', () => {
    it('renders both tabs when both feature flags are enabled', () => {
        renderComponents('custom');

        expect(screen.getByRole('tab', {name: 'Custom Components'})).toBeInTheDocument();
        expect(screen.getByRole('tab', {name: 'API Connectors'})).toBeInTheDocument();
    });

    it('hides the tab bar when only one feature flag is enabled', () => {
        hoisted.mockIsFeatureFlagEnabled.mockImplementation((flag: string) => flag === 'ff-1024');

        renderComponents('custom');

        expect(screen.queryByRole('tab', {name: 'Custom Components'})).not.toBeInTheDocument();
    });

    it('shows the custom components empty state on the custom tab', () => {
        renderComponents('custom');

        expect(screen.getByText('No Custom Components')).toBeInTheDocument();
    });

    it('shows the API connectors empty state and endpoint panel on the api-connectors tab', () => {
        renderComponents('api-connectors');

        expect(screen.getByText('No API Connectors')).toBeInTheDocument();
        expect(screen.getByTestId('endpoint-detail-panel')).toBeInTheDocument();
    });

    it('renders the shared New Component menu in the header', () => {
        renderComponents('custom');

        expect(screen.getAllByRole('button', {name: /New Component/})).not.toHaveLength(0);
    });
});
