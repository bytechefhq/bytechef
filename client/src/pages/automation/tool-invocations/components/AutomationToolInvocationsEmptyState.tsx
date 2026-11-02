import {TOOL_INVOCATIONS_DOCUMENTATION_URL} from '@/pages/automation/journey/automationJourneySteps';
import ToolInvocationsEmptyState, {
    type ToolInvocationsConnectStepI,
} from '@/pages/automation/tool-invocations/components/ToolInvocationsEmptyState';

const CONNECT_AI_CLIENT_STEP: ToolInvocationsConnectStepI = {
    description: "Add the server's URL to Claude, Cursor or another MCP client.",
    id: 'connectAiClient',
    label: 'Connect an AI client',
};

interface AutomationToolInvocationsEmptyStateProps {
    hasMcpServer: boolean;
}

const AutomationToolInvocationsEmptyState = ({hasMcpServer}: AutomationToolInvocationsEmptyStateProps) => (
    <ToolInvocationsEmptyState
        connectStep={CONNECT_AI_CLIENT_STEP}
        createMcpServerDescription="Pick the component actions and workflows to expose as tools."
        description="Each time an AI client calls a tool on one of your MCP servers, the call is recorded here with its outcome and duration. Tool inputs and outputs are never stored. Two steps to get there:"
        documentationUrl={TOOL_INVOCATIONS_DOCUMENTATION_URL}
        hasMcpServer={hasMcpServer}
        mcpServersPath="/automation/mcp-servers"
    />
);

export default AutomationToolInvocationsEmptyState;
