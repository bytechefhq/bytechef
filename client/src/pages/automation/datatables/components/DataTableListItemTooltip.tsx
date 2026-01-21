import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';

interface DataTableListItemTooltipProps {
    lastModifiedDate?: number | null;
}

const DataTableListItemTooltip = ({lastModifiedDate}: DataTableListItemTooltipProps) => {
    const formattedLastModifiedDate = lastModifiedDate ? new Date(lastModifiedDate).toLocaleString() : null;

    return (
        <Tooltip>
            <TooltipTrigger>
                <div className="flex items-center text-sm text-muted-foreground sm:mt-0">
                    <span className="text-xs">{`Last modified: ${formattedLastModifiedDate ?? 'N/A'}`}</span>
                </div>
            </TooltipTrigger>

            <TooltipContent>{formattedLastModifiedDate ?? 'Last Modified Date'}</TooltipContent>
        </Tooltip>
    );
};

export default DataTableListItemTooltip;
