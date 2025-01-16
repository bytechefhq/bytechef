/* tslint:disable */
/* eslint-disable */
/**
 * The Automation Configuration Internal API
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
  CreateProjectDeploymentWorkflowJob200Response,
  Environment,
  ProjectDeployment,
  ProjectDeploymentWorkflow,
} from '../models/index';
import {
    CreateProjectDeploymentWorkflowJob200ResponseFromJSON,
    CreateProjectDeploymentWorkflowJob200ResponseToJSON,
    EnvironmentFromJSON,
    EnvironmentToJSON,
    ProjectDeploymentFromJSON,
    ProjectDeploymentToJSON,
    ProjectDeploymentWorkflowFromJSON,
    ProjectDeploymentWorkflowToJSON,
} from '../models/index';

export interface CreateProjectDeploymentRequest {
    projectDeployment: ProjectDeployment;
}

export interface CreateProjectDeploymentWorkflowJobRequest {
    id: number;
    workflowId: string;
}

export interface DeleteProjectDeploymentRequest {
    id: number;
}

export interface EnableProjectDeploymentRequest {
    id: number;
    enable: boolean;
}

export interface EnableProjectDeploymentWorkflowRequest {
    id: number;
    workflowId: string;
    enable: boolean;
}

export interface GetProjectDeploymentRequest {
    id: number;
}

export interface GetWorkspaceProjectDeploymentsRequest {
    id: number;
    environment?: Environment;
    projectId?: number;
    tagId?: number;
    includeAllFields?: boolean;
}

export interface UpdateProjectDeploymentRequest {
    id: number;
    projectDeployment: ProjectDeployment;
}

export interface UpdateProjectDeploymentWorkflowRequest {
    id: number;
    projectDeploymentWorkflowId: number;
    projectDeploymentWorkflow: Omit<ProjectDeploymentWorkflow, 'createdBy'|'createdDate'|'id'|'lastModifiedBy'|'lastModifiedDate'|'workflowReferenceCode'>;
}

/**
 * 
 */
export class ProjectDeploymentApi extends runtime.BaseAPI {

