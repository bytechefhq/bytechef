/* tslint:disable */
/* eslint-disable */
/**
 * The Platform Configuration API
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
import type { HelpModel } from './HelpModel';
import {
    HelpModelFromJSON,
    HelpModelFromJSONTyped,
    HelpModelToJSON,
} from './HelpModel';

/**
 * An action is a portion of reusable code that accomplish a specific task. When building a workflow, each action is represented as a task inside the workflow. The task 'type' property is defined as [component name]/v[component version]/[action name]. Action properties are used to set properties of the task inside the workflow.
 * @export
 * @interface ActionDefinitionBasicModel
 */
export interface ActionDefinitionBasicModel {
    /**
     * The description.
     * @type {string}
     * @memberof ActionDefinitionBasicModel
     */
    description?: string;
    /**
     * 
     * @type {HelpModel}
     * @memberof ActionDefinitionBasicModel
     */
    help?: HelpModel;
    /**
     * The action name.
     * @type {string}
     * @memberof ActionDefinitionBasicModel
     */
    name: string;
    /**
     * The title
     * @type {string}
     * @memberof ActionDefinitionBasicModel
     */
    title?: string;
}

/**
 * Check if a given object implements the ActionDefinitionBasicModel interface.
 */
export function instanceOfActionDefinitionBasicModel(value: object): boolean {
    if (!('name' in value)) return false;
    return true;
}

export function ActionDefinitionBasicModelFromJSON(json: any): ActionDefinitionBasicModel {
    return ActionDefinitionBasicModelFromJSONTyped(json, false);
}

export function ActionDefinitionBasicModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): ActionDefinitionBasicModel {
    if (json == null) {
        return json;
    }
    return {
        
        'description': json['description'] == null ? undefined : json['description'],
        'help': json['help'] == null ? undefined : HelpModelFromJSON(json['help']),
        'name': json['name'],
        'title': json['title'] == null ? undefined : json['title'],
    };
}

export function ActionDefinitionBasicModelToJSON(value?: ActionDefinitionBasicModel | null): any {
    if (value == null) {
        return value;
    }
    return {
        
        'description': value['description'],
        'help': HelpModelToJSON(value['help']),
        'name': value['name'],
        'title': value['title'],
    };
}

