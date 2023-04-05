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


import * as runtime from '../runtime';
import type {
  ConnectionDefinitionBasicModel,
  ConnectionDefinitionModel,
} from '../models';
import {
    ConnectionDefinitionBasicModelFromJSON,
    ConnectionDefinitionBasicModelToJSON,
    ConnectionDefinitionModelFromJSON,
    ConnectionDefinitionModelToJSON,
} from '../models';

export interface GetComponentConnectionDefinitionRequest {
    componentName: string;
    componentVersion: number;
}

export interface GetComponentConnectionDefinitionsRequest {
    componentName: string;
    componentVersion: number;
}

/**
 * 
 */
export class ConnectionDefinitionsApi extends runtime.BaseAPI {

    /**
     * Get connection definition for a component.
     * Get connection definition for a component.
     */
    async getComponentConnectionDefinitionRaw(requestParameters: GetComponentConnectionDefinitionRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<ConnectionDefinitionModel>> {
        if (requestParameters.componentName === null || requestParameters.componentName === undefined) {
            throw new runtime.RequiredError('componentName','Required parameter requestParameters.componentName was null or undefined when calling getComponentConnectionDefinition.');
        }

        if (requestParameters.componentVersion === null || requestParameters.componentVersion === undefined) {
            throw new runtime.RequiredError('componentVersion','Required parameter requestParameters.componentVersion was null or undefined when calling getComponentConnectionDefinition.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/component-definitions/{componentName}/{componentVersion}/connection-definition`.replace(`{${"componentName"}}`, encodeURIComponent(String(requestParameters.componentName))).replace(`{${"componentVersion"}}`, encodeURIComponent(String(requestParameters.componentVersion))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => ConnectionDefinitionModelFromJSON(jsonValue));
    }

    /**
     * Get connection definition for a component.
     * Get connection definition for a component.
     */
    async getComponentConnectionDefinition(requestParameters: GetComponentConnectionDefinitionRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<ConnectionDefinitionModel> {
        const response = await this.getComponentConnectionDefinitionRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Get all compatible connection definitions for a component.
     * Get all compatible connection definitions for a component.
     */
    async getComponentConnectionDefinitionsRaw(requestParameters: GetComponentConnectionDefinitionsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<ConnectionDefinitionBasicModel>>> {
        if (requestParameters.componentName === null || requestParameters.componentName === undefined) {
            throw new runtime.RequiredError('componentName','Required parameter requestParameters.componentName was null or undefined when calling getComponentConnectionDefinitions.');
        }

        if (requestParameters.componentVersion === null || requestParameters.componentVersion === undefined) {
            throw new runtime.RequiredError('componentVersion','Required parameter requestParameters.componentVersion was null or undefined when calling getComponentConnectionDefinitions.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/component-definitions/{componentName}/{componentVersion}/connection-definitions`.replace(`{${"componentName"}}`, encodeURIComponent(String(requestParameters.componentName))).replace(`{${"componentVersion"}}`, encodeURIComponent(String(requestParameters.componentVersion))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(ConnectionDefinitionBasicModelFromJSON));
    }

    /**
     * Get all compatible connection definitions for a component.
     * Get all compatible connection definitions for a component.
     */
    async getComponentConnectionDefinitions(requestParameters: GetComponentConnectionDefinitionsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<ConnectionDefinitionBasicModel>> {
        const response = await this.getComponentConnectionDefinitionsRaw(requestParameters, initOverrides);
        return await response.value();
    }

}
