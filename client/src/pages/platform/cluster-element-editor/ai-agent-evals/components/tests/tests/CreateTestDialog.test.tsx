import {TooltipProvider} from '@/components/ui/tooltip';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import CreateTestDialog from '../CreateTestDialog';

// The field hints are Tooltips, which need the provider the app mounts at its root.
const TooltipHarness = ({children}: {children: ReactNode}) => <TooltipProvider>{children}</TooltipProvider>;

const onClose = vi.fn();
const onCreate = vi.fn();

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('CreateTestDialog', () => {
    describe('rendering', () => {
        it('should render the title', () => {
            render(
                <TooltipHarness>
                    <CreateTestDialog onClose={onClose} onCreate={onCreate} />
                </TooltipHarness>
            );

            expect(screen.getByRole('heading', {name: 'Create Test'})).toBeInTheDocument();
        });

        it('should render the close, cancel and create controls', () => {
            render(
                <TooltipHarness>
                    <CreateTestDialog onClose={onClose} onCreate={onCreate} />
                </TooltipHarness>
            );

            expect(screen.getByRole('button', {name: 'Close'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Create'})).toBeInTheDocument();
        });

        it('should disable creating until a name is entered', () => {
            render(
                <TooltipHarness>
                    <CreateTestDialog onClose={onClose} onCreate={onCreate} />
                </TooltipHarness>
            );

            expect(screen.getByRole('button', {name: 'Create'})).toBeDisabled();
        });
    });

    describe('interactions', () => {
        it('should enable creating once a name is entered', async () => {
            const user = userEvent.setup();

            render(
                <TooltipHarness>
                    <CreateTestDialog onClose={onClose} onCreate={onCreate} />
                </TooltipHarness>
            );

            await user.type(screen.getByLabelText(/Name/), 'My test');

            expect(screen.getByRole('button', {name: 'Create'})).toBeEnabled();
        });

        it('should create the test with the entered name', async () => {
            const user = userEvent.setup();

            render(
                <TooltipHarness>
                    <CreateTestDialog onClose={onClose} onCreate={onCreate} />
                </TooltipHarness>
            );

            await user.type(screen.getByLabelText(/Name/), 'My test');
            await user.click(screen.getByRole('button', {name: 'Create'}));

            expect(onCreate).toHaveBeenCalledTimes(1);
            expect(onCreate.mock.calls[0][0]).toBe('My test');
        });

        it('should call onClose when cancel is clicked', async () => {
            const user = userEvent.setup();

            render(
                <TooltipHarness>
                    <CreateTestDialog onClose={onClose} onCreate={onCreate} />
                </TooltipHarness>
            );

            await user.click(screen.getByRole('button', {name: 'Cancel'}));

            expect(onClose).toHaveBeenCalledTimes(1);
        });

        it('should call onClose when the close button is clicked', async () => {
            const user = userEvent.setup();

            render(
                <TooltipHarness>
                    <CreateTestDialog onClose={onClose} onCreate={onCreate} />
                </TooltipHarness>
            );

            await user.click(screen.getByRole('button', {name: 'Close'}));

            expect(onClose).toHaveBeenCalledTimes(1);
        });
    });
});
