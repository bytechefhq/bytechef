import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuLabel,
    DropdownMenuRadioGroup,
    DropdownMenuRadioItem,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ChevronDownIcon, type LucideIcon} from 'lucide-react';
import {ReactNode} from 'react';

export interface BreadcrumbTitleSelectItemI {
    disabled?: boolean;
    id: string;
    name: string;
    publishedVersion?: number;
}

interface BreadcrumbTitleSelectProps {
    ariaLabel: string;
    badge: ReactNode;
    currentId?: string;
    icon: LucideIcon;
    items?: BreadcrumbTitleSelectItemI[];
    menuLabel: string;
    name: string;
    onValueChange?: (id: string) => void;
}

const BreadcrumbTitleSelect = ({
    ariaLabel,
    badge,
    currentId,
    icon: Icon,
    items,
    menuLabel,
    name,
    onValueChange,
}: BreadcrumbTitleSelectProps) => {
    if (!onValueChange || !items || items.length === 0) {
        return (
            <div className="flex max-w-96 min-w-0 items-center space-x-2">
                <Tooltip>
                    <TooltipTrigger asChild>
                        <h1 className="truncate">{name}</h1>
                    </TooltipTrigger>

                    {name.length > 43 && <TooltipContent>{name}</TooltipContent>}
                </Tooltip>

                {badge}
            </div>
        );
    }

    return (
        <DropdownMenu>
            <Tooltip>
                <TooltipTrigger asChild>
                    <DropdownMenuTrigger
                        aria-label={ariaLabel}
                        className="-ml-1.5 flex max-w-96 min-w-0 items-center gap-2 rounded-md px-1.5 py-1 text-content-neutral-primary outline-hidden hover:bg-surface-neutral-primary-hover data-[state=open]:bg-surface-neutral-primary-hover"
                    >
                        <h1 className="truncate">{name}</h1>

                        {badge}

                        <ChevronDownIcon className="size-4 shrink-0 text-content-neutral-secondary" />
                    </DropdownMenuTrigger>
                </TooltipTrigger>

                {name.length > 43 && <TooltipContent>{name}</TooltipContent>}
            </Tooltip>

            <DropdownMenuContent align="start" className="max-h-96 w-80 overflow-y-auto">
                <DropdownMenuLabel>{menuLabel}</DropdownMenuLabel>

                <DropdownMenuRadioGroup onValueChange={onValueChange} value={currentId ?? ''}>
                    {items.map((item) => (
                        <DropdownMenuRadioItem
                            className="pl-2 data-[state=checked]:bg-surface-brand-secondary data-[state=checked]:text-content-brand-primary [&>span:first-child]:hidden"
                            disabled={item.disabled}
                            key={item.id}
                            title={item.name.length > 30 ? item.name : undefined}
                            value={item.id}
                        >
                            <Icon className="size-4 shrink-0" />

                            <span className="truncate">{item.name}</span>

                            {item.publishedVersion !== undefined && (
                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <span className="ml-auto flex size-4 shrink-0 items-center justify-center">
                                            <span
                                                aria-hidden="true"
                                                className="size-2 rounded-full bg-surface-success-primary"
                                            />

                                            <span className="sr-only">Published V{item.publishedVersion}</span>
                                        </span>
                                    </TooltipTrigger>

                                    <TooltipContent side="right">Published V{item.publishedVersion}</TooltipContent>
                                </Tooltip>
                            )}
                        </DropdownMenuRadioItem>
                    ))}
                </DropdownMenuRadioGroup>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default BreadcrumbTitleSelect;

export const getPublishedVersion = (
    lastPublishedDate: Date | undefined,
    lastVersion: number | undefined,
    lastVersionPublished: boolean
): number | undefined => {
    if (!lastPublishedDate || lastVersion === undefined) {
        return undefined;
    }

    return lastVersionPublished ? lastVersion : lastVersion - 1;
};
