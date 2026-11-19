import {ComponentDefinition} from '@/shared/middleware/platform/configuration';

import {getComponentInputGroupLabel} from './getComponentInputGroupLabel';

export interface ComponentPropertyInputValuesI {
    componentName?: string;
    componentVersion?: number;
    groupName?: string;
    label?: string;
    name?: string;
}

export interface ResolveComponentPropertyInputArgsI {
    componentDefinition?: ComponentDefinition;
    componentName: string;
    componentVersion?: number;
    currentLabel: string;
    currentName: string;
    selection: string;
}

export function getComponentPropertyInputValues({
    componentDefinition,
    componentName,
    componentVersion,
    currentLabel,
    currentName,
    selection,
}: ResolveComponentPropertyInputArgsI): ComponentPropertyInputValuesI {
    const [kind, ...rest] = selection.split(':');

    const groupName = rest.join(':');

    if (kind !== 'group' || !groupName) {
        return {componentName, componentVersion};
    }

    const group = componentDefinition?.inputs?.find((current) => current.name === groupName);

    const itemLabel = group ? getComponentInputGroupLabel(group) : groupName;

    return {
        componentName,
        componentVersion,
        groupName,
        label: currentLabel === '' ? itemLabel : currentLabel,
        name: currentName === '' ? groupName : currentName,
    };
}
