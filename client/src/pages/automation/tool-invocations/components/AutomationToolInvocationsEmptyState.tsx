import Button from '@/components/Button/Button';
import EmptyState from '@/components/EmptyState/EmptyState';
import JourneySteps, {type JourneyStepI} from '@/components/JourneySteps/JourneySteps';
import {TOOL_INVOCATIONS_DOCUMENTATION_URL} from '@/pages/automation/journey/automationJourneySteps';
import {useEnvironmentScopeNote} from '@/shared/hooks/useEnvironmentScopeNote';
import {ActivityIcon} from 'lucide-react';
import {useMemo} from 'react';
import {useNavigate} from 'react-router-dom';

interface AutomationToolInvocationsEmptyStateProps {
    hasMcpServer: boolean;
}

/*
 * Tool calls are only recorded once an AI client talks to an MCP server, so a new user is shown what is left: creating
 * a server in this environment, then pointing an AI client at it.
 */
const AutomationToolInvocationsEmptyState = ({hasMcpServer}: AutomationToolInvocationsEmptyStateProps) => {
    const navigate = useNavigate();

    const environmentScopeNote = useEnvironmentScopeNote('Tool invocations');

    const steps: JourneyStepI[] = useMemo(() => {
        const goToMcpServersButton = (
            <Button label="Go to MCP Servers" onClick={() => void navigate('/automation/mcp-servers')} size="sm" />
        );

        return [
            {
                ...(!hasMcpServer && {
                    action: goToMcpServersButton,
                    description: 'Pick the component actions and workflows to expose as tools.',
                }),
                id: 'createMcpServer',
                label: 'Create an MCP server',
                status: hasMcpServer ? 'complete' : 'current',
            },
            {
                ...(hasMcpServer && {
                    action: goToMcpServersButton,
                    description: "Add the server's URL to Claude, Cursor or another MCP client.",
                }),
                id: 'connectAiClient',
                label: 'Connect an AI client',
                status: hasMcpServer ? 'current' : 'upcoming',
            },
        ];
    }, [hasMcpServer, navigate]);

    return (
        <EmptyState
            description="Each time an AI client calls a tool on one of your MCP servers, the call is recorded here with its outcome and duration. Tool inputs and outputs are never stored. Two steps to get there:"
            docsLink={{href: TOOL_INVOCATIONS_DOCUMENTATION_URL, label: 'How tool invocations work'}}
            icon={<ActivityIcon />}
            note={environmentScopeNote}
            title="No tool calls yet"
        >
            <JourneySteps aria-label="Getting started" className="max-w-md" orientation="vertical" steps={steps} />
        </EmptyState>
    );
};

export default AutomationToolInvocationsEmptyState;
