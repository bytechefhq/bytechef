import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert';
import {AI_PROVIDERS_LINK_LABEL, AI_PROVIDERS_SETTINGS_PATH} from '@/shared/constants';

const KnowledgeBaseEmbeddingInactiveAlert = () => (
    <div className="flex size-full items-center justify-center p-4">
        <Alert className="h-fit max-w-2xl" variant="destructive">
            <AlertTitle>No embedding model is active</AlertTitle>

            <AlertDescription className="flex flex-col gap-1">
                <span>
                    Knowledge Base documents can&apos;t be processed until an embedding-capable AI provider is activated
                    for this environment.
                </span>

                <a className="font-medium underline" href={AI_PROVIDERS_SETTINGS_PATH}>
                    {AI_PROVIDERS_LINK_LABEL}
                </a>
            </AlertDescription>
        </Alert>
    </div>
);

export default KnowledgeBaseEmbeddingInactiveAlert;
