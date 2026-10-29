import {ComponentDefinition} from '@/shared/middleware/platform/configuration';
import {ComponentIcon, WrenchIcon} from 'lucide-react';
import InlineSVG from 'react-inlinesvg';

interface AgentToolRowLabelProps {
    actionName: string;
    componentDefinition?: ComponentDefinition;
    componentName: string;
}

const AgentToolRowLabel = ({actionName, componentDefinition, componentName}: AgentToolRowLabelProps) => {
    const action = componentDefinition?.actions?.find((candidate) => candidate.name === actionName);

    return (
        <div className="flex min-w-0 items-center gap-2">
            <span className="flex size-5 flex-none items-center justify-center">
                {componentDefinition?.icon ? (
                    <InlineSVG
                        className="size-5"
                        loader={<ComponentIcon className="size-5 text-content-neutral-primary" />}
                        src={componentDefinition.icon}
                    />
                ) : (
                    <WrenchIcon aria-hidden className="size-3.5 text-content-neutral-tertiary" />
                )}
            </span>

            <span className="truncate font-medium">
                {componentDefinition?.title || componentName}

                <span className="text-content-neutral-tertiary"> / </span>

                {action?.title || actionName}
            </span>
        </div>
    );
};

export default AgentToolRowLabel;
