import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import Switch from '@/components/Switch/Switch';
import {Accordion, AccordionContent, AccordionItem, AccordionTrigger} from '@/components/ui/accordion';
import WebSearchProviderForm from '@/pages/settings/platform/web-search-providers/components/WebSearchProviderForm';
import WebSearchProviderTestResult from '@/pages/settings/platform/web-search-providers/components/WebSearchProviderTestResult';
import {
    WebSearchProvidersQuery,
    useDeleteWebSearchProviderMutation,
    useEnableWebSearchProviderMutation,
    useTestWebSearchProviderMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';
import InlineSVG from 'react-inlinesvg';

import './WebSearchProviderList.css';

type WebSearchProviderType = WebSearchProvidersQuery['webSearchProviders'][number];

interface WebSearchProviderListProps {
    environment: number;
    webSearchProviders: WebSearchProviderType[];
}

const WebSearchProviderList = ({environment, webSearchProviders}: WebSearchProviderListProps) => {
    const [editingProviderNames, setEditingProviderNames] = useState<string[]>([]);
    const [openItem, setOpenItem] = useState('');

    const queryClient = useQueryClient();

    const invalidateWebSearchProviders = () => queryClient.invalidateQueries({queryKey: ['webSearchProviders']});

    const deleteWebSearchProviderMutation = useDeleteWebSearchProviderMutation({
        onSuccess: invalidateWebSearchProviders,
    });

    const enableWebSearchProviderMutation = useEnableWebSearchProviderMutation({
        onSuccess: invalidateWebSearchProviders,
    });

    const testWebSearchProviderMutation = useTestWebSearchProviderMutation();

    const testedProviderName = testWebSearchProviderMutation.variables?.name;

    const setEditing = (name: string, editing: boolean) => {
        testWebSearchProviderMutation.reset();

        setEditingProviderNames((previousNames) =>
            editing
                ? [...previousNames.filter((previousName) => previousName !== name), name]
                : previousNames.filter((previousName) => previousName !== name)
        );
    };

    const handleCheckedChange = (enabled: boolean, webSearchProvider: WebSearchProviderType) => {
        if (enabled && !webSearchProvider.maskedApiKey) {
            setEditing(webSearchProvider.name, true);
            setOpenItem(webSearchProvider.name);

            return;
        }

        enableWebSearchProviderMutation.mutate({
            enabled,
            environment: String(environment),
            name: webSearchProvider.name,
        });
    };

    return (
        <div className="w-full self-start p-4 pt-0">
            <Accordion
                className="w-full rounded-md border"
                collapsible
                onValueChange={setOpenItem}
                type="single"
                value={openItem}
            >
                {webSearchProviders.map((webSearchProvider) => {
                    const editing =
                        !webSearchProvider.maskedApiKey || editingProviderNames.includes(webSearchProvider.name);

                    return (
                        <AccordionItem
                            className="web-search-provider px-4"
                            key={webSearchProvider.name}
                            value={webSearchProvider.name}
                        >
                            <div className="flex w-full items-center justify-between gap-3">
                                <AccordionTrigger className="flex flex-1 cursor-pointer items-center justify-start gap-3 py-3 text-left hover:no-underline">
                                    {webSearchProvider.icon && (
                                        <span className="flex-none">
                                            <InlineSVG className="size-6" src={webSearchProvider.icon} />
                                        </span>
                                    )}

                                    <div className="flex flex-col">
                                        <span className="flex items-center gap-2">
                                            <span className="text-sm font-semibold">{webSearchProvider.label}</span>

                                            {webSearchProvider.apiKeyFromProperties && (
                                                <Badge
                                                    label="Application properties"
                                                    styleType="secondary-outline"
                                                    weight="semibold"
                                                />
                                            )}
                                        </span>

                                        <span className="text-xs text-muted-foreground">
                                            {webSearchProvider.description}
                                        </span>
                                    </div>
                                </AccordionTrigger>

                                <Switch
                                    aria-label={`Enable ${webSearchProvider.label}`}
                                    checked={webSearchProvider.enabled}
                                    disabled={enableWebSearchProviderMutation.isPending}
                                    onCheckedChange={(enabled) => handleCheckedChange(enabled, webSearchProvider)}
                                />
                            </div>

                            <AccordionContent className="pb-3 pl-9">
                                {editing ? (
                                    <WebSearchProviderForm
                                        environment={environment}
                                        onClose={() => setEditing(webSearchProvider.name, false)}
                                        showCancel={!!webSearchProvider.maskedApiKey}
                                        webSearchProvider={webSearchProvider}
                                    />
                                ) : (
                                    <div className="flex flex-col gap-1">
                                        <div className="flex items-center gap-2">
                                            <span className="text-base text-muted-foreground">API Key:</span>

                                            <span className="text-base">{webSearchProvider.maskedApiKey}</span>
                                        </div>

                                        {webSearchProvider.baseUrl !== webSearchProvider.defaultBaseUrl && (
                                            <div className="flex items-center gap-2">
                                                <span className="text-base text-muted-foreground">Base URL:</span>

                                                <span className="text-base">{webSearchProvider.baseUrl}</span>
                                            </div>
                                        )}

                                        {testedProviderName === webSearchProvider.name && (
                                            <WebSearchProviderTestResult
                                                result={testWebSearchProviderMutation.data?.testWebSearchProvider}
                                            />
                                        )}

                                        <div className="flex items-center gap-2">
                                            <Button
                                                onClick={() => setEditing(webSearchProvider.name, true)}
                                                variant="link"
                                            >
                                                Edit
                                            </Button>

                                            <Button
                                                disabled={testWebSearchProviderMutation.isPending}
                                                onClick={() =>
                                                    testWebSearchProviderMutation.mutate({
                                                        environment: String(environment),
                                                        name: webSearchProvider.name,
                                                    })
                                                }
                                                variant="link"
                                            >
                                                Test
                                            </Button>

                                            {!webSearchProvider.apiKeyFromProperties && (
                                                <Button
                                                    disabled={deleteWebSearchProviderMutation.isPending}
                                                    onClick={() =>
                                                        deleteWebSearchProviderMutation.mutate({
                                                            environment: String(environment),
                                                            name: webSearchProvider.name,
                                                        })
                                                    }
                                                    variant="link"
                                                >
                                                    Remove
                                                </Button>
                                            )}
                                        </div>
                                    </div>
                                )}
                            </AccordionContent>
                        </AccordionItem>
                    );
                })}
            </Accordion>
        </div>
    );
};

export default WebSearchProviderList;
