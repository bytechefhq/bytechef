import Button from '@/components/Button/Button';
import EmptyState from '@/components/EmptyState/EmptyState';
import JourneySteps, {type JourneyStepI} from '@/components/JourneySteps/JourneySteps';
import {EMBEDDED_TOOL_INVOCATIONS_DOCUMENTATION_URL} from '@/ee/pages/embedded/embeddedDocumentationUrls';
import {useEnvironmentScopeNote} from '@/shared/hooks/useEnvironmentScopeNote';
import {ActivityIcon} from 'lucide-react';
import {useMemo} from 'react';
import {useNavigate} from 'react-router-dom';

interface EmbeddedToolInvocationsEmptyStateProps {
    hasMcpServer: boolean;
}

/*
 * Embedded tool calls come from connected users' AI agents through an embedded MCP server, or from the Embedded API.
 * The API needs no setup on this side, so the guide covers the MCP path: creating a server in this environment, then
 * connected users pointing their AI agents at it.
 */
const EmbeddedToolInvocationsEmptyState = ({hasMcpServer}: EmbeddedToolInvocationsEmptyStateProps) => {
    const navigate = useNavigate();

    const environmentScopeNote = useEnvironmentScopeNote('Tool invocations');

    const steps: JourneyStepI[] = useMemo(() => {
        const goToMcpServersButton = (
            <Button label="Go to MCP Servers" onClick={() => void navigate('/embedded/mcp-servers')} size="sm" />
        );

        return [
            {
                ...(!hasMcpServer && {
                    action: goToMcpServersButton,
                    description: 'Pick the integration components to expose as tools.',
                }),
                id: 'createMcpServer',
                label: 'Create an MCP server',
                status: hasMcpServer ? 'complete' : 'current',
            },
            {
                ...(hasMcpServer && {
                    action: goToMcpServersButton,
                    description: "Connected users add the server's URL to their AI agent.",
                }),
                id: 'connectAiAgent',
                label: 'Connect an AI agent',
                status: hasMcpServer ? 'current' : 'upcoming',
            },
        ];
    }, [hasMcpServer, navigate]);

    return (
        <EmptyState
            description="Each time a connected user's AI agent calls a tool on an MCP server, or your backend runs an action or tool through the Embedded API, the call is recorded here with its outcome and duration. Inputs and outputs are never stored. The Embedded API needs no setup here; for MCP, two steps:"
            docsLink={{href: EMBEDDED_TOOL_INVOCATIONS_DOCUMENTATION_URL, label: 'How tool invocations work'}}
            icon={<ActivityIcon />}
            note={environmentScopeNote}
            title="No tool calls yet"
        >
            <JourneySteps aria-label="Getting started" className="max-w-md" orientation="vertical" steps={steps} />
        </EmptyState>
    );
};

export default EmbeddedToolInvocationsEmptyState;
