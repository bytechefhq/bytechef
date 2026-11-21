import {render, screen} from '@/shared/util/test-utils';
import {describe, expect, it} from 'vitest';

import CustomComponentDefinitionItemList from '../CustomComponentDefinitionItemList';

describe('CustomComponentDefinitionItemList', () => {
    it('shows the empty message when there are no items', () => {
        render(<CustomComponentDefinitionItemList emptyMessage="No actions defined" items={[]} />);

        expect(screen.getByText('No actions defined')).toBeInTheDocument();
        expect(screen.queryByRole('list')).not.toBeInTheDocument();
    });

    it('shows the title, name and description of each item', () => {
        render(
            <CustomComponentDefinitionItemList
                emptyMessage="No actions defined"
                items={[{description: 'Creates a task', name: 'createTask', title: 'Create Task'}]}
            />
        );

        expect(screen.getByText('Create Task')).toBeInTheDocument();
        expect(screen.getByText('createTask')).toBeInTheDocument();
        expect(screen.getByText('Creates a task')).toBeInTheDocument();
        expect(screen.queryByText('No actions defined')).not.toBeInTheDocument();
    });

    it('falls back to the name when an item has no title', () => {
        render(<CustomComponentDefinitionItemList emptyMessage="No triggers defined" items={[{name: 'newTask'}]} />);

        expect(screen.getAllByText('newTask')).toHaveLength(2);
    });
});
