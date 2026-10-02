import {type AiAgentEvalTestsQuery} from '@/shared/middleware/graphql';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import RunTestDialog from '../RunTestDialog';

// RunTestDialog types its prop from this query row, but does not export the alias.
type EvalTestListItemType = AiAgentEvalTestsQuery['aiAgentEvalTests'][number];

const hoisted = vi.hoisted(() => ({
    handleRunTest: vi.fn(),
    state: {
        isPending: false,
        selectedScenarioIds: new Set<string>(),
    },
}));

vi.mock('@/pages/platform/cluster-element-editor/ai-agent-evals/components/tests/hooks/useRunTestDialog', () => ({
    default: () => ({
        handleRunTest: hoisted.handleRunTest,
        judges: [],
        scenarios: [],
        selectedJudgeIds: new Set<string>(),
        selectedScenarioIds: hoisted.state.selectedScenarioIds,
        startRunMutation: {isPending: hoisted.state.isPending},
        toggleJudgeId: vi.fn(),
        toggleScenarioId: vi.fn(),
    }),
}));

const onClose = vi.fn();

const defaultProps = {
    onClose,
    test: {id: 't1', name: 'Smoke suite'} as EvalTestListItemType,
    workflowId: 'w1',
    workflowNodeName: 'agent_1',
};

beforeEach(() => {
    windowResizeObserver();
    hoisted.state.isPending = false;
    hoisted.state.selectedScenarioIds = new Set<string>();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('RunTestDialog', () => {
    describe('rendering', () => {
        it('should title the dialog after the test', () => {
            render(<RunTestDialog {...defaultProps} />);

            expect(screen.getByRole('heading', {name: 'Run Test — Smoke suite'})).toBeInTheDocument();
        });

        it('should render the close, cancel and run controls', () => {
            render(<RunTestDialog {...defaultProps} />);

            expect(screen.getByRole('button', {name: 'Close'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Run Test'})).toBeInTheDocument();
        });

        it('should disable running until a scenario is selected', () => {
            render(<RunTestDialog {...defaultProps} />);

            expect(screen.getByRole('button', {name: 'Run Test'})).toBeDisabled();
        });

        it('should enable running once a scenario is selected', () => {
            hoisted.state.selectedScenarioIds = new Set(['s1']);

            render(<RunTestDialog {...defaultProps} />);

            expect(screen.getByRole('button', {name: 'Run Test'})).toBeEnabled();
        });

        it('should show the starting label while the run is pending', () => {
            hoisted.state.isPending = true;
            hoisted.state.selectedScenarioIds = new Set(['s1']);

            render(<RunTestDialog {...defaultProps} />);

            expect(screen.getByRole('button', {name: 'Starting...'})).toBeDisabled();
        });
    });

    describe('interactions', () => {
        it('should start the run when the action is clicked', async () => {
            const user = userEvent.setup();

            hoisted.state.selectedScenarioIds = new Set(['s1']);

            render(<RunTestDialog {...defaultProps} />);

            await user.click(screen.getByRole('button', {name: 'Run Test'}));

            expect(hoisted.handleRunTest).toHaveBeenCalledTimes(1);
        });

        it('should call onClose when cancel is clicked', async () => {
            const user = userEvent.setup();

            render(<RunTestDialog {...defaultProps} />);

            await user.click(screen.getByRole('button', {name: 'Cancel'}));

            expect(onClose).toHaveBeenCalledTimes(1);
        });

        it('should call onClose when the close button is clicked', async () => {
            const user = userEvent.setup();

            render(<RunTestDialog {...defaultProps} />);

            await user.click(screen.getByRole('button', {name: 'Close'}));

            expect(onClose).toHaveBeenCalledTimes(1);
        });
    });
});
