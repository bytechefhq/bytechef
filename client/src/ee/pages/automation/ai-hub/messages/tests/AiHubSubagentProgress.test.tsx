import AiHubSubagentProgress from '@/ee/pages/automation/ai-hub/messages/AiHubSubagentProgress';
import {
    type SubagentProgressEntryI,
    type ToolCallEntryI,
} from '@/shared/components/ai-chat/stores/useAiChatToolCallStore';
import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

type ProgressEntryType = Pick<ToolCallEntryI, 'progress' | 'progressLabel' | 'progressOutcome' | 'status'>;

const steps = (...texts: string[]): SubagentProgressEntryI[] =>
    texts.map((text, index) => ({text, timestamp: index + 1}));

const runningEntry = (overrides: Partial<ProgressEntryType> = {}): ProgressEntryType => ({
    progress: [],
    status: 'running',
    ...overrides,
});

describe('AiHubSubagentProgress', () => {
    it('shows the fallback text until a step arrives', () => {
        render(<AiHubSubagentProgress entry={runningEntry()} fallbackText="Building workflow..." />);

        expect(screen.getByRole('status')).toHaveTextContent('Building workflow...');
    });

    it('keeps the fallback prominent and the label beneath it before the first step', () => {
        render(
            <AiHubSubagentProgress
                entry={runningEntry({progressLabel: 'Build a Gmail to Slack workflow'})}
                fallbackText="Building workflow..."
            />
        );

        expect(screen.getByText('Building workflow...')).toHaveClass('font-medium');
        expect(screen.getByText('Build a Gmail to Slack workflow')).toBeInTheDocument();
    });

    it('shows the latest step prominently and keeps earlier steps collapsed', () => {
        render(
            <AiHubSubagentProgress
                entry={runningEntry({
                    progress: steps('Reading the build instructions', 'Searching components', 'Saving the workflow'),
                    progressLabel: 'Build a Gmail to Slack workflow',
                })}
                fallbackText="Building workflow..."
            />
        );

        expect(screen.getByText('Saving the workflow')).toHaveClass('font-medium');
        expect(screen.queryByText('Building workflow...')).toBeNull();
        expect(screen.queryByText('Searching components')).toBeNull();

        const toggle = screen.getByRole('button', {expanded: false, name: /2 earlier steps/});

        fireEvent.click(toggle);

        expect(screen.getByRole('button', {expanded: true, name: /2 earlier steps/})).toBeInTheDocument();
        expect(screen.getByText('Reading the build instructions')).toBeInTheDocument();
        expect(screen.getByText('Searching components')).toBeInTheDocument();
    });

    it('uses the singular for one earlier step', () => {
        render(<AiHubSubagentProgress entry={runningEntry({progress: steps('Searching components', 'Saving')})} />);

        expect(screen.getByRole('button', {name: /^1 earlier step$/})).toBeInTheDocument();
    });

    it('clears once the delegate is done', () => {
        const {container} = render(
            <AiHubSubagentProgress
                entry={runningEntry({progress: steps('Saving the workflow'), progressOutcome: 'done'})}
                fallbackText="Building workflow..."
            />
        );

        expect(container).toBeEmptyDOMElement();
    });

    it('clears once the call is no longer running', () => {
        const {container} = render(
            <AiHubSubagentProgress
                entry={{progress: steps('Saving the workflow'), status: 'success'}}
                fallbackText="Building workflow..."
            />
        );

        expect(container).toBeEmptyDOMElement();
    });

    it('shows the failed state with the label and the step it failed at', () => {
        render(
            <AiHubSubagentProgress
                entry={{
                    progress: steps('Searching components', 'Saving the workflow'),
                    progressLabel: 'Build a Gmail to Slack workflow',
                    progressOutcome: 'failed',
                    status: 'success',
                }}
                fallbackText="Building workflow..."
            />
        );

        const status = screen.getByRole('status');

        expect(status).toHaveTextContent('Failed');
        expect(status).toHaveTextContent('Build a Gmail to Slack workflow');
        expect(status).toHaveTextContent('at Saving the workflow');
        expect(status.querySelector('.animate-spin')).toBeNull();
    });
});
