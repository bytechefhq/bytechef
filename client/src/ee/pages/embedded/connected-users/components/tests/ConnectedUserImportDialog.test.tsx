import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectedUserImportDialog from '../ConnectedUserImportDialog';

const hoisted = vi.hoisted(() => ({
    importConnectedUsersMock: vi.fn(),
}));

vi.mock('@/ee/shared/middleware/embedded/connected-user', () => ({
    ConnectedUserApi: class {
        importConnectedUsers = hoisted.importConnectedUsersMock;
    },
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 2}),
}));

beforeEach(() => {
    windowResizeObserver();

    hoisted.importConnectedUsersMock.mockResolvedValue({
        createdCount: 2,
        rejectedRows: [{reason: 'Wrong number of cells', rowNumber: 4}],
        skippedExternalIds: ['existing', 'existing'],
    });
});

afterEach(() => {
    resetAll();
});

describe('ConnectedUserImportDialog', () => {
    it('uploads the file with the environment id and shows the summary', async () => {
        render(<ConnectedUserImportDialog onClose={vi.fn()} />);

        expect(
            screen.getByText('Columns: externalId (required), name, email, plus any metadata columns')
        ).toBeInTheDocument();

        const file = new File(['externalId\na\nb'], 'users.csv', {type: 'text/csv'});

        await userEvent.upload(screen.getByLabelText('CSV file'), file);
        await userEvent.click(screen.getByRole('button', {name: 'Import'}));

        await waitFor(() => expect(hoisted.importConnectedUsersMock).toHaveBeenCalledWith({environmentId: 2, file}));

        expect(await screen.findByText('Created 2')).toBeInTheDocument();
        expect(screen.getByText('Skipped (duplicates):')).toBeInTheDocument();
        expect(screen.getAllByText('existing')).toHaveLength(1);
        expect(screen.getByText('Row 4: Wrong number of cells')).toBeInTheDocument();
    });

    it('keeps Import disabled until a file is chosen', () => {
        render(<ConnectedUserImportDialog onClose={vi.fn()} />);

        expect(screen.getByRole('button', {name: 'Import'})).toBeDisabled();
    });
});
