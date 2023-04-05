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
  ActionDefinitionBasicModel,
  ActionDefinitionModel,
} from '../models';
import {
    ActionDefinitionBasicModelFromJSON,
    ActionDefinitionBasicModelToJSON,
    ActionDefinitionModelFromJSON,
    ActionDefinitionModelToJSON,
} from '../models';

export interface GetComponentActionDefinitionRequest {
    componentName: string;
    componentVersion: number;
    actionName: string;
}

export interface GetComponentActionDefinitionsRequest {
    componentName: string;
    componentVersion: number;
}

/**
 * 
 */
export class ActionDefinitionsApi extends runtime.BaseAPI {

    /**
     * Get an action definition of a component.
     * Get an action definition of a component.
     */
    async getComponentActionDefinitionRaw(requestParameters: GetComponentActionDefinitionRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<ActionDefinitionModel>> {
        if (requestParameters.componentName === null || requestParameters.componentName === undefined) {
            throw new runtime.RequiredError('componentName','Required parameter requestParameters.componentName was null or undefined when calling getComponentActionDefinition.');
        }

        if (requestParameters.componentVersion === null || requestParameters.componentVersion === undefined) {
            throw new runtime.RequiredError('componentVersion','Required parameter requestParameters.componentVersion was null or undefined when calling getComponentActionDefinition.');
        }

        if (requestParameters.actionName === null || requestParameters.actionName === undefined) {
            throw new runtime.RequiredError('actionName','Required parameter requestParameters.actionName was null or undefined when calling getComponentActionDefinition.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/component-definitions/{componentName}/{componentVersion}/action-definitions/{actionName}`.replace(`{${"componentName"}}`, encodeURIComponent(String(requestParameters.componentName))).replace(`{${"componentVersion"}}`, encodeURIComponent(String(requestParameters.componentVersion))).replace(`{${"actionName"}}`, encodeURIComponent(String(requestParameters.actionName))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => ActionDefinitionModelFromJSON(jsonValue));
    }

    /**
     * Get an action definition of a component.
     * Get an action definition of a component.
     */
    async getComponentActionDefinition(requestParameters: GetComponentActionDefinitionRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<ActionDefinitionModel> {
        const response = await this.getComponentActionDefinitionRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Get a list of action definitions for a component.
     * Get a list of action definitions for a component.
     */
    async getComponentActionDefinitionsRaw(requestParameters: GetComponentActionDefinitionsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<ActionDefinitionBasicModel>>> {
        if (requestParameters.componentName === null || requestParameters.componentName === undefined) {
            throw new runtime.RequiredError('componentName','Required parameter requestParameters.componentName was null or undefined when calling getComponentActionDefinitions.');
        }

        if (requestParameters.componentVersion === null || requestParameters.componentVersion === undefined) {
            throw new runtime.RequiredError('componentVersion','Required parameter requestParameters.componentVersion was null or undefined when calling getComponentActionDefinitions.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/component-definitions/{componentName}/{componentVersion}/action-definitions`.replace(`{${"componentName"}}`, encodeURIComponent(String(requestParameters.componentName))).replace(`{${"componentVersion"}}`, encodeURIComponent(String(requestParameters.componentVersion))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(ActionDefinitionBasicModelFromJSON));
    }

    /**
     * Get a list of action definitions for a component.
     * Get a list of action definitions for a component.
     */
    async getComponentActionDefinitions(requestParameters: GetComponentActionDefinitionsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<ActionDefinitionBasicModel>> {
        const response = await this.getComponentActionDefinitionsRaw(requestParameters, initOverrides);
        return await response.value();
    }

}
