import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import TranscriptDialog from '../TranscriptDialog';

const hoisted = vi.hoisted(() => ({
    state: {
        error: undefined as unknown,
        groupedTurns: [] as unknown[],
        isLoading: false,
        transcriptData: undefined as unknown,
    },
}));

vi.mock('@/pages/platform/cluster-element-editor/ai-agent-evals/components/runs/hooks/useTranscriptDialog', () => ({
    default: () => hoisted.state,
}));

const onClose = vi.fn();

const defaultProps = {onClose, resultId: 'r1', scenarioName: 'Happy path'};

beforeEach(() => {
    windowResizeObserver();
    hoisted.state.error = undefined;
    hoisted.state.groupedTurns = [];
    hoisted.state.isLoading = false;
    hoisted.state.transcriptData = undefined;
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('TranscriptDialog', () => {
    describe('rendering', () => {
        it('should title the dialog after the scenario', () => {
            render(<TranscriptDialog {...defaultProps} />);

            expect(screen.getByRole('heading', {name: 'Conversation Transcript - Happy path'})).toBeInTheDocument();
        });

        it('should describe what the dialog shows', () => {
            render(<TranscriptDialog {...defaultProps} />);

            expect(screen.getByText('Conversation transcript for this scenario result.')).toBeInTheDocument();
        });

        it('should render the close control', () => {
            render(<TranscriptDialog {...defaultProps} />);

            expect(screen.getByRole('button', {name: 'Close'})).toBeInTheDocument();
        });
    });

    describe('interactions', () => {
        it('should call onClose when the close button is clicked', async () => {
            const user = userEvent.setup();

            render(<TranscriptDialog {...defaultProps} />);

            await user.click(screen.getByRole('button', {name: 'Close'}));

            expect(onClose).toHaveBeenCalledTimes(1);
        });
    });
});
