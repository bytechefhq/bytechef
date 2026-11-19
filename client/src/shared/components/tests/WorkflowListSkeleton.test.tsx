import {render, screen, within} from '@/shared/util/test-utils';
import {describe, expect, it} from 'vitest';

import WorkflowListSkeleton from '../WorkflowListSkeleton';

describe('WorkflowListSkeleton', () => {
    it('renders a busy list with the given label', () => {
        render(<WorkflowListSkeleton label="Loading workflows of HNB Statistika" workflowCount={2} />);

        expect(screen.getByRole('list', {name: 'Loading workflows of HNB Statistika'})).toHaveAttribute(
            'aria-busy',
            'true'
        );
    });

    it('renders one row per workflow, capped at three', () => {
        render(<WorkflowListSkeleton label="Loading workflows" workflowCount={25} />);

        expect(within(screen.getByRole('list', {name: 'Loading workflows'})).getAllByRole('listitem')).toHaveLength(3);
    });

    it('renders one row when the workflow count is unknown', () => {
        render(<WorkflowListSkeleton label="Loading workflows" />);

        expect(within(screen.getByRole('list', {name: 'Loading workflows'})).getAllByRole('listitem')).toHaveLength(1);
    });

    it('lays each row out like a workflow row, with a name column, the components and a menu button', () => {
        render(<WorkflowListSkeleton label="Loading workflows" workflowCount={1} />);

        const row = within(screen.getByRole('list', {name: 'Loading workflows'})).getByRole('listitem');

        expect(row).toHaveClass('py-1', 'pl-3');
        expect(row.querySelector('.w-80')).toBeInTheDocument();
        expect(row.querySelector('.size-9')).toBeInTheDocument();
    });

    it('puts the modified date under the name only when asked, as project rows do below xl', () => {
        const {rerender} = render(<WorkflowListSkeleton label="Loading workflows" workflowCount={1} />);

        expect(screen.queryByTestId('modified-date-under-name')).not.toBeInTheDocument();

        rerender(<WorkflowListSkeleton label="Loading workflows" modifiedDateUnderName workflowCount={1} />);

        expect(screen.getByTestId('modified-date-under-name')).toHaveClass('xl:hidden');
    });
});
