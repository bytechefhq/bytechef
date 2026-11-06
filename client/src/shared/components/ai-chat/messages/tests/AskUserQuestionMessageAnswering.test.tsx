import {aiChatAskedQuestionsStore} from '@/shared/components/ai-chat/stores/useAiChatAskedQuestionsStore';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AskUserQuestionMessage, {type AskUserQuestionDataI} from '../AskUserQuestionMessage';

const appendedMessages: Array<{content: Array<{text: string; type: string}>; role: string}> = [];

vi.mock('@assistant-ui/react', async () => {
    const actual = await vi.importActual<typeof import('@assistant-ui/react')>('@assistant-ui/react');

    return {
        ...actual,
        useAui: () => ({
            thread: {
                append: (message: {content: Array<{text: string; type: string}>; role: string}) => {
                    appendedMessages.push(message);
                },
            },
        }),
    };
});

type QuestionType = AskUserQuestionDataI['questions'][number];

const singleQuestion = (question: Partial<QuestionType> = {}): QuestionType => ({
    header: 'Messaging',
    multiSelect: false,
    options: [
        {description: 'Slack messaging', label: 'slack'},
        {description: 'Discord messaging', label: 'discord'},
    ],
    question: 'Which messaging component?',
    ...question,
});

const renderMessage = (questions: QuestionType[]) =>
    render(
        <AskUserQuestionMessage
            data={{awaitingAnswer: true, kind: 'ask-user-question', questions}}
            name="ask-user-question"
            status={{type: 'complete'}}
            type="data"
        />
    );

const appendedTexts = () => appendedMessages.map((message) => message.content[0].text);

beforeEach(() => {
    appendedMessages.length = 0;

    aiChatAskedQuestionsStore.getState().reset();
});

