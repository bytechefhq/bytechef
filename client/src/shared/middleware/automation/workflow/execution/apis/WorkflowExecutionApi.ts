/* tslint:disable */
/* eslint-disable */
/**
 * The Automation Execution Internal API
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
  Environment,
  Page,
  WorkflowExecution,
} from '../models/index';
import {
    EnvironmentFromJSON,
    EnvironmentToJSON,
    PageFromJSON,
    PageToJSON,
    WorkflowExecutionFromJSON,
    WorkflowExecutionToJSON,
} from '../models/index';

export interface GetWorkflowExecutionRequest {
    id: number;
}

export interface GetWorkflowExecutionsPageRequest {
    environment?: Environment;
    jobStatus?: GetWorkflowExecutionsPageJobStatusEnum;
    jobStartDate?: Date;
    jobEndDate?: Date;
    projectId?: number;
    projectInstanceId?: number;
    workflowId?: string;
    pageNumber?: number;
}

/**
 * 
 */
export class WorkflowExecutionApi extends runtime.BaseAPI {

    /**
     * Get workflow executions by id.
     * Get workflow executions by id
     */
    async getWorkflowExecutionRaw(requestParameters: GetWorkflowExecutionRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<WorkflowExecution>> {
        if (requestParameters['id'] == null) {
            throw new runtime.RequiredError(
                'id',
                'Required parameter "id" was null or undefined when calling getWorkflowExecution().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/workflow-executions/{id}`.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters['id']))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => WorkflowExecutionFromJSON(jsonValue));
    }

    /**
     * Get workflow executions by id.
     * Get workflow executions by id
     */
    async getWorkflowExecution(requestParameters: GetWorkflowExecutionRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<WorkflowExecution> {
        const response = await this.getWorkflowExecutionRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Get project workflow executions.
     * Get project workflow executions
     */
    async getWorkflowExecutionsPageRaw(requestParameters: GetWorkflowExecutionsPageRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Page>> {
        const queryParameters: any = {};

        if (requestParameters['environment'] != null) {
            queryParameters['environment'] = requestParameters['environment'];
        }

        if (requestParameters['jobStatus'] != null) {
            queryParameters['jobStatus'] = requestParameters['jobStatus'];
        }

        if (requestParameters['jobStartDate'] != null) {
            queryParameters['jobStartDate'] = (requestParameters['jobStartDate'] as any).toISOString();
        }

        if (requestParameters['jobEndDate'] != null) {
            queryParameters['jobEndDate'] = (requestParameters['jobEndDate'] as any).toISOString();
        }

        if (requestParameters['projectId'] != null) {
            queryParameters['projectId'] = requestParameters['projectId'];
        }

        if (requestParameters['projectInstanceId'] != null) {
            queryParameters['projectInstanceId'] = requestParameters['projectInstanceId'];
        }

        if (requestParameters['workflowId'] != null) {
            queryParameters['workflowId'] = requestParameters['workflowId'];
        }

        if (requestParameters['pageNumber'] != null) {
            queryParameters['pageNumber'] = requestParameters['pageNumber'];
        }

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/workflow-executions`,
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => PageFromJSON(jsonValue));
    }

    /**
     * Get project workflow executions.
     * Get project workflow executions
     */
    async getWorkflowExecutionsPage(requestParameters: GetWorkflowExecutionsPageRequest = {}, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Page> {
        const response = await this.getWorkflowExecutionsPageRaw(requestParameters, initOverrides);
        return await response.value();
    }

}

/**
 * @export
 */
export const GetWorkflowExecutionsPageJobStatusEnum = {
    Created: 'CREATED',
    Started: 'STARTED',
    Stopped: 'STOPPED',
    Failed: 'FAILED',
    Completed: 'COMPLETED'
} as const;
export type GetWorkflowExecutionsPageJobStatusEnum = typeof GetWorkflowExecutionsPageJobStatusEnum[keyof typeof GetWorkflowExecutionsPageJobStatusEnum];
