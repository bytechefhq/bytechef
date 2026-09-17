import {TooltipProvider} from '@/components/ui/tooltip';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useClusterElementsDataStore from '../stores/useClusterElementsDataStore';
import ClusterElementsWorkflowEditor from './ClusterElementsWorkflowEditor';

vi.mock('../hooks/useClusterElementsWorkflowEditor', () => ({
    default: () => ({
        clusterElementsEdgeTypes: {},
        clusterElementsNodeTypes: {},
        edges: [],
        handleNodesChange: vi.fn(),
        handleResetLayout: vi.fn(),
        nodes: [],
    }),
}));

const renderEditor = () =>
    render(
        <TooltipProvider>
            <ClusterElementsWorkflowEditor />
        </TooltipProvider>
    );

describe('ClusterElementsWorkflowEditor - lock button', () => {
    beforeEach(() => {
        useClusterElementsDataStore.setState({edges: [], nodes: [], nodesLocked: true});
    });

    it('renders the unlock affordance when locked', async () => {
        renderEditor();

        // The canvas (and its toolbar) mounts only after the settle delay, so
        // wait for the control to appear rather than querying synchronously.
        expect(await screen.findByLabelText('Unlock node movement')).toBeInTheDocument();
    });

    it('toggles nodesLocked when clicked', async () => {
        const user = userEvent.setup();

        renderEditor();

        await user.click(await screen.findByLabelText('Unlock node movement'));

        expect(useClusterElementsDataStore.getState().nodesLocked).toBe(false);
        expect(screen.getByLabelText('Lock node movement')).toBeInTheDocument();
    });
});
