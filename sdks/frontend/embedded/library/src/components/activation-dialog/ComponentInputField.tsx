import {useEffect, useState} from 'react';
import {createApiClient} from '../shared/apiClient';
import styles from './ActivationDialog.module.css';
import {TemplateInputComponentReferenceType, TemplateInputOptionType} from './types';

interface LoadedOptionsType {
    connectionId: number;
    failed: boolean;
    options: TemplateInputOptionType[];
}

interface ComponentInputFieldProps {
    componentReference: TemplateInputComponentReferenceType;
    componentTitle: string;
    connectionId?: number;
    fetch: ReturnType<typeof createApiClient>['fetch'];
    label: string;
    name: string;
    onChange: (value: string | undefined) => void;
    required?: boolean;
    value: string;
}

const ComponentInputField = ({
    componentReference,
    componentTitle,
    connectionId,
    fetch,
    label,
    name,
    onChange,
    required,
    value,
}: ComponentInputFieldProps) => {
    const [loadedOptions, setLoadedOptions] = useState<LoadedOptionsType>();

    const {componentName, componentVersion, groupName} = componentReference;
    const id = `activation-input-${name}`;
    const loaded = connectionId !== undefined && loadedOptions?.connectionId === connectionId;
    const options = loaded ? loadedOptions.options : [];

    let placeholder = `Select ${label}`;

    if (connectionId !== undefined && !loaded) {
        placeholder = 'Loading...';
    } else if (loaded && loadedOptions.failed) {
        placeholder = "Couldn't load the options";
    }

    useEffect(() => {
        if (connectionId === undefined) {
            return;
        }

        let cancelled = false;

        fetch<TemplateInputOptionType[]>('/api/embedded/v1/automation/component-input-options', {
            body: {
                componentName,
                componentVersion,
                connectionId,
                groupName,
                lookupDependsOnValues: {},
                propertyName: groupName,
            },
            method: 'POST',
        })
            .then((fetchedOptions) => ({connectionId, failed: false, options: fetchedOptions ?? []}))
            .catch((error: unknown) => {
                console.error(`Failed to load the ${componentName} options of ${name}:`, error);

                return {connectionId, failed: true, options: []};
            })
            .then((result: LoadedOptionsType) => {
                if (!cancelled) {
                    setLoadedOptions(result);
                }
            });

        return () => {
            cancelled = true;
        };
    }, [componentName, componentVersion, connectionId, fetch, groupName, name]);

    return (
        <div className={styles.field}>
            <label className={styles.label} htmlFor={id}>
                {label}

                {required && <span className={styles.required}>*</span>}
            </label>

            <select
                className={styles.select}
                disabled={!loaded || loadedOptions.failed}
                id={id}
                onChange={(event) => onChange(event.target.value === '' ? undefined : event.target.value)}
                required={required}
                value={value}
            >
                <option value="">{placeholder}</option>

                {options.map((option) => (
                    <option key={option.value} value={option.value}>
                        {option.label}
                    </option>
                ))}
            </select>

            {connectionId === undefined && <p className={styles.notice}>Pick a {componentTitle} connection first</p>}
        </div>
    );
};

export default ComponentInputField;
