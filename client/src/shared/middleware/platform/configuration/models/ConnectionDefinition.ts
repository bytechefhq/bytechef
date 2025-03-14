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
import type { Authorization } from './Authorization';
import {
    AuthorizationFromJSON,
    AuthorizationFromJSONTyped,
    AuthorizationToJSON,
    AuthorizationToJSONTyped,
} from './Authorization';
import type { Property } from './Property';
import {
    PropertyFromJSON,
    PropertyFromJSONTyped,
    PropertyToJSON,
    PropertyToJSONTyped,
} from './Property';

/**
 * Definition of a connection to an outside service.
 * @export
 * @interface ConnectionDefinition
 */
export interface ConnectionDefinition {
    /**
     * If a connection requires an authorization to be defined or not
     * @type {boolean}
     * @memberof ConnectionDefinition
     */
    authorizationRequired?: boolean;
    /**
     * 
     * @type {Array<Authorization>}
     * @memberof ConnectionDefinition
     */
    authorizations?: Array<Authorization>;
    /**
     * Defines the base URI for all future HTTP requests.
     * @type {string}
     * @memberof ConnectionDefinition
     */
    baseUri?: string;
    /**
     * The description used from the connection's component.
     * @type {string}
     * @memberof ConnectionDefinition
     */
    componentDescription?: string;
    /**
     * The component name used from the connection's component.
     * @type {string}
     * @memberof ConnectionDefinition
     */
    componentName: string;
    /**
     * The properties of the connection.
     * @type {Array<Property>}
     * @memberof ConnectionDefinition
     */
    properties?: Array<Property>;
    /**
     * The title used from the connection's component.
     * @type {string}
     * @memberof ConnectionDefinition
     */
    componentTitle?: string;
    /**
     * The version of a connection.
     * @type {number}
     * @memberof ConnectionDefinition
     */
    version: number;
}

/**
 * Check if a given object implements the ConnectionDefinition interface.
 */
export function instanceOfConnectionDefinition(value: object): value is ConnectionDefinition {
    if (!('componentName' in value) || value['componentName'] === undefined) return false;
    if (!('version' in value) || value['version'] === undefined) return false;
    return true;
}

export function ConnectionDefinitionFromJSON(json: any): ConnectionDefinition {
    return ConnectionDefinitionFromJSONTyped(json, false);
}

export function ConnectionDefinitionFromJSONTyped(json: any, ignoreDiscriminator: boolean): ConnectionDefinition {
    if (json == null) {
        return json;
    }
    return {
        
        'authorizationRequired': json['authorizationRequired'] == null ? undefined : json['authorizationRequired'],
        'authorizations': json['authorizations'] == null ? undefined : ((json['authorizations'] as Array<any>).map(AuthorizationFromJSON)),
        'baseUri': json['baseUri'] == null ? undefined : json['baseUri'],
        'componentDescription': json['componentDescription'] == null ? undefined : json['componentDescription'],
        'componentName': json['componentName'],
        'properties': json['properties'] == null ? undefined : ((json['properties'] as Array<any>).map(PropertyFromJSON)),
        'componentTitle': json['componentTitle'] == null ? undefined : json['componentTitle'],
        'version': json['version'],
    };
}

export function ConnectionDefinitionToJSON(json: any): ConnectionDefinition {
    return ConnectionDefinitionToJSONTyped(json, false);
}

export function ConnectionDefinitionToJSONTyped(value?: ConnectionDefinition | null, ignoreDiscriminator: boolean = false): any {
    if (value == null) {
        return value;
    }

    return {
        
        'authorizationRequired': value['authorizationRequired'],
        'authorizations': value['authorizations'] == null ? undefined : ((value['authorizations'] as Array<any>).map(AuthorizationToJSON)),
        'baseUri': value['baseUri'],
        'componentDescription': value['componentDescription'],
        'componentName': value['componentName'],
        'properties': value['properties'] == null ? undefined : ((value['properties'] as Array<any>).map(PropertyToJSON)),
        'componentTitle': value['componentTitle'],
        'version': value['version'],
    };
}