    /**
     * Create a new project deployment.
     * Create a new project deployment
     */
    async createProjectDeploymentRaw(requestParameters: CreateProjectDeploymentRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<number>> {
        if (requestParameters['projectDeployment'] == null) {
            throw new runtime.RequiredError(
                'projectDeployment',
                'Required parameter "projectDeployment" was null or undefined when calling createProjectDeployment().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/project-deployments`,
            method: 'POST',
            headers: headerParameters,
            query: queryParameters,
            body: ProjectDeploymentToJSON(requestParameters['projectDeployment']),
        }, initOverrides);

        if (this.isJsonMime(response.headers.get('content-type'))) {
            return new runtime.JSONApiResponse<number>(response);
        } else {
            return new runtime.TextApiResponse(response) as any;
        }
    }

    /**
     * Create a new project deployment.
     * Create a new project deployment
     */
    async createProjectDeployment(requestParameters: CreateProjectDeploymentRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<number> {
        const response = await this.createProjectDeploymentRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Create a request for running a new job.
     * Create a request for running a new job
     */
    async createProjectDeploymentWorkflowJobRaw(requestParameters: CreateProjectDeploymentWorkflowJobRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<CreateProjectDeploymentWorkflowJob200Response>> {
        if (requestParameters['id'] == null) {
            throw new runtime.RequiredError(
                'id',
                'Required parameter "id" was null or undefined when calling createProjectDeploymentWorkflowJob().'
            );
        }

        if (requestParameters['workflowId'] == null) {
            throw new runtime.RequiredError(
                'workflowId',
                'Required parameter "workflowId" was null or undefined when calling createProjectDeploymentWorkflowJob().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/project-deployments/{id}/workflows/{workflowId}/jobs`.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters['id']))).replace(`{${"workflowId"}}`, encodeURIComponent(String(requestParameters['workflowId']))),
            method: 'POST',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => CreateProjectDeploymentWorkflowJob200ResponseFromJSON(jsonValue));
    }

    /**
     * Create a request for running a new job.
     * Create a request for running a new job
     */
    async createProjectDeploymentWorkflowJob(requestParameters: CreateProjectDeploymentWorkflowJobRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<CreateProjectDeploymentWorkflowJob200Response> {
        const response = await this.createProjectDeploymentWorkflowJobRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Delete a project deployment.
     * Delete a project deployment
     */
    async deleteProjectDeploymentRaw(requestParameters: DeleteProjectDeploymentRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<void>> {
        if (requestParameters['id'] == null) {
            throw new runtime.RequiredError(
                'id',
                'Required parameter "id" was null or undefined when calling deleteProjectDeployment().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/project-deployments/{id}`.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters['id']))),
            method: 'DELETE',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.VoidApiResponse(response);
    }

    /**
     * Delete a project deployment.
     * Delete a project deployment
     */
    async deleteProjectDeployment(requestParameters: DeleteProjectDeploymentRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<void> {
        await this.deleteProjectDeploymentRaw(requestParameters, initOverrides);
    }

    /**
     * Enable/disable a project deployment.
     * Enable/disable a project deployment
     */
    async enableProjectDeploymentRaw(requestParameters: EnableProjectDeploymentRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<void>> {
        if (requestParameters['id'] == null) {
            throw new runtime.RequiredError(
                'id',
                'Required parameter "id" was null or undefined when calling enableProjectDeployment().'
            );
        }

        if (requestParameters['enable'] == null) {
            throw new runtime.RequiredError(
                'enable',
                'Required parameter "enable" was null or undefined when calling enableProjectDeployment().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/project-deployments/{id}/enable/{enable}`.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters['id']))).replace(`{${"enable"}}`, encodeURIComponent(String(requestParameters['enable']))),
            method: 'PATCH',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.VoidApiResponse(response);
    }

    /**
     * Enable/disable a project deployment.
     * Enable/disable a project deployment
     */
    async enableProjectDeployment(requestParameters: EnableProjectDeploymentRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<void> {
        await this.enableProjectDeploymentRaw(requestParameters, initOverrides);
    }

    /**
     * Enable/disable a workflow of a project deployment.
     * Enable/disable a workflow of a project deployment
     */
    async enableProjectDeploymentWorkflowRaw(requestParameters: EnableProjectDeploymentWorkflowRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<void>> {
        if (requestParameters['id'] == null) {
            throw new runtime.RequiredError(
                'id',
                'Required parameter "id" was null or undefined when calling enableProjectDeploymentWorkflow().'
            );
        }

        if (requestParameters['workflowId'] == null) {
            throw new runtime.RequiredError(
                'workflowId',
                'Required parameter "workflowId" was null or undefined when calling enableProjectDeploymentWorkflow().'
            );
        }

        if (requestParameters['enable'] == null) {
            throw new runtime.RequiredError(
                'enable',
                'Required parameter "enable" was null or undefined when calling enableProjectDeploymentWorkflow().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/project-deployments/{id}/workflows/{workflowId}/enable/{enable}`.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters['id']))).replace(`{${"workflowId"}}`, encodeURIComponent(String(requestParameters['workflowId']))).replace(`{${"enable"}}`, encodeURIComponent(String(requestParameters['enable']))),
            method: 'PATCH',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.VoidApiResponse(response);
    }

    /**
     * Enable/disable a workflow of a project deployment.
     * Enable/disable a workflow of a project deployment
     */
    async enableProjectDeploymentWorkflow(requestParameters: EnableProjectDeploymentWorkflowRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<void> {
        await this.enableProjectDeploymentWorkflowRaw(requestParameters, initOverrides);
    }

    /**
     * Get a project deployment by id.
     * Get a project deployment by id
     */
    async getProjectDeploymentRaw(requestParameters: GetProjectDeploymentRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<ProjectDeployment>> {
        if (requestParameters['id'] == null) {
            throw new runtime.RequiredError(
                'id',
                'Required parameter "id" was null or undefined when calling getProjectDeployment().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/project-deployments/{id}`.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters['id']))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => ProjectDeploymentFromJSON(jsonValue));
    }

    /**
     * Get a project deployment by id.
     * Get a project deployment by id
     */
    async getProjectDeployment(requestParameters: GetProjectDeploymentRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<ProjectDeployment> {
        const response = await this.getProjectDeploymentRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Get project deployments.
     * Get project deployments
     */
    async getWorkspaceProjectDeploymentsRaw(requestParameters: GetWorkspaceProjectDeploymentsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<ProjectDeployment>>> {
        if (requestParameters['id'] == null) {
            throw new runtime.RequiredError(
                'id',
                'Required parameter "id" was null or undefined when calling getWorkspaceProjectDeployments().'
            );
        }

        const queryParameters: any = {};

        if (requestParameters['environment'] != null) {
            queryParameters['environment'] = requestParameters['environment'];
        }

        if (requestParameters['projectId'] != null) {
            queryParameters['projectId'] = requestParameters['projectId'];
        }

        if (requestParameters['tagId'] != null) {
            queryParameters['tagId'] = requestParameters['tagId'];
        }

        if (requestParameters['includeAllFields'] != null) {
            queryParameters['includeAllFields'] = requestParameters['includeAllFields'];
        }

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/workspaces/{id}/project-deployments`.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters['id']))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(ProjectDeploymentFromJSON));
    }

    /**
     * Get project deployments.
     * Get project deployments
     */
    async getWorkspaceProjectDeployments(requestParameters: GetWorkspaceProjectDeploymentsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<ProjectDeployment>> {
        const response = await this.getWorkspaceProjectDeploymentsRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Update an existing project deployment.
     * Update an existing project deployment
     */
    async updateProjectDeploymentRaw(requestParameters: UpdateProjectDeploymentRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<void>> {
        if (requestParameters['id'] == null) {
            throw new runtime.RequiredError(
                'id',
                'Required parameter "id" was null or undefined when calling updateProjectDeployment().'
            );
        }

        if (requestParameters['projectDeployment'] == null) {
            throw new runtime.RequiredError(
                'projectDeployment',
                'Required parameter "projectDeployment" was null or undefined when calling updateProjectDeployment().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/project-deployments/{id}`.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters['id']))),
            method: 'PUT',
            headers: headerParameters,
            query: queryParameters,
            body: ProjectDeploymentToJSON(requestParameters['projectDeployment']),
        }, initOverrides);

        return new runtime.VoidApiResponse(response);
    }

    /**
     * Update an existing project deployment.
     * Update an existing project deployment
     */
    async updateProjectDeployment(requestParameters: UpdateProjectDeploymentRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<void> {
        await this.updateProjectDeploymentRaw(requestParameters, initOverrides);
    }

    /**
     * Update an existing project deployment workflow.
     * Update an existing project deployment workflow
     */
    async updateProjectDeploymentWorkflowRaw(requestParameters: UpdateProjectDeploymentWorkflowRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<void>> {
        if (requestParameters['id'] == null) {
            throw new runtime.RequiredError(
                'id',
                'Required parameter "id" was null or undefined when calling updateProjectDeploymentWorkflow().'
            );
        }

        if (requestParameters['projectDeploymentWorkflowId'] == null) {
            throw new runtime.RequiredError(
                'projectDeploymentWorkflowId',
                'Required parameter "projectDeploymentWorkflowId" was null or undefined when calling updateProjectDeploymentWorkflow().'
            );
        }

        if (requestParameters['projectDeploymentWorkflow'] == null) {
            throw new runtime.RequiredError(
                'projectDeploymentWorkflow',
                'Required parameter "projectDeploymentWorkflow" was null or undefined when calling updateProjectDeploymentWorkflow().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/project-deployments/{id}/project-deployment-workflows/{projectDeploymentWorkflowId}`.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters['id']))).replace(`{${"projectDeploymentWorkflowId"}}`, encodeURIComponent(String(requestParameters['projectDeploymentWorkflowId']))),
            method: 'PUT',
            headers: headerParameters,
            query: queryParameters,
            body: ProjectDeploymentWorkflowToJSON(requestParameters['projectDeploymentWorkflow']),
        }, initOverrides);

        return new runtime.VoidApiResponse(response);
    }

    /**
     * Update an existing project deployment workflow.
     * Update an existing project deployment workflow
     */
    async updateProjectDeploymentWorkflow(requestParameters: UpdateProjectDeploymentWorkflowRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<void> {
        await this.updateProjectDeploymentWorkflowRaw(requestParameters, initOverrides);
    }

}