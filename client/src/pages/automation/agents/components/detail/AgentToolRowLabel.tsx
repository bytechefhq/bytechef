import {ComponentDefinition} from '@/shared/middleware/platform/configuration';
import {ComponentIcon, WrenchIcon} from 'lucide-react';
import InlineSVG from 'react-inlinesvg';

interface AgentToolRowLabelProps {
    actionName: string;
    componentDefinition?: ComponentDefinition;
    componentName: string;
}

/**
 * The component logo and human titles for one attached tool — "Airtable / Create Record" rather than the raw
 * "airtable / createRecord" stored on the element row.
 *
 * The definition arrives from {@link AgentToolRow}, which resolves it once per row and also reads `clusterRoot`
 * from it to decide whether the row is configurable. Resolving it here as well would split one row's decision
 * across two components for no gain — the card's own list query cannot supply it either, being gated on the Add
 * Tool dialog being open, and the action TITLE only comes with the full component definition.
 *
 * Falls back to the stored names while the definition loads or if it cannot be resolved — a tool whose component
 * was removed still has to render something.
 */
const AgentToolRowLabel = ({actionName, componentDefinition, componentName}: AgentToolRowLabelProps) => {
    const action = componentDefinition?.actions?.find((candidate) => candidate.name === actionName);

    return (
        <div className="flex min-w-0 items-center gap-2">
            <span className="flex size-5 flex-none items-center justify-center">
                {componentDefinition?.icon ? (
                    <InlineSVG
                        className="size-5"
                        loader={<ComponentIcon className="size-5 text-gray-700" />}
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
