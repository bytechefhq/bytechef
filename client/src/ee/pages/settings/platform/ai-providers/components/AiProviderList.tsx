import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import Switch from '@/components/Switch/Switch';
import {Accordion, AccordionContent, AccordionItem, AccordionTrigger} from '@/components/ui/accordion';
import AiProviderForm from '@/ee/pages/settings/platform/ai-providers/components/AiProviderForm';
import {AiProvider} from '@/ee/shared/middleware/platform/configuration';
import {useEnableAiProviderMutation} from '@/ee/shared/mutations/platform/aiProvider.mutations';
import {AiProviderKeys} from '@/ee/shared/queries/platform/aiProviders.queries';
import {WorkflowNodeOptionKeys} from '@/shared/queries/platform/workflowNodeOptions.queries';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useState} from 'react';
import InlineSVG from 'react-inlinesvg';

import './AiProviderList.css';

const AiProviderList = ({aiProviders, environment}: {aiProviders: AiProvider[]; environment: number}) => {
    const [enabledItems, setEnabledItems] = useState<{[key: number]: boolean}>({});
    const [openItem, setOpenItem] = useState<string>();
    const [showForm, setShowForm] = useState<{[key: number]: boolean}>({});

    const queryClient = useQueryClient();

    const enableAiProviderMutation = useEnableAiProviderMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: AiProviderKeys.aiProviders(environment),
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
                environment,
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
        <div className="w-full p-4">
            <Accordion
                className="w-full rounded-md border"
                collapsible
                onValueChange={setOpenItem}
                type="single"
                value={openItem}
            >
                {aiProviders &&
                    aiProviders.map((aiProvider) => (
                        <AccordionItem className="ai-provider px-4" key={aiProvider.id} value={`item-${aiProvider.id}`}>
                            <div className="flex w-full items-center justify-between gap-3">
                                <AccordionTrigger className="flex flex-1 cursor-pointer items-center justify-start gap-3 py-3 text-left hover:no-underline">
                                    <span className="flex-none">
                                        <InlineSVG className="size-6" key={aiProvider.name!} src={aiProvider.icon!} />
                                    </span>

                                    <div className="flex flex-col">
                                        <span className="flex items-center gap-2">
                                            <span className="text-sm font-semibold">{aiProvider.name}</span>

                                            {aiProvider.supportsEmbeddings && (
                                                <Badge
                                                    label="Embeddings"
                                                    styleType="secondary-outline"
                                                    weight="semibold"
                                                />
                                            )}
                                        </span>

                                        <span className="text-xs text-muted-foreground">
                                            Configure {aiProvider.name} credentials
                                        </span>
                                    </div>
                                </AccordionTrigger>

                                <Switch
                                    checked={enabledItems[aiProvider.id!]}
                                    onCheckedChange={(value) => handleOnCheckedChange(value, aiProvider)}
                                />
                            </div>

                            <AccordionContent className="pb-3 pl-9">
                                {showForm[aiProvider.id!] ? (
                                    <AiProviderForm
                                        environment={environment}
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
        </div>
    );
};

export default AiProviderList;
