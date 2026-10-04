import {McpServer} from '@/shared/middleware/graphql';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it} from 'vitest';

import McpServerTabs from '../McpServerTabs';

const renderTabs = () =>
    render(
        <McpServerTabs
            connectContent={<div>Connect content</div>}
            mcpComponentDialog={() => null}
            mcpServer={{id: '1', name: 'mcpserver1'} as McpServer}
            toolsContent={<div>Tools content</div>}
            workflowDialog={() => null}
        />
    );

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
});

describe('McpServerTabs', () => {
    it('opens on the Tools tab with the Add Component button', () => {
        renderTabs();

        expect(screen.getByText('Tools content')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Add Component'})).toBeInTheDocument();
    });

    it('hides the Add Component button on the Connect tab', async () => {
        renderTabs();

        await userEvent.click(screen.getByRole('tab', {name: 'Connect'}));

        expect(screen.getByText('Connect content')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Add Component'})).not.toBeInTheDocument();
    });

    it('shows the Add Component button again when switching back to the Tools tab', async () => {
        renderTabs();

        await userEvent.click(screen.getByRole('tab', {name: 'Connect'}));
        await userEvent.click(screen.getByRole('tab', {name: 'Tools'}));

        expect(screen.getByRole('button', {name: 'Add Component'})).toBeInTheDocument();
    });
});
