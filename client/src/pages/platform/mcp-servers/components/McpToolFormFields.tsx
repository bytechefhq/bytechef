import LetAiFillAllButton from '@/pages/platform/mcp-servers/components/LetAiFillAllButton';
import {
    LoadPropertyOptionsType,
    collectHiddenToolPaths,
    computeAiFillValues,
    isEmptyToolValue,
    loadNoPropertyOptions,
    summarizeAiFillCandidates,
} from '@/pages/platform/mcp-servers/utils/mcpToolAiFill';
import Properties from '@/pages/platform/workflow-editor/components/properties/Properties';
import {PropertyAllType} from '@/shared/types';
import {useEffect, useMemo, useRef, useState} from 'react';
import {UseFormReturn} from 'react-hook-form';

interface McpToolFormFieldsProps {
    displayConditions?: Record<string, boolean>;
    form: UseFormReturn<Record<string, unknown>>;
    loadOptions?: LoadPropertyOptionsType;
    properties: PropertyAllType[];
    values: Record<string, unknown>;
}

interface OptionsProbeResultI {
    fillable: boolean;
    optionsCandidatesKey: string;
}

const HEADER_PROPERTY_NAMES = new Set(['toolDescription', 'toolName']);

const McpToolFormFields = ({displayConditions, form, loadOptions, properties, values}: McpToolFormFieldsProps) => {
    const [advancedPropertiesOpen, setAdvancedPropertiesOpen] = useState(false);
    const [isFilling, setIsFilling] = useState(false);
    const [optionsProbeResult, setOptionsProbeResult] = useState<OptionsProbeResultI>();

    const candidateParamsRef = useRef({
        hiddenPaths: new Set<string>(),
        includeAdvanced: false,
        properties,
    });

    const {control, formState} = form;

    const headerProperties = useMemo(
        () => properties.filter((property) => property.name && HEADER_PROPERTY_NAMES.has(property.name)),
        [properties]
    );

    const bodyProperties = useMemo(
        () => properties.filter((property) => !property.name || !HEADER_PROPERTY_NAMES.has(property.name)),
        [properties]
    );

    const hiddenPaths = useMemo(
        () => collectHiddenToolPaths(properties, displayConditions),
        [displayConditions, properties]
    );

    const {hasStaticCandidates, optionsCandidatesKey} = useMemo(
        () =>
            summarizeAiFillCandidates({
                hiddenPaths,
                includeAdvanced: advancedPropertiesOpen,
                includeConditional: true,
                optionsLoadable: !!loadOptions,
                properties,
                values,
            }),
        [advancedPropertiesOpen, hiddenPaths, loadOptions, properties, values]
    );

    const hasEligibleFields =
        hasStaticCandidates ||
        (optionsCandidatesKey !== '' &&
            optionsProbeResult?.optionsCandidatesKey === optionsCandidatesKey &&
            optionsProbeResult.fillable);

    candidateParamsRef.current = {hiddenPaths, includeAdvanced: advancedPropertiesOpen, properties};

    const handleClick = async () => {
        setIsFilling(true);

        try {
            const aiValues = await computeAiFillValues({
                hiddenPaths,
                includeAdvanced: advancedPropertiesOpen,
                includeConditional: true,
                loadOptions: loadOptions || loadNoPropertyOptions,
                properties,
                values: form.getValues(),
            });

            for (const [path, expression] of Object.entries(aiValues)) {
                if (isEmptyToolValue(form.getValues(path))) {
                    form.setValue(path, expression, {shouldDirty: true});
                }
            }
        } finally {
            setIsFilling(false);
        }
    };

    useEffect(() => {
        if (hasStaticCandidates || optionsCandidatesKey === '' || !loadOptions) {
            return;
        }

        let cancelled = false;

        const probeOptions = async () => {
            const aiValues = await computeAiFillValues({
                ...candidateParamsRef.current,
                includeConditional: true,
                loadOptions,
                values: form.getValues(),
            }).catch(() => ({}));

            if (!cancelled) {
                setOptionsProbeResult({fillable: Object.keys(aiValues).length > 0, optionsCandidatesKey});
            }
        };

        void probeOptions();

        return () => {
            cancelled = true;
        };
    }, [form, hasStaticCandidates, loadOptions, optionsCandidatesKey]);

    return (
        <fieldset className="space-y-4 border-0 p-0">
            {headerProperties.length > 0 && (
                <Properties
                    control={control}
                    controlPath=""
                    formDisplayConditions={displayConditions}
                    formState={formState}
                    properties={headerProperties}
                    toolsMode
                />
            )}

            <LetAiFillAllButton disabled={!hasEligibleFields || isFilling} onClick={handleClick} />

            {bodyProperties.length > 0 && (
                <Properties
                    control={control}
                    controlPath=""
                    formDisplayConditions={displayConditions}
                    formState={formState}
                    onAdvancedPropertiesOpenChange={setAdvancedPropertiesOpen}
                    properties={bodyProperties}
                    toolsMode
                />
            )}
        </fieldset>
    );
};

export default McpToolFormFields;
