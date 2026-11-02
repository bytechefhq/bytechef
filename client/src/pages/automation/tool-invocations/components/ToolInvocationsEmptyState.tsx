import Button from '@/components/Button/Button';
import EmptyState from '@/components/EmptyState/EmptyState';
import JourneySteps, {type JourneyStepI} from '@/components/JourneySteps/JourneySteps';
import {useEnvironmentScopeNote} from '@/shared/hooks/useEnvironmentScopeNote';
import {ActivityIcon} from 'lucide-react';
import {useMemo} from 'react';
import {useNavigate} from 'react-router-dom';

export interface ToolInvocationsConnectStepI {
    description: string;
    id: string;
    label: string;
}

interface ToolInvocationsEmptyStateProps {
    connectStep: ToolInvocationsConnectStepI;
    createMcpServerDescription: string;
    description: string;
    documentationUrl: string;
    hasMcpServer: boolean;
    mcpServersPath: string;
}

/*
 * Tool calls are only recorded once an AI client talks to an MCP server, so a new user is shown what is left: creating
 * a server in this environment, then connecting an AI client to it. Automation and embedded differ only in wording and
 * where the MCP Servers page lives.
 */
const ToolInvocationsEmptyState = ({
    connectStep,
    createMcpServerDescription,
    description,
    documentationUrl,
    hasMcpServer,
    mcpServersPath,
}: ToolInvocationsEmptyStateProps) => {
    const navigate = useNavigate();

    const environmentScopeNote = useEnvironmentScopeNote('Tool invocations');

    const steps: JourneyStepI[] = useMemo(() => {
        const goToMcpServersButton = (
            <Button label="Go to MCP Servers" onClick={() => void navigate(mcpServersPath)} size="sm" />
        );

        return [
            {
                ...(!hasMcpServer && {
                    action: goToMcpServersButton,
                    description: createMcpServerDescription,
                }),
                id: 'createMcpServer',
                label: 'Create an MCP server',
                status: hasMcpServer ? 'complete' : 'current',
            },
            {
                ...(hasMcpServer && {
                    action: goToMcpServersButton,
                    description: connectStep.description,
                }),
                id: connectStep.id,
                label: connectStep.label,
                status: hasMcpServer ? 'current' : 'upcoming',
            },
        ];
    }, [connectStep, createMcpServerDescription, hasMcpServer, mcpServersPath, navigate]);

    return (
        <EmptyState
            description={description}
            docsLink={{href: documentationUrl, label: 'How tool invocations work'}}
            icon={<ActivityIcon />}
            note={environmentScopeNote}
            title="No tool calls yet"
        >
            <JourneySteps aria-label="Getting started" className="max-w-md" orientation="vertical" steps={steps} />
        </EmptyState>
    );
};

export default ToolInvocationsEmptyState;
