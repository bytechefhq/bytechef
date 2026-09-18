import {FINAL_PLACEHOLDER_NODE_ID} from '@/shared/constants';
import {NodeDataType} from '@/shared/types';
import {render, screen} from '@testing-library/react';
import {ReactFlowProvider} from '@xyflow/react';
import {describe, expect, it, vi} from 'vitest';

import PlaceholderNode from '../PlaceholderNode';

vi.mock('../../components/WorkflowNodesPopoverMenu', () => ({
    default: ({children}: {children: React.ReactNode}) => <div>{children}</div>,
}));

vi.mock('../../providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({updateWorkflowMutation: undefined}),
}));

const renderPlaceholder = (id: string) =>
    render(
        <ReactFlowProvider>
            <PlaceholderNode data={{label: '+'} as NodeDataType} id={id} />
        </ReactFlowProvider>
    );

describe('PlaceholderNode', () => {
    it('renders the final placeholder as a dashed box with a plus icon', () => {
        renderPlaceholder(FINAL_PLACEHOLDER_NODE_ID);

        const placeholderBox = screen.getByTitle('Click to add a node');

        expect(placeholderBox).toHaveClass('size-12', 'border-dashed');
        expect(placeholderBox.querySelector('svg')).toBeInTheDocument();
        expect(placeholderBox).not.toHaveTextContent('+');
    });

    it('keeps an in-chain placeholder as the small text chip', () => {
        renderPlaceholder('condition_1-placeholder');

        const placeholderBox = screen.getByTitle('Click to add a node');

        expect(placeholderBox).not.toHaveClass('border-dashed');
        expect(placeholderBox).toHaveTextContent('+');
    });
});
