import Button from '@/components/Button/Button';
import EmptyState from '@/components/EmptyState/EmptyState';
import LoadingIcon from '@/components/LoadingIcon';
import {Accordion, AccordionContent, AccordionItem, AccordionTrigger} from '@/components/ui/accordion';
import {Input} from '@/components/ui/input';
import {
    IntegrationComponentElementI,
    filterIntegrationComponentElements,
} from '@/ee/pages/embedded/integrations/components/integration-list/integrationComponentElementUtils';
import {AlertTriangleIcon, SearchIcon, WrenchIcon} from 'lucide-react';
import {ReactNode, useMemo, useState} from 'react';

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
    const [searchText, setSearchText] = useState('');

    const filteredElements = useMemo(
        () => filterIntegrationComponentElements(elements, searchText),
        [elements, searchText]
    );

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
        <fieldset className="flex flex-col gap-2 border-0">
            <div className="relative w-full max-w-xs self-end">
                <SearchIcon className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground" />

                <Input
                    aria-label={`Search ${noun}`}
                    className="pl-8"
                    onChange={(event) => setSearchText(event.target.value)}
                    placeholder={`Search ${noun}...`}
                    value={searchText}
                />
            </div>

            {filteredElements.length === 0 ? (
                <p className="py-4 text-center text-sm text-muted-foreground">No {noun} match your search.</p>
            ) : (
                <Accordion className="rounded-md border border-border/50" type="multiple">
                    {filteredElements.map((element) => (
                        <AccordionItem className="border-border/50" key={element.id} value={element.id}>
                            <AccordionTrigger className="px-4 py-3 hover:no-underline">
                                {element.title}
                            </AccordionTrigger>

                            <AccordionContent className="px-4">
                                <dl className="grid grid-cols-[7rem_1fr] gap-x-4 gap-y-2 text-sm">
                                    <dt className="text-muted-foreground">Description</dt>

                                    <dd>{element.description || 'No description.'}</dd>

                                    <dt className="text-muted-foreground">{idLabel}</dt>

                                    <dd className="font-mono text-xs leading-5 break-all">{element.id}</dd>
                                </dl>
                            </AccordionContent>
                        </AccordionItem>
                    ))}
                </Accordion>
            )}
        </fieldset>
    );
};

export default IntegrationComponentElementList;
