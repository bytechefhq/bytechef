import {TooltipProvider} from '@/components/ui/tooltip';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectionWorkflowList, {type ConnectionWorkflowI} from '../ConnectionWorkflowList';

const connectionWorkflows: ConnectionWorkflowI[] = [
    {
        id: '1',
        parentName: 'Sales Ops',
        usage: 'DEPLOYMENT',
        version: 2,
        workflowName: 'Sync leads from the CRM into the warehouse every night',
    },
];

const renderList = () =>
    render(
        <TooltipProvider>
            <ConnectionWorkflowList connectionWorkflows={connectionWorkflows} isLoading={false} />
        </TooltipProvider>
    );

const mockTextWidths = (scrollWidth: number, clientWidth: number) => {
    vi.spyOn(HTMLElement.prototype, 'scrollWidth', 'get').mockReturnValue(scrollWidth);
    vi.spyOn(HTMLElement.prototype, 'clientWidth', 'get').mockReturnValue(clientWidth);
};

describe('ConnectionWorkflowList', () => {
    beforeEach(() => {
        windowResizeObserver();
    });

    afterEach(() => {
        vi.restoreAllMocks();

        resetAll();
    });

    it('shows the full workflow name on hover when it is cut off', async () => {
        mockTextWidths(400, 120);

        renderList();

        await userEvent.hover(screen.getByText('Sync leads from the CRM into the warehouse every night'));

        expect(await screen.findByRole('tooltip')).toHaveTextContent(
            'Sync leads from the CRM into the warehouse every night'
        );
    });

    it('shows no tooltip on hover when the name fits', async () => {
        mockTextWidths(120, 120);

        renderList();

        await userEvent.hover(screen.getByText('Sync leads from the CRM into the warehouse every night'));

        expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
    });
});
