import {WorkflowChangeType} from '@/shared/middleware/graphql';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

import VersionWorkflowChangesList from '../VersionWorkflowChangesList';

const changes = {
    baseVersion: 1,
    version: 2,
    workflows: [
        {changeCount: 1, changeType: WorkflowChangeType.Added, label: 'First', workflowUuid: 'uuid-1'},
        {changeCount: 3, changeType: WorkflowChangeType.Modified, label: null, workflowUuid: 'uuid-2'},
        {changeCount: 0, changeType: WorkflowChangeType.Removed, label: 'Gone', workflowUuid: 'uuid-3'},
    ],
};

describe('VersionWorkflowChangesList', () => {
    it('renders the summary and one row per changed workflow with badges', () => {
        render(<VersionWorkflowChangesList changes={changes} isLoading={false} onWorkflowClick={vi.fn()} />);

        expect(screen.getByText('3 workflows changed (1 added, 1 modified, 1 removed)')).toBeInTheDocument();
        expect(screen.getAllByRole('button')).toHaveLength(3);
        expect(screen.getByLabelText('Open changes of First')).toBeInTheDocument();
        expect(screen.getByLabelText('Open changes of uuid-2')).toBeInTheDocument();
        expect(screen.getByText('Added')).toBeInTheDocument();
        expect(screen.getByText('Modified')).toBeInTheDocument();
        expect(screen.getByText('Removed')).toBeInTheDocument();
        expect(screen.getByText('1 change')).toBeInTheDocument();
        expect(screen.getByText('3 changes')).toBeInTheDocument();
        expect(screen.queryByText('0 changes')).not.toBeInTheDocument();
    });

    it('calls onWorkflowClick with the workflow uuid', async () => {
        const onWorkflowClick = vi.fn();

        render(<VersionWorkflowChangesList changes={changes} isLoading={false} onWorkflowClick={onWorkflowClick} />);

        await userEvent.click(screen.getByLabelText('Open changes of First'));

        expect(onWorkflowClick).toHaveBeenCalledWith('uuid-1');
    });

    it('shows the loading icon while loading', () => {
        const {container} = render(<VersionWorkflowChangesList isLoading onWorkflowClick={vi.fn()} />);

        expect(container.querySelector('svg.animate-spin')).toBeInTheDocument();
    });

    it('renders the empty state', () => {
        render(
            <VersionWorkflowChangesList
                changes={{version: 1, workflows: []}}
                isLoading={false}
                onWorkflowClick={vi.fn()}
            />
        );

        expect(screen.getByText('No workflow changes.')).toBeInTheDocument();
        expect(screen.queryByRole('button')).not.toBeInTheDocument();
    });
});
