import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AgentInstructionsCard from './AgentInstructionsCard';

const {updateAiAgentMutate} = vi.hoisted(() => ({
    updateAiAgentMutate: vi.fn(),
}));

// Stubbed rather than rendered: the card's own contract is the dirty guard and the mutation payload, and a
// ProseMirror surface cannot be typed into under jsdom. MarkdownEditor has its own test file for the
// markdown rendering and the blur round-trip.
vi.mock('@/shared/components/markdown-editor/MarkdownEditor', () => ({
    default: ({
        ariaLabel,
        onBlur,
        onChange,
        placeholder,
        value,
    }: {
        ariaLabel?: string;
        onBlur?: (markdown: string) => void;
        onChange?: (markdown: string) => void;
        placeholder?: string;
        value: string;
    }) => (
        <textarea
            aria-label={ariaLabel}
            onBlur={(event) => onBlur?.(event.target.value)}
            onChange={(event) => onChange?.(event.target.value)}
            placeholder={placeholder}
            value={value}
        />
    ),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useUpdateAiAgentMutation: () => ({isPending: false, mutate: updateAiAgentMutate}),
}));

const wrap = (ui: ReactNode) => {
    const queryClient = new QueryClient({defaultOptions: {mutations: {retry: false}, queries: {retry: false}}});

    return <QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>;
};

describe('AgentInstructionsCard', () => {
    beforeEach(() => {
        updateAiAgentMutate.mockReset();
    });

    it('saves the edited markdown on blur', async () => {
        const user = userEvent.setup();

        render(wrap(<AgentInstructionsCard agentId="agent-1" instructions="Be terse." />));

        const editor = screen.getByLabelText('Instructions');

        await user.clear(editor);
        await user.type(editor, '# Tone');
        await user.tab();

        expect(updateAiAgentMutate).toHaveBeenCalledWith({input: {id: 'agent-1', instructions: '# Tone'}});
    });

    it('does not save when the field is blurred without an edit', async () => {
        const user = userEvent.setup();

        render(wrap(<AgentInstructionsCard agentId="agent-1" instructions="* Be terse." />));

        await user.click(screen.getByLabelText('Instructions'));
        await user.tab();

        expect(updateAiAgentMutate).not.toHaveBeenCalled();
    });

    it('renders an empty editor when the agent has no instructions', () => {
        render(wrap(<AgentInstructionsCard agentId="agent-1" instructions={null} />));

        expect(screen.getByLabelText('Instructions')).toHaveValue('');
    });
});
