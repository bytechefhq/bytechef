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
import type { ResourcesModel } from './ResourcesModel';
import {
    ResourcesModelFromJSON,
    ResourcesModelFromJSONTyped,
    ResourcesModelToJSON,
} from './ResourcesModel';

/**
 * A task dispatcher defines a strategy for dispatching tasks to be executed.
 * @export
 * @interface TaskDispatcherDefinitionBasicModel
 */
export interface TaskDispatcherDefinitionBasicModel {
    /**
     * 
     * @type {DisplayModel}
     * @memberof TaskDispatcherDefinitionBasicModel
     */
    display: DisplayModel;
    /**
     * The task dispatcher name..
     * @type {string}
     * @memberof TaskDispatcherDefinitionBasicModel
     */
    name: string;
    /**
     * 
     * @type {ResourcesModel}
     * @memberof TaskDispatcherDefinitionBasicModel
     */
    resources?: ResourcesModel;
}

/**
 * Check if a given object implements the TaskDispatcherDefinitionBasicModel interface.
 */
export function instanceOfTaskDispatcherDefinitionBasicModel(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "display" in value;
    isInstance = isInstance && "name" in value;

    return isInstance;
}

export function TaskDispatcherDefinitionBasicModelFromJSON(json: any): TaskDispatcherDefinitionBasicModel {
    return TaskDispatcherDefinitionBasicModelFromJSONTyped(json, false);
}

export function TaskDispatcherDefinitionBasicModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): TaskDispatcherDefinitionBasicModel {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'display': DisplayModelFromJSON(json['display']),
        'name': json['name'],
        'resources': !exists(json, 'resources') ? undefined : ResourcesModelFromJSON(json['resources']),
    };
}

export function TaskDispatcherDefinitionBasicModelToJSON(value?: TaskDispatcherDefinitionBasicModel | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'display': DisplayModelToJSON(value.display),
        'name': value.name,
        'resources': ResourcesModelToJSON(value.resources),
    };
}

