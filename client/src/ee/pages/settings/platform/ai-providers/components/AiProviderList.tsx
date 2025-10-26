import {Accordion, AccordionContent, AccordionItem} from '@/components/ui/accordion';
import {Button} from '@/components/ui/button';
import {Switch} from '@/components/ui/switch';
import AiProviderForm from '@/ee/pages/settings/platform/ai-providers/components/AiProviderForm';
import {AiProvider} from '@/ee/shared/middleware/platform/configuration';
import {useEnableAiProviderMutation} from '@/ee/shared/mutations/platform/aiProvider.mutations';
import {AiProviderKeys} from '@/ee/shared/queries/platform/aiProviders.queries';
import {WorkflowNodeOptionKeys} from '@/shared/queries/platform/workflowNodeOptions.queries';
import * as AccordionPrimitive from '@radix-ui/react-accordion';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useState} from 'react';
import InlineSVG from 'react-inlinesvg';

const AiProviderList = ({aiProviders}: {aiProviders: AiProvider[]}) => {
    const [enabledItems, setEnabledItems] = useState<{[key: number]: boolean}>({});
    const [openItem, setOpenItem] = useState<string>();
    const [showForm, setShowForm] = useState<{[key: number]: boolean}>({});

    const queryClient = useQueryClient();

    const enableAiProviderMutation = useEnableAiProviderMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: AiProviderKeys.aiProviders,
            });
            queryClient.invalidateQueries({
                queryKey: WorkflowNodeOptionKeys.workflowNodeOptions,
            });
        },
    });

    const handleOnCheckedChange = (value: boolean, aiProvider: AiProvider) => {
        setEnabledItems((prev) => ({
            ...prev,
            [aiProvider.id!]: value,
        }));

        setOpenItem(undefined);

        if (!aiProvider.apiKey && value) {
            setOpenItem(`item-${aiProvider.id}`);
        }

        if (aiProvider.apiKey) {
            enableAiProviderMutation.mutate({
                enable: value,
                id: aiProvider.id!,
            });
        }
    };

    useEffect(() => {
        if (aiProviders) {
            const enabledItems: {[key: string]: boolean} = {};

            aiProviders.forEach((aiProvider) => {
                enabledItems[aiProvider.id!] = !!aiProvider.enabled;
                showForm[aiProvider.id!] = !aiProvider.apiKey;
            });

            setEnabledItems(enabledItems);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [aiProviders]);

    return (
        <Accordion
            className="w-full px-4 3xl:mx-auto 3xl:w-4/5"
            collapsible
            onValueChange={setOpenItem}
            type="single"
            value={openItem}
        >
            {aiProviders &&
                aiProviders.map((aiProvider) => (
                    <AccordionItem key={aiProvider.id} value={`item-${aiProvider.id}`}>
                        <div className="flex w-full items-center justify-between">
                            <AccordionPrimitive.Header>
                                <AccordionPrimitive.Trigger className="flex flex-1 cursor-pointer items-center justify-between py-4 text-left transition-all">
                                    <div className="flex items-center gap-3">
                                        <InlineSVG
                                            className="size-9 flex-none"
                                            key={aiProvider.name!}
                                            src={aiProvider.icon!}
                                        />

                                        <div className="mr-1 flex flex-col">
                                            <div className="text-lg">{aiProvider.name}</div>

                                            <div className="text-sm text-muted-foreground">
                                                Configure credentials {aiProvider.name} for AI provider
                                            </div>
                                        </div>
                                    </div>
                                </AccordionPrimitive.Trigger>
                            </AccordionPrimitive.Header>

                            <div>
                                <Switch
                                    checked={enabledItems[aiProvider.id!]}
                                    onCheckedChange={(value) => handleOnCheckedChange(value, aiProvider)}
                                />
                            </div>
                        </div>

                        <AccordionContent className="my-2 w-6/12 pl-12">
                            {showForm[aiProvider.id!] ? (
                                <AiProviderForm
                                    id={aiProvider.id!}
                                    onClose={() =>
                                        setShowForm((prev) => ({
                                            ...prev,
                                            [aiProvider.id!]: false,
                                        }))
                                    }
                                    showCancel={!!aiProvider.apiKey}
                                />
                            ) : (
                                <div className="flex items-center gap-2">
                                    <span className="text-base text-muted-foreground">API Key: </span>

                                    <span className="text-base">{aiProvider.apiKey}</span>

                                    <Button
                                        onClick={() =>
                                            setShowForm((prev) => ({
                                                ...prev,
                                                [aiProvider.id!]: true,
                                            }))
                                        }
                                        variant="link"
                                    >
                                        Edit
                                    </Button>
                                </div>
                            )}
                        </AccordionContent>
                    </AccordionItem>
                ))}
        </Accordion>
    );
};

export default AiProviderList;
