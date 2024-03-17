package com.bytechef.automation.configuration.repository;

import com.bytechef.automation.configuration.domain.ProjectInstance;
import com.bytechef.platform.constant.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public class CustomProjectInstanceRepositoryImpl implements CustomProjectInstanceRepository {

    private final JdbcClient jdbcClient;

    @SuppressFBWarnings("EI")
    public CustomProjectInstanceRepositoryImpl(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<ProjectInstance> findAllProjectInstances(Environment environment, Long projectId, Long tagId) {
        List<Object> arguments = new ArrayList<>();
        String query = "SELECT project_instance.* FROM project_instance ";

        if (tagId != null) {
            query += "JOIN project_instance_tag ON project_instance.id = project_instance_tag.project_instance_id ";
        }

        if (environment != null || projectId != null || tagId != null) {
            query += "WHERE ";
        }

        if (environment != null) {
            arguments.add(environment.getId());

            query += "environment = ? ";
        }

        if (projectId != null) {
            arguments.add(projectId);

            if (environment != null) {
                query += "AND ";
            }

            query += "project_id = ? ";
        }

        if (tagId != null) {
            arguments.add(tagId);

            if (environment != null || projectId != null) {
                query += "AND ";
            }

            query += "tag_id = ? ";
        }

        query += "ORDER BY project_instance.name ASC, project_instance.enabled DESC";

        List<ProjectInstance> projectInstances = jdbcClient.sql(query)
            .params(arguments)
            .query(ProjectInstance.class)
            .list();

        for (ProjectInstance projectInstance : projectInstances) {
            projectInstance.setTagIds(
                jdbcClient
                    .sql(
                        "SELECT project_instance_tag.tag_id FROM project_instance_tag WHERE project_instance_id = ?")
                    .param(projectInstance.getId())
                    .query(Long.class)
                    .list()
            );
        }

        return projectInstances;
    }
}
