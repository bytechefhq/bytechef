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
            workspaceAiGatewayProviders: [{enabled: true, id: '20', name: 'OpenAI', type: 'OPENAI'}],
        },
        isLoading: false,
    }),
    useWorkspaceAiModelsQuery: () => ({
        data: {
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
        Element.prototype.hasPointerCapture = vi.fn();
        Element.prototype.releasePointerCapture = vi.fn();
        Element.prototype.scrollIntoView = vi.fn();

        hoisted.mutate.mockReset();
        hoisted.workspaceId = 1;
    });

    it('mounts without crashing and renders the prompt input', () => {
        render(<AiGatewayPlayground />);

        const textareas = screen.queryAllByRole('textbox');

        expect(textareas.length).toBeGreaterThan(0);
    });

    it('renders model provider selector sourced from the workspace providers query', async () => {
        render(<AiGatewayPlayground />);

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

        const runButtons = screen.queryAllByRole('button', {name: /run/i});

        for (const runButton of runButtons) {
            if (!runButton.hasAttribute('disabled')) {
                await user.click(runButton);
            }
        }

        expect(hoisted.mutate).not.toHaveBeenCalled();
    });

    it('runs the selected model as a provider-qualified identifier under the current workspace', async () => {
        render(<AiGatewayPlayground />);

        const user = userEvent.setup();

        await user.click(screen.getByRole('combobox'));
        await user.click(await screen.findByRole('option', {name: /gpt-4o \(OpenAI\)/i}));

        const textareas = screen.queryAllByRole('textbox');

        await user.click(textareas[0]);
        await user.keyboard('hello from test');

        await user.click(screen.getByRole('button', {name: /run/i}));

        expect(hoisted.mutate).toHaveBeenCalledWith({
            input: expect.objectContaining({
                messages: [{content: 'hello from test', role: 'USER'}],
                model: 'openai/gpt-4o',
                workspaceId: '1',
            }),
        });
    });
});
