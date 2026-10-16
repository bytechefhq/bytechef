import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';

const MAX_VISIBLE_ICONS = 4;

interface ComponentIconI {
    icon?: string;
    name?: string;
    title?: string;
}

interface ComponentIconStackProps {
    components?: ComponentIconI[];
}

const ICON_CLASS_NAME = 'flex size-9 shrink-0 items-center justify-center rounded-full border bg-background p-1.5';

const ComponentIconStack = ({components = []}: ComponentIconStackProps) => {
    if (!components.length) {
        return null;
    }

    const overflowing = components.length > MAX_VISIBLE_ICONS;
    const visibleComponents = overflowing ? components.slice(0, MAX_VISIBLE_ICONS - 1) : components;
    const hiddenComponents = overflowing ? components.slice(MAX_VISIBLE_ICONS - 1) : [];

    return (
        <div className="mr-auto flex items-center -space-x-2">
            {visibleComponents.map((component) => (
                <div
                    className={ICON_CLASS_NAME}
                    key={`component-${component.name}`}
                    title={component.title || component.name}
                >
                    {component.icon && <InlineSVG className="size-5 flex-none" src={component.icon} />}
                </div>
            ))}

            {overflowing && (
                <Tooltip>
                    <TooltipTrigger asChild>
                        <div className={twMerge(ICON_CLASS_NAME, 'text-xs font-medium text-content-neutral-secondary')}>
                            +{hiddenComponents.length}
                        </div>
                    </TooltipTrigger>

                    <TooltipContent>
                        <ul>
                            {hiddenComponents.map((component) => (
                                <li key={`hidden-component-${component.name}`}>{component.title || component.name}</li>
                            ))}
                        </ul>
                    </TooltipContent>
                </Tooltip>
            )}
        </div>
    );
};

export default ComponentIconStack;
