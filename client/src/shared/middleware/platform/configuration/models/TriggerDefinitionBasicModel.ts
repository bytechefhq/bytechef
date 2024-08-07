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
import type { HelpModel } from './HelpModel';
import {
    HelpModelFromJSON,
    HelpModelFromJSONTyped,
    HelpModelToJSON,
} from './HelpModel';
import type { TriggerTypeModel } from './TriggerTypeModel';
import {
    TriggerTypeModelFromJSON,
    TriggerTypeModelFromJSONTyped,
    TriggerTypeModelToJSON,
} from './TriggerTypeModel';

/**
 * A trigger definition defines ways to trigger workflows from the outside services.
 * @export
 * @interface TriggerDefinitionBasicModel
 */
export interface TriggerDefinitionBasicModel {
    /**
     * The description.
     * @type {string}
     * @memberof TriggerDefinitionBasicModel
     */
    description?: string;
    /**
     * 
     * @type {HelpModel}
     * @memberof TriggerDefinitionBasicModel
     */
    help?: HelpModel;
    /**
     * The action name.
     * @type {string}
     * @memberof TriggerDefinitionBasicModel
     */
    name: string;
    /**
     * The title
     * @type {string}
     * @memberof TriggerDefinitionBasicModel
     */
    title?: string;
    /**
     * 
     * @type {TriggerTypeModel}
     * @memberof TriggerDefinitionBasicModel
     */
    type: TriggerTypeModel;
}

/**
 * Check if a given object implements the TriggerDefinitionBasicModel interface.
 */
export function instanceOfTriggerDefinitionBasicModel(value: object): boolean {
    if (!('name' in value)) return false;
    if (!('type' in value)) return false;
    return true;
}

export function TriggerDefinitionBasicModelFromJSON(json: any): TriggerDefinitionBasicModel {
    return TriggerDefinitionBasicModelFromJSONTyped(json, false);
}

export function TriggerDefinitionBasicModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): TriggerDefinitionBasicModel {
    if (json == null) {
        return json;
    }
    return {
        
        'description': json['description'] == null ? undefined : json['description'],
        'help': json['help'] == null ? undefined : HelpModelFromJSON(json['help']),
        'name': json['name'],
        'title': json['title'] == null ? undefined : json['title'],
        'type': TriggerTypeModelFromJSON(json['type']),
    };
}

export function TriggerDefinitionBasicModelToJSON(value?: TriggerDefinitionBasicModel | null): any {
    if (value == null) {
        return value;
    }
    return {
        
        'description': value['description'],
        'help': HelpModelToJSON(value['help']),
        'name': value['name'],
        'title': value['title'],
        'type': TriggerTypeModelToJSON(value['type']),
    };
}

