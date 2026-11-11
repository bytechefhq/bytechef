import Button from '@/components/Button/Button';
import EmptyState from '@/components/EmptyState/EmptyState';
import JourneySteps, {type JourneyStepI} from '@/components/JourneySteps/JourneySteps';
import {useEnvironmentScopeNote} from '@/shared/hooks/useEnvironmentScopeNote';
import {BookOpenIcon, HammerIcon} from 'lucide-react';
import {useMemo} from 'react';
import {useNavigate} from 'react-router-dom';

export interface ExternalInvocationsApiPathI {
    actionLabel: string;
    description: string;
    documentationLabel: string;
    documentationUrl: string;
    path: string;
    title: string;
}

export interface ExternalInvocationsConnectStepI {
    description: string;
    id: string;
    label: string;
}

interface ExternalInvocationsEmptyStateProps {
    apiPath?: ExternalInvocationsApiPathI;
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
const PATH_CARD_CLASS_NAME = 'flex flex-col gap-3 rounded-md border border-stroke-neutral-secondary p-4 text-left';

const ExternalInvocationsEmptyState = ({
    apiPath,
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
            {apiPath ? (
                <div className="flex w-full max-w-xl flex-col gap-3 text-left">
                    <section aria-labelledby="external-invocations-mcp-path" className={PATH_CARD_CLASS_NAME}>
                        <h3 className="text-sm font-semibold" id="external-invocations-mcp-path">
                            Through an MCP server
                        </h3>

                        <JourneySteps aria-label="Getting started" orientation="vertical" steps={steps} />
                    </section>

                    <div aria-hidden="true" className="flex items-center gap-3 text-xs text-content-neutral-secondary">
                        <span className="h-px flex-1 bg-stroke-neutral-secondary" />

                        <span>or</span>

                        <span className="h-px flex-1 bg-stroke-neutral-secondary" />
                    </div>

                    <section aria-labelledby="external-invocations-api-path" className={PATH_CARD_CLASS_NAME}>
                        <h3 className="text-sm font-semibold" id="external-invocations-api-path">
                            {apiPath.title}
                        </h3>

                        <p className="text-sm text-content-neutral-secondary">{apiPath.description}</p>

                        <div className="flex flex-wrap items-center gap-4">
                            <Button
                                label={apiPath.actionLabel}
                                onClick={() => void navigate(apiPath.path)}
                                size="sm"
                                variant="outline"
                            />

                            <a
                                className="inline-flex items-center gap-1 text-sm font-medium text-content-brand-primary hover:underline"
                                href={apiPath.documentationUrl}
                                rel="noopener noreferrer"
                                target="_blank"
                            >
                                <BookOpenIcon aria-hidden="true" className="size-4" />

                                {apiPath.documentationLabel}
                            </a>
                        </div>
                    </section>
                </div>
            ) : (
                <JourneySteps
                    aria-label="Getting started"
                    className="w-full max-w-xl"
                    orientation="vertical"
                    steps={steps}
                />
            )}
        </EmptyState>
    );
};

export default ExternalInvocationsEmptyState;
