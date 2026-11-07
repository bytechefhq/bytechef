import Badge, {BadgeStyleType} from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import Switch from '@/components/Switch/Switch';
import {Accordion, AccordionContent, AccordionItem, AccordionTrigger} from '@/components/ui/accordion';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import AiProviderForm from '@/pages/settings/platform/ai-providers/components/AiProviderForm';
import {AiProvider} from '@/shared/middleware/platform/configuration';
import {useEnableAiProviderMutation} from '@/shared/mutations/platform/aiProvider.mutations';
import {AiProviderKeys} from '@/shared/queries/platform/aiProviders.queries';
import {WorkflowNodeOptionKeys} from '@/shared/queries/platform/workflowNodeOptions.queries';
import {useQueryClient} from '@tanstack/react-query';
import {EditIcon} from 'lucide-react';
import {useEffect, useState} from 'react';
import InlineSVG from 'react-inlinesvg';

import './AiProviderList.css';

interface AiProviderBadgeI {
    description: string;
    isShown: (aiProvider: AiProvider) => boolean;
    label: string;
    styleType: BadgeStyleType;
}

const AI_PROVIDER_BADGES: AiProviderBadgeI[] = [
    {
        description: 'Provides chat models for text generation, used by AI components, agents and Copilot.',
        isShown: (aiProvider) => !!aiProvider.supportsText,
        label: 'Text',
        styleType: 'secondary-outline',
    },
    {
        description: 'Provides models that generate images from text prompts.',
        isShown: (aiProvider) => !!aiProvider.supportsImage,
        label: 'Image',
        styleType: 'secondary-outline',
    },
    {
        description: 'Provides embedding models that turn text into vectors for knowledge bases and semantic search.',
        isShown: (aiProvider) => !!aiProvider.supportsEmbeddings,
        label: 'Embeddings',
        styleType: 'secondary-outline',
    },
    {
        description: "Copilot uses this provider's embeddings to search the ByteChef documentation.",
        isShown: (aiProvider) => !!aiProvider.copilotDocsProvider,
        label: 'Copilot Docs',
        styleType: 'primary-outline',
    },
];

const isOllamaProvider = (aiProvider: AiProvider) => aiProvider.name?.toLowerCase() === 'ollama';

// Ollama runs locally and needs no API key, so it counts as configured; other providers require an API key.
const isConfigured = (aiProvider: AiProvider) => isOllamaProvider(aiProvider) || !!aiProvider.apiKey;

const AiProviderList = ({aiProviders, environment}: {aiProviders: AiProvider[]; environment: number}) => {
    const [enabledItems, setEnabledItems] = useState<{[key: number]: boolean}>({});
    const [openItem, setOpenItem] = useState('');
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

        setOpenItem('');

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
        <div className="w-full self-start p-4 pt-0 3xl:mx-auto 3xl:w-4/5">
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

                                            {AI_PROVIDER_BADGES.filter((badge) => badge.isShown(aiProvider)).map(
                                                (badge) => (
                                                    <Tooltip delayDuration={500} key={badge.label}>
                                                        <TooltipTrigger asChild>
                                                            <Badge
                                                                label={badge.label}
                                                                styleType={badge.styleType}
                                                                weight="semibold"
                                                            />
                                                        </TooltipTrigger>

                                                        <TooltipContent>{badge.description}</TooltipContent>
                                                    </Tooltip>
                                                )
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
                                            className="ml-2"
                                            icon={<EditIcon />}
                                            label="Edit"
                                            onClick={() =>
                                                setShowForm((prev) => ({
                                                    ...prev,
                                                    [aiProvider.id!]: true,
                                                }))
                                            }
                                            size="sm"
                                            variant="outline"
                                        />
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
