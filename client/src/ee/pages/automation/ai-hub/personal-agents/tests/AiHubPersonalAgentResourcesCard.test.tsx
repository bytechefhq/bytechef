import {TooltipProvider} from '@/components/ui/tooltip';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

// vi.hoisted is the only place top-level variables can live and still be referenced inside vi.mock factories
// — vi.mock calls hoist to the top of the file, BEFORE module-scope `const` declarations execute. Without
// hoisted refs the factories crash at "Cannot access X before initialization" on every test run.
const {
    addResourceMutateAsyncMock,
    addResourceMutateMock,
    addToolMutateAsyncMock,
    addToolMutateMock,
    removeResourceMutateMock,
    removeToolMutateMock,
    resourcePickerOnSelectRef,
} = vi.hoisted(() => {
    // Capture the onSelect callback passed to ResourcePickerMenu so tests can simulate user picks
    // without driving the real nested popover.
    const resourcePickerOnSelectRef: {current: ((selection: {id: string; kind: string; name: string}) => void) | null} =
        {current: null};

    return {
        addResourceMutateAsyncMock: vi.fn().mockResolvedValue({}),
        addResourceMutateMock: vi.fn(),
        addToolMutateAsyncMock: vi.fn().mockResolvedValue({}),
        addToolMutateMock: vi.fn(),
        removeResourceMutateMock: vi.fn(),
        removeToolMutateMock: vi.fn(),
        resourcePickerOnSelectRef,
    };
});

vi.mock('@/ee/pages/automation/ai-hub/personal-agents/hooks/useAiHubPersonalAgents', () => ({
    useAddAiHubPersonalAgentResourceMutation: vi.fn().mockReturnValue({
        isPending: false,
        mutate: addResourceMutateMock,
        mutateAsync: addResourceMutateAsyncMock,
    }),
    useAddAiHubPersonalAgentToolMutation: vi.fn().mockReturnValue({
        isPending: false,
        mutate: addToolMutateMock,
        mutateAsync: addToolMutateAsyncMock,
    }),
    useRemoveAiHubPersonalAgentResourceMutation: vi.fn().mockReturnValue({
        isPending: false,
        mutate: removeResourceMutateMock,
        mutateAsync: vi.fn(),
    }),
    useRemoveAiHubPersonalAgentToolMutation: vi.fn().mockReturnValue({
        isPending: false,
        mutate: removeToolMutateMock,
        mutateAsync: vi.fn(),
    }),
}));

// Capture onSelect from ResourcePickerMenu so tests can simulate picks without rendering the real popover.
vi.mock('@/ee/pages/automation/ai-hub/resource-picker/ResourcePickerMenu', () => ({
    default: ({
        onSelect,
        trigger,
    }: {
        onSelect: (sel: {id: string; kind: string; name: string}) => void;
        trigger: ReactNode;
    }) => {
        // Store the latest onSelect so tests can invoke it directly.
        resourcePickerOnSelectRef.current = onSelect;

        return <div data-testid="resource-picker-menu">{trigger}</div>;
    },
}));

// ToolsBranchBody uses useGetComponentDefinitionsQuery + useGetComponentDefinitionQuery — mock both so
// the component tree renders cleanly even though we never open the Tools branch in these tests.
vi.mock('@/shared/queries/automation/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: vi.fn().mockReturnValue({data: [], isLoading: false}),
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    useGetComponentDefinitionQuery: vi.fn().mockReturnValue({data: undefined, isLoading: false}),
}));

// useFilteredComponentDefinitions is used inside ToolsBranchBody — mock it to avoid real debounce logic.
vi.mock('@/pages/platform/workflow-editor/hooks/useFilteredComponentDefinitions', () => ({
    useFilteredComponentDefinitions: vi.fn().mockReturnValue({
        componentsWithActions: [],
        filter: '',
        isSearchFetching: false,
        setFilter: vi.fn(),
    }),
}));

// AiHubPersonalAgentToolConfigDialog renders a heavy modal tree — stub it out since configure-dialog
// behaviour is not under test here.
vi.mock('@/ee/pages/automation/ai-hub/personal-agents/AiHubPersonalAgentToolConfigDialog', () => ({
    default: () => null,
}));

import AiHubPersonalAgentResourcesCard, {
    AiHubPersonalAgentPendingResourceI,
    AiHubPersonalAgentPendingToolI,
} from '../AiHubPersonalAgentResourcesCard';
import {AiHubPersonalAgentResourceI, AiHubPersonalAgentToolI} from '../hooks/useAiHubPersonalAgents';

const wrap = (ui: ReactNode) =>
    render(
        <QueryClientProvider client={new QueryClient({defaultOptions: {queries: {retry: false}}})}>
            <TooltipProvider>{ui}</TooltipProvider>
        </QueryClientProvider>
    );

const makeTool = (overrides: Partial<AiHubPersonalAgentToolI> = {}): AiHubPersonalAgentToolI => ({
    aiHubPersonalAgentId: 1,
    componentName: 'slack',
    componentVersion: 1,
    connectionId: null,
    id: 10,
    operationName: 'sendMessage',
    parameters: {},
    ...overrides,
});

const makeResource = (overrides: Partial<AiHubPersonalAgentResourceI> = {}): AiHubPersonalAgentResourceI => ({
    aiHubPersonalAgentId: 1,
    createdAt: '2026-01-01T00:00:00Z',
    id: 20,
    kind: 'FILE',
    resourceId: 'file-1',
    resourceName: 'notes.md',
    ...overrides,
});

