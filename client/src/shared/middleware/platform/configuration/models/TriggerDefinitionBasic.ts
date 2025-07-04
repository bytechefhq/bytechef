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
import type { TriggerType } from './TriggerType';
import {
    TriggerTypeFromJSON,
    TriggerTypeFromJSONTyped,
    TriggerTypeToJSON,
    TriggerTypeToJSONTyped,
} from './TriggerType';
import type { Help } from './Help';
import {
    HelpFromJSON,
    HelpFromJSONTyped,
    HelpToJSON,
    HelpToJSONTyped,
} from './Help';

/**
 * A trigger definition defines ways to trigger workflows from the outside services.
 * @export
 * @interface TriggerDefinitionBasic
 */
export interface TriggerDefinitionBasic {
    /**
     * The component name.
     * @type {string}
     * @memberof TriggerDefinitionBasic
     */
    componentName: string;
    /**
     * The component version.
     * @type {number}
     * @memberof TriggerDefinitionBasic
     */
    componentVersion: number;
    /**
     * The description.
     * @type {string}
     * @memberof TriggerDefinitionBasic
     */
    description?: string;
    /**
     * 
     * @type {Help}
     * @memberof TriggerDefinitionBasic
     */
    help?: Help;
    /**
     * The action name.
     * @type {string}
     * @memberof TriggerDefinitionBasic
     */
    name: string;
    /**
     * Does trigger defines output.
     * @type {boolean}
     * @memberof TriggerDefinitionBasic
     */
    outputDefined: boolean;
    /**
     * Does trigger defines output function.
     * @type {boolean}
     * @memberof TriggerDefinitionBasic
     */
    outputFunctionDefined: boolean;
    /**
     * Does trigger defines output schema.
     * @type {boolean}
     * @memberof TriggerDefinitionBasic
     */
    outputSchemaDefined?: boolean;
    /**
     * The title
     * @type {string}
     * @memberof TriggerDefinitionBasic
     */
    title?: string;
    /**
     * 
     * @type {TriggerType}
     * @memberof TriggerDefinitionBasic
     */
    type: TriggerType;
}



/**
 * Check if a given object implements the TriggerDefinitionBasic interface.
 */
export function instanceOfTriggerDefinitionBasic(value: object): value is TriggerDefinitionBasic {
    if (!('componentName' in value) || value['componentName'] === undefined) return false;
    if (!('componentVersion' in value) || value['componentVersion'] === undefined) return false;
    if (!('name' in value) || value['name'] === undefined) return false;
    if (!('outputDefined' in value) || value['outputDefined'] === undefined) return false;
    if (!('outputFunctionDefined' in value) || value['outputFunctionDefined'] === undefined) return false;
    if (!('type' in value) || value['type'] === undefined) return false;
    return true;
}

export function TriggerDefinitionBasicFromJSON(json: any): TriggerDefinitionBasic {
    return TriggerDefinitionBasicFromJSONTyped(json, false);
}

export function TriggerDefinitionBasicFromJSONTyped(json: any, ignoreDiscriminator: boolean): TriggerDefinitionBasic {
    if (json == null) {
        return json;
    }
    return {
        
        'componentName': json['componentName'],
        'componentVersion': json['componentVersion'],
        'description': json['description'] == null ? undefined : json['description'],
        'help': json['help'] == null ? undefined : HelpFromJSON(json['help']),
        'name': json['name'],
        'outputDefined': json['outputDefined'],
        'outputFunctionDefined': json['outputFunctionDefined'],
        'outputSchemaDefined': json['outputSchemaDefined'] == null ? undefined : json['outputSchemaDefined'],
        'title': json['title'] == null ? undefined : json['title'],
        'type': TriggerTypeFromJSON(json['type']),
    };
}

export function TriggerDefinitionBasicToJSON(json: any): TriggerDefinitionBasic {
    return TriggerDefinitionBasicToJSONTyped(json, false);
}

export function TriggerDefinitionBasicToJSONTyped(value?: TriggerDefinitionBasic | null, ignoreDiscriminator: boolean = false): any {
    if (value == null) {
        return value;
    }

    return {
        
        'componentName': value['componentName'],
        'componentVersion': value['componentVersion'],
        'description': value['description'],
        'help': HelpToJSON(value['help']),
        'name': value['name'],
        'outputDefined': value['outputDefined'],
        'outputFunctionDefined': value['outputFunctionDefined'],
        'outputSchemaDefined': value['outputSchemaDefined'],
        'title': value['title'],
        'type': TriggerTypeToJSON(value['type']),
    };
}

