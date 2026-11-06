import {render, screen, within} from '@/shared/util/test-utils';
import {describe, expect, it} from 'vitest';

import WorkflowGroupCardSkeleton from '../WorkflowGroupCardSkeleton';

describe('WorkflowGroupCardSkeleton', () => {
    it('should render a busy card with the given label', () => {
        render(<WorkflowGroupCardSkeleton label="Loading deployments" workflowCount={1} />);

        expect(screen.getByLabelText('Loading deployments')).toHaveAttribute('aria-busy', 'true');
    });

    it('should render one workflow row per workflow, capped at three', () => {
        render(<WorkflowGroupCardSkeleton label="Loading deployments" workflowCount={5} />);

        expect(within(screen.getByRole('list', {name: 'Loading workflows'})).getAllByRole('listitem')).toHaveLength(3);
    });

    it('should render no workflows section when there are no workflows', () => {
        render(<WorkflowGroupCardSkeleton label="Loading deployments" workflowCount={0} />);

        expect(screen.queryByRole('list', {name: 'Loading workflows'})).not.toBeInTheDocument();
    });

    it('should render a run button placeholder per row only when requested', () => {
        const {rerender} = render(<WorkflowGroupCardSkeleton label="Loading deployments" workflowCount={2} />);

        expect(screen.queryAllByTestId('workflow-run-button-skeleton')).toHaveLength(0);

        rerender(<WorkflowGroupCardSkeleton label="Loading deployments" showWorkflowRunButton workflowCount={2} />);

        expect(screen.getAllByTestId('workflow-run-button-skeleton')).toHaveLength(2);
    });
});
