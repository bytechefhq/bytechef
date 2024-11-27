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
import type { Help } from './Help';
import {
    HelpFromJSON,
    HelpFromJSONTyped,
    HelpToJSON,
    HelpToJSONTyped,
} from './Help';

/**
 * An action is a portion of reusable code that accomplish a specific task. When building a workflow, each action is represented as a task inside the workflow. The task 'type' property is defined as [component name]/v[component version]/[action name]. Action properties are used to set properties of the task inside the workflow.
 * @export
 * @interface ActionDefinitionBasic
 */
export interface ActionDefinitionBasic {
    /**
     * The description.
     * @type {string}
     * @memberof ActionDefinitionBasic
     */
    description?: string;
    /**
     * 
     * @type {Help}
     * @memberof ActionDefinitionBasic
     */
    help?: Help;
    /**
     * The action name.
     * @type {string}
     * @memberof ActionDefinitionBasic
     */
    name: string;
    /**
     * The title
     * @type {string}
     * @memberof ActionDefinitionBasic
     */
    title?: string;
}

/**
 * Check if a given object implements the ActionDefinitionBasic interface.
 */
export function instanceOfActionDefinitionBasic(value: object): value is ActionDefinitionBasic {
    if (!('name' in value) || value['name'] === undefined) return false;
    return true;
}

export function ActionDefinitionBasicFromJSON(json: any): ActionDefinitionBasic {
    return ActionDefinitionBasicFromJSONTyped(json, false);
}

export function ActionDefinitionBasicFromJSONTyped(json: any, ignoreDiscriminator: boolean): ActionDefinitionBasic {
    if (json == null) {
        return json;
    }
    return {
        
        'description': json['description'] == null ? undefined : json['description'],
        'help': json['help'] == null ? undefined : HelpFromJSON(json['help']),
        'name': json['name'],
        'title': json['title'] == null ? undefined : json['title'],
    };
}

export function ActionDefinitionBasicToJSON(json: any): ActionDefinitionBasic {
    return ActionDefinitionBasicToJSONTyped(json, false);
}

export function ActionDefinitionBasicToJSONTyped(value?: ActionDefinitionBasic | null, ignoreDiscriminator: boolean = false): any {
    if (value == null) {
        return value;
    }

    return {
        
        'description': value['description'],
        'help': HelpToJSON(value['help']),
        'name': value['name'],
        'title': value['title'],
    };
}

