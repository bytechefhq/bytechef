import Badge from '@/components/Badge/Badge';
import {buttonVariants} from '@/components/ui/button';
import McpServerConfigurationCode from '@/shared/components/mcp-server/McpServerConfigurationCode';
import {ArrowRightIcon} from 'lucide-react';
import {Fragment} from 'react';
import {twMerge} from 'tailwind-merge';

import {McpConnectStepI} from './mcpConnectClients';

interface McpConnectStepPropsI {
    mcpServerUrl: string;
    number: number;
    onRefresh?: () => void;
    step: McpConnectStepI;
}

const McpConnectStep = ({mcpServerUrl, number, onRefresh, step}: McpConnectStepPropsI) => (
    <li className="flex items-start gap-4">
        <div className="flex size-7 shrink-0 items-center justify-center rounded-full bg-muted text-sm font-semibold text-muted-foreground">
            {number}
        </div>

        <div className="min-w-0 flex-1 space-y-3">
            <h3 className="font-semibold text-foreground">{step.title}</h3>

            {step.path && (
                <div className="flex flex-wrap items-center gap-2 text-sm text-muted-foreground">
                    {step.path.map((pathItem, index) => (
                        <Fragment key={pathItem}>
                            {index > 0 && <span aria-hidden="true">→</span>}

                            <Badge label={pathItem} styleType="outline-outline" />
                        </Fragment>
                    ))}
                </div>
            )}

            {step.description && <p className="text-sm text-muted-foreground">{step.description}</p>}

            {step.action && (
                <a className={twMerge(buttonVariants(), 'gap-2')} href={step.action.href}>
                    {step.action.label}

                    <ArrowRightIcon className="size-4" />
                </a>
            )}

            {step.codeSnippet && (
                <McpServerConfigurationCode
                    codeSnippet={step.codeSnippet}
                    onRefresh={step.codeSnippet === mcpServerUrl ? onRefresh : undefined}
                />
            )}
        </div>
    </li>
);

export default McpConnectStep;
