import ExternalInvocationsEmptyState, {
    type ExternalInvocationsConnectStepI,
} from '@/pages/automation/external-invocations/components/ExternalInvocationsEmptyState';
import {EXTERNAL_INVOCATIONS_DOCUMENTATION_URL} from '@/pages/automation/journey/automationJourneySteps';

const CONNECT_AI_CLIENT_STEP: ExternalInvocationsConnectStepI = {
    description: "Add the server's URL to Claude, Cursor or another MCP client.",
    id: 'connectAiClient',
    label: 'Connect an AI client',
};

interface AutomationExternalInvocationsEmptyStateProps {
    hasMcpServer: boolean;
}

const AutomationExternalInvocationsEmptyState = ({hasMcpServer}: AutomationExternalInvocationsEmptyStateProps) => (
    <ExternalInvocationsEmptyState
        connectStep={CONNECT_AI_CLIENT_STEP}
        createMcpServerDescription="Pick the component actions and workflows to expose as tools."
        description="Each time an AI client calls a tool on one of your MCP servers, the call is recorded here with its outcome and duration. Tool inputs and outputs are never stored. Two steps to get there:"
        documentationUrl={EXTERNAL_INVOCATIONS_DOCUMENTATION_URL}
        hasMcpServer={hasMcpServer}
        mcpServersPath="/automation/mcp-servers"
    />
);

export default AutomationExternalInvocationsEmptyState;
