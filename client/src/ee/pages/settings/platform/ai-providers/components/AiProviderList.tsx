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

const isOllamaProvider = (aiProvider: AiProvider) => aiProvider.name?.toLowerCase() === 'ollama';

// Ollama runs locally and needs no API key, so it counts as configured; other providers require an API key.
const isConfigured = (aiProvider: AiProvider) => isOllamaProvider(aiProvider) || !!aiProvider.apiKey;

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

        const configured = isConfigured(aiProvider);

        if (!configured && value) {
            setOpenItem(`item-${aiProvider.id}`);
        }

        if (configured) {
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
                showForm[aiProvider.id!] = !isConfigured(aiProvider);
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

                                            {aiProvider.supportsText && (
                                                <Badge label="Text" styleType="secondary-outline" weight="semibold" />
                                            )}

                                            {aiProvider.supportsImage && (
                                                <Badge label="Image" styleType="secondary-outline" weight="semibold" />
                                            )}

                                            {aiProvider.supportsEmbeddings && (
                                                <Badge
                                                    label="Embeddings"
                                                    styleType="secondary-outline"
                                                    weight="semibold"
                                                />
                                            )}

                                            {aiProvider.copilotDocsProvider && (
                                                <Badge
                                                    label="Copilot Docs"
                                                    styleType="primary-outline"
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
                                        aiProvider={aiProvider}
                                        environment={environment}
                                        onClose={() =>
                                            setShowForm((prev) => ({
                                                ...prev,
                                                [aiProvider.id!]: false,
                                            }))
                                        }
                                        showCancel={isConfigured(aiProvider)}
                                    />
                                ) : (
                                    <div className="flex items-center gap-2">
                                        <span className="text-base text-muted-foreground">
                                            {isOllamaProvider(aiProvider) ? 'Base URL: ' : 'API Key: '}
                                        </span>

                                        <span className="text-base">
                                            {isOllamaProvider(aiProvider)
                                                ? aiProvider.url || 'http://localhost:11434'
                                                : aiProvider.apiKey}
                                        </span>

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
