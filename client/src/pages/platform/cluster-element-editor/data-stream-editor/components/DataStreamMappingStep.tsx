import Button from '@/components/Button/Button';
import useDataStreamMapping from '@/pages/platform/cluster-element-editor/data-stream-editor/hooks/useDataStreamMapping';
import Properties from '@/pages/platform/workflow-editor/components/properties/Properties';
import {SparklesIcon} from 'lucide-react';

export default function DataStreamMappingStep() {
    const {
        autoMapping,
        destinationLabel,
        displayConditionsQuery,
        handleAutoMap,
        hasSourceAndDestination,
        processor,
        processorProperties,
        propertiesKey,
        sourceLabel,
    } = useDataStreamMapping();

    return (
        <div className="space-y-6 py-4">
            <div>
                <h2 className="text-lg font-semibold">Field Mapping</h2>

                <p className="mt-1 text-sm text-muted-foreground">
                    Map fields from {sourceLabel} to {destinationLabel}. Configure the field mappings for the processor.
                </p>
            </div>

            {processor && hasSourceAndDestination && (
                <Button
                    className="w-fit"
                    disabled={autoMapping}
                    icon={<SparklesIcon />}
                    label={autoMapping ? 'Mapping...' : 'Auto-map matching fields'}
                    onClick={handleAutoMap}
                    size="sm"
                    variant="outline"
                />
            )}

            {processor && processorProperties.length > 0 && (
                <Properties
                    customClassName="p-0"
                    displayConditionsQuery={displayConditionsQuery}
                    key={propertiesKey}
                    operationName={processor.operationName}
                    properties={processorProperties}
                />
            )}
        </div>
    );
}
