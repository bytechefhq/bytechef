import {render, screen} from '@/shared/util/test-utils';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AiGatewayPlayground from '../AiGatewayPlayground';

const hoisted = vi.hoisted(() => ({
    mutate: vi.fn(),
    workspaceId: 1,
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: Record<string, unknown>) => unknown) =>
        selector({currentWorkspaceId: hoisted.workspaceId}),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    PlaygroundChatRole: {Assistant: 'ASSISTANT', System: 'SYSTEM', User: 'USER'},
    usePlaygroundChatCompletionMutation: () => ({
        isPending: false,
        mutate: hoisted.mutate,
    }),
    useWorkspaceAiGatewayModelsQuery: () => ({
        data: {
            workspaceAiGatewayModels: [
                {
                    enabled: true,
                    id: '1',
                    model: {id: '10', name: 'gpt-4o', provider: {id: '20', type: 'OPENAI'}},
                    providerId: '20',
                },
            ],
        },
        isLoading: false,
    }),
    useWorkspaceAiGatewayProvidersQuery: () => ({
        data: {
            workspaceAiGatewayProviders: [{enabled: true, id: '20', name: 'OpenAI'}],
        },
        isLoading: false,
    }),
}));

vi.mock('@/shared/util/cookie-utils', () => ({
    getCookie: () => 'xsrf-token-stub',
}));

describe('AiGatewayPlayground', () => {
    beforeEach(() => {
        hoisted.mutate.mockReset();
        hoisted.workspaceId = 1;
    });

    it('mounts without crashing and renders the prompt input', () => {
        render(<AiGatewayPlayground />);

        // The playground always exposes either a textarea for the user prompt or a message composer, depending on
        // chat vs text mode. Verify at least one text input surface exists so the component tree rendered fully.
        const textareas = screen.queryAllByRole('textbox');

        expect(textareas.length).toBeGreaterThan(0);
    });

    it('renders model provider selector sourced from the workspace providers query', () => {
        render(<AiGatewayPlayground />);

        // The model row shows the "OPENAI" provider type (or the model name). Assert the provider identifier is in
        // the DOM — a regression that broke the providers query would blank this out.
        expect(screen.queryAllByText(/OPENAI|gpt-4o/i).length).toBeGreaterThan(0);
    });

    it('does not fire the chat-completion mutation when no model is selected', async () => {
        render(<AiGatewayPlayground />);

        const user = userEvent.setup();

        const textareas = screen.queryAllByRole('textbox');
        const firstTextarea = textareas[0];

        expect(firstTextarea).toBeDefined();

        await user.click(firstTextarea);
        await user.keyboard('hello from test');

        // The Run button is guarded by selectedModelLeft — without a selection the click must be a no-op rather than
        // posting an empty request to the GraphQL endpoint (which would 500 or silently log).
        const runButtons = screen.queryAllByRole('button', {name: /run/i});

        for (const runButton of runButtons) {
            if (!runButton.hasAttribute('disabled')) {
                await user.click(runButton);
            }
        }

        expect(hoisted.mutate).not.toHaveBeenCalled();
    });
});
