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
import type { AuthorizationTypeModel } from './AuthorizationTypeModel';
import {
    AuthorizationTypeModelFromJSON,
    AuthorizationTypeModelFromJSONTyped,
    AuthorizationTypeModelToJSON,
} from './AuthorizationTypeModel';
import type { DisplayModel } from './DisplayModel';
import {
    DisplayModelFromJSON,
    DisplayModelFromJSONTyped,
    DisplayModelToJSON,
} from './DisplayModel';
import type { PropertyModel } from './PropertyModel';
import {
    PropertyModelFromJSON,
    PropertyModelFromJSONTyped,
    PropertyModelToJSON,
} from './PropertyModel';

/**
 * Contains information required for a connection's authorization.
 * @export
 * @interface AuthorizationModel
 */
export interface AuthorizationModel {
    /**
     * 
     * @type {DisplayModel}
     * @memberof AuthorizationModel
     */
    display?: DisplayModel;
    /**
     * The authorization name.
     * @type {string}
     * @memberof AuthorizationModel
     */
    name?: string;
    /**
     * The redirect URI used for OAuth2 callback URL.
     * @type {string}
     * @memberof AuthorizationModel
     */
    readonly redirectUri?: string;
    /**
     * Properties of the connection.
     * @type {Array<PropertyModel>}
     * @memberof AuthorizationModel
     */
    properties?: Array<PropertyModel>;
    /**
     * 
     * @type {AuthorizationTypeModel}
     * @memberof AuthorizationModel
     */
    type?: AuthorizationTypeModel;
}

/**
 * Check if a given object implements the AuthorizationModel interface.
 */
export function instanceOfAuthorizationModel(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function AuthorizationModelFromJSON(json: any): AuthorizationModel {
    return AuthorizationModelFromJSONTyped(json, false);
}

export function AuthorizationModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): AuthorizationModel {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'display': !exists(json, 'display') ? undefined : DisplayModelFromJSON(json['display']),
        'name': !exists(json, 'name') ? undefined : json['name'],
        'redirectUri': !exists(json, 'redirectUri') ? undefined : json['redirectUri'],
        'properties': !exists(json, 'properties') ? undefined : ((json['properties'] as Array<any>).map(PropertyModelFromJSON)),
        'type': !exists(json, 'type') ? undefined : AuthorizationTypeModelFromJSON(json['type']),
    };
}

export function AuthorizationModelToJSON(value?: AuthorizationModel | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'display': DisplayModelToJSON(value.display),
        'name': value.name,
        'properties': value.properties === undefined ? undefined : ((value.properties as Array<any>).map(PropertyModelToJSON)),
        'type': AuthorizationTypeModelToJSON(value.type),
    };
}

