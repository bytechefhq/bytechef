import Button from '@/components/Button/Button';
import {A2aServer} from '@/shared/middleware/graphql';
import {useCopyToClipboard} from '@uidotdev/usehooks';
import {CheckIcon, CopyIcon} from 'lucide-react';
import {useState} from 'react';

interface A2aServerConnectUrlProps {
    description: string;
    label: string;
    url: string;
}

const A2aServerConnectUrl = ({description, label, url}: A2aServerConnectUrlProps) => {
    const [copied, setCopied] = useState(false);

    const [, copyToClipboard] = useCopyToClipboard();

    const handleCopy = () => {
        copyToClipboard(url);

        setCopied(true);

        setTimeout(() => setCopied(false), 2000);
    };

    return (
        <fieldset className="space-y-2 border-0">
            <h3 className="font-semibold text-foreground">{label}</h3>

            <p className="text-sm text-muted-foreground">{description}</p>

            <div className="flex items-center gap-2 rounded-md border border-border bg-muted/30 px-3 py-2">
                <code className="flex-1 font-mono text-sm break-all text-foreground">{url}</code>

                <Button
                    aria-label={`Copy ${label}`}
                    icon={copied ? <CheckIcon /> : <CopyIcon />}
                    onClick={handleCopy}
                    size="icon"
                    variant="ghost"
                />
            </div>
        </fieldset>
    );
};

interface A2aServerConnectProps {
    a2aServer: A2aServer;
}

const A2aServerConnect = ({a2aServer}: A2aServerConnectProps) => {
    if (!a2aServer.secretKey) {
        return <p className="text-sm text-muted-foreground">This server has no URL yet.</p>;
    }

    const endpointUrl = `${window.location.origin}/api/automation/a2a/${a2aServer.secretKey}`;

    return (
        <div className="space-y-6">
            <A2aServerConnectUrl
                description="Point an A2A client at this URL to discover the agent and the skills it exposes."
                label="Agent Card URL"
                url={`${endpointUrl}/.well-known/agent-card.json`}
            />

            <A2aServerConnectUrl
                description="The JSON-RPC endpoint the client sends messages to."
                label="Endpoint URL"
                url={endpointUrl}
            />

            {a2aServer.authenticationRequired ? (
                <p className="text-sm text-muted-foreground">
                    Requests require an API key created under Settings, sent as an Authorization: Bearer header.
                </p>
            ) : (
                <p className="text-sm text-muted-foreground">
                    This server does not require authentication, so the URLs can be used as is.
                </p>
            )}
        </div>
    );
};

export default A2aServerConnect;
