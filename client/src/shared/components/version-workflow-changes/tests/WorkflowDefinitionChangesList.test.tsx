import {WorkflowNodeChangeKind, WorkflowNodeChangeTarget} from '@/shared/middleware/graphql';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {describe, expect, it} from 'vitest';

import WorkflowDefinitionChangesList from '../WorkflowDefinitionChangesList';

const changes = [
    {
        kind: WorkflowNodeChangeKind.Added,
        label: 'Send email',
        name: 'gmail_1',
        parentPath: 'loop_1',
        target: WorkflowNodeChangeTarget.Task,
        type: 'gmail/v1/sendEmail',
        valueChanges: [],
    },
    {
        kind: WorkflowNodeChangeKind.Modified,
        label: null,
        name: 'http_1',
        parentPath: null,
        target: WorkflowNodeChangeTarget.Task,
        type: 'http/v1/get',
        valueChanges: [
            {newValue: '"https://b"', oldValue: '"https://a"', path: 'parameters.url'},
            {newValue: '"x"', oldValue: null, path: 'parameters.header'},
            {newValue: null, oldValue: '"y"', path: 'parameters.body'},
        ],
    },
];

describe('WorkflowDefinitionChangesList', () => {
    it('renders a section per change with kind badge, name, type and parent path', () => {
        render(<WorkflowDefinitionChangesList changes={changes} />);

        expect(screen.getByText('Added')).toBeInTheDocument();
        expect(screen.getByText('Modified')).toBeInTheDocument();
        expect(screen.getByText('Send email')).toBeInTheDocument();
        expect(screen.getByText('http_1')).toBeInTheDocument();
        expect(screen.getByText('gmail/v1/sendEmail')).toBeInTheDocument();
        expect(screen.getByText('in loop_1')).toBeInTheDocument();
    });

    it('renders value changes with old and new values and a dash for missing sides', () => {
        render(<WorkflowDefinitionChangesList changes={changes} />);

        expect(screen.getByText('parameters.url')).toBeInTheDocument();
        expect(screen.getByText('"https://a" → "https://b"')).toBeInTheDocument();
        expect(screen.getByText('— → "x"')).toBeInTheDocument();
        expect(screen.getByText('"y" → —')).toBeInTheDocument();
    });

    it('collapses a long value and expands it on demand', async () => {
        const longValue = `"${'a'.repeat(300)}"`;

        render(
            <WorkflowDefinitionChangesList
                changes={[
                    {
                        ...changes[1],
                        valueChanges: [{newValue: longValue, oldValue: null, path: 'metadata'}],
                    },
                ]}
            />
        );

        expect(screen.queryByText(`— → ${longValue}`)).not.toBeInTheDocument();
        expect(screen.getByText(/^— → "a+…$/)).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Show more'}));

        expect(screen.getByText(`— → ${longValue}`)).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Show less'}));

        expect(screen.getByText(/^— → "a+…$/)).toBeInTheDocument();
    });

    it('renders the empty state', () => {
        render(<WorkflowDefinitionChangesList changes={[]} />);

        expect(screen.getByText('No structural changes.')).toBeInTheDocument();
    });

    it('titles a workflow-level change as Workflow', () => {
        render(
            <WorkflowDefinitionChangesList
                changes={[
                    {
                        kind: WorkflowNodeChangeKind.Modified,
                        label: null,
                        name: null,
                        parentPath: null,
                        target: WorkflowNodeChangeTarget.Workflow,
                        type: null,
                        valueChanges: [{newValue: '"New"', oldValue: '"Old"', path: 'label'}],
                    },
                ]}
            />
        );

        expect(screen.getByText('Workflow')).toBeInTheDocument();
        expect(screen.getByText('"Old" → "New"')).toBeInTheDocument();
    });
});
