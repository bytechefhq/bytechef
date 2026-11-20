import {render, screen} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationProjectConfigurationDialogWorkflowsStepItem from '../AutomationProjectConfigurationDialogWorkflowsStepItem';

const {configurationQueryMock, projectQueryMock} = vi.hoisted(() => ({
    configurationQueryMock: vi.fn(),
    projectQueryMock: vi.fn(),
}));

const providableValues = {
    connectionSlots: [],
    inputs: [{label: 'Contact page URL', name: 'contactPageUrl', required: true, type: 'string'}],
    providedConnections: {},
    providedInputs: {},
};

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useAutomationProjectConfigurationWorkflowProvidableValuesQuery: configurationQueryMock,
    useAutomationProjectWorkflowProvidableValuesQuery: projectQueryMock,
}));

const workflow = {enabled: true, label: 'Rate Ticket', workflowUuid: 'rate-uuid'};

describe('AutomationProjectConfigurationDialogWorkflowsStepItem', () => {
    beforeEach(() => {
        configurationQueryMock.mockReset();
        projectQueryMock.mockReset();
    });

    it('reads the providable values of the project version before the configuration exists', () => {
        configurationQueryMock.mockReturnValue({data: undefined});
        projectQueryMock.mockReturnValue({data: {automationProjectWorkflowProvidableValues: providableValues}});

        render(
            <AutomationProjectConfigurationDialogWorkflowsStepItem
                environmentId="2"
                onWorkflowDraftChange={vi.fn()}
                projectId="7"
                projectVersion={3}
                workflow={workflow}
            />
        );

        expect(projectQueryMock).toHaveBeenCalledWith(
            {environmentId: '2', projectId: '7', projectVersion: 3, workflowUuid: 'rate-uuid'},
            {enabled: true}
        );
        expect(configurationQueryMock).toHaveBeenCalledWith(expect.anything(), {enabled: false});
        expect(screen.getByRole('tab', {name: /Inputs\s*\(1\)/})).toBeInTheDocument();
    });

    it('reads the providable values of the configuration when it exists', () => {
        configurationQueryMock.mockReturnValue({
            data: {automationProjectConfigurationWorkflowProvidableValues: providableValues},
        });
        projectQueryMock.mockReturnValue({data: undefined});

        render(
            <AutomationProjectConfigurationDialogWorkflowsStepItem
                configurationId="5"
                environmentId="2"
                onWorkflowDraftChange={vi.fn()}
                projectId="7"
                workflow={workflow}
            />
        );

        expect(projectQueryMock).toHaveBeenCalledWith(expect.anything(), {enabled: false});
        expect(screen.getByRole('tab', {name: /Inputs\s*\(1\)/})).toBeInTheDocument();
    });
});
