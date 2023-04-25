/* tslint:disable */
/* eslint-disable */
/**
 * Definition API
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: 1
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

import { exists, mapValues } from '../runtime';
import type { ActionDefinitionBasicModel } from './ActionDefinitionBasicModel';
import {
    ActionDefinitionBasicModelFromJSON,
    ActionDefinitionBasicModelFromJSONTyped,
    ActionDefinitionBasicModelToJSON,
} from './ActionDefinitionBasicModel';
import type { ConnectionDefinitionBasicModel } from './ConnectionDefinitionBasicModel';
import {
    ConnectionDefinitionBasicModelFromJSON,
    ConnectionDefinitionBasicModelFromJSONTyped,
    ConnectionDefinitionBasicModelToJSON,
} from './ConnectionDefinitionBasicModel';
import type { ResourcesModel } from './ResourcesModel';
import {
    ResourcesModelFromJSON,
    ResourcesModelFromJSONTyped,
    ResourcesModelToJSON,
} from './ResourcesModel';
import type { TriggerDefinitionBasicModel } from './TriggerDefinitionBasicModel';
import {
    TriggerDefinitionBasicModelFromJSON,
    TriggerDefinitionBasicModelFromJSONTyped,
    TriggerDefinitionBasicModelToJSON,
} from './TriggerDefinitionBasicModel';

/**
 * A component contains a set of reusable code(actions) that accomplish specific tasks, triggers and connections if there is a need for a connection to an outside service.
 * @export
 * @interface ComponentDefinitionModel
 */
export interface ComponentDefinitionModel {
    /**
     * The list of all available actions the component can perform.
     * @type {Array<ActionDefinitionBasicModel>}
     * @memberof ComponentDefinitionModel
     */
    actions?: Array<ActionDefinitionBasicModel>;
    /**
     * The category.
     * @type {string}
     * @memberof ComponentDefinitionModel
     */
    category?: string;
    /**
     * 
     * @type {ConnectionDefinitionBasicModel}
     * @memberof ComponentDefinitionModel
     */
    connection?: ConnectionDefinitionBasicModel;
    /**
     * The description.
     * @type {string}
     * @memberof ComponentDefinitionModel
     */
    description?: string;
    /**
     * The icon.
     * @type {string}
     * @memberof ComponentDefinitionModel
     */
    icon?: string;
    /**
     * The name.
     * @type {string}
     * @memberof ComponentDefinitionModel
     */
    name: string;
    /**
     * 
     * @type {ResourcesModel}
     * @memberof ComponentDefinitionModel
     */
    resources?: ResourcesModel;
    /**
     * Tags for categorization.
     * @type {Array<string>}
     * @memberof ComponentDefinitionModel
     */
    tags?: Array<string>;
    /**
     * The title
     * @type {string}
     * @memberof ComponentDefinitionModel
     */
    title?: string;
    /**
     * The list of all available triggers the component can perform.
     * @type {Array<TriggerDefinitionBasicModel>}
     * @memberof ComponentDefinitionModel
     */
    triggers?: Array<TriggerDefinitionBasicModel>;
    /**
     * The version of a component.
     * @type {number}
     * @memberof ComponentDefinitionModel
     */
    version: number;
}

/**
 * Check if a given object implements the ComponentDefinitionModel interface.
 */
export function instanceOfComponentDefinitionModel(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "name" in value;
    isInstance = isInstance && "version" in value;

    return isInstance;
}

export function ComponentDefinitionModelFromJSON(json: any): ComponentDefinitionModel {
    return ComponentDefinitionModelFromJSONTyped(json, false);
}

export function ComponentDefinitionModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): ComponentDefinitionModel {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'actions': !exists(json, 'actions') ? undefined : ((json['actions'] as Array<any>).map(ActionDefinitionBasicModelFromJSON)),
        'category': !exists(json, 'category') ? undefined : json['category'],
        'connection': !exists(json, 'connection') ? undefined : ConnectionDefinitionBasicModelFromJSON(json['connection']),
        'description': !exists(json, 'description') ? undefined : json['description'],
        'icon': !exists(json, 'icon') ? undefined : json['icon'],
        'name': json['name'],
        'resources': !exists(json, 'resources') ? undefined : ResourcesModelFromJSON(json['resources']),
        'tags': !exists(json, 'tags') ? undefined : json['tags'],
        'title': !exists(json, 'title') ? undefined : json['title'],
        'triggers': !exists(json, 'triggers') ? undefined : ((json['triggers'] as Array<any>).map(TriggerDefinitionBasicModelFromJSON)),
        'version': json['version'],
    };
}

export function ComponentDefinitionModelToJSON(value?: ComponentDefinitionModel | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'actions': value.actions === undefined ? undefined : ((value.actions as Array<any>).map(ActionDefinitionBasicModelToJSON)),
        'category': value.category,
        'connection': ConnectionDefinitionBasicModelToJSON(value.connection),
        'description': value.description,
        'icon': value.icon,
        'name': value.name,
        'resources': ResourcesModelToJSON(value.resources),
        'tags': value.tags,
        'title': value.title,
        'triggers': value.triggers === undefined ? undefined : ((value.triggers as Array<any>).map(TriggerDefinitionBasicModelToJSON)),
        'version': value.version,
    };
}

