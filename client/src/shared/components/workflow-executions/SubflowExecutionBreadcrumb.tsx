import Button from '@/components/Button/Button';
import {
    Breadcrumb,
    BreadcrumbEllipsis,
    BreadcrumbItem,
    BreadcrumbList,
    BreadcrumbPage,
    BreadcrumbSeparator,
} from '@/components/ui/breadcrumb';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ChevronLeftIcon, WorkflowIcon} from 'lucide-react';
import {CSSProperties, useEffect, useRef, useState} from 'react';

interface TruncatedLabelProps {
    className?: string;
    label: string;
    style?: CSSProperties;
}

export const TruncatedLabel = ({className, label, style}: TruncatedLabelProps) => {
    const [isTruncated, setIsTruncated] = useState(false);

    const labelRef = useRef<HTMLSpanElement>(null);

    useEffect(() => {
        const element = labelRef.current;

        if (element) {
            setIsTruncated(element.scrollWidth > element.clientWidth);
        }
    }, [label]);

    const labelSpan = (
        <span
            className={className}
            ref={labelRef}
            style={{display: 'block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', ...style}}
        >
            {label}
        </span>
    );

    if (!isTruncated) {
        return labelSpan;
    }

    return (
        <Tooltip>
            <TooltipTrigger asChild>{labelSpan}</TooltipTrigger>

            <TooltipContent>{label}</TooltipContent>
        </Tooltip>
    );
};

export interface BreadcrumbEntryI {
    label: string;
    onNavigate?: () => void;
}

const SubflowExecutionBreadcrumb = ({items, onBackClick}: {items: BreadcrumbEntryI[]; onBackClick: () => void}) => {
    const [containerWidth, setContainerWidth] = useState(0);

    const containerRef = useRef<HTMLDivElement>(null);

    const hasEllipsis = items.length > 2;
    const numSeparators = items.length > 1 ? (hasEllipsis ? 2 : 1) : 0;
    const numTextItems = items.length === 1 ? 1 : 2;
    const fixedWidth = 12 + 12 + 16 + numSeparators * 10 + (hasEllipsis ? 24 : 0);
    const maxItemWidth = Math.max(60, (containerWidth - fixedWidth) / numTextItems);

    const firstItem = items[0];
    const lastItem = items[items.length - 1];
    const middleItems = items.slice(1, -1);

    useEffect(() => {
        const container = containerRef.current;

        if (!container) {
            return;
        }

        const observer = new ResizeObserver(([entry]) => {
            setContainerWidth(entry.contentRect.width);
        });

        observer.observe(container);

        return () => observer.disconnect();
    }, []);

    return (
        <div className="flex h-9 items-center overflow-hidden bg-surface-neutral-primary px-3 py-2" ref={containerRef}>
            <Button
                className="text-content-neutral-tertiary hover:bg-transparent hover:text-content-neutral-primary active:bg-transparent active:text-content-neutral-primary"
                icon={<ChevronLeftIcon className="size-3 shrink-0" />}
                onClick={onBackClick}
                size="iconXs"
                variant="ghost"
            />

            <Breadcrumb>
                <BreadcrumbList className="flex-nowrap gap-1 sm:gap-1">
                    <BreadcrumbItem>
                        <Button
                            className="text-content-neutral-tertiary hover:bg-transparent hover:text-content-neutral-primary active:bg-transparent active:text-content-neutral-primary"
                            icon={<WorkflowIcon className="size-3 shrink-0" />}
                            onClick={firstItem?.onNavigate}
                            size="xxs"
                            variant="ghost"
                        >
                            {firstItem && (
                                <TruncatedLabel
                                    className="text-xs leading-4 font-medium"
                                    label={firstItem.label}
                                    style={{maxWidth: maxItemWidth}}
                                />
                            )}
                        </Button>
                    </BreadcrumbItem>

                    {items.length > 2 && (
                        <>
                            <BreadcrumbSeparator>
                                <span className="text-content-neutral-tertiary">/</span>
                            </BreadcrumbSeparator>

                            <BreadcrumbItem>
                                <DropdownMenu>
                                    <DropdownMenuTrigger asChild>
                                        <span className="cursor-pointer text-content-neutral-tertiary hover:text-content-neutral-primary">
                                            <BreadcrumbEllipsis className="size-3" />
                                        </span>
                                    </DropdownMenuTrigger>

                                    <DropdownMenuContent align="start">
                                        {middleItems.map((item) => (
                                            <DropdownMenuItem key={item.label} onClick={item.onNavigate}>
                                                {item.label}
                                            </DropdownMenuItem>
                                        ))}
                                    </DropdownMenuContent>
                                </DropdownMenu>
                            </BreadcrumbItem>
                        </>
                    )}

                    {items.length > 1 && (
                        <>
                            <BreadcrumbSeparator>
                                <span className="text-content-neutral-tertiary">/</span>
                            </BreadcrumbSeparator>

                            <BreadcrumbItem>
                                <BreadcrumbPage>
                                    <TruncatedLabel
                                        className="text-xs leading-4 font-medium text-content-neutral-primary"
                                        label={lastItem.label}
                                        style={{maxWidth: maxItemWidth}}
                                    />
                                </BreadcrumbPage>
                            </BreadcrumbItem>
                        </>
                    )}
                </BreadcrumbList>
            </Breadcrumb>
        </div>
    );
};

export default SubflowExecutionBreadcrumb;
