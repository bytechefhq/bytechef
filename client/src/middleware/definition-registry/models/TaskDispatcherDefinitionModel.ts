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
import type { DisplayModel } from './DisplayModel';
import {
    DisplayModelFromJSON,
    DisplayModelFromJSONTyped,
    DisplayModelToJSON,
} from './DisplayModel';
import type { PropertyModel } from './PropertyModel';
import {
    PropertyModelFromJSON,
    PropertyModelFromJSONTyped,
    PropertyModelToJSON,
} from './PropertyModel';
import type { ResourcesModel } from './ResourcesModel';
import {
    ResourcesModelFromJSON,
    ResourcesModelFromJSONTyped,
    ResourcesModelToJSON,
} from './ResourcesModel';

/**
 * A task dispatcher defines a strategy for dispatching tasks to be executed.
 * @export
 * @interface TaskDispatcherDefinitionModel
 */
export interface TaskDispatcherDefinitionModel {
    /**
     * 
     * @type {DisplayModel}
     * @memberof TaskDispatcherDefinitionModel
     */
    display?: DisplayModel;
    /**
     * The task dispatcher name..
     * @type {string}
     * @memberof TaskDispatcherDefinitionModel
     */
    name?: string;
    /**
     * The output schema of a task dispatching result.
     * @type {Array<PropertyModel>}
     * @memberof TaskDispatcherDefinitionModel
     */
    outputSchema?: Array<PropertyModel>;
    /**
     * A list of task dispatcher properties.
     * @type {Array<PropertyModel>}
     * @memberof TaskDispatcherDefinitionModel
     */
    properties?: Array<PropertyModel>;
    /**
     * 
     * @type {ResourcesModel}
     * @memberof TaskDispatcherDefinitionModel
     */
    resources?: ResourcesModel;
    /**
     * The version of a task dispatcher.
     * @type {number}
     * @memberof TaskDispatcherDefinitionModel
     */
    version?: number;
    /**
     * Properties used to define tasks to be dispatched.
     * @type {Array<PropertyModel>}
     * @memberof TaskDispatcherDefinitionModel
     */
    taskProperties?: Array<PropertyModel>;
}

/**
 * Check if a given object implements the TaskDispatcherDefinitionModel interface.
 */
export function instanceOfTaskDispatcherDefinitionModel(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function TaskDispatcherDefinitionModelFromJSON(json: any): TaskDispatcherDefinitionModel {
    return TaskDispatcherDefinitionModelFromJSONTyped(json, false);
}

export function TaskDispatcherDefinitionModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): TaskDispatcherDefinitionModel {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'display': !exists(json, 'display') ? undefined : DisplayModelFromJSON(json['display']),
        'name': !exists(json, 'name') ? undefined : json['name'],
        'outputSchema': !exists(json, 'outputSchema') ? undefined : ((json['outputSchema'] as Array<any>).map(PropertyModelFromJSON)),
        'properties': !exists(json, 'properties') ? undefined : ((json['properties'] as Array<any>).map(PropertyModelFromJSON)),
        'resources': !exists(json, 'resources') ? undefined : ResourcesModelFromJSON(json['resources']),
        'version': !exists(json, 'version') ? undefined : json['version'],
        'taskProperties': !exists(json, 'taskProperties') ? undefined : ((json['taskProperties'] as Array<any>).map(PropertyModelFromJSON)),
    };
}

export function TaskDispatcherDefinitionModelToJSON(value?: TaskDispatcherDefinitionModel | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'display': DisplayModelToJSON(value.display),
        'name': value.name,
        'outputSchema': value.outputSchema === undefined ? undefined : ((value.outputSchema as Array<any>).map(PropertyModelToJSON)),
        'properties': value.properties === undefined ? undefined : ((value.properties as Array<any>).map(PropertyModelToJSON)),
        'resources': ResourcesModelToJSON(value.resources),
        'version': value.version,
        'taskProperties': value.taskProperties === undefined ? undefined : ((value.taskProperties as Array<any>).map(PropertyModelToJSON)),
    };
}

