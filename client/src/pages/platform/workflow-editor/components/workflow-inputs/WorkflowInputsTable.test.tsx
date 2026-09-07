import {render, resetAll, screen} from '@/shared/util/test-utils';
import {afterEach, describe, expect, it, vi} from 'vitest';

import WorkflowInputsTable from './WorkflowInputsTable';

const longTestValue =
    'a-very-long-test-value-that-would-otherwise-stretch-the-workflow-inputs-panel-past-its-visible-width';

const workflowInputs = [
    {
        label: 'Long Input',
        name: 'longInput',
        required: false,
        type: 'string',
    },
];

const renderTable = () =>
    render(
        <WorkflowInputsTable
            openDeleteDialog={vi.fn()}
            openEditDialog={vi.fn()}
            workflowInputs={workflowInputs}
            workflowTestConfigurationInputs={{longInput: longTestValue}}
        />
    );

afterEach(() => {
    resetAll();
});

describe('WorkflowInputsTable', () => {
    it('should truncate a long test value instead of widening the table', () => {
        renderTable();

        const testValueCell = screen.getByText(longTestValue);

        expect(testValueCell).toHaveClass('truncate');

        expect(testValueCell.closest('table')).toHaveClass('table-fixed');
    });

    it('should expose the full test value through the cell title', () => {
        renderTable();

        expect(screen.getByTitle(longTestValue)).toBeInTheDocument();
    });
});
