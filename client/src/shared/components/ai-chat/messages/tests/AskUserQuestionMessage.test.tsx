import {aiChatAskedQuestionsStore} from '@/shared/components/ai-chat/stores/useAiChatAskedQuestionsStore';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const threadMessages: Array<unknown> = [];
const appendCalls: Array<unknown> = [];

vi.mock('@assistant-ui/react', async () => {
    const actual = await vi.importActual<typeof import('@assistant-ui/react')>('@assistant-ui/react');

    return {
        ...actual,
        useThreadRuntime: vi.fn(() => ({
            append: (message: unknown) => {
                appendCalls.push(message);
                threadMessages.push(message);
            },
            getState: () => ({messages: threadMessages}),
            subscribe: () => () => {},
        })),
    };
});

const SINGLE_SELECT_DATA = {
    awaitingAnswer: true,
    kind: 'ask-user-question' as const,
    questions: [
        {
            header: 'Pick',
            multiSelect: false,
            options: [
                {description: 'Slack messaging', label: 'slack'},
                {description: 'Discord messaging', label: 'discord'},
            ],
            question: 'Which messaging component?',
        },
    ],
};

const MULTI_SELECT_DATA = {
    awaitingAnswer: true,
    kind: 'ask-user-question' as const,
    questions: [
        {
            header: 'Capabilities',
            multiSelect: true,
            options: [
                {description: 'Read', label: 'read'},
                {description: 'Write', label: 'write'},
                {description: 'Delete', label: 'delete'},
            ],
            question: 'Which capabilities?',
        },
    ],
};

const WIZARD_DATA = {
    awaitingAnswer: true,
    kind: 'ask-user-question' as const,
    questions: [
        {
            header: 'CHANNEL',
            multiSelect: false,
            options: [{label: '#general'}, {label: '#announcements'}],
            question: 'Which Slack channel?',
        },
        {
            header: 'MESSAGE',
            multiSelect: false,
            options: [{label: 'Good morning!'}, {label: 'Daily standup'}],
            question: 'What message?',
        },
        {
            header: 'TIMEZONE',
            multiSelect: false,
            options: [{label: 'UTC'}, {label: 'US/Eastern'}],
            question: 'Which timezone?',
        },
    ],
};

const LLM_PROVIDED_OTHER_DATA = {
    awaitingAnswer: true,
    kind: 'ask-user-question' as const,
    questions: [
        {
            multiSelect: false,
            // LLM emits its own "Other" option — our injected "Other…" affordance must NOT also render.
            options: [{label: '#general'}, {label: '#team'}, {label: 'Other'}],
            question: 'Which channel?',
        },
    ],
};

const LARGE_SINGLE_SELECT_DATA = {
    awaitingAnswer: true,
    kind: 'ask-user-question' as const,
    questions: [
        {
            header: 'Channel',
            multiSelect: false,
            options: [
                {label: 'general'},
                {label: 'random'},
                {label: 'testing'},
                {label: 'sales-team'},
                {label: 'tech-team'},
                {label: 'testing-again'},
                {label: 'intercapital-test'},
                {label: 'pevex-test'},
                {label: 'pto-test'},
            ],
            question: 'Which Slack channel?',
        },
    ],
};

const LARGE_MULTI_SELECT_DATA = {
    awaitingAnswer: true,
    kind: 'ask-user-question' as const,
    questions: [
        {
            header: 'Channels',
            multiSelect: true,
            options: [
                {label: 'general'},
                {label: 'random'},
                {label: 'testing'},
                {label: 'sales-team'},
                {label: 'tech-team'},
                {label: 'testing-again'},
                {label: 'intercapital-test'},
                {label: 'pevex-test'},
                {label: 'pto-test'},
            ],
            question: 'Which channels?',
        },
    ],
};

type AnyDataType =
    | typeof SINGLE_SELECT_DATA
    | typeof MULTI_SELECT_DATA
    | typeof WIZARD_DATA
    | typeof LLM_PROVIDED_OTHER_DATA
    | typeof LARGE_MULTI_SELECT_DATA
    | typeof LARGE_SINGLE_SELECT_DATA;

const renderMessage = async (data: AnyDataType) => {
    const {default: AskUserQuestionMessage} = await import('../AskUserQuestionMessage');

    return render(
        <AskUserQuestionMessage data={data} name="ask-user-question" status={{type: 'complete'}} type="data" />
    );
};

