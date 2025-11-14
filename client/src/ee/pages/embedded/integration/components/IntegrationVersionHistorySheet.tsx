import Badge from '@/components/Badge/Badge';
import {Accordion, AccordionContent, AccordionItem, AccordionTrigger} from '@/components/ui/accordion';
import {Sheet, SheetCloseButton, SheetContent, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import {IntegrationStatus} from '@/ee/shared/middleware/embedded/configuration';
import {useGetIntegrationVersionsQuery} from '@/ee/shared/queries/embedded/integrationVersions.queries';
import {ProjectStatus} from '@/shared/middleware/automation/configuration';

interface IntegrationVersionHistorySheetProps {
    onClose: () => void;
    integrationId: number;
}

const IntegrationVersionHistorySheet = ({integrationId, onClose}: IntegrationVersionHistorySheetProps) => {
    const {data: integrationVersions} = useGetIntegrationVersionsQuery(integrationId);

    return (
        <Sheet onOpenChange={() => onClose()} open>
            <SheetContent
                className="flex flex-col p-4 sm:max-w-workflow-sidebar-project-version-history-sheet-width"
                onFocusOutside={(event) => event.preventDefault()}
                onPointerDownOutside={(event) => event.preventDefault()}
            >
                <SheetHeader className="flex flex-row items-center justify-between space-y-0">
                    <SheetTitle>Integration Version History</SheetTitle>

                    <SheetCloseButton />
                </SheetHeader>

                <div className="overflow-y-auto">
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
