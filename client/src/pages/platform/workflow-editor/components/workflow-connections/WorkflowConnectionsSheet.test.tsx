import {Workflow} from '@/shared/middleware/platform/configuration';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowConnectionsSheet from './WorkflowConnectionsSheet';
import {useSaveWorkflowConnectionsStore} from './hooks/useSaveWorkflowConnections';

vi.mock('./WorkflowConnectionsSheetContent', async () => {
    const {SheetClose} = await vi.importActual<typeof import('@/components/ui/sheet')>('@/components/ui/sheet');

    return {
        default: () => <SheetClose>Close the sheet</SheetClose>,
    };
});

const onSheetOpenChange = vi.fn();

const workflow = {id: 'workflow-1', tasks: [], triggers: []} as unknown as Workflow;

function renderSheet() {
    return render(<WorkflowConnectionsSheet onSheetOpenChange={onSheetOpenChange} sheetOpen workflow={workflow} />);
}

beforeEach(() => {
    useSaveWorkflowConnectionsStore.setState({pendingBatchCount: 0});
});

afterEach(() => {
    vi.clearAllMocks();
});

describe('WorkflowConnectionsSheet', () => {
    it('ignores close requests while a save is running', async () => {
        useSaveWorkflowConnectionsStore.setState({pendingBatchCount: 1});

        renderSheet();

        await userEvent.click(await screen.findByRole('button', {name: 'Close the sheet'}));
        await userEvent.keyboard('{Escape}');

        expect(onSheetOpenChange).not.toHaveBeenCalled();
        expect(screen.getByRole('dialog', {name: 'Workflow Connections'})).toBeInTheDocument();
    });

    it('closes normally when no save is running', async () => {
        renderSheet();

        await userEvent.click(await screen.findByRole('button', {name: 'Close the sheet'}));

        expect(onSheetOpenChange).toHaveBeenCalledWith(false);

        onSheetOpenChange.mockClear();

        await userEvent.keyboard('{Escape}');

        expect(onSheetOpenChange).toHaveBeenCalledWith(false);
    });
});
