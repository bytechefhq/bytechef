import {NodeDataType} from '@/shared/types';
import {render, screen} from '@testing-library/react';
import {ReactFlowProvider} from '@xyflow/react';
import {describe, expect, it, vi} from 'vitest';

import ReadOnlyNode from './ReadOnlyNode';

vi.mock('../stores/useLayoutDirectionStore', () => ({
    default: (selector: (state: {layoutDirection: string}) => unknown) => selector({layoutDirection: 'TB'}),
}));

const BASE_DATA = {
    label: 'Approval',
    name: 'approval_1',
    workflowNodeName: 'approval_1',
} as unknown as NodeDataType;

function renderNode(data: NodeDataType) {
    return render(
        <ReactFlowProvider>
            <ReadOnlyNode data={data} id="approval_1" />
        </ReactFlowProvider>
    );
}

describe('ReadOnlyNode', () => {
    it('renders muted when the node is effectively disabled', () => {
        const {container} = renderNode({...BASE_DATA, isEffectivelyDisabled: true});

        expect(container.firstElementChild?.className).toContain('opacity-50');
        expect(container.firstElementChild?.className).toContain('grayscale');
    });

    it('does not render muted when the node is not effectively disabled', () => {
        const {container} = renderNode(BASE_DATA);

        expect(container.firstElementChild?.className).not.toContain('opacity-50');
        expect(container.firstElementChild?.className).not.toContain('grayscale');
    });

    it('shows the disabled badge only for the node own disabled flag', () => {
        renderNode({...BASE_DATA, disabled: true, isEffectivelyDisabled: true});

        expect(screen.getByTitle('Disabled — skipped during execution')).toBeInTheDocument();
    });

    it('does not show the disabled badge for a node only muted via ancestor derivation', () => {
        renderNode({...BASE_DATA, isEffectivelyDisabled: true});

        expect(screen.queryByTitle('Disabled — skipped during execution')).not.toBeInTheDocument();
    });
});
