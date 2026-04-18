import {useEffect, useMemo} from 'react';
import {Control, FieldValues, useWatch} from 'react-hook-form';

interface FormLookupValuesWatcherPropsI {
    arrayIndex?: number;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    control: Control<any, any>;
    optionsLookupDependsOn?: Array<string>;
    propertiesLookupDependsOn?: Array<string>;
    setLookupDependsOnValues: (values: Array<unknown> | undefined) => void;
}

/**
 * Subscribes to react-hook-form value changes for a property's lookup-dependency paths so
 * dynamic-property and options queries refetch as the user types, instead of waiting for a
 * save round-trip to propagate new values through currentComponent.parameters.
 */
const FormLookupValuesWatcher = ({
    arrayIndex,
    control,
    optionsLookupDependsOn,
    propertiesLookupDependsOn,
    setLookupDependsOnValues,
}: FormLookupValuesWatcherPropsI) => {
    const dependsOnPaths = propertiesLookupDependsOn ?? optionsLookupDependsOn;

    const resolvedPaths = useMemo(
        () => dependsOnPaths?.map((path) => path.replace('[index]', `[${arrayIndex}]`)) ?? [],
        [arrayIndex, dependsOnPaths]
    );

    const watchedValues = useWatch({control: control as Control<FieldValues>, name: resolvedPaths}) as unknown[];

    useEffect(() => {
        if (!dependsOnPaths || dependsOnPaths.length === 0) {
            return;
        }

        const sanitizedValues = (watchedValues ?? []).map((value) =>
            typeof value === 'string' && value.startsWith('=fromAi(') ? undefined : value
        );

        setLookupDependsOnValues(sanitizedValues);
    }, [dependsOnPaths, setLookupDependsOnValues, watchedValues]);

    return null;
};

export default FormLookupValuesWatcher;