describe('AskUserQuestionMessage', () => {
    beforeEach(() => {
        threadMessages.length = 0;
        appendCalls.length = 0;
        aiChatAskedQuestionsStore.getState().reset();
    });

    it('renders a single-step card with option buttons + Other… affordance', async () => {
        await renderMessage(SINGLE_SELECT_DATA);

        expect(screen.getByText(/which messaging component/i)).toBeInTheDocument();
        // Single-question case has no step counter.
        expect(screen.queryByText(/question \d+ of/i)).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'slack'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'discord'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: /other/i})).toBeInTheDocument();
    });

    it('appends "User picked: <label>" when single-question single-select option is clicked', async () => {
        await renderMessage(SINGLE_SELECT_DATA);

        await userEvent.click(screen.getByRole('button', {name: 'slack'}));

        expect(appendCalls).toHaveLength(1);
        expect(appendCalls[0]).toMatchObject({
            content: [{text: 'User picked: slack', type: 'text'}],
            role: 'user',
        });
    });

    it('renders a step counter and walks through wizard steps without appending until the last one', async () => {
        await renderMessage(WIZARD_DATA);

        // Step 1.
        expect(screen.getByText(/question 1 of 3/i)).toBeInTheDocument();
        expect(screen.getByText(/which slack channel/i)).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: '#general'}));

        // Advances to step 2 without appending.
        expect(screen.getByText(/question 2 of 3/i)).toBeInTheDocument();
        expect(screen.getByText(/what message/i)).toBeInTheDocument();
        expect(appendCalls).toHaveLength(0);

        await userEvent.click(screen.getByRole('button', {name: 'Good morning!'}));

        // Step 3.
        expect(screen.getByText(/question 3 of 3/i)).toBeInTheDocument();
        expect(appendCalls).toHaveLength(0);

        await userEvent.click(screen.getByRole('button', {name: 'UTC'}));

        // Final answer triggers a single combined system message with all labeled Q → A pairs.
        expect(appendCalls).toHaveLength(1);
        expect(appendCalls[0]).toMatchObject({
            content: [
                {
                    text:
                        'User picked:\n' +
                        '- Which Slack channel? → #general\n' +
                        '- What message? → Good morning!\n' +
                        '- Which timezone? → UTC',
                    type: 'text',
                },
            ],
            role: 'user',
        });
    });

    it('shows a Previous link from step 2 onward and lets the user navigate back', async () => {
        await renderMessage(WIZARD_DATA);

        // Step 1: no Previous yet.
        expect(screen.queryByRole('button', {name: /previous/i})).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: '#general'}));

        // Step 2 — Previous appears.
        expect(screen.getByRole('button', {name: /previous/i})).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: /previous/i}));

        // Back on step 1.
        expect(screen.getByText(/question 1 of 3/i)).toBeInTheDocument();
        expect(screen.getByText(/which slack channel/i)).toBeInTheDocument();
    });

    it('skips the injected "Other…" affordance when an option labeled "Other" is already supplied', async () => {
        // Regression guard against a screenshot bug where both an LLM-provided "Other" option AND our injected
        // "Other…" appeared side by side. The LLM's option takes precedence; clicking it opens the same inline
        // text input the injected affordance would have.
        await renderMessage(LLM_PROVIDED_OTHER_DATA);

        const allOtherButtons = screen
            .getAllByRole('button')
            .filter((button) => /other/i.test(button.textContent ?? ''));

        expect(allOtherButtons).toHaveLength(1);
        // And the one that's there is the LLM's literal "Other" (no ellipsis added by us).
        expect(allOtherButtons[0]).toHaveTextContent(/^Other$/);
    });

    it('treats clicking an LLM-supplied "Other" option as a free-form trigger, not a literal submit', async () => {
        await renderMessage(LLM_PROVIDED_OTHER_DATA);

        await userEvent.click(screen.getByRole('button', {name: /^other$/i}));

        // Opens the inline input instead of appending "User picked: Other".
        expect(screen.getByPlaceholderText(/type your answer/i)).toBeInTheDocument();
        expect(appendCalls).toHaveLength(0);
    });

    it('multi-select last step shows "Submit all" and joins selections with ", "', async () => {
        await renderMessage(MULTI_SELECT_DATA);

        expect(screen.getByRole('button', {name: /submit all/i})).toBeDisabled();

        await userEvent.click(screen.getByRole('checkbox', {name: /read/i}));
        await userEvent.click(screen.getByRole('checkbox', {name: /write/i}));
        await userEvent.click(screen.getByRole('button', {name: /submit all/i}));

        expect(appendCalls[0]).toMatchObject({
            content: [{text: 'User picked: read, write', type: 'text'}],
            role: 'user',
        });
    });

    it('after answering the wizard, re-mounting shows the persisted "Picked" summary instead of prompting again', async () => {
        await renderMessage(WIZARD_DATA);

        await userEvent.click(screen.getByRole('button', {name: '#general'}));
        await userEvent.click(screen.getByRole('button', {name: 'Good morning!'}));
        await userEvent.click(screen.getByRole('button', {name: 'UTC'}));

        // Re-mount: the wizard should NOT walk the user through the questions again.
        const {default: AskUserQuestionMessage} = await import('../AskUserQuestionMessage');

        render(
            <AskUserQuestionMessage
                data={WIZARD_DATA}
                name="ask-user-question"
                status={{type: 'complete'}}
                type="data"
            />
        );

        expect(screen.queryByText(/question 1 of 3/i)).not.toBeInTheDocument();
        // The persisted summary echoes the answers back.
        expect(screen.getAllByText(/picked/i).length).toBeGreaterThan(0);
        expect(screen.getAllByText(/#general/i).length).toBeGreaterThan(0);
    });

    it('renders a searchable combobox (not stacked buttons) when single-select options exceed the threshold', async () => {
        await renderMessage(LARGE_SINGLE_SELECT_DATA);

        expect(screen.queryByRole('button', {name: 'pto-test'})).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('combobox'));

        await userEvent.click(screen.getByRole('option', {name: 'tech-team'}));

        expect(appendCalls).toHaveLength(1);
        expect(JSON.stringify(appendCalls[0])).toContain('User picked: tech-team');
    });

    it('renders a MultiSelect (not stacked checkboxes) when multi-select options exceed the threshold', async () => {
        await renderMessage(LARGE_MULTI_SELECT_DATA);

        expect(screen.queryByRole('checkbox', {name: 'pto-test'})).not.toBeInTheDocument();
        expect(screen.getByText(/select/i)).toBeInTheDocument();
    });

    it('renders nothing when the questions array is empty', async () => {
        const {container} = await renderMessage({...SINGLE_SELECT_DATA, questions: []});

        expect(container.firstChild).toBeNull();
    });
});
