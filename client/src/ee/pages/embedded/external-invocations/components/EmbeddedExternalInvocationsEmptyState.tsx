import {EMBEDDED_EXTERNAL_INVOCATIONS_DOCUMENTATION_URL} from '@/ee/pages/embedded/embeddedDocumentationUrls';
import ExternalInvocationsEmptyState, {
    type ExternalInvocationsConnectStepI,
} from '@/pages/automation/external-invocations/components/ExternalInvocationsEmptyState';

const CONNECT_AI_AGENT_STEP: ExternalInvocationsConnectStepI = {
    description: "Connected users add the server's URL to their AI agent.",
    id: 'connectAiAgent',
    label: 'Connect an AI agent',
};

interface EmbeddedExternalInvocationsEmptyStateProps {
    hasMcpServer: boolean;
}

/*
 * Embedded tool calls come from connected users' AI agents through an embedded MCP server, or from the Embedded API.
 * The API needs no setup on this side, so the guide covers the MCP path.
 */
const EmbeddedExternalInvocationsEmptyState = ({hasMcpServer}: EmbeddedExternalInvocationsEmptyStateProps) => (
    <ExternalInvocationsEmptyState
        connectStep={CONNECT_AI_AGENT_STEP}
        createMcpServerDescription="Pick the integration components to expose as tools."
        description="Each time a connected user's AI agent calls a tool on an MCP server, or your backend runs an action or tool through the Embedded API, the call is recorded here with its outcome and duration. Inputs and outputs are never stored. The Embedded API needs no setup here; for MCP, two steps:"
        documentationUrl={EMBEDDED_EXTERNAL_INVOCATIONS_DOCUMENTATION_URL}
        hasMcpServer={hasMcpServer}
        mcpServersPath="/embedded/mcp-servers"
    />
);

export default EmbeddedExternalInvocationsEmptyState;
