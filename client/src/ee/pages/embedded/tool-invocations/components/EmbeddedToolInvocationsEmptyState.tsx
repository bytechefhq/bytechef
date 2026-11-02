import {EMBEDDED_TOOL_INVOCATIONS_DOCUMENTATION_URL} from '@/ee/pages/embedded/embeddedDocumentationUrls';
import ToolInvocationsEmptyState, {
    type ToolInvocationsConnectStepI,
} from '@/pages/automation/tool-invocations/components/ToolInvocationsEmptyState';

const CONNECT_AI_AGENT_STEP: ToolInvocationsConnectStepI = {
    description: "Connected users add the server's URL to their AI agent.",
    id: 'connectAiAgent',
    label: 'Connect an AI agent',
};

interface EmbeddedToolInvocationsEmptyStateProps {
    hasMcpServer: boolean;
}

/*
 * Embedded tool calls come from connected users' AI agents through an embedded MCP server, or from the Embedded API.
 * The API needs no setup on this side, so the guide covers the MCP path.
 */
const EmbeddedToolInvocationsEmptyState = ({hasMcpServer}: EmbeddedToolInvocationsEmptyStateProps) => (
    <ToolInvocationsEmptyState
        connectStep={CONNECT_AI_AGENT_STEP}
        createMcpServerDescription="Pick the integration components to expose as tools."
        description="Each time a connected user's AI agent calls a tool on an MCP server, or your backend runs an action or tool through the Embedded API, the call is recorded here with its outcome and duration. Inputs and outputs are never stored. The Embedded API needs no setup here; for MCP, two steps:"
        documentationUrl={EMBEDDED_TOOL_INVOCATIONS_DOCUMENTATION_URL}
        hasMcpServer={hasMcpServer}
        mcpServersPath="/embedded/mcp-servers"
    />
);

export default EmbeddedToolInvocationsEmptyState;
