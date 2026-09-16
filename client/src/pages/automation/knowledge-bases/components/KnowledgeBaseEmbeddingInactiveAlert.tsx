import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert';

const KnowledgeBaseEmbeddingInactiveAlert = () => (
    <div className="flex size-full items-center justify-center p-4">
        <Alert className="h-fit max-w-2xl" variant="destructive">
            <AlertTitle>No embedding model is active</AlertTitle>

            <AlertDescription className="flex flex-col gap-1">
                <span>
                    Knowledge Base documents can&apos;t be processed until an embedding-capable AI provider is activated
                    for this environment.
                </span>

                <a className="font-medium underline" href="/automation/settings/ai-providers">
                    Go to AI Providers
                </a>
            </AlertDescription>
        </Alert>
    </div>
);

export default KnowledgeBaseEmbeddingInactiveAlert;
