import {NodeDataType} from '@/shared/types';
import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowIssuesStore from '../../stores/useWorkflowIssuesStore';
import WorkflowIssuesSidebar from '../WorkflowIssuesSidebar';

const hoisted = vi.hoisted(() => ({
    openNodeDetails: vi.fn(),
}));

vi.mock('../../utils/openNodeDetails', () => ({default: hoisted.openNodeDetails}));

const nodeData = {componentName: 'dataTable', name: 'dataTable_2', workflowNodeName: 'dataTable_2'} as NodeDataType;

vi.mock('../../stores/useWorkflowDataStore', () => ({
    default: (selector: (state: {nodes: Array<{data: NodeDataType; id: string}>}) => unknown) =>
        selector({nodes: [{data: nodeData, id: 'dataTable_2'}]}),
}));

describe('WorkflowIssuesSidebar', () => {
    beforeEach(() => {
        hoisted.openNodeDetails.mockClear();
        useWorkflowIssuesStore.getState().reset();
    });

    it('shows an empty state when there are no issues', () => {
        render(<WorkflowIssuesSidebar visible />);

        expect(screen.getByText('No issues found')).toBeInTheDocument();
    });

    it('groups issues by node and opens the node when a row is clicked', () => {
        useWorkflowIssuesStore.getState().setValidatorIssues([
            {
                kind: 'MISSING_RESOURCE',
                message: "Table does not have primary key column 'id': dt_0_conversations",
                nodeName: 'dataTable_2',
                propertyPath: 'table',
                severity: 'ERROR',
                source: 'VALIDATOR',
            },
            {
                kind: 'MISSING_REQUIRED',
                message: 'Missing required property: id',
                nodeName: 'dataTable_2',
                propertyPath: 'id',
                severity: 'ERROR',
                source: 'VALIDATOR',
            },
        ]);

        render(<WorkflowIssuesSidebar visible />);

        expect(screen.getAllByRole('heading', {level: 3})).toHaveLength(1);
        expect(screen.getByText('dataTable_2')).toBeInTheDocument();

        fireEvent.click(screen.getByText('Missing required property: id'));

        expect(hoisted.openNodeDetails).toHaveBeenCalledWith(nodeData, 'properties');
    });
});
