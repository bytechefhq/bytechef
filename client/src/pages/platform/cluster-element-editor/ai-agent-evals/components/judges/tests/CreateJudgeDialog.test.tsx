import {TooltipProvider} from '@/components/ui/tooltip';
import {AiAgentJudgeType} from '@/shared/middleware/graphql-types';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import CreateJudgeDialog from '../CreateJudgeDialog';

// The dialog's info tooltips need the provider the app mounts at its root.
const TooltipHarness = ({children}: {children: ReactNode}) => <TooltipProvider>{children}</TooltipProvider>;

const onClose = vi.fn();
const onCreate = vi.fn();
const onUpdate = vi.fn();

const editData = {
    configuration: {},
    id: 'judge-1',
    name: 'Existing judge',
    type: AiAgentJudgeType.LlmRule,
};

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('CreateJudgeDialog', () => {
    describe('create mode', () => {
        it('should render the create title', () => {
            render(
                <TooltipHarness>
                    <CreateJudgeDialog onClose={onClose} onCreate={onCreate} />
                </TooltipHarness>
            );

            expect(screen.getByText('Create Judge')).toBeInTheDocument();
        });

        it('should label the primary action Create', () => {
            render(
                <TooltipHarness>
                    <CreateJudgeDialog onClose={onClose} onCreate={onCreate} />
                </TooltipHarness>
            );

            expect(screen.getByRole('button', {name: 'Create'})).toBeInTheDocument();
        });

        it('should disable the primary action until a name is entered', () => {
            render(
                <TooltipHarness>
                    <CreateJudgeDialog onClose={onClose} onCreate={onCreate} />
                </TooltipHarness>
            );

            expect(screen.getByRole('button', {name: 'Create'})).toBeDisabled();
        });

        it('should enable the primary action once a name is entered', async () => {
            const user = userEvent.setup();

            render(
                <TooltipHarness>
                    <CreateJudgeDialog onClose={onClose} onCreate={onCreate} />
                </TooltipHarness>
            );

            await user.type(screen.getByLabelText(/Name/), 'My judge');

            expect(screen.getByRole('button', {name: 'Create'})).toBeEnabled();
        });
    });

    describe('edit mode', () => {
        it('should render the edit title', () => {
            render(
                <TooltipHarness>
                    <CreateJudgeDialog editData={editData} onClose={onClose} onCreate={onCreate} onUpdate={onUpdate} />
                </TooltipHarness>
            );

            expect(screen.getByText('Edit Judge')).toBeInTheDocument();
        });

        it('should label the primary action Save', () => {
            render(
                <TooltipHarness>
                    <CreateJudgeDialog editData={editData} onClose={onClose} onCreate={onCreate} onUpdate={onUpdate} />
                </TooltipHarness>
            );

            expect(screen.getByRole('button', {name: 'Save'})).toBeInTheDocument();
        });

        it('should prefill the name from the judge', () => {
            render(
                <TooltipHarness>
                    <CreateJudgeDialog editData={editData} onClose={onClose} onCreate={onCreate} onUpdate={onUpdate} />
                </TooltipHarness>
            );

            expect(screen.getByLabelText(/Name/)).toHaveValue('Existing judge');
        });
    });

    describe('dialog chrome', () => {
        it('should render the close and cancel controls', () => {
            render(
                <TooltipHarness>
                    <CreateJudgeDialog onClose={onClose} onCreate={onCreate} />
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
                    <CreateJudgeDialog onClose={onClose} onCreate={onCreate} />
                </TooltipHarness>
            );

            await user.click(screen.getByRole('button', {name: 'Cancel'}));

            expect(onClose).toHaveBeenCalledTimes(1);
        });

        it('should call onClose when the close button is clicked', async () => {
            const user = userEvent.setup();

            render(
                <TooltipHarness>
                    <CreateJudgeDialog onClose={onClose} onCreate={onCreate} />
                </TooltipHarness>
            );

            await user.click(screen.getByRole('button', {name: 'Close'}));

            expect(onClose).toHaveBeenCalledTimes(1);
        });

        it('should create the judge with the entered name', async () => {
            const user = userEvent.setup();

            render(
                <TooltipHarness>
                    <CreateJudgeDialog onClose={onClose} onCreate={onCreate} />
                </TooltipHarness>
            );

            await user.type(screen.getByLabelText(/Name/), 'My judge');
            await user.click(screen.getByRole('button', {name: 'Create'}));

            expect(onCreate).toHaveBeenCalledWith('My judge', expect.anything(), expect.anything());
        });
    });
});
