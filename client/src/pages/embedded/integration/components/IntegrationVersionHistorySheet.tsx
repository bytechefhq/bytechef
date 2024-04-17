import {Accordion, AccordionContent, AccordionItem, AccordionTrigger} from '@/components/ui/accordion';
import {Badge} from '@/components/ui/badge';
import {Sheet, SheetContent, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import {ProjectStatusModel} from '@/middleware/automation/configuration';
import {IntegrationStatusModel} from '@/middleware/embedded/configuration';
import {useGetIntegrationVersionsQuery} from '@/queries/embedded/integrationVersions.queries';
import * as SheetPrimitive from '@radix-ui/react-dialog';
import {Cross2Icon} from '@radix-ui/react-icons';

interface IntegrationVersionHistorySheetProps {
    onClose: () => void;
    integrationId: number;
}

const IntegrationVersionHistorySheet = ({integrationId, onClose}: IntegrationVersionHistorySheetProps) => {
    const {data: integrationVersions} = useGetIntegrationVersionsQuery(integrationId);

    return (
        <Sheet onOpenChange={() => onClose()} open>
            <SheetContent
                className="flex flex-col p-4 sm:max-w-[500px]"
                onFocusOutside={(event) => event.preventDefault()}
                onPointerDownOutside={(event) => event.preventDefault()}
            >
                <SheetHeader>
                    <SheetTitle>
                        <div className="flex items-center justify-between">
                            <span>Integration Version History</span>

                            <SheetPrimitive.Close asChild>
                                <Cross2Icon className="size-4 cursor-pointer opacity-70" />
                            </SheetPrimitive.Close>
                        </div>
                    </SheetTitle>
                </SheetHeader>

                <div className="overflow-y-auto">
                    <Accordion type="single">
                        {integrationVersions &&
                            integrationVersions.map((integrationVersion) => (
                                <AccordionItem
                                    key={integrationVersion.version}
                                    value={integrationVersion.version?.toString() || ''}
                                >
                                    <AccordionTrigger disabled={integrationVersion.status === ProjectStatusModel.Draft}>
                                        <div className="flex w-full items-center justify-between pr-2">
                                            <span className="text-sm font-semibold">{`V${integrationVersion.version}`}</span>

                                            <div className="flex items-center space-x-4">
                                                {integrationVersion.publishedDate && (
                                                    <span className="text-sm">
                                                        {`${integrationVersion.publishedDate?.toLocaleDateString()} ${integrationVersion.publishedDate?.toLocaleTimeString()}`}
                                                    </span>
                                                )}

                                                <Badge
                                                    variant={
                                                        integrationVersion.status === IntegrationStatusModel.Published
                                                            ? 'success_outline'
                                                            : 'secondary'
                                                    }
                                                >
                                                    {integrationVersion.status === IntegrationStatusModel.Published
                                                        ? `Published`
                                                        : 'Draft'}
                                                </Badge>
                                            </div>
                                        </div>
                                    </AccordionTrigger>

                                    <AccordionContent>{integrationVersion.description}</AccordionContent>
                                </AccordionItem>
                            ))}
                    </Accordion>
                </div>
            </SheetContent>
        </Sheet>
    );
};

export default IntegrationVersionHistorySheet;
