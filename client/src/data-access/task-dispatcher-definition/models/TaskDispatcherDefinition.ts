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
import type { Property } from './Property';
import {
    PropertyFromJSON,
    PropertyFromJSONTyped,
    PropertyToJSON,
} from './Property';
import type { Resources } from './Resources';
import {
    ResourcesFromJSON,
    ResourcesFromJSONTyped,
    ResourcesToJSON,
} from './Resources';

/**
 * A task dispatcher defines a strategy for dispatching tasks to be executed.
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
     * The connection name.
     * @type {string}
     * @memberof TaskDispatcherDefinition
     */
    name?: string;
    /**
     * The output schema of a task dispatching result.
     * @type {Array<Property>}
     * @memberof TaskDispatcherDefinition
     */
    output?: Array<Property>;
    /**
     * The list of task dispatcher properties.
     * @type {Array<Property>}
     * @memberof TaskDispatcherDefinition
     */
    properties?: Array<Property>;
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
    /**
     * Properties used to define tasks to be dispatched.
     * @type {Array<Property>}
     * @memberof TaskDispatcherDefinition
     */
    taskProperties?: Array<Property>;
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
        'name': !exists(json, 'name') ? undefined : json['name'],
        'output': !exists(json, 'output') ? undefined : ((json['output'] as Array<any>).map(PropertyFromJSON)),
        'properties': !exists(json, 'properties') ? undefined : ((json['properties'] as Array<any>).map(PropertyFromJSON)),
        'resources': !exists(json, 'resources') ? undefined : ResourcesFromJSON(json['resources']),
        'version': !exists(json, 'version') ? undefined : json['version'],
        'taskProperties': !exists(json, 'taskProperties') ? undefined : ((json['taskProperties'] as Array<any>).map(PropertyFromJSON)),
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
        'name': value.name,
        'output': value.output === undefined ? undefined : ((value.output as Array<any>).map(PropertyToJSON)),
        'properties': value.properties === undefined ? undefined : ((value.properties as Array<any>).map(PropertyToJSON)),
        'resources': ResourcesToJSON(value.resources),
        'version': value.version,
        'taskProperties': value.taskProperties === undefined ? undefined : ((value.taskProperties as Array<any>).map(PropertyToJSON)),
    };
}

