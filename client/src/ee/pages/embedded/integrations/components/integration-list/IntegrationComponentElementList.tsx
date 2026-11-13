import Button from '@/components/Button/Button';
import EmptyState from '@/components/EmptyState/EmptyState';
import LoadingIcon from '@/components/LoadingIcon';
import {Accordion, AccordionContent, AccordionItem, AccordionTrigger} from '@/components/ui/accordion';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {IntegrationComponentElementI} from '@/ee/pages/embedded/integrations/components/integration-list/integrationComponentElementUtils';
import {useCopyToClipboard} from '@uidotdev/usehooks';
import {AlertTriangleIcon, ClipboardIcon, WrenchIcon} from 'lucide-react';
import {ReactNode} from 'react';
import {toast} from 'sonner';

interface IntegrationComponentElementListProps {
    elements: IntegrationComponentElementI[];
    emptyIcon?: ReactNode;
    idLabel: string;
    isError: boolean;
    isLoading: boolean;
    noun: string;
    onRetry: () => void;
}

const IntegrationComponentElementList = ({
    elements,
    emptyIcon,
    idLabel,
    isError,
    isLoading,
    noun,
    onRetry,
}: IntegrationComponentElementListProps) => {
    const [, copyToClipboard] = useCopyToClipboard();

    const handleCopyIdClick = async (id: string) => {
        await copyToClipboard(id);

        toast(`${idLabel} copied to clipboard`);
    };

    if (isLoading) {
        return (
            <div className="flex items-center gap-2 py-4 text-sm text-muted-foreground">
                <LoadingIcon />

                <span>Loading {noun}...</span>
            </div>
        );
    }

    if (isError) {
        return (
            <EmptyState
                actions={<Button label="Retry" onClick={onRetry} variant="outline" />}
                description={`The ${noun} of this integration could not be loaded. Please try again.`}
                icon={<AlertTriangleIcon />}
                title={`Failed to load ${noun}`}
            />
        );
    }

    if (elements.length === 0) {
        return (
            <EmptyState
                description={`This integration's component does not provide any ${noun}.`}
                icon={emptyIcon || <WrenchIcon />}
                title={`No ${noun}`}
            />
        );
    }

    return (
        <Accordion
            className="rounded-md border border-border/50 [--radix-collapsible-content-height:auto]"
            type="multiple"
        >
            {elements.map((element) => (
                <AccordionItem className="border-border/50" key={element.id} value={element.id}>
                    <AccordionTrigger className="px-4 py-3 hover:no-underline">{element.title}</AccordionTrigger>

                    <AccordionContent className="px-4">
                        <dl className="grid grid-cols-[7rem_1fr] gap-x-4 gap-y-2 text-sm">
                            <dt className="text-muted-foreground">Description</dt>

                            <dd>{element.description || 'No description.'}</dd>

                            <dt className="text-muted-foreground">{idLabel}</dt>

                            <dd className="flex items-center gap-x-1">
                                <span className="font-mono text-xs leading-5 break-all">{element.id}</span>

                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <Button
                                            aria-label={`Copy ${idLabel}`}
                                            icon={<ClipboardIcon />}
                                            onClick={() => handleCopyIdClick(element.id)}
                                            size="iconXs"
                                            variant="ghost"
                                        />
                                    </TooltipTrigger>

                                    <TooltipContent>Copy {idLabel}</TooltipContent>
                                </Tooltip>
                            </dd>
                        </dl>
                    </AccordionContent>
                </AccordionItem>
            ))}
        </Accordion>
    );
};

export default IntegrationComponentElementList;
