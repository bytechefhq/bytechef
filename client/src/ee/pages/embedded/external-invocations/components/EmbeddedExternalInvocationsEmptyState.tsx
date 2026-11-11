import {
    EMBEDDED_COMPONENT_KIT_DOCUMENTATION_URL,
    EMBEDDED_EXTERNAL_INVOCATIONS_DOCUMENTATION_URL,
} from '@/ee/pages/embedded/embeddedDocumentationUrls';
import ExternalInvocationsEmptyState, {
    type ExternalInvocationsApiPathI,
    type ExternalInvocationsConnectStepI,
} from '@/pages/automation/external-invocations/components/ExternalInvocationsEmptyState';

const EMBEDDED_API_PATH: ExternalInvocationsApiPathI = {
    actionLabel: 'Open Getting Started',
    description:
        'Your backend runs an action or tool for a connected user. Nothing to set up here: create an API key and send one call. Getting Started has a ready-to-run curl.',
    documentationLabel: 'Component Kit',
    documentationUrl: EMBEDDED_COMPONENT_KIT_DOCUMENTATION_URL,
    path: '/embedded/getting-started',
    title: 'Through the Embedded API',
};

const CONNECT_AI_AGENT_STEP: ExternalInvocationsConnectStepI = {
    description: "Connected users add the server's URL to their AI agent.",
    id: 'connectAiAgent',
    label: 'Connect an AI agent',
};

interface EmbeddedExternalInvocationsEmptyStateProps {
    hasMcpServer: boolean;
}

const EmbeddedExternalInvocationsEmptyState = ({hasMcpServer}: EmbeddedExternalInvocationsEmptyStateProps) => (
    <ExternalInvocationsEmptyState
        apiPath={EMBEDDED_API_PATH}
        connectStep={CONNECT_AI_AGENT_STEP}
        createMcpServerDescription="Pick the integration components to expose as tools."
        description="Each time a connected user's AI agent calls a tool on an MCP server, or your backend runs an action or tool through the Embedded API, the call is recorded here with its outcome and duration. Inputs and outputs are never stored. Calls arrive one of two ways:"
        documentationUrl={EMBEDDED_EXTERNAL_INVOCATIONS_DOCUMENTATION_URL}
        hasMcpServer={hasMcpServer}
        mcpServersPath="/embedded/mcp-servers"
    />
);

export default EmbeddedExternalInvocationsEmptyState;
