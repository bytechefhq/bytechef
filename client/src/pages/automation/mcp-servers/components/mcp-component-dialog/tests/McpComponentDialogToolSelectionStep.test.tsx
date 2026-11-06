import {render, resetAll, screen} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import McpComponentDialogToolSelectionStep from '../McpComponentDialogToolSelectionStep';

const hoisted = vi.hoisted(() => ({
    isLoadingComponentDefinition: false,
    toolElements: [] as Array<{description?: string; name: string; title?: string}>,
}));

vi.mock('../hooks/useMcpComponentDialogToolSelectionStep', () => ({
    default: () => ({
        allToolsSelected: false,
        componentDefinition: undefined,
        connections: [],
        currentWorkspaceId: 1,
        handleSelectAllTools: vi.fn(),
        handleToolToggle: vi.fn(),
        isLoadingComponentDefinition: hoisted.isLoadingComponentDefinition,
        isLoadingConnections: false,
        refetchConnections: vi.fn(),
        selectAllCheckboxRef: {current: null},
        toolElements: hoisted.toolElements,
    }),
}));

vi.mock('@/shared/queries/automation/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({data: []}),
}));

vi.mock('@/shared/queries/automation/connections.queries', () => ({
    ConnectionKeys: {connectionTags: ['connectionTags'], connections: ['connections']},
    useGetConnectionTagsQuery: () => ({data: []}),
}));

vi.mock('@/shared/mutations/automation/connections.mutations', () => ({
    useCreateConnectionMutation: vi.fn(),
}));

const renderStep = () =>
    render(
        <McpComponentDialogToolSelectionStep
            onConnectionChange={vi.fn()}
            onToolsChange={vi.fn()}
            open
            selectedComponent={null}
            selectedConnection={null}
            selectedTools={[]}
        />
    );

describe('McpComponentDialogToolSelectionStep', () => {
    beforeEach(() => {
        hoisted.isLoadingComponentDefinition = false;
        hoisted.toolElements = [];
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    it('shows skeleton tool rows while the component definition loads', () => {
        hoisted.isLoadingComponentDefinition = true;

        renderStep();

        expect(screen.getByRole('status', {name: 'Loading tools'})).toBeInTheDocument();
        expect(screen.queryByText('No tools available for this component.')).not.toBeInTheDocument();
    });

    it('lists the tools once the component definition has loaded', () => {
        hoisted.toolElements = [{description: 'Sends a message', name: 'sendMessage', title: 'Send Message'}];

        renderStep();

        expect(screen.queryByRole('status', {name: 'Loading tools'})).not.toBeInTheDocument();
        expect(screen.getByText('Send Message')).toBeInTheDocument();
    });
});
