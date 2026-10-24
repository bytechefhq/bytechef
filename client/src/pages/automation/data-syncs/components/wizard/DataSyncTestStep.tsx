import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import useDataSyncTestStep from '@/pages/automation/data-syncs/hooks/useDataSyncTestStep';
import PropertyField from '@/pages/platform/workflow-editor/components/PropertyField';
import SchemaProperties from '@/pages/platform/workflow-editor/components/SchemaProperties';
import {PropertyAllType} from '@/shared/types';
import {useCopyToClipboard} from '@uidotdev/usehooks';
import {AlertCircleIcon, PlayIcon} from 'lucide-react';

interface DataSyncTestStepProps {
    draftWorkflowId: string;
}

export default function DataSyncTestStep({draftWorkflowId}: DataSyncTestStepProps) {
    const {
        handleTestClick,
        hasItems,
        hasProperties,
        outputFetching,
        outputSchema,
        rootWorkflowNodeName,
        sampleOutput,
        testError,
        testing,
    } = useDataSyncTestStep({draftWorkflowId});

    const [copiedValue, copyToClipboard] = useCopyToClipboard();

    return (
        <div className="space-y-6 py-4">
            <div>
                <h2 className="text-lg font-semibold">Test Sync</h2>

                <p className="mt-1 text-sm text-muted-foreground">
                    Run the sync once against the draft with real data.
                </p>
            </div>

            {testError && !testing && (
                <div className="flex items-center gap-2 rounded-md border border-destructive/50 bg-destructive/10 p-3 text-sm text-destructive">
                    <AlertCircleIcon className="size-4 shrink-0" />

                    <div>
                        <p className="font-medium">Test failed</p>

                        <p className="mt-1 text-destructive/80">
                            Check your source, destination, and connection configurations.
                        </p>
                    </div>
                </div>
            )}

            {!testing && !outputSchema && (
                <div className="flex items-center justify-center py-8">
                    <Button icon={<PlayIcon />} label={testError ? 'Retry' : 'Test'} onClick={handleTestClick} />
                </div>
            )}

            {!testing && outputSchema && (
                <div>
                    <div className="mb-2 flex items-center justify-between">
                        <h3 className="text-sm text-content-neutral-secondary">Output Schema</h3>

                        <Button
                            disabled={testing}
                            icon={<PlayIcon />}
                            label="Retest"
                            onClick={handleTestClick}
                            size="sm"
                            variant="outline"
                        />
                    </div>

                    <PropertyField
                        copiedValue={copiedValue}
                        copyToClipboard={copyToClipboard}
                        label={rootWorkflowNodeName}
                        property={outputSchema}
                        sampleOutput={sampleOutput}
                        valueToCopy={'${' + rootWorkflowNodeName + '}'}
                        workflowNodeName={rootWorkflowNodeName}
                    />

                    {hasProperties && sampleOutput && (
                        <SchemaProperties
                            copiedValue={copiedValue}
                            copyToClipboard={copyToClipboard}
                            properties={(outputSchema as PropertyAllType).properties!}
                            sampleOutput={sampleOutput}
                            workflowNodeName={rootWorkflowNodeName}
                        />
                    )}

                    {hasItems && sampleOutput && (
                        <div className="ml-3 flex flex-col overflow-y-auto border-l border-l-border/50 pl-1">
                            <SchemaProperties
                                copiedValue={copiedValue}
                                copyToClipboard={copyToClipboard}
                                properties={(outputSchema as PropertyAllType).items!}
                                sampleOutput={sampleOutput}
                                workflowNodeName={rootWorkflowNodeName}
                            />
                        </div>
                    )}
                </div>
            )}

            {testing && (
                <div className="flex items-center justify-center gap-2 py-8">
                    <LoadingIcon />

                    <span className="text-lg">Running sync...</span>
                </div>
            )}

            {outputFetching && !outputSchema && !testing && (
                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                    <LoadingIcon />

                    <span>Loading output...</span>
                </div>
            )}
        </div>
    );
}
