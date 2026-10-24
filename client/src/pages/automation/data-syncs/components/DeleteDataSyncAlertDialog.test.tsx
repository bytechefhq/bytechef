import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';

import DeleteDataSyncAlertDialog from './DeleteDataSyncAlertDialog';

describe('DeleteDataSyncAlertDialog', () => {
    it('names the data sync being deleted', () => {
        render(<DeleteDataSyncAlertDialog dataSyncTitle="CRM to DB" onClose={vi.fn()} onDelete={vi.fn()} />);

        expect(screen.getByText(/permanently delete the data sync CRM to DB/)).toBeInTheDocument();
    });

    it('calls onDelete when the confirm button is clicked', async () => {
        const onDelete = vi.fn();

        render(<DeleteDataSyncAlertDialog dataSyncTitle="CRM to DB" onClose={vi.fn()} onDelete={onDelete} />);

        await userEvent.click(screen.getByRole('button', {name: 'Confirm Data Sync Deletion'}));

        expect(onDelete).toHaveBeenCalled();
    });

    it('calls onClose when cancelled', async () => {
        const onClose = vi.fn();

        render(<DeleteDataSyncAlertDialog dataSyncTitle="CRM to DB" onClose={onClose} onDelete={vi.fn()} />);

        await userEvent.click(screen.getByRole('button', {name: 'Cancel'}));

        expect(onClose).toHaveBeenCalled();
    });
});
