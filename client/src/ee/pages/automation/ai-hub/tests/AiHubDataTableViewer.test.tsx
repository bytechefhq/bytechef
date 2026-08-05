import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {describe, expect, it, vi} from 'vitest';

import AiHubDataTableViewer from '../AiHubDataTableViewer';

vi.mock('@/pages/automation/datatable/EmbeddableDataTable', () => ({
    default: ({dataTableId}: {dataTableId: string}) => (
        <div data-datatable-id={dataTableId} data-testid="embeddable-data-table" />
    ),
}));

const wrap = (ui: React.ReactNode) => render(<MemoryRouter>{ui}</MemoryRouter>);

describe('AiHubDataTableViewer', () => {
    it('renders the table name and Open in full view link with the correct href', () => {
        wrap(<AiHubDataTableViewer dataTableId="table-99" name="Contacts" />);

        expect(screen.getByText('Contacts')).toBeInTheDocument();

        const link = screen.getByRole('link', {name: /open in full view/i});

        expect(link).toBeInTheDocument();
        expect(link).toHaveAttribute('href', '/automation/datatables/table-99');
        expect(link).toHaveAttribute('target', '_blank');
    });

    it('renders EmbeddableDataTable with the correct dataTableId', () => {
        wrap(<AiHubDataTableViewer dataTableId="table-99" name="Contacts" />);

        const embed = screen.getByTestId('embeddable-data-table');

        expect(embed).toBeInTheDocument();
        expect(embed).toHaveAttribute('data-datatable-id', 'table-99');
    });
});
