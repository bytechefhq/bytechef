import {fireEvent, render, screen, waitFor} from '@/shared/util/test-utils';
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
    useWorkspaceAiGatewayProvidersQuery: () => ({
        data: {
            workspaceAiGatewayProviders: [{enabled: true, id: '20', name: 'OpenAI'}],
        },
        isLoading: false,
    }),
    useWorkspaceAiModelsQuery: () => ({
        data: {
            // Flat shape, matching the generated WorkspaceAiModelsQuery type the component reads
            // (model.name / model.providerId), not a nested model object.
            workspaceAiModels: [{enabled: true, id: '1', name: 'gpt-4o', providerId: '20'}],
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

    it('renders model provider selector sourced from the workspace providers query', async () => {
        render(<AiGatewayPlayground />);

        // The model options live inside a closed Radix Select, so open it first. Each option renders the model name
        // plus its provider name resolved from the providers query — "gpt-4o (OpenAI)". Asserting the provider name
        // appears guards the providers-query wiring: a regression that broke it would blank out the "(OpenAI)" suffix.
        fireEvent.click(screen.getByRole('combobox'));

        await waitFor(() => {
            expect(screen.getByRole('option', {name: /gpt-4o \(OpenAI\)/i})).toBeInTheDocument();
        });
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
