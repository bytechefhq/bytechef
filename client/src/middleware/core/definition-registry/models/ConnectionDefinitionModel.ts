/* tslint:disable */
/* eslint-disable */
/**
 * Core Definition API
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
import type { AuthorizationModel } from './AuthorizationModel';
import {
    AuthorizationModelFromJSON,
    AuthorizationModelFromJSONTyped,
    AuthorizationModelToJSON,
} from './AuthorizationModel';
import type { PropertyModel } from './PropertyModel';
import {
    PropertyModelFromJSON,
    PropertyModelFromJSONTyped,
    PropertyModelToJSON,
} from './PropertyModel';

/**
 * Definition of a connection to an outside service.
 * @export
 * @interface ConnectionDefinitionModel
 */
export interface ConnectionDefinitionModel {
    /**
     *
     * @type {Array<AuthorizationModel>}
     * @memberof ConnectionDefinitionModel
     */
    authorizations?: Array<AuthorizationModel>;
    /**
     * Defines the base URI for all future HTTP requests.
     * @type {string}
     * @memberof ConnectionDefinitionModel
     */
    baseUri?: string;
    /**
     * The description used from the connection's component.
     * @type {string}
     * @memberof ConnectionDefinitionModel
     */
    componentDescription?: string;
    /**
     * The connection name used from the connection's component.
     * @type {string}
     * @memberof ConnectionDefinitionModel
     */
    componentName: string;
    /**
     * The properties of the connection.
     * @type {Array<PropertyModel>}
     * @memberof ConnectionDefinitionModel
     */
    properties?: Array<PropertyModel>;
    /**
     * The title used from the connection's component.
     * @type {string}
     * @memberof ConnectionDefinitionModel
     */
    componentTitle?: string;
    /**
     * The version of a connection.
     * @type {number}
     * @memberof ConnectionDefinitionModel
     */
    version: number;
}

/**
 * Check if a given object implements the ConnectionDefinitionModel interface.
 */
export function instanceOfConnectionDefinitionModel(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "componentName" in value;
    isInstance = isInstance && "version" in value;

    return isInstance;
}

export function ConnectionDefinitionModelFromJSON(json: any): ConnectionDefinitionModel {
    return ConnectionDefinitionModelFromJSONTyped(json, false);
}

export function ConnectionDefinitionModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): ConnectionDefinitionModel {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {

        'authorizations': !exists(json, 'authorizations') ? undefined : ((json['authorizations'] as Array<any>).map(AuthorizationModelFromJSON)),
        'baseUri': !exists(json, 'baseUri') ? undefined : json['baseUri'],
        'componentDescription': !exists(json, 'componentDescription') ? undefined : json['componentDescription'],
        'componentName': json['componentName'],
        'properties': !exists(json, 'properties') ? undefined : ((json['properties'] as Array<any>).map(PropertyModelFromJSON)),
        'componentTitle': !exists(json, 'componentTitle') ? undefined : json['componentTitle'],
        'version': json['version'],
    };
}

export function ConnectionDefinitionModelToJSON(value?: ConnectionDefinitionModel | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {

        'authorizations': value.authorizations === undefined ? undefined : ((value.authorizations as Array<any>).map(AuthorizationModelToJSON)),
        'baseUri': value.baseUri,
        'componentDescription': value.componentDescription,
        'componentName': value.componentName,
        'properties': value.properties === undefined ? undefined : ((value.properties as Array<any>).map(PropertyModelToJSON)),
        'componentTitle': value.componentTitle,
        'version': value.version,
    };
}

