/* tslint:disable */
/* eslint-disable */
/**
 * The Platform Configuration Internal API
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: 1
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

import { mapValues } from '../runtime';
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
     * The description.
     * @type {string}
     * @memberof TaskDispatcherDefinitionBasicModel
     */
    description?: string;
    /**
     * The icon.
     * @type {string}
     * @memberof TaskDispatcherDefinitionBasicModel
     */
    icon?: string;
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
    /**
     * The title
     * @type {string}
     * @memberof TaskDispatcherDefinitionBasicModel
     */
    title?: string;
}

/**
 * Check if a given object implements the TaskDispatcherDefinitionBasicModel interface.
 */
export function instanceOfTaskDispatcherDefinitionBasicModel(value: object): boolean {
    if (!('name' in value)) return false;
    return true;
}

export function TaskDispatcherDefinitionBasicModelFromJSON(json: any): TaskDispatcherDefinitionBasicModel {
    return TaskDispatcherDefinitionBasicModelFromJSONTyped(json, false);
}

export function TaskDispatcherDefinitionBasicModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): TaskDispatcherDefinitionBasicModel {
    if (json == null) {
        return json;
    }
    return {
        
        'description': json['description'] == null ? undefined : json['description'],
        'icon': json['icon'] == null ? undefined : json['icon'],
        'name': json['name'],
        'resources': json['resources'] == null ? undefined : ResourcesModelFromJSON(json['resources']),
        'title': json['title'] == null ? undefined : json['title'],
    };
}

export function TaskDispatcherDefinitionBasicModelToJSON(value?: TaskDispatcherDefinitionBasicModel | null): any {
    if (value == null) {
        return value;
    }
    return {
        
        'description': value['description'],
        'icon': value['icon'],
        'name': value['name'],
        'resources': ResourcesModelToJSON(value['resources']),
        'title': value['title'],
    };
}