const editModeProps = (tools: AiHubPersonalAgentToolI[], resources: AiHubPersonalAgentResourceI[]) => ({
    aiHubPersonalAgentId: 1,
    environmentId: 0,
    resources,
    tools,
    workspaceId: 7,
});

const createModeProps = (
    pendingTools: AiHubPersonalAgentPendingToolI[],
    pendingResources: AiHubPersonalAgentPendingResourceI[],
    overrides: {
        onAddPendingResource?: (resource: AiHubPersonalAgentPendingResourceI) => void;
        onAddPendingTool?: (tool: AiHubPersonalAgentPendingToolI) => void;
        onRemovePendingResource?: (index: number) => void;
        onRemovePendingTool?: (index: number) => void;
    } = {}
) => ({
    environmentId: 0,
    onAddPendingResource: overrides.onAddPendingResource ?? vi.fn(),
    onAddPendingTool: overrides.onAddPendingTool ?? vi.fn(),
    onRemovePendingResource: overrides.onRemovePendingResource ?? vi.fn(),
    onRemovePendingTool: overrides.onRemovePendingTool ?? vi.fn(),
    pendingResources,
    pendingTools,
    workspaceId: 7,
});

beforeEach(() => {
    addResourceMutateAsyncMock.mockReset().mockResolvedValue({});
    addResourceMutateMock.mockReset();
    addToolMutateAsyncMock.mockReset().mockResolvedValue({});
    removeResourceMutateMock.mockReset();
    removeToolMutateMock.mockReset();
    resourcePickerOnSelectRef.current = null;
});

describe('AiHubPersonalAgentResourcesCard', () => {
    describe('edit mode', () => {
        it('renders existing tools and resources', () => {
            const tools = [
                makeTool({componentName: 'slack', id: 10, operationName: 'sendMessage'}),
                makeTool({componentName: 'github', id: 11, operationName: 'createIssue'}),
            ];
            const resources = [
                makeResource({id: 20, kind: 'FILE', resourceName: 'notes.md'}),
                makeResource({id: 21, kind: 'WORKFLOW', resourceName: 'my-workflow'}),
            ];

            wrap(<AiHubPersonalAgentResourcesCard {...editModeProps(tools, resources)} />);

            // ToolRow falls back to componentName when no definition is fetched — the mock returns undefined.
            expect(screen.getByText('slack')).toBeInTheDocument();
            expect(screen.getByText('github')).toBeInTheDocument();

            // ResourceRow renders resourceName directly.
            expect(screen.getByText('notes.md')).toBeInTheDocument();
            expect(screen.getByText('my-workflow')).toBeInTheDocument();
        });

        it('fires the add-resource mutation when a resource is selected via the picker', async () => {
            wrap(<AiHubPersonalAgentResourcesCard {...editModeProps([], [])} />);

            // ResourcePickerMenu is mocked; invoke its captured onSelect to simulate a user pick.
            expect(resourcePickerOnSelectRef.current).not.toBeNull();

            await resourcePickerOnSelectRef.current!({id: 'file-1', kind: 'file', name: 'notes.md'});

            expect(addResourceMutateAsyncMock).toHaveBeenCalledTimes(1);
            expect(addResourceMutateAsyncMock).toHaveBeenCalledWith({
                input: {
                    aiHubPersonalAgentId: '1',
                    kind: 'FILE',
                    resourceId: 'file-1',
                    resourceName: 'notes.md',
                    workspaceId: '7',
                },
            });
        });

        it('fires the remove-resource mutation when the remove control is clicked', async () => {
            const resources = [makeResource({id: 20, resourceName: 'notes.md'})];

            wrap(<AiHubPersonalAgentResourcesCard {...editModeProps([], resources)} />);

            // Open the 3-dot dropdown on the resource row.
            const ellipsisButtons = screen.getAllByRole('button', {name: ''});

            // The resource row's ellipsis button is after the heading row; click the last one.
            await userEvent.click(ellipsisButtons[ellipsisButtons.length - 1]);

            const removeButton = await screen.findByRole('menuitem', {name: /remove/i});

            await userEvent.click(removeButton);

            expect(removeResourceMutateMock).toHaveBeenCalledTimes(1);
            expect(removeResourceMutateMock).toHaveBeenCalledWith({
                id: '20',
                workspaceId: '7',
            });
        });
    });

    describe('create mode', () => {
        it('renders pending resources and routes a pick to onAddPendingResource', async () => {
            const pendingResources: AiHubPersonalAgentPendingResourceI[] = [
                {kind: 'FILE', resourceId: 'file-99', resourceName: 'draft.txt'},
            ];
            const onAddPendingResource = vi.fn();

            wrap(
                <AiHubPersonalAgentResourcesCard {...createModeProps([], pendingResources, {onAddPendingResource})} />
            );

            // The pending resource row should be visible.
            expect(screen.getByText('draft.txt')).toBeInTheDocument();

            // Invoke the mocked picker's onSelect — should go to onAddPendingResource, not the mutation.
            expect(resourcePickerOnSelectRef.current).not.toBeNull();

            await resourcePickerOnSelectRef.current!({id: 'file-2', kind: 'knowledgeBase', name: 'kb-docs'});

            expect(onAddPendingResource).toHaveBeenCalledTimes(1);
            expect(onAddPendingResource).toHaveBeenCalledWith({
                kind: 'KNOWLEDGE_BASE',
                resourceId: 'file-2',
                resourceName: 'kb-docs',
            });

            // The resource add-mutation must NOT be called in create mode.
            expect(addResourceMutateAsyncMock).not.toHaveBeenCalled();
        });
    });
});
