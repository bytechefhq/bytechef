import {NodeDataType} from '@/shared/types';
import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import {calculateNodeWidth} from '../../cluster-element-editor/utils/clusterElementsUtils';
import ReadOnlyClusterRootNode from './ReadOnlyClusterRootNode';

vi.mock('./ClusterFrameShell', () => ({default: ({children}: {children: React.ReactNode}) => <div>{children}</div>}));
vi.mock('./ClusterSlotHandles', () => ({default: () => null}));
vi.mock('./DisabledNodeBadge', () => ({default: () => <span data-testid="disabled-node-badge" />}));
vi.mock('../hooks/useWorkflowTestNodeStates', () => ({default: () => ({})}));
vi.mock('../../cluster-element-editor/utils/clusterElementsUtils', async (importOriginal) => ({
    ...(await importOriginal<object>()),
    getFilteredClusterElementTypes: () => [{name: 'model'}, {name: 'tools'}],
}));

const BASE_DATA = {
    componentName: 'aiAgent',
    label: 'AI Agent',
    name: 'aiAgent_1',
    operationName: 'chat',
    workflowNodeName: 'aiAgent_1',
} as NodeDataType;

function getCard() {
    return screen.getByText('AI Agent').closest('div.rounded-md') as HTMLElement;
}

describe('ReadOnlyClusterRootNode', () => {
    it('keeps the card at the width the frame layout reserved for it', () => {
        render(<ReadOnlyClusterRootNode data={BASE_DATA} id="aiAgent_1" />);

        expect(getCard().style.width).toBe(`${calculateNodeWidth(2)}px`);
    });

    it('mutes the card when the root is effectively disabled', () => {
        render(<ReadOnlyClusterRootNode data={{...BASE_DATA, isEffectivelyDisabled: true}} id="aiAgent_1" />);

        expect(getCard()).toHaveClass('opacity-50', 'grayscale');
        expect(screen.queryByTestId('disabled-node-badge')).not.toBeInTheDocument();
    });

    it('shows the disabled badge for the root own disabled flag', () => {
        render(
            <ReadOnlyClusterRootNode
                data={{...BASE_DATA, disabled: true, isEffectivelyDisabled: true}}
                id="aiAgent_1"
            />
        );

        expect(screen.getByTestId('disabled-node-badge')).toBeInTheDocument();
    });

    it('leaves an enabled root unmuted', () => {
        render(<ReadOnlyClusterRootNode data={BASE_DATA} id="aiAgent_1" />);

        expect(getCard()).not.toHaveClass('opacity-50');
    });
});
