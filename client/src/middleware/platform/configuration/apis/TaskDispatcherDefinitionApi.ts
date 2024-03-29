/* tslint:disable */
/* eslint-disable */
/**
 * The Platform Configuration API
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: 1
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


import * as runtime from '../runtime';
import type {
  TaskDispatcherDefinitionBasicModel,
  TaskDispatcherDefinitionModel,
} from '../models/index';
import {
    TaskDispatcherDefinitionBasicModelFromJSON,
    TaskDispatcherDefinitionBasicModelToJSON,
    TaskDispatcherDefinitionModelFromJSON,
    TaskDispatcherDefinitionModelToJSON,
} from '../models/index';

export interface GetTaskDispatcherDefinitionRequest {
    taskDispatcherName: string;
    taskDispatcherVersion: number;
}

export interface GetTaskDispatcherDefinitionVersionsRequest {
    taskDispatcherName: string;
}

/**
 * 
 */
export class TaskDispatcherDefinitionApi extends runtime.BaseAPI {

    /**
     * Get a task dispatcher definition.
     * Get a task dispatcher definition
     */
    async getTaskDispatcherDefinitionRaw(requestParameters: GetTaskDispatcherDefinitionRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<TaskDispatcherDefinitionModel>> {
        if (requestParameters['taskDispatcherName'] == null) {
            throw new runtime.RequiredError(
                'taskDispatcherName',
                'Required parameter "taskDispatcherName" was null or undefined when calling getTaskDispatcherDefinition().'
            );
        }

        if (requestParameters['taskDispatcherVersion'] == null) {
            throw new runtime.RequiredError(
                'taskDispatcherVersion',
                'Required parameter "taskDispatcherVersion" was null or undefined when calling getTaskDispatcherDefinition().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/task-dispatcher-definitions/{taskDispatcherName}/{taskDispatcherVersion}`.replace(`{${"taskDispatcherName"}}`, encodeURIComponent(String(requestParameters['taskDispatcherName']))).replace(`{${"taskDispatcherVersion"}}`, encodeURIComponent(String(requestParameters['taskDispatcherVersion']))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => TaskDispatcherDefinitionModelFromJSON(jsonValue));
    }

    /**
     * Get a task dispatcher definition.
     * Get a task dispatcher definition
     */
    async getTaskDispatcherDefinition(requestParameters: GetTaskDispatcherDefinitionRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<TaskDispatcherDefinitionModel> {
        const response = await this.getTaskDispatcherDefinitionRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Get all task dispatcher definition versions of a task dispatcher.
     * Get all task dispatcher definition versions of a task dispatcher
     */
    async getTaskDispatcherDefinitionVersionsRaw(requestParameters: GetTaskDispatcherDefinitionVersionsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<TaskDispatcherDefinitionBasicModel>>> {
        if (requestParameters['taskDispatcherName'] == null) {
            throw new runtime.RequiredError(
                'taskDispatcherName',
                'Required parameter "taskDispatcherName" was null or undefined when calling getTaskDispatcherDefinitionVersions().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/task-dispatcher-definitions/{taskDispatcherName}/versions`.replace(`{${"taskDispatcherName"}}`, encodeURIComponent(String(requestParameters['taskDispatcherName']))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(TaskDispatcherDefinitionBasicModelFromJSON));
    }

    /**
     * Get all task dispatcher definition versions of a task dispatcher.
     * Get all task dispatcher definition versions of a task dispatcher
     */
    async getTaskDispatcherDefinitionVersions(requestParameters: GetTaskDispatcherDefinitionVersionsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<TaskDispatcherDefinitionBasicModel>> {
        const response = await this.getTaskDispatcherDefinitionVersionsRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Get all task dispatcher definitions.
     * Get all task dispatcher definitions
     */
    async getTaskDispatcherDefinitionsRaw(initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<TaskDispatcherDefinitionModel>>> {
        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/task-dispatcher-definitions`,
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(TaskDispatcherDefinitionModelFromJSON));
    }

    /**
     * Get all task dispatcher definitions.
     * Get all task dispatcher definitions
     */
    async getTaskDispatcherDefinitions(initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<TaskDispatcherDefinitionModel>> {
        const response = await this.getTaskDispatcherDefinitionsRaw(initOverrides);
        return await response.value();
    }

}
