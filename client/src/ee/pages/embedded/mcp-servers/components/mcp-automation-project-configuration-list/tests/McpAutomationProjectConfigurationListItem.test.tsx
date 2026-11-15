import {render, screen} from '@/shared/util/test-utils';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';

import McpAutomationProjectConfigurationListItem from '../McpAutomationProjectConfigurationListItem';
import {McpAutomationProjectConfigurationItemType} from '../hooks/useMcpAutomationProjectConfigurationList';

vi.mock('../McpAutomationProjectConfigurationListItemDropdownMenu', () => ({default: () => null}));

const item = {
    automationProjectConfigurationEnabled: true,
    automationProjectConfigurationId: '100',
    automationProjectConfigurationName: 'Production config',
    id: 'm1',
    mcpServerId: 's1',
    projectId: '10',
    projectName: 'Onboarding',
    projectVersion: 2,
    workflows: [
        {available: true, enabled: true, id: 'w1', label: 'Workflow One', parameters: null, workflowUuid: 'uuid-1'},
        {available: false, enabled: true, id: 'w2', label: 'Workflow Two', parameters: null, workflowUuid: 'uuid-2'},
    ],
} as McpAutomationProjectConfigurationItemType;

describe('McpAutomationProjectConfigurationListItem', () => {
    it('listItemMarksUnavailableWorkflow', async () => {
        render(<McpAutomationProjectConfigurationListItem mcpAutomationProjectConfiguration={item} />);

        expect(screen.getByText('Onboarding')).toBeInTheDocument();
        expect(screen.getByText('Production config')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Expand automation'}));

        expect(screen.getByText('Workflow One')).toBeInTheDocument();
        expect(screen.getAllByText('Not available')).toHaveLength(1);
        expect(screen.getByText('Workflow Two').closest('[data-available]')).toHaveAttribute('data-available', 'false');
    });
});
