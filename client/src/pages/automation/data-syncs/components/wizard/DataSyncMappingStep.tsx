import Button from '@/components/Button/Button';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import useDataSyncMapping from '@/pages/automation/data-syncs/hooks/useDataSyncMapping';
import {DataSync} from '@/shared/middleware/graphql';
import {PlusIcon, SparklesIcon, Trash2Icon} from 'lucide-react';
import {useMemo} from 'react';

interface DataSyncMappingStepProps {
    dataSync: DataSync;
}

export default function DataSyncMappingStep({dataSync}: DataSyncMappingStepProps) {
    const {
        autoMapping,
        destinationOptions,
        handleAddMapping,
        handleAutoMap,
        handleMappingChange,
        handleRemoveMapping,
        hasSourceAndDestination,
        mappings,
        optionsLoadFailed,
        optionsLoading,
        processor,
        sourceOptions,
    } = useDataSyncMapping({dataSync});

    const fieldsUnavailable = useMemo(
        () => !optionsLoading && !optionsLoadFailed && sourceOptions.length === 0 && destinationOptions.length === 0,
        [destinationOptions, optionsLoadFailed, optionsLoading, sourceOptions]
    );

    return (
        <div className="space-y-6 py-4">
            <div>
                <h2 className="text-lg font-semibold">Field Mapping</h2>

                <p className="mt-1 text-sm text-muted-foreground">
                    Map each source field to the destination field it should fill.
                </p>
            </div>

            {!hasSourceAndDestination && (
                <p className="text-sm text-muted-foreground">Configure a source and a destination first.</p>
            )}

            {processor && optionsLoading && <p className="text-sm text-muted-foreground">Loading available fields…</p>}

            {processor && !optionsLoading && optionsLoadFailed && (
                <p className="text-sm text-muted-foreground">
                    Couldn&apos;t load the source and destination fields. Try again in a moment.
                </p>
            )}

            {processor && !optionsLoading && !optionsLoadFailed && fieldsUnavailable && (
                <p className="text-sm text-muted-foreground">
                    This source and destination don&apos;t expose their fields automatically, so mapping them isn&apos;t
                    supported here yet.
                </p>
            )}

            {processor && !optionsLoading && !optionsLoadFailed && !fieldsUnavailable && (
                <>
                    <div className="flex items-center gap-2">
                        <Button
                            disabled={autoMapping}
                            icon={<SparklesIcon />}
                            label={autoMapping ? 'Mapping...' : 'Auto-map matching fields'}
                            onClick={handleAutoMap}
                            size="sm"
                            variant="outline"
                        />

                        <Button
                            icon={<PlusIcon />}
                            label="Add mapping"
                            onClick={handleAddMapping}
                            size="sm"
                            variant="ghost"
                        />
                    </div>

                    <fieldset className="space-y-2 border-0">
                        {mappings.map((mapping, index) => (
                            <div className="flex items-center gap-2" key={index}>
                                <Select
                                    onValueChange={(value) => handleMappingChange(index, {sourceField: value})}
                                    value={mapping.sourceField}
                                >
                                    <SelectTrigger aria-label={`Source field ${index + 1}`}>
                                        <SelectValue placeholder="Source field" />
                                    </SelectTrigger>

                                    <SelectContent>
                                        {sourceOptions.map((option) => (
                                            <SelectItem key={option.value} value={option.value}>
                                                {option.label}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>

                                <span className="text-sm text-muted-foreground">to</span>

                                <Select
                                    onValueChange={(value) => handleMappingChange(index, {destinationField: value})}
                                    value={mapping.destinationField}
                                >
                                    <SelectTrigger aria-label={`Destination field ${index + 1}`}>
                                        <SelectValue placeholder="Destination field" />
                                    </SelectTrigger>

                                    <SelectContent>
                                        {destinationOptions.map((option) => (
                                            <SelectItem key={option.value} value={option.value}>
                                                {option.label}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>

                                <Button
                                    aria-label={`Remove mapping ${index + 1}`}
                                    icon={<Trash2Icon />}
                                    onClick={() => handleRemoveMapping(index)}
                                    size="icon"
                                    variant="ghost"
                                />
                            </div>
                        ))}
                    </fieldset>
                </>
            )}
        </div>
    );
}