describe('AskUserQuestionMessage answering', () => {
    describe('a single question', () => {
        it('shows its header without a step counter', () => {
            renderMessage([singleQuestion()]);

            expect(screen.getByText('Messaging')).toBeInTheDocument();
            expect(screen.queryByText(/Question 1 of/)).not.toBeInTheDocument();
        });

        it('shows each option description as the option title', () => {
            renderMessage([singleQuestion()]);

            expect(screen.getByRole('button', {name: 'slack'})).toHaveAttribute('title', 'Slack messaging');
        });

        it('collapses into the picked summary once answered', async () => {
            renderMessage([singleQuestion()]);

            await userEvent.click(screen.getByRole('button', {name: 'discord'}));

            expect(appendedMessages).toEqual([{content: [{text: 'User picked: discord', type: 'text'}], role: 'user'}]);
            expect(screen.getByText('discord').parentElement).toHaveTextContent('Picked: discord');
            expect(screen.queryByRole('button', {name: 'slack'})).not.toBeInTheDocument();
        });
    });

    describe('a free-form answer', () => {
        it('submits the trimmed text typed after choosing Other', async () => {
            renderMessage([singleQuestion()]);

            await userEvent.click(screen.getByRole('button', {name: 'Other…'}));

            const answerInput = screen.getByPlaceholderText('Type your answer…');

            expect(answerInput).toHaveFocus();

            await userEvent.type(answerInput, '  telegram  {Enter}');

            expect(appendedTexts()).toEqual(['User picked: telegram']);
        });

        it('refuses to submit blank text', async () => {
            renderMessage([singleQuestion()]);

            await userEvent.click(screen.getByRole('button', {name: 'Other…'}));

            const submitButton = screen.getByRole('button', {name: 'Submit all'});

            expect(submitButton).toBeDisabled();

            await userEvent.type(screen.getByPlaceholderText('Type your answer…'), '   {Enter}');

            expect(submitButton).toBeDisabled();
            expect(appendedMessages).toHaveLength(0);
        });

        it('submits typed text with the submit button', async () => {
            renderMessage([singleQuestion()]);

            await userEvent.click(screen.getByRole('button', {name: 'Other…'}));
            await userEvent.type(screen.getByPlaceholderText('Type your answer…'), 'teams');
            await userEvent.click(screen.getByRole('button', {name: 'Submit all'}));

            expect(appendedTexts()).toEqual(['User picked: teams']);
        });

        it('goes back to the options and forgets the text on Escape', async () => {
            renderMessage([singleQuestion()]);

            await userEvent.click(screen.getByRole('button', {name: 'Other…'}));
            await userEvent.type(screen.getByPlaceholderText('Type your answer…'), 'draft{Escape}');

            expect(screen.queryByPlaceholderText('Type your answer…')).not.toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'slack'})).toBeInTheDocument();

            await userEvent.click(screen.getByRole('button', {name: 'Other…'}));

            expect(screen.getByPlaceholderText('Type your answer…')).toHaveValue('');
        });

        it('goes back to the options from the cancel button', async () => {
            renderMessage([singleQuestion()]);

            await userEvent.click(screen.getByRole('button', {name: 'Other…'}));
            await userEvent.type(screen.getByPlaceholderText('Type your answer…'), 'draft');
            await userEvent.click(screen.getByTitle('Cancel and go back to the option buttons'));

            expect(screen.getByRole('button', {name: 'slack'})).toBeInTheDocument();
            expect(appendedMessages).toHaveLength(0);
        });

        it('opens the free-form input from the Other entry of a long option list', async () => {
            renderMessage([
                singleQuestion({
                    options: Array.from({length: 9}, (_, index) => ({label: `channel-${index + 1}`})),
                }),
            ]);

            await userEvent.click(screen.getByRole('combobox'));
            await userEvent.click(screen.getByRole('option', {name: 'Other…'}));
            await userEvent.type(screen.getByPlaceholderText('Type your answer…'), 'channel-42{Enter}');

            expect(appendedTexts()).toEqual(['User picked: channel-42']);
        });
    });

    describe('a multi-select question', () => {
        const capabilitiesQuestion = (question: Partial<QuestionType> = {}) =>
            singleQuestion({
                header: 'Capabilities',
                multiSelect: true,
                options: [{description: 'Read records', label: 'read'}, {label: 'write'}, {label: 'delete'}],
                question: 'Which capabilities?',
                ...question,
            });

        it('enables submitting only while something is checked', async () => {
            renderMessage([capabilitiesQuestion()]);

            const submitButton = screen.getByRole('button', {name: 'Submit all'});

            expect(submitButton).toBeDisabled();

            await userEvent.click(screen.getByRole('checkbox', {name: /read/}));

            expect(submitButton).toBeEnabled();

            await userEvent.click(screen.getByRole('checkbox', {name: /read/}));

            expect(submitButton).toBeDisabled();
        });

        it('shows each option description under its label', () => {
            renderMessage([capabilitiesQuestion()]);

            expect(screen.getByRole('checkbox', {name: /Read records/})).toBeInTheDocument();
        });

        it('submits the selections picked from a long option list', async () => {
            renderMessage([
                capabilitiesQuestion({
                    options: Array.from({length: 9}, (_, index) => ({label: `scope-${index + 1}`})),
                }),
            ]);

            await userEvent.click(screen.getByRole('button', {name: /Select/}));
            await userEvent.click(screen.getByRole('option', {name: 'scope-2'}));
            await userEvent.click(screen.getByRole('option', {name: 'scope-5'}));
            await userEvent.keyboard('{Escape}');
            await userEvent.click(screen.getByRole('button', {name: 'Submit all'}));

            expect(appendedTexts()).toEqual(['User picked: scope-2, scope-5']);
        });
    });

    describe('a wizard of several questions', () => {
        it('submits every answer as one labelled summary and keeps it on the card', async () => {
            renderMessage([
                singleQuestion({
                    header: 'Channel',
                    options: [{label: '#general'}, {label: '#random'}],
                    question: 'Where?',
                }),
                singleQuestion({
                    header: 'Scope',
                    multiSelect: true,
                    options: [{label: 'read'}, {label: 'write'}],
                    question: 'Which scopes?',
                }),
            ]);

            expect(screen.getByText('Question 1 of 2')).toBeInTheDocument();
            expect(screen.getByText('Channel')).toBeInTheDocument();

            await userEvent.click(screen.getByRole('button', {name: '#random'}));

            expect(screen.getByText('Question 2 of 2')).toBeInTheDocument();

            await userEvent.click(screen.getByRole('checkbox', {name: 'write'}));
            await userEvent.click(screen.getByRole('checkbox', {name: 'read'}));
            await userEvent.click(screen.getByRole('button', {name: 'Submit all'}));

            const expectedSummary = '- Where? → #random\n- Which scopes? → write, read';

            expect(appendedTexts()).toEqual([`User picked:\n${expectedSummary}`]);
            expect(screen.getByText(/Where\? → #random/)).toHaveTextContent(
                '- Where? → #random - Which scopes? → write, read'
            );
        });

        it('labels the free-form submit button Next before the last question', async () => {
            renderMessage([singleQuestion(), singleQuestion({question: 'And then?'})]);

            await userEvent.click(screen.getByRole('button', {name: 'Other…'}));

            expect(screen.getByRole('button', {name: 'Next'})).toBeInTheDocument();
        });

        it('offers the options of the next question after a free-form answer', async () => {
            renderMessage([
                singleQuestion({question: 'Which messaging component?'}),
                singleQuestion({options: [{label: 'UTC'}, {label: 'CET'}], question: 'Which timezone?'}),
            ]);

            await userEvent.click(screen.getByRole('button', {name: 'Other…'}));
            await userEvent.type(screen.getByPlaceholderText('Type your answer…'), 'telegram{Enter}');

            expect(screen.getByText('Which timezone?')).toBeInTheDocument();
            expect(screen.queryByPlaceholderText('Type your answer…')).not.toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'UTC'})).toBeInTheDocument();
        });

        it('starts the next multi-select question with nothing checked', async () => {
            renderMessage([
                singleQuestion({multiSelect: true, options: [{label: 'read'}, {label: 'write'}], question: 'Scopes?'}),
                singleQuestion({multiSelect: true, options: [{label: 'daily'}, {label: 'weekly'}], question: 'When?'}),
            ]);

            await userEvent.click(screen.getByRole('checkbox', {name: 'read'}));
            await userEvent.click(screen.getByRole('button', {name: 'Next'}));

            expect(screen.getByText('When?')).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Submit all'})).toBeDisabled();

            await userEvent.click(screen.getByRole('checkbox', {name: 'weekly'}));
            await userEvent.click(screen.getByRole('button', {name: 'Submit all'}));

            expect(appendedTexts()).toEqual(['User picked:\n- Scopes? → read\n- When? → weekly']);
        });

        it('restores the earlier selections when going back to a multi-select question', async () => {
            renderMessage([
                singleQuestion({multiSelect: true, options: [{label: 'read'}, {label: 'write'}], question: 'Scopes?'}),
                singleQuestion({multiSelect: true, options: [{label: 'daily'}, {label: 'weekly'}], question: 'When?'}),
            ]);

            await userEvent.click(screen.getByRole('checkbox', {name: 'write'}));
            await userEvent.click(screen.getByRole('button', {name: 'Next'}));
            await userEvent.click(screen.getByRole('checkbox', {name: 'daily'}));
            await userEvent.click(screen.getByRole('button', {name: 'Previous'}));

            expect(screen.getByText('Scopes?')).toBeInTheDocument();
            expect(screen.getByRole('checkbox', {name: 'write'})).toBeChecked();
            expect(screen.getByRole('checkbox', {name: 'read'})).not.toBeChecked();
        });

        it('replaces an earlier answer that is changed after going back', async () => {
            renderMessage([
                singleQuestion({options: [{label: 'slack'}, {label: 'discord'}], question: 'Which app?'}),
                singleQuestion({options: [{label: 'UTC'}, {label: 'CET'}], question: 'Which timezone?'}),
            ]);

            await userEvent.click(screen.getByRole('button', {name: 'slack'}));
            await userEvent.click(screen.getByRole('button', {name: 'Previous'}));
            await userEvent.click(screen.getByRole('button', {name: 'discord'}));
            await userEvent.click(screen.getByRole('button', {name: 'CET'}));

            expect(appendedTexts()).toEqual(['User picked:\n- Which app? → discord\n- Which timezone? → CET']);
        });
    });
});
