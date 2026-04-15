import {render, screen} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AiObservabilityTraces from '../AiObservabilityTraces';

const hoisted = vi.hoisted(() => ({
    currentWorkspaceId: 1,
    traces: [] as Array<Record<string, unknown>>,
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: Record<string, unknown>) => unknown) =>
        selector({currentWorkspaceId: hoisted.currentWorkspaceId}),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    AiObservabilityTraceSource: {API: 'API', COPILOT: 'COPILOT', PLAYGROUND: 'PLAYGROUND'},
    AiObservabilityTraceStatus: {ACTIVE: 'ACTIVE', COMPLETED: 'COMPLETED', ERROR: 'ERROR'},
    useAiGatewayTagsQuery: () => ({data: {aiGatewayTags: []}, isLoading: false}),
    useAiObservabilityTracesQuery: () => ({
        data: {aiObservabilityTraces: hoisted.traces},
        isLoading: false,
    }),
}));

describe('AiObservabilityTraces', () => {
    beforeEach(() => {
        hoisted.currentWorkspaceId = 1;
        hoisted.traces = [];
    });

    it('renders the empty state when no traces match the filters', () => {
        render(<AiObservabilityTraces onSelectTrace={vi.fn()} />);

        expect(screen.getByText('No Traces')).toBeInTheDocument();
    });

    it('renders rows when the query returns traces', () => {
        hoisted.traces = [
            {
                createdDate: new Date().toISOString(),
                id: '1',
                name: 'trace-named-foo',
                source: 'API',
                status: 'COMPLETED',
                totalCost: 0.01,
                totalInputTokens: 100,
                totalLatencyMs: 250,
                totalOutputTokens: 50,
                userId: 'user-named-bar',
            },
        ];

        render(<AiObservabilityTraces onSelectTrace={vi.fn()} />);

        expect(screen.getByText('trace-named-foo')).toBeInTheDocument();
        expect(screen.getByText('user-named-bar')).toBeInTheDocument();
    });
});
