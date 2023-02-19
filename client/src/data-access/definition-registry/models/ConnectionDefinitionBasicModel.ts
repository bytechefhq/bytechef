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

/**
 * Definition of a connection to an outside service.
 * @export
 * @interface ConnectionDefinitionBasicModel
 */
export interface ConnectionDefinitionBasicModel {
    /**
     * The name of a component this connection can be used for.
     * @type {string}
     * @memberof ConnectionDefinitionBasicModel
     */
    componentName: string;
    /**
     * 
     * @type {DisplayModel}
     * @memberof ConnectionDefinitionBasicModel
     */
    display: DisplayModel;
    /**
     * The component version.
     * @type {number}
     * @memberof ConnectionDefinitionBasicModel
     */
    version: number;
}

/**
 * Check if a given object implements the ConnectionDefinitionBasicModel interface.
 */
export function instanceOfConnectionDefinitionBasicModel(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "componentName" in value;
    isInstance = isInstance && "display" in value;
    isInstance = isInstance && "version" in value;

    return isInstance;
}

export function ConnectionDefinitionBasicModelFromJSON(json: any): ConnectionDefinitionBasicModel {
    return ConnectionDefinitionBasicModelFromJSONTyped(json, false);
}

export function ConnectionDefinitionBasicModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): ConnectionDefinitionBasicModel {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'componentName': json['componentName'],
        'display': DisplayModelFromJSON(json['display']),
        'version': json['version'],
    };
}

export function ConnectionDefinitionBasicModelToJSON(value?: ConnectionDefinitionBasicModel | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'componentName': value.componentName,
        'display': DisplayModelToJSON(value.display),
        'version': value.version,
    };
}

