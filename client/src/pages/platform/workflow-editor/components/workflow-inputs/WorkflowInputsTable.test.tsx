import {WorkflowInput} from '@/shared/middleware/platform/configuration';
import {render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

import WorkflowInputsTable from './WorkflowInputsTable';

const workflowInputs = [{label: 'Order ID', name: 'orderId', required: true, type: 'STRING'}] as WorkflowInput[];

const renderTable = (codeWorkflow: boolean) =>
    render(
        <WorkflowInputsTable
            codeWorkflow={codeWorkflow}
            internalOnlyVisible={false}
            openDeleteDialog={() => {}}
            openEditDialog={() => {}}
            workflowInputs={workflowInputs}
            workflowTestConfigurationInputs={{orderId: 'ORD-1'}}
        />
    );

describe('WorkflowInputsTable', () => {
    it('offers edit and delete for a visually built workflow', () => {
        renderTable(false);

        expect(screen.getByRole('button', {name: 'Edit input'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Delete input'})).toBeInTheDocument();
    });

    it('drops delete for a code workflow, whose source owns the declaration', () => {
        renderTable(true);

        // Edit stays: it is how a test value is set.
        expect(screen.getByRole('button', {name: 'Edit input'})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Delete input'})).not.toBeInTheDocument();
    });

    it('still shows the test value for a code workflow', () => {
        renderTable(true);

        expect(screen.getByText('ORD-1')).toBeInTheDocument();
    });
});
