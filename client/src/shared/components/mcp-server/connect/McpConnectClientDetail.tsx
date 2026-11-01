import Button from '@/components/Button/Button';
import {Alert, AlertTitle} from '@/components/ui/alert';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {InfoCircledIcon} from '@radix-ui/react-icons';
import {ArrowUpRightIcon, CheckIcon, CopyIcon} from 'lucide-react';
import {useEffect, useMemo, useRef, useState} from 'react';

import McpConnectStep from './McpConnectStep';
import {McpConnectAuthModeType} from './mcpConnectAuthMode';
import {McpConnectClientI, McpConnectTabI} from './mcpConnectClients';

const THEN_TRY_PROMPT = 'What ByteChef tools do you have? List a few things you could run for me.';

interface McpConnectClientDetailPropsI {
    apiKeySource: string;
    client: McpConnectClientI;
    mcpServerUrl: string;
    mode: McpConnectAuthModeType;
    onRefresh?: () => void;
}

interface McpConnectTabContentPropsI {
    mcpServerUrl: string;
    onRefresh?: () => void;
    tab: McpConnectTabI;
}

const McpConnectTabContent = ({mcpServerUrl, onRefresh, tab}: McpConnectTabContentPropsI) => {
    const refreshStepIndex = tab.steps.findIndex((step) => step.codeSnippet?.includes(mcpServerUrl));

    return (
        <div className="space-y-6">
            {tab.notice && (
                <Alert className="flex items-center gap-2" variant="default">
                    <span>
                        <InfoCircledIcon />
                    </span>

                    <AlertTitle className="mb-0 text-muted-foreground">{tab.notice}</AlertTitle>
                </Alert>
            )}

            {tab.steps.length > 0 && (
                <ol className="space-y-6">
                    {tab.steps.map((step, index) => (
                        <McpConnectStep
                            key={step.title}
                            number={index + 1}
                            onRefresh={index === refreshStepIndex ? onRefresh : undefined}
                            step={step}
                        />
                    ))}
                </ol>
            )}
        </div>
    );
};

const McpConnectClientDetail = ({
    apiKeySource,
    client,
    mcpServerUrl,
    mode,
    onRefresh,
}: McpConnectClientDetailPropsI) => {
    const [promptCopied, setPromptCopied] = useState(false);

    const promptCopiedTimeoutRef = useRef<ReturnType<typeof setTimeout>>(undefined);

    const tabs = useMemo(
        () => client.getTabs({apiKeySource, mode, url: mcpServerUrl}),
        [apiKeySource, client, mcpServerUrl, mode]
    );

    const Logo = client.logo;

    const handleCopyPrompt = () => {
        navigator.clipboard.writeText(THEN_TRY_PROMPT).then(
            () => {
                setPromptCopied(true);

                clearTimeout(promptCopiedTimeoutRef.current);

                promptCopiedTimeoutRef.current = setTimeout(() => setPromptCopied(false), 2000);
            },
            () => setPromptCopied(false)
        );
    };

    useEffect(() => () => clearTimeout(promptCopiedTimeoutRef.current), []);

    return (
        <div className="space-y-6">
            <div className="flex items-start gap-3 border-b border-border pb-4">
                <Logo className="mt-1 size-7 shrink-0 text-foreground" />

                <div className="min-w-0">
                    <h2 className="text-lg font-semibold text-foreground">Connect to {client.title}</h2>

                    {client.subtitle && <p className="text-sm text-muted-foreground">{client.subtitle}</p>}

                    <a
                        className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground hover:underline"
                        href={client.docsUrl}
                        rel="noreferrer"
                        target="_blank"
                    >
                        {`${client.title} docs`}

                        <ArrowUpRightIcon className="size-3.5" />
                    </a>
                </div>
            </div>

            {tabs.length > 1 ? (
                <Tabs defaultValue={tabs[0].id} key={client.id}>
                    <TabsList>
                        {tabs.map((tab) => (
                            <TabsTrigger key={tab.id} value={tab.id}>
                                {tab.label}
                            </TabsTrigger>
                        ))}
                    </TabsList>

                    {tabs.map((tab) => (
                        <TabsContent className="mt-6" key={tab.id} value={tab.id}>
                            <McpConnectTabContent mcpServerUrl={mcpServerUrl} onRefresh={onRefresh} tab={tab} />
                        </TabsContent>
                    ))}
                </Tabs>
            ) : (
                <McpConnectTabContent mcpServerUrl={mcpServerUrl} onRefresh={onRefresh} tab={tabs[0]} />
            )}

            <div className="flex items-start justify-between gap-4 rounded-lg bg-muted/50 p-4">
                <div className="space-y-1">
                    <p className="text-sm font-medium text-muted-foreground">Then try</p>

                    <p className="text-sm text-foreground">{THEN_TRY_PROMPT}</p>
                </div>

                <Button
                    aria-label={promptCopied ? 'Prompt copied' : 'Copy prompt'}
                    icon={promptCopied ? <CheckIcon /> : <CopyIcon />}
                    onClick={handleCopyPrompt}
                    size="iconSm"
                    variant="outline"
                />
            </div>
        </div>
    );
};

export default McpConnectClientDetail;
