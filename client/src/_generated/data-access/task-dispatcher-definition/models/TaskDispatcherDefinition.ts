/* tslint:disable */
/* eslint-disable */
/**
 * OpenAPI definition
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: v1
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

import { exists, mapValues } from '../runtime';
import type { Display } from './Display';
import {
    DisplayFromJSON,
    DisplayFromJSONTyped,
    DisplayToJSON,
} from './Display';
import type { Resources } from './Resources';
import {
    ResourcesFromJSON,
    ResourcesFromJSONTyped,
    ResourcesToJSON,
} from './Resources';
import type { TaskDispatcherDefinitionInputsInner } from './TaskDispatcherDefinitionInputsInner';
import {
    TaskDispatcherDefinitionInputsInnerFromJSON,
    TaskDispatcherDefinitionInputsInnerFromJSONTyped,
    TaskDispatcherDefinitionInputsInnerToJSON,
} from './TaskDispatcherDefinitionInputsInner';

/**
 * 
 * @export
 * @interface TaskDispatcherDefinition
 */
export interface TaskDispatcherDefinition {
    /**
     * 
     * @type {Display}
     * @memberof TaskDispatcherDefinition
     */
    display?: Display;
    /**
     * 
     * @type {Array<TaskDispatcherDefinitionInputsInner>}
     * @memberof TaskDispatcherDefinition
     */
    inputs?: Array<TaskDispatcherDefinitionInputsInner>;
    /**
     * 
     * @type {string}
     * @memberof TaskDispatcherDefinition
     */
    name?: string;
    /**
     * 
     * @type {Resources}
     * @memberof TaskDispatcherDefinition
     */
    resources?: Resources;
    /**
     * 
     * @type {number}
     * @memberof TaskDispatcherDefinition
     */
    version?: number;
}

/**
 * Check if a given object implements the TaskDispatcherDefinition interface.
 */
export function instanceOfTaskDispatcherDefinition(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function TaskDispatcherDefinitionFromJSON(json: any): TaskDispatcherDefinition {
    return TaskDispatcherDefinitionFromJSONTyped(json, false);
}

export function TaskDispatcherDefinitionFromJSONTyped(json: any, ignoreDiscriminator: boolean): TaskDispatcherDefinition {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'display': !exists(json, 'display') ? undefined : DisplayFromJSON(json['display']),
        'inputs': !exists(json, 'inputs') ? undefined : ((json['inputs'] as Array<any>).map(TaskDispatcherDefinitionInputsInnerFromJSON)),
        'name': !exists(json, 'name') ? undefined : json['name'],
        'resources': !exists(json, 'resources') ? undefined : ResourcesFromJSON(json['resources']),
        'version': !exists(json, 'version') ? undefined : json['version'],
    };
}

export function TaskDispatcherDefinitionToJSON(value?: TaskDispatcherDefinition | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'display': DisplayToJSON(value.display),
        'inputs': value.inputs === undefined ? undefined : ((value.inputs as Array<any>).map(TaskDispatcherDefinitionInputsInnerToJSON)),
        'name': value.name,
        'resources': ResourcesToJSON(value.resources),
        'version': value.version,
    };
}

