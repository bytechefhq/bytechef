import LoadingDots from '@/components/LoadingDots';
import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert';
import {Badge} from '@/components/ui/badge';
import {Card, CardContent, CardFooter} from '@/components/ui/card';
import {
    IntegrationMarketplaceKeys,
    useGetMarketplaceIntegrationsQuery,
} from '@/ee/pages/embedded/integration-marketplace/queries/integrationMarketplace.queries';
import {getEmbedParentOrigin} from '@/ee/pages/embedded/shared/useEmbedHandshake';
import {IntegrationBasic} from '@/ee/shared/middleware/embedded/public';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect} from 'react';
import InlineSVG from 'react-inlinesvg';

const CONNECT_MESSAGE_TYPE = 'EMBED_OPEN_CONNECT_DIALOG';

const INTEGRATIONS_CHANGED_MESSAGE_TYPE = 'EMBED_INTEGRATIONS_CHANGED';

const requestConnect = (integrationId: number) => {
    const parentOrigin = getEmbedParentOrigin();

    if (!parentOrigin) {
        return;
    }

    window.parent.postMessage({integrationId: String(integrationId), type: CONNECT_MESSAGE_TYPE}, parentOrigin);
};

const isConnected = (integration: IntegrationBasic) => (integration.integrationInstances?.length ?? 0) > 0;

const IntegrationMarketplaceView = () => {
    const {data: integrations, error, isLoading} = useGetMarketplaceIntegrationsQuery();

    const queryClient = useQueryClient();

    useEffect(() => {
        const handleMessage = (event: MessageEvent) => {
            if (
                event.source !== window.parent ||
                event.origin !== getEmbedParentOrigin() ||
                event.data?.type !== INTEGRATIONS_CHANGED_MESSAGE_TYPE
            ) {
                return;
            }

            void queryClient.invalidateQueries({queryKey: IntegrationMarketplaceKeys.integrations});
        };

        window.addEventListener('message', handleMessage);

        return () => window.removeEventListener('message', handleMessage);
    }, [queryClient]);

    if (isLoading) {
        return (
            <div className="flex size-full items-center justify-center" data-testid="marketplace-loading">
                <LoadingDots />
            </div>
        );
    }

    return (
        <div className="flex size-full flex-col gap-4 overflow-y-auto bg-(--hub-surface) p-4 text-foreground">
            {error && (
                <Alert variant="destructive">
                    <AlertTitle>Unable to load integrations</AlertTitle>

                    <AlertDescription>{error.message}</AlertDescription>
                </Alert>
            )}

            {!error && !integrations?.length && (
                <p className="text-sm text-muted-foreground">There are no integrations available yet.</p>
            )}

            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4" data-testid="marketplace-catalog">
                {(integrations || []).map((integration) => (
                    <Card
                        className="gap-4 border-transparent bg-(--hub-card) py-4 text-center shadow-none"
                        key={integration.id}
                    >
                        <CardContent className="flex flex-1 flex-col items-center gap-3 px-4">
                            {integration.icon && <InlineSVG className="size-14" src={integration.icon} />}

                            <h3 className="text-sm font-medium">{integration.name || integration.componentName}</h3>

                            {integration.description && (
                                <p className="line-clamp-3 text-sm text-muted-foreground">{integration.description}</p>
                            )}

                            <Badge variant="outline">{integration.componentName}</Badge>
                        </CardContent>

                        <CardFooter className="mt-auto justify-center border-t px-4 pt-4">
                            <button
                                className={
                                    isConnected(integration)
                                        ? 'text-sm font-semibold text-(--hub-enable)'
                                        : 'text-sm font-semibold'
                                }
                                onClick={() => requestConnect(integration.id!)}
                                type="button"
                            >
                                {isConnected(integration) ? 'Connected' : 'Connect'}
                            </button>
                        </CardFooter>
                    </Card>
                ))}
            </div>
        </div>
    );
};

export default IntegrationMarketplaceView;
