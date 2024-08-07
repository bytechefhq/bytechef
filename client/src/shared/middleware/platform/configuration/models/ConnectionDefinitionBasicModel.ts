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
/**
 * Definition of a connection to an outside service.
 * @export
 * @interface ConnectionDefinitionBasicModel
 */
export interface ConnectionDefinitionBasicModel {
    /**
     * The description used from the connection's component.
     * @type {string}
     * @memberof ConnectionDefinitionBasicModel
     */
    componentDescription?: string;
    /**
     * The component name used from the connection's component.
     * @type {string}
     * @memberof ConnectionDefinitionBasicModel
     */
    componentName: string;
    /**
     * The title used from the connection's component
     * @type {string}
     * @memberof ConnectionDefinitionBasicModel
     */
    componentTitle?: string;
    /**
     * The version of a connection.
     * @type {number}
     * @memberof ConnectionDefinitionBasicModel
     */
    version: number;
}

/**
 * Check if a given object implements the ConnectionDefinitionBasicModel interface.
 */
export function instanceOfConnectionDefinitionBasicModel(value: object): boolean {
    if (!('componentName' in value)) return false;
    if (!('version' in value)) return false;
    return true;
}

export function ConnectionDefinitionBasicModelFromJSON(json: any): ConnectionDefinitionBasicModel {
    return ConnectionDefinitionBasicModelFromJSONTyped(json, false);
}

export function ConnectionDefinitionBasicModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): ConnectionDefinitionBasicModel {
    if (json == null) {
        return json;
    }
    return {
        
        'componentDescription': json['componentDescription'] == null ? undefined : json['componentDescription'],
        'componentName': json['componentName'],
        'componentTitle': json['componentTitle'] == null ? undefined : json['componentTitle'],
        'version': json['version'],
    };
}

export function ConnectionDefinitionBasicModelToJSON(value?: ConnectionDefinitionBasicModel | null): any {
    if (value == null) {
        return value;
    }
    return {
        
        'componentDescription': value['componentDescription'],
        'componentName': value['componentName'],
        'componentTitle': value['componentTitle'],
        'version': value['version'],
    };
}

