import {NodeDataType} from '@/shared/types';
import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import ClusterRootNode from './ClusterRootNode';

vi.mock('./AiAgentNode', () => ({default: () => <div data-testid="ai-agent-node" />}));
vi.mock('./WorkflowNode', () => ({default: () => <div data-testid="workflow-node" />}));

describe('ClusterRootNode', () => {
    const data = {clusterRoot: true, componentName: 'aiAgent', workflowNodeName: 'aiAgent_1'} as NodeDataType;

    it('draws a box-mode root with the card the dialog uses', () => {
        render(
            <ClusterRootNode
                data={{
                    ...data,
                    clusterFrame: {clusterRootId: 'aiAgent_1', contentOrigin: {x: 0, y: 40}, height: 320, width: 640},
                }}
                id="aiAgent_1"
            />
        );

        expect(screen.getByTestId('workflow-node')).toBeInTheDocument();
        expect(screen.queryByTestId('ai-agent-node')).not.toBeInTheDocument();
    });

    it('keeps the compact agent card outside box mode', () => {
        render(<ClusterRootNode data={data} id="aiAgent_1" />);

        expect(screen.getByTestId('ai-agent-node')).toBeInTheDocument();
        expect(screen.queryByTestId('workflow-node')).not.toBeInTheDocument();
    });
});
