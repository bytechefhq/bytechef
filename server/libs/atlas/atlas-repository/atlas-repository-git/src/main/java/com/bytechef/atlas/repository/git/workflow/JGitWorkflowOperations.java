
/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.atlas.repository.git.workflow;

import com.bytechef.atlas.domain.Workflow;
import com.bytechef.atlas.repository.workflow.mapper.WorkflowResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.FileSystemUtils;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
public class JGitWorkflowOperations implements GitWorkflowOperations {

    private static final Logger logger = LoggerFactory.getLogger(JGitWorkflowOperations.class);

    private static final String LATEST = "latest";

    private File repositoryDir;
    private final String url;
    private final String branch;
    private final String[] searchPaths;
    private final String username;
    private final String password;

    public JGitWorkflowOperations(String url, String branch, String[] searchPaths, String username, String password) {
        this.url = url;
        this.branch = branch;
        this.searchPaths = searchPaths;
        this.username = username;
        this.password = password;
    }

    @Override
    public List<WorkflowResource> getHeadFiles() {
        Repository repository = getRepository();

        return getHeadFiles(repository, searchPaths);
    }

    private List<WorkflowResource> getHeadFiles(Repository repository, String... searchPaths) {
        List<String> searchPathsList = Arrays.asList(searchPaths);
        List<WorkflowResource> workflowResources = new ArrayList<>();

        try (ObjectReader objectReader = repository.newObjectReader();
            RevWalk revWalk = new RevWalk(objectReader);
            TreeWalk treeWalk = new TreeWalk(repository, objectReader);) {
            ObjectId id = repository.resolve(Constants.HEAD);

            if (id == null) {
                return List.of();
            }

            RevCommit revCommit = revWalk.parseCommit(id);

            RevTree revTree = revCommit.getTree();

            treeWalk.addTree(revTree);
            treeWalk.setRecursive(true);

            while (treeWalk.next()) {
                String path = treeWalk.getPathString();

                if (!path.startsWith(".") &&
                    (CollectionUtils.isEmpty(searchPathsList) ||
                        CollectionUtils.contains(searchPathsList.iterator(), path))) {

                    ObjectId objectId = treeWalk.getObjectId(0);

                    logger.debug("Loading {} [{}]", path, objectId.name());

                    workflowResources.add(readBlob(repository, path.substring(0, path.indexOf('.')), objectId.name()));
                }
            }

            return workflowResources;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private synchronized Repository getRepository() {
        clear();

        logger.info("Cloning {} {}", url, branch);

        CloneCommand cloneCommand = Git.cloneRepository()
            .setURI(url)
            .setBranch(branch)
            .setDirectory(repositoryDir);

        if (username != null && password != null) {
            cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password));
        }

        try (Git git = cloneCommand.call()) {
            return (git.getRepository());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public WorkflowResource getFile(String fileId) {
        try {
            Repository repository = getRepository();
            int blobIdDelim = fileId.lastIndexOf(':');

            if (blobIdDelim > -1) {
                String path = fileId.substring(0, blobIdDelim);
                String blobId = fileId.substring(blobIdDelim + 1);

                return readBlob(repository, path, blobId);
            } else {
                return readBlob(repository, fileId, LATEST);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private WorkflowResource readBlob(Repository repository, String path, String blobId) throws Exception {
        try (ObjectReader reader = repository.newObjectReader()) {
            if (blobId.equals(LATEST)) {
                List<WorkflowResource> headFiles = getHeadFiles(repository, path);

                Assert.notEmpty(headFiles, "could not find: " + path + ":" + blobId);

                return headFiles.get(0);
            }

            ObjectId objectId = repository.resolve(blobId);

            Assert.notNull(objectId, "could not find: " + path + ":" + blobId);

            ObjectLoader objectLoader = reader.open(objectId);

            AbbreviatedObjectId abbreviatedObjectId = reader.abbreviate(objectId);

            return new WorkflowResource(
                path + ":" + abbreviatedObjectId.name(),
                new ByteArrayResource(objectLoader.getBytes()), Workflow.Format.parse(path));
        }
    }

    private void clear() {
        if (repositoryDir != null) {
            FileSystemUtils.deleteRecursively(repositoryDir);
        }

        Path path;

        try {
            path = Files.createTempDirectory("jgit_");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        repositoryDir = path.toFile();
    }
}
