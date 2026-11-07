import {PropertyAllType} from '@/shared/types';
import {render, screen} from '@testing-library/react';
import {Control, FieldValues, useForm} from 'react-hook-form';
import {describe, expect, it, vi} from 'vitest';

import WorkflowInputComponentTestValue from './WorkflowInputComponentTestValue';

vi.mock('@/shared/queries/platform/workflowNodeOptions.queries', () => ({
    useGetWorkflowNodeOptionsQuery: () => ({
        data: [
            {label: '#general', value: 'C1'},
            {label: '#random', value: 'C2'},
        ],
        isLoading: false,
    }),
}));

const Harness = ({
    backingWorkflowNodeName,
    members,
    workflowId,
}: {
    backingWorkflowNodeName?: string;
    members: PropertyAllType[];
    workflowId?: string;
}) => {
    const {control} = useForm();

    return (
        <WorkflowInputComponentTestValue
            backingWorkflowNodeName={backingWorkflowNodeName}
            control={control as unknown as Control<FieldValues>}
            members={members}
            workflowId={workflowId}
        />
    );
};

const dynamicMember = {label: 'Channel', name: 'channel', optionsDataSource: {}} as unknown as PropertyAllType;
const plainMember = {label: 'Name', name: 'name'} as unknown as PropertyAllType;

describe('WorkflowInputComponentTestValue', () => {
    it('renders a live dropdown for a member with dynamic options and a backing node', () => {
        render(<Harness backingWorkflowNodeName="sendMessage_1" members={[dynamicMember]} workflowId="wf1" />);

        expect(screen.getByRole('option', {name: '#general'})).toBeInTheDocument();
        expect(screen.getByRole('option', {name: '#random'})).toBeInTheDocument();
    });

    it('renders a text field for a member without dynamic options', () => {
        render(<Harness backingWorkflowNodeName="sendMessage_1" members={[plainMember]} workflowId="wf1" />);

        expect(screen.getByPlaceholderText('Enter value')).toBeInTheDocument();
        expect(screen.queryByRole('option')).not.toBeInTheDocument();
    });

    it('falls back to a text field with a connection hint when no backing node is available', () => {
        render(<Harness members={[dynamicMember]} workflowId="wf1" />);

        expect(screen.getByText(/Configure a connection/)).toBeInTheDocument();
        expect(screen.getByPlaceholderText('Enter value')).toBeInTheDocument();
    });
});
