import Badge from '@/components/Badge/Badge';
import {Accordion, AccordionContent, AccordionItem, AccordionTrigger} from '@/components/ui/accordion';
import {Sheet, SheetCloseButton, SheetContent, SheetTitle} from '@/components/ui/sheet';
import {IntegrationStatus} from '@/ee/shared/middleware/embedded/configuration';
import {useGetIntegrationVersionsQuery} from '@/ee/shared/queries/embedded/integrationVersions.queries';
import {ProjectStatus} from '@/shared/middleware/automation/configuration';
import {VisuallyHidden} from 'radix-ui';

interface IntegrationVersionHistorySheetProps {
    onClose: () => void;
    integrationId: number;
}

const IntegrationVersionHistorySheet = ({integrationId, onClose}: IntegrationVersionHistorySheetProps) => {
    const {data: integrationVersions} = useGetIntegrationVersionsQuery(integrationId);

    return (
        <Sheet onOpenChange={() => onClose()} open>
            <VisuallyHidden.Root>
                <SheetTitle>Integration Version History</SheetTitle>
            </VisuallyHidden.Root>

            <SheetContent
                className="bottom-4 right-4 top-3 flex h-auto flex-col gap-0 rounded-md bg-surface-neutral-secondary p-0 sm:max-w-workflow-sidebar-project-version-history-sheet-width"
                onFocusOutside={(event) => event.preventDefault()}
                onPointerDownOutside={(event) => event.preventDefault()}
            >
                <header className="flex w-full shrink-0 items-center justify-between gap-x-3 rounded-t-md border-b border-b-border/50 bg-surface-neutral-primary p-3">
                    <span className="text-lg font-semibold">Integration Version History</span>

                    <SheetCloseButton />
                </header>

                <div className="flex min-h-0 flex-1 flex-col overflow-y-auto p-3">
                    {integrationVersions && (
                        <Accordion
                            defaultValue={
                                integrationVersions.length > 1 ? [integrationVersions[1]!.version!.toString()] : []
                            }
                            type="multiple"
                        >
                            {integrationVersions.map((integrationVersion) => (
                                <AccordionItem
                                    key={integrationVersion.version}
                                    value={integrationVersion.version?.toString() || ''}
                                >
                                    <AccordionTrigger disabled={integrationVersion.status === ProjectStatus.Draft}>
                                        <div className="flex w-full items-center justify-between pr-2">
                                            <span className="text-sm font-semibold">{`V${integrationVersion.version}`}</span>

                                            <div className="flex items-center space-x-4">
                                                {integrationVersion.publishedDate && (
                                                    <span className="text-sm">
                                                        {`${integrationVersion.publishedDate?.toLocaleDateString()} ${integrationVersion.publishedDate?.toLocaleTimeString()}`}
                                                    </span>
                                                )}

                                                <Badge
                                                    label={integrationVersion.status ?? ''}
                                                    styleType={
                                                        integrationVersion.status === IntegrationStatus.Published
                                                            ? 'success-outline'
                                                            : 'secondary-filled'
                                                    }
                                                    weight="semibold"
                                                />
                                            </div>
                                        </div>
                                    </AccordionTrigger>

                                    <AccordionContent className="text-muted-foreground">
                                        {integrationVersion.description
                                            ? integrationVersion.description
                                            : 'No description.'}
                                    </AccordionContent>
                                </AccordionItem>
                            ))}
                        </Accordion>
                    )}
                </div>
            </SheetContent>
        </Sheet>
    );
};

export default IntegrationVersionHistorySheet;
