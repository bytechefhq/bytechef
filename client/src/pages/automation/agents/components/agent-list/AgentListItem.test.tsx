import {TooltipProvider} from '@/components/ui/tooltip';
import {AiAgent} from '@/shared/middleware/graphql';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import AgentListItem from './AgentListItem';

// vi.hoisted is the only place top-level variables can live and still be referenced inside vi.mock
// factories — vi.mock calls hoist above module-scope `const` declarations.
const {navigateMock} = vi.hoisted(() => ({
    navigateMock: vi.fn(),
}));

vi.mock('react-router-dom', () => ({
    useNavigate: () => navigateMock,
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: vi.fn((selector) => selector({currentWorkspaceId: 7})),
}));

// The row's Deploy button imports ProjectDeploymentDialog, which transitively reaches ConnectionDialog and a
// chain of module-scope enum reads off the graphql module this file mocks wholesale.
vi.mock('@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialog', () => ({
    default: () => <div data-testid="project-deployment-dialog" />,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAiAgentTagsQuery: vi.fn().mockReturnValue({data: {aiAgentTags: []}}),
    useDeleteAiAgentMutation: vi.fn().mockReturnValue({isPending: false, mutate: vi.fn()}),
    usePublishAiAgentMutation: vi.fn().mockReturnValue({isPending: false, mutate: vi.fn()}),
    useUpdateAiAgentTagsMutation: vi.fn().mockReturnValue({isPending: false, mutate: vi.fn()}),
}));

const renderItem = (agent: Partial<AiAgent>) => {
    const queryClient = new QueryClient({defaultOptions: {mutations: {retry: false}, queries: {retry: false}}});

    // TooltipProvider: the row's TagList — and the scheduled marker — render Radix Tooltips, which throw
    // outside a provider. The app mounts one globally in main.tsx, so this only stands in for that.
    return render(
        <QueryClientProvider client={queryClient}>
            <TooltipProvider>
                <AgentListItem
                    agent={
                        {
                            channels: [],
                            description: null,
                            elements: [],
                            id: '1',
                            lastPublishedVersion: 0,
                            projectId: '5',
                            tags: [],
                            title: 'Refund Agent',
                            ...agent,
                            // eslint-disable-next-line @typescript-eslint/no-explicit-any
                        } as any
                    }
                />
            </TooltipProvider>
        </QueryClientProvider>
    );
};

describe('AgentListItem', () => {
    it('marks an agent that has a schedule channel', () => {
        renderItem({
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            channels: [{channelType: 'schedule', id: '1', parameters: {expression: '30 9 * * ?'}} as any],
        });

        expect(screen.getByLabelText('Scheduled')).toBeInTheDocument();
    });

    it('renders no marker for an agent with no schedule channel', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        renderItem({channels: [{channelType: 'chat', id: '1', parameters: {}} as any]});

        expect(screen.queryByLabelText('Scheduled')).not.toBeInTheDocument();
    });
});
