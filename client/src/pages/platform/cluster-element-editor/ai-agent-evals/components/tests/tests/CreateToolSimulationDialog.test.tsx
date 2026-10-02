import {TooltipProvider} from '@/components/ui/tooltip';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import CreateToolSimulationDialog from '../CreateToolSimulationDialog';

// The field hints are Tooltips, which need the provider the app mounts at its root.
const TooltipHarness = ({children}: {children: ReactNode}) => <TooltipProvider>{children}</TooltipProvider>;

const onClose = vi.fn();
const onCreate = vi.fn().mockResolvedValue(undefined);
const onUpdate = vi.fn().mockResolvedValue(undefined);

const editData = {
    id: 'sim-1',
    responsePrompt: 'Return a fixed payload',
    simulationModel: undefined,
    toolName: 'search',
};

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('CreateToolSimulationDialog', () => {
    describe('create mode', () => {
        it('should render the add title', () => {
            render(
                <TooltipHarness>
                    <CreateToolSimulationDialog onClose={onClose} onCreate={onCreate} />
                </TooltipHarness>
            );

            expect(screen.getByRole('heading', {name: 'Add Tool Simulation'})).toBeInTheDocument();
        });
    });

    describe('edit mode', () => {
        it('should render the edit title', () => {
            render(
                <TooltipHarness>
                    <CreateToolSimulationDialog
                        editData={editData}
                        onClose={onClose}
                        onCreate={onCreate}
                        onUpdate={onUpdate}
                    />
                </TooltipHarness>
            );

            expect(screen.getByRole('heading', {name: 'Edit Tool Simulation'})).toBeInTheDocument();
        });
    });

    describe('dialog chrome', () => {
        it('should render the close and cancel controls', () => {
            render(
                <TooltipHarness>
                    <CreateToolSimulationDialog onClose={onClose} onCreate={onCreate} />
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
                    <CreateToolSimulationDialog onClose={onClose} onCreate={onCreate} />
                </TooltipHarness>
            );

            await user.click(screen.getByRole('button', {name: 'Cancel'}));

            expect(onClose).toHaveBeenCalledTimes(1);
        });

        it('should call onClose when the close button is clicked', async () => {
            const user = userEvent.setup();

            render(
                <TooltipHarness>
                    <CreateToolSimulationDialog onClose={onClose} onCreate={onCreate} />
                </TooltipHarness>
            );

            await user.click(screen.getByRole('button', {name: 'Close'}));

            expect(onClose).toHaveBeenCalledTimes(1);
        });
    });
});
