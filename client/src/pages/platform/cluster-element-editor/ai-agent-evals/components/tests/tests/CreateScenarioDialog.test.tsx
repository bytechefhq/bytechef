import {TooltipProvider} from '@/components/ui/tooltip';
import {AiAgentScenarioType} from '@/shared/middleware/graphql';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import CreateScenarioDialog from '../CreateScenarioDialog';

// The field hints are Tooltips, which need the provider the app mounts at its root.
const TooltipHarness = ({children}: {children: ReactNode}) => <TooltipProvider>{children}</TooltipProvider>;

const onClose = vi.fn();
const onCreate = vi.fn();
const onUpdate = vi.fn();

const editData = {
    expectedOutput: null,
    id: 's1',
    maxTurns: 10,
    name: 'Existing scenario',
    numberOfRuns: 1,
    personaPrompt: null,
    type: AiAgentScenarioType.SingleTurn,
    userMessage: 'Hello',
};

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('CreateScenarioDialog', () => {
    describe('create mode', () => {
        it('should render the create title', () => {
            render(
                <TooltipHarness>
                    <CreateScenarioDialog agentEvalTestId="t1" onClose={onClose} onCreate={onCreate} />
                </TooltipHarness>
            );

            expect(screen.getByRole('heading', {name: 'Create Scenario'})).toBeInTheDocument();
        });

        it('should label the primary action Create', () => {
            render(
                <TooltipHarness>
                    <CreateScenarioDialog agentEvalTestId="t1" onClose={onClose} onCreate={onCreate} />
                </TooltipHarness>
            );

            expect(screen.getByRole('button', {name: 'Create'})).toBeInTheDocument();
        });

        it('should disable the primary action until a name is entered', () => {
            render(
                <TooltipHarness>
                    <CreateScenarioDialog agentEvalTestId="t1" onClose={onClose} onCreate={onCreate} />
                </TooltipHarness>
            );

            expect(screen.getByRole('button', {name: 'Create'})).toBeDisabled();
        });
    });

    describe('edit mode', () => {
        it('should render the edit title', () => {
            render(
                <TooltipHarness>
                    <CreateScenarioDialog
                        agentEvalTestId="t1"
                        editData={editData}
                        onClose={onClose}
                        onCreate={onCreate}
                        onUpdate={onUpdate}
                    />
                </TooltipHarness>
            );

            expect(screen.getByRole('heading', {name: 'Edit Scenario'})).toBeInTheDocument();
        });

        it('should label the primary action Save and enable it', () => {
            render(
                <TooltipHarness>
                    <CreateScenarioDialog
                        agentEvalTestId="t1"
                        editData={editData}
                        onClose={onClose}
                        onCreate={onCreate}
                        onUpdate={onUpdate}
                    />
                </TooltipHarness>
            );

            expect(screen.getByRole('button', {name: 'Save'})).toBeEnabled();
        });
    });

    describe('dialog chrome', () => {
        it('should render the close and cancel controls', () => {
            render(
                <TooltipHarness>
                    <CreateScenarioDialog agentEvalTestId="t1" onClose={onClose} onCreate={onCreate} />
                </TooltipHarness>
            );

            expect(screen.getByRole('button', {name: 'Close'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
        });
    });

    describe('interactions', () => {
        it('should call onClose when cancel is clicked', async () => {
            const user = userEvent.setup();

            render(
                <TooltipHarness>
                    <CreateScenarioDialog agentEvalTestId="t1" onClose={onClose} onCreate={onCreate} />
                </TooltipHarness>
            );

            await user.click(screen.getByRole('button', {name: 'Cancel'}));

            expect(onClose).toHaveBeenCalledTimes(1);
        });

        it('should call onClose when the close button is clicked', async () => {
            const user = userEvent.setup();

            render(
                <TooltipHarness>
                    <CreateScenarioDialog agentEvalTestId="t1" onClose={onClose} onCreate={onCreate} />
                </TooltipHarness>
            );

            await user.click(screen.getByRole('button', {name: 'Close'}));

            expect(onClose).toHaveBeenCalledTimes(1);
        });
    });
});
