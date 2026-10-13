import {TooltipProvider} from '@/components/ui/tooltip';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectedUserProjectWorkflowListItem, {
    ConnectedUserProjectWorkflowType,
} from '../ConnectedUserProjectWorkflowListItem';

const hoisted = vi.hoisted(() => ({
    copyToClipboard: vi.fn(),
}));

vi.mock('@uidotdev/usehooks', () => ({
    useCopyToClipboard: () => [null, hoisted.copyToClipboard],
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<object>()),
    useDeleteConnectedUserProjectWorkflowMutation: () => ({isPending: false, mutate: vi.fn()}),
    useEnableConnectedUserProjectWorkflowMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

const STATIC_WEBHOOK_URL = 'https://bytechef.example.com/webhooks/abc123';

const createConnectedUserProjectWorkflow = (staticWebhookUrl: string | null): ConnectedUserProjectWorkflowType => ({
    connectedUserId: '1050',
    enabled: true,
    id: '1052',
    lastExecutionDate: null,
    projectId: '0',
    staticWebhookUrl,
    workflow: {
        id: 'be530417-2226-41d4-aae9-c4fc76f0791a',
        label: 'WhatsApp Agent - Human reply',
        triggers: [],
    },
    workflowUuid: 'be530417-2226-41d4-aae9-c4fc76f0791a',
    workflowVersion: 5,
});

const renderListItem = (connectedUserProjectWorkflow: ConnectedUserProjectWorkflowType) =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <TooltipProvider>
                <ul>
                    <ConnectedUserProjectWorkflowListItem connectedUserProjectWorkflow={connectedUserProjectWorkflow} />
                </ul>
            </TooltipProvider>
        </QueryClientProvider>
    );

describe('ConnectedUserProjectWorkflowListItem', () => {
    beforeEach(() => {
        hoisted.copyToClipboard.mockClear();
    });

    it('shows the static webhook url and copies it', async () => {
        renderListItem(createConnectedUserProjectWorkflow(STATIC_WEBHOOK_URL));

        expect(screen.getByText(STATIC_WEBHOOK_URL)).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Copy webhook URL'}));

        expect(hoisted.copyToClipboard).toHaveBeenCalledWith(STATIC_WEBHOOK_URL);
    });

    it('shows no webhook url for a workflow without a static webhook trigger', () => {
        renderListItem(createConnectedUserProjectWorkflow(null));

        expect(screen.getByText('WhatsApp Agent - Human reply')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Copy webhook URL'})).not.toBeInTheDocument();
    });
});
