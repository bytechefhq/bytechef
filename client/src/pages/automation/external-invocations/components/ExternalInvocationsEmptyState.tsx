import Button from '@/components/Button/Button';
import EmptyState from '@/components/EmptyState/EmptyState';
import JourneySteps, {type JourneyStepI} from '@/components/JourneySteps/JourneySteps';
import {useEnvironmentScopeNote} from '@/shared/hooks/useEnvironmentScopeNote';
import {HammerIcon} from 'lucide-react';
import {useMemo} from 'react';
import {useNavigate} from 'react-router-dom';

export interface ExternalInvocationsConnectStepI {
    description: string;
    id: string;
    label: string;
}

interface ExternalInvocationsEmptyStateProps {
    connectStep: ExternalInvocationsConnectStepI;
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
const ExternalInvocationsEmptyState = ({
    connectStep,
    createMcpServerDescription,
    description,
    documentationUrl,
    hasMcpServer,
    mcpServersPath,
}: ExternalInvocationsEmptyStateProps) => {
    const navigate = useNavigate();

    const environmentScopeNote = useEnvironmentScopeNote('External invocations');

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
            docsLink={{href: documentationUrl, label: 'How external invocations work'}}
            icon={<HammerIcon />}
            note={environmentScopeNote}
            title="No tool calls yet"
        >
            <JourneySteps aria-label="Getting started" className="max-w-md" orientation="vertical" steps={steps} />
        </EmptyState>
    );
};

export default ExternalInvocationsEmptyState;
