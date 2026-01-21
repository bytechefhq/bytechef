import Badge from '@/components/Badge/Badge';
import {Card} from '@/components/ui/card';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {AlertTriangleIcon, CheckIcon, CopyIcon, RefreshCwIcon} from 'lucide-react';
import {useState} from 'react';
import {twMerge} from 'tailwind-merge';

const McpServerConfigurationCode = ({codeSnippet, onRefresh}: {codeSnippet: string; onRefresh: () => void}) => {
    const [copied, setCopied] = useState(false);
    const [refreshed, setRefreshed] = useState(false);

    const handleCopy = () => {
        navigator.clipboard.writeText(codeSnippet);

        setCopied(true);

        setTimeout(() => setCopied(false), 2000);
    };

    const handleRefresh = () => {
        setRefreshed(true);

        setTimeout(() => setRefreshed(false), 300);

        onRefresh();
    };

    return (
        <Card className="relative overflow-hidden border-border bg-card">
            <div className="flex items-center justify-between border-b border-border bg-muted/30 px-4 py-2">
                <div className="flex items-center gap-2">
                    <Tooltip>
                        <TooltipTrigger>
                            <Badge
                                className="flex items-center gap-1 border border-destructive"
                                icon={<AlertTriangleIcon className="size-4 text-destructive" />}
                                label="Sensitive"
                                styleType="outline-outline"
                            />
                        </TooltipTrigger>

                        <TooltipContent>
                            This URL gives access to your tools and data. Only share it with trusted applications.
                        </TooltipContent>
                    </Tooltip>
                </div>

                <div className="flex items-center gap-2">
                    <button className="rounded p-1 hover:bg-muted" disabled={refreshed} onClick={handleRefresh}>
                        <RefreshCwIcon
                            className={twMerge('size-4 text-muted-foreground', refreshed && 'animate-spin')}
                        />
                    </button>

                    <button className="rounded p-1 hover:bg-muted" disabled={refreshed} onClick={handleCopy}>
                        {copied ? (
                            <CheckIcon className="size-4" />
                        ) : (
                            <CopyIcon className="size-4 text-muted-foreground" />
                        )}
                    </button>
                </div>
            </div>

            <pre className="overflow-x-auto p-4">
                <div className="relative max-w-screen-lg">
                    <code className="font-mono text-sm leading-relaxed text-foreground">{codeSnippet}</code>
                </div>
            </pre>
        </Card>
    );
};

export default McpServerConfigurationCode;
