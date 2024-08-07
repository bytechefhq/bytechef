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
import type { PropertyModel } from './PropertyModel';
import {
    PropertyModelFromJSON,
    PropertyModelFromJSONTyped,
    PropertyModelToJSON,
} from './PropertyModel';

/**
 * An action is a portion of reusable code that accomplish a specific task. When building a workflow, each action is represented as a task inside the workflow. The task 'type' property is defined as [component name]/v[component version]/[action name]. Action properties are used to set properties of the task inside the workflow.
 * @export
 * @interface ActionDefinitionModel
 */
export interface ActionDefinitionModel {
    /**
     * The component name.
     * @type {string}
     * @memberof ActionDefinitionModel
     */
    componentName?: string;
    /**
     * The component version.
     * @type {number}
     * @memberof ActionDefinitionModel
     */
    componentVersion?: number;
    /**
     * Does action define dynamic output schema.
     * @type {boolean}
     * @memberof ActionDefinitionModel
     */
    dynamicOutput: boolean;
    /**
     * The description.
     * @type {string}
     * @memberof ActionDefinitionModel
     */
    description?: string;
    /**
     * 
     * @type {HelpModel}
     * @memberof ActionDefinitionModel
     */
    help?: HelpModel;
    /**
     * The action name.
     * @type {string}
     * @memberof ActionDefinitionModel
     */
    name: string;
    /**
     * Does action define output schema.
     * @type {boolean}
     * @memberof ActionDefinitionModel
     */
    outputDefined: boolean;
    /**
     * The list of action properties.
     * @type {Array<PropertyModel>}
     * @memberof ActionDefinitionModel
     */
    properties?: Array<PropertyModel>;
    /**
     * The title
     * @type {string}
     * @memberof ActionDefinitionModel
     */
    title?: string;
    /**
     * Does action define dynamic node description.
     * @type {boolean}
     * @memberof ActionDefinitionModel
     */
    workflowNodeDescriptionDefined?: boolean;
}

/**
 * Check if a given object implements the ActionDefinitionModel interface.
 */
export function instanceOfActionDefinitionModel(value: object): boolean {
    if (!('dynamicOutput' in value)) return false;
    if (!('name' in value)) return false;
    if (!('outputDefined' in value)) return false;
    return true;
}

export function ActionDefinitionModelFromJSON(json: any): ActionDefinitionModel {
    return ActionDefinitionModelFromJSONTyped(json, false);
}

export function ActionDefinitionModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): ActionDefinitionModel {
    if (json == null) {
        return json;
    }
    return {
        
        'componentName': json['componentName'] == null ? undefined : json['componentName'],
        'componentVersion': json['componentVersion'] == null ? undefined : json['componentVersion'],
        'dynamicOutput': json['dynamicOutput'],
        'description': json['description'] == null ? undefined : json['description'],
        'help': json['help'] == null ? undefined : HelpModelFromJSON(json['help']),
        'name': json['name'],
        'outputDefined': json['outputDefined'],
        'properties': json['properties'] == null ? undefined : ((json['properties'] as Array<any>).map(PropertyModelFromJSON)),
        'title': json['title'] == null ? undefined : json['title'],
        'workflowNodeDescriptionDefined': json['workflowNodeDescriptionDefined'] == null ? undefined : json['workflowNodeDescriptionDefined'],
    };
}

export function ActionDefinitionModelToJSON(value?: ActionDefinitionModel | null): any {
    if (value == null) {
        return value;
    }
    return {
        
        'componentName': value['componentName'],
        'componentVersion': value['componentVersion'],
        'dynamicOutput': value['dynamicOutput'],
        'description': value['description'],
        'help': HelpModelToJSON(value['help']),
        'name': value['name'],
        'outputDefined': value['outputDefined'],
        'properties': value['properties'] == null ? undefined : ((value['properties'] as Array<any>).map(PropertyModelToJSON)),
        'title': value['title'],
        'workflowNodeDescriptionDefined': value['workflowNodeDescriptionDefined'],
    };
}

