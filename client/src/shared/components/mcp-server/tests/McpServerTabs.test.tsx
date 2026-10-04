import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it} from 'vitest';

import McpServerTabs from '../McpServerTabs';

const renderTabs = () =>
    render(
        <McpServerTabs
            addToolsButton={<button>Add tools</button>}
            connectContent={<div>Connect content</div>}
            toolsContent={<div>Tools content</div>}
        />
    );

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
});

describe('McpServerTabs', () => {
    it('opens on the Tools tab with the add tools button', () => {
        renderTabs();

        expect(screen.getByText('Tools content')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Add tools'})).toBeInTheDocument();
    });

    it('hides the add tools button on the Connect tab', async () => {
        renderTabs();

        await userEvent.click(screen.getByRole('tab', {name: 'Connect'}));

        expect(screen.getByText('Connect content')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Add tools'})).not.toBeInTheDocument();
    });

    it('shows the add tools button again when switching back to the Tools tab', async () => {
        renderTabs();

        await userEvent.click(screen.getByRole('tab', {name: 'Connect'}));
        await userEvent.click(screen.getByRole('tab', {name: 'Tools'}));

        expect(screen.getByRole('button', {name: 'Add tools'})).toBeInTheDocument();
    });
});
