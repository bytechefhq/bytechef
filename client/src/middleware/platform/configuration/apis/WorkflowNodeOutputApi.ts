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
  WorkflowNodeOutputModel,
} from '../models/index';
import {
    WorkflowNodeOutputModelFromJSON,
    WorkflowNodeOutputModelToJSON,
} from '../models/index';

export interface GetWorkflowNodeOutputRequest {
    id: string;
    workflowNodeName: string;
}

export interface GetWorkflowNodeOutputsRequest {
    id: string;
    lastWorkflowNodeName?: string;
}

/**
 * 
 */
export class WorkflowNodeOutputApi extends runtime.BaseAPI {

    /**
     * Get workflow node output of an action task or trigger used in a workflow.
     * Get workflow node output of an action task or trigger used in a workflow
     */
    async getWorkflowNodeOutputRaw(requestParameters: GetWorkflowNodeOutputRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<WorkflowNodeOutputModel>> {
        if (requestParameters['id'] == null) {
            throw new runtime.RequiredError(
                'id',
                'Required parameter "id" was null or undefined when calling getWorkflowNodeOutput().'
            );
        }

        if (requestParameters['workflowNodeName'] == null) {
            throw new runtime.RequiredError(
                'workflowNodeName',
                'Required parameter "workflowNodeName" was null or undefined when calling getWorkflowNodeOutput().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/workflows/{id}/outputs/{workflowNodeName}`.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters['id']))).replace(`{${"workflowNodeName"}}`, encodeURIComponent(String(requestParameters['workflowNodeName']))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => WorkflowNodeOutputModelFromJSON(jsonValue));
    }

    /**
     * Get workflow node output of an action task or trigger used in a workflow.
     * Get workflow node output of an action task or trigger used in a workflow
     */
    async getWorkflowNodeOutput(requestParameters: GetWorkflowNodeOutputRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<WorkflowNodeOutputModel> {
        const response = await this.getWorkflowNodeOutputRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Get all workflow node outputs used in a workflow.
     * Get all dynamic workflow node outputs used in a workflow
     */
    async getWorkflowNodeOutputsRaw(requestParameters: GetWorkflowNodeOutputsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<WorkflowNodeOutputModel>>> {
        if (requestParameters['id'] == null) {
            throw new runtime.RequiredError(
                'id',
                'Required parameter "id" was null or undefined when calling getWorkflowNodeOutputs().'
            );
        }

        const queryParameters: any = {};

        if (requestParameters['lastWorkflowNodeName'] != null) {
            queryParameters['lastWorkflowNodeName'] = requestParameters['lastWorkflowNodeName'];
        }

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/workflows/{id}/outputs`.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters['id']))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(WorkflowNodeOutputModelFromJSON));
    }

    /**
     * Get all workflow node outputs used in a workflow.
     * Get all dynamic workflow node outputs used in a workflow
     */
    async getWorkflowNodeOutputs(requestParameters: GetWorkflowNodeOutputsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<WorkflowNodeOutputModel>> {
        const response = await this.getWorkflowNodeOutputsRaw(requestParameters, initOverrides);
        return await response.value();
    }

}
