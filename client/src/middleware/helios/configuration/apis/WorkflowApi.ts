/* tslint:disable */
/* eslint-disable */
/**
 * Project Configuration API
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
  WorkflowModel,
  WorkflowRequestModel,
} from '../models/index';
import {
    WorkflowModelFromJSON,
    WorkflowModelToJSON,
    WorkflowRequestModelFromJSON,
    WorkflowRequestModelToJSON,
} from '../models/index';

export interface DeleteProjectWorkflowRequest {
    id: number;
    workflowId: string;
}

export interface DuplicateWorkflowRequest {
    id: number;
    workflowId: string;
}

export interface GetProjectWorkflowsRequest {
    id: number;
}

export interface GetWorkflowRequest {
    id: string;
}

export interface UpdateWorkflowRequest {
    id: string;
    workflowRequestModel: WorkflowRequestModel;
}

/**
 * 
 */
export class WorkflowApi extends runtime.BaseAPI {

    /**
     * Delete a workflow.
     * Delete a workflow
     */
    async deleteProjectWorkflowRaw(requestParameters: DeleteProjectWorkflowRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<void>> {
        if (requestParameters.id === null || requestParameters.id === undefined) {
            throw new runtime.RequiredError('id','Required parameter requestParameters.id was null or undefined when calling deleteProjectWorkflow.');
        }

        if (requestParameters.workflowId === null || requestParameters.workflowId === undefined) {
            throw new runtime.RequiredError('workflowId','Required parameter requestParameters.workflowId was null or undefined when calling deleteProjectWorkflow.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/projects/{id}/workflows/{workflowId}`.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters.id))).replace(`{${"workflowId"}}`, encodeURIComponent(String(requestParameters.workflowId))),
            method: 'DELETE',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.VoidApiResponse(response);
    }

    /**
     * Delete a workflow.
     * Delete a workflow
     */
    async deleteProjectWorkflow(requestParameters: DeleteProjectWorkflowRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<void> {
        await this.deleteProjectWorkflowRaw(requestParameters, initOverrides);
    }

    /**
     * Duplicates existing workflow.
     * Duplicates existing workflow.
     */
    async duplicateWorkflowRaw(requestParameters: DuplicateWorkflowRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<WorkflowModel>> {
        if (requestParameters.id === null || requestParameters.id === undefined) {
            throw new runtime.RequiredError('id','Required parameter requestParameters.id was null or undefined when calling duplicateWorkflow.');
        }

        if (requestParameters.workflowId === null || requestParameters.workflowId === undefined) {
            throw new runtime.RequiredError('workflowId','Required parameter requestParameters.workflowId was null or undefined when calling duplicateWorkflow.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/projects/{id}/workflows/{workflowId}/duplicate`.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters.id))).replace(`{${"workflowId"}}`, encodeURIComponent(String(requestParameters.workflowId))),
            method: 'POST',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => WorkflowModelFromJSON(jsonValue));
    }

    /**
     * Duplicates existing workflow.
     * Duplicates existing workflow.
     */
    async duplicateWorkflow(requestParameters: DuplicateWorkflowRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<WorkflowModel> {
        const response = await this.duplicateWorkflowRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Get workflows for particular project.
     * Get workflows for particular project.
     */
    async getProjectWorkflowsRaw(requestParameters: GetProjectWorkflowsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<WorkflowModel>>> {
        if (requestParameters.id === null || requestParameters.id === undefined) {
            throw new runtime.RequiredError('id','Required parameter requestParameters.id was null or undefined when calling getProjectWorkflows.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/projects/{id}/workflows`.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters.id))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(WorkflowModelFromJSON));
    }

    /**
     * Get workflows for particular project.
     * Get workflows for particular project.
     */
    async getProjectWorkflows(requestParameters: GetProjectWorkflowsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<WorkflowModel>> {
        const response = await this.getProjectWorkflowsRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Get a workflow by id.
     * Get a workflow by id
     */
    async getWorkflowRaw(requestParameters: GetWorkflowRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<WorkflowModel>> {
        if (requestParameters.id === null || requestParameters.id === undefined) {
            throw new runtime.RequiredError('id','Required parameter requestParameters.id was null or undefined when calling getWorkflow.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/workflows/{id}`.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters.id))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => WorkflowModelFromJSON(jsonValue));
    }

    /**
     * Get a workflow by id.
     * Get a workflow by id
     */
    async getWorkflow(requestParameters: GetWorkflowRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<WorkflowModel> {
        const response = await this.getWorkflowRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Get workflow definitions.
     * Get workflow definitions
     */
    async getWorkflowsRaw(initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<WorkflowModel>>> {
        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/workflows`,
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(WorkflowModelFromJSON));
    }

    /**
     * Get workflow definitions.
     * Get workflow definitions
     */
    async getWorkflows(initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<WorkflowModel>> {
        const response = await this.getWorkflowsRaw(initOverrides);
        return await response.value();
    }

    /**
     * Update an existing workflow.
     * Update an existing workflow
     */
    async updateWorkflowRaw(requestParameters: UpdateWorkflowRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<WorkflowModel>> {
        if (requestParameters.id === null || requestParameters.id === undefined) {
            throw new runtime.RequiredError('id','Required parameter requestParameters.id was null or undefined when calling updateWorkflow.');
        }

        if (requestParameters.workflowRequestModel === null || requestParameters.workflowRequestModel === undefined) {
            throw new runtime.RequiredError('workflowRequestModel','Required parameter requestParameters.workflowRequestModel was null or undefined when calling updateWorkflow.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/workflows/{id}`.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters.id))),
            method: 'PUT',
            headers: headerParameters,
            query: queryParameters,
            body: WorkflowRequestModelToJSON(requestParameters.workflowRequestModel),
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => WorkflowModelFromJSON(jsonValue));
    }

    /**
     * Update an existing workflow.
     * Update an existing workflow
     */
    async updateWorkflow(requestParameters: UpdateWorkflowRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<WorkflowModel> {
        const response = await this.updateWorkflowRaw(requestParameters, initOverrides);
        return await response.value();
    }

}
