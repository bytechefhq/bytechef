/*
 * Copyright 2016-2020 the original author or authors.
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
 * Modifications copyright (C) 2025 ByteChef
 */

package com.bytechef.atlas.configuration.repository.git.operations;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.workflow.mapper.WorkflowResource;
import com.bytechef.commons.util.CollectionUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
public class JGitWorkflowOperations implements GitWorkflowOperations {

    private static final Logger log = LoggerFactory.getLogger(JGitWorkflowOperations.class);

    private static final String LATEST = "latest";
    private static final ReentrantLock LOCK = new ReentrantLock();

    private final String branch;
    private final List<String> extensions;
    private File repositoryDir;
    private final List<String> searchPaths;
    private final String url;
    private final String username;
    private final String password;

    @SuppressFBWarnings("EI")
    public JGitWorkflowOperations(
        String url, String branch, List<String> extensions, List<String> searchPaths, String username,
        String password) {

        this.branch = branch;
        this.password = password;
        this.extensions = extensions;
        this.searchPaths = searchPaths;
        this.url = url;
        this.username = username;
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

    @Override
    public HeadFiles getHeadFiles() {
        Repository repository = getRepository();

        List<WorkflowResource> workflowResources = getHeadFiles(repository, extensions, searchPaths);

        GitInfo gitInfo;

        try {
            RevCommit latestRevCommit = new Git(repository)
                .log()
                .setMaxCount(1)
                .call()
                .iterator()
                .next();

            gitInfo = new GitInfo(latestRevCommit.getName(), latestRevCommit.getShortMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return new HeadFiles(workflowResources, gitInfo);
    }

    @Override
    public List<String> getRemoteBranches() {
        try {
            LsRemoteCommand lsRemoteCommand = Git.lsRemoteRepository()
                .setRemote(url)
                .setHeads(true);

            lsRemoteCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password));

            Collection<Ref> refs = lsRemoteCommand.call();

            return refs.stream()
                .map(ref -> ref.getName()
                    .replace("refs/heads/", ""))
                .sorted()
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch remote branches", e);
        }
    }

    @Override
    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    public String write(List<WorkflowResource> workflowResources, String commitMessage) {
        try {
            LOCK.lock();

            Repository repository = getRepository();

            Git git = new Git(repository);

            try {
                RevWalk revWalk = new RevWalk(repository);

                try (TreeWalk treeWalk = new TreeWalk(repository)) {
                    RevCommit commitToCheck = revWalk.parseCommit(repository.resolve("HEAD"));

                    treeWalk.addTree(commitToCheck.getTree());
                    treeWalk.addTree(new DirCacheIterator(repository.readDirCache()));
                    treeWalk.addTree(new FileTreeIterator(repository));
                    treeWalk.setRecursive(true);

                    while (treeWalk.next()) {
                        git.rm()
                            .addFilepattern(treeWalk.getPathString())
                            .call();
                    }
                }

                for (WorkflowResource workflowResource : workflowResources) {
                    File workflowFile = new File(repositoryDir, Objects.requireNonNull(workflowResource.getFilename()));

                    Path path = workflowFile.toPath();

                    Files.copy(
                        workflowResource.getInputStream(), path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                    git.add()
                        .addFilepattern(".")
                        .call();
                }

                git.commit()
                    .setMessage(commitMessage)
                    .call();

                RevCommit latestRevCommit = new Git(repository)
                    .log()
                    .setMaxCount(1)
                    .call()
                    .iterator()
                    .next();

                String latestCommitHash = latestRevCommit.getName();

                git.push()
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password))
                    .call();

                return latestCommitHash;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } finally {
            LOCK.unlock();
        }
    }

    private List<WorkflowResource> getHeadFiles(
        Repository repository, List<String> extensions, List<String> searchPaths) {

        List<WorkflowResource> workflowResources = new ArrayList<>();

        try (ObjectReader objectReader = repository.newObjectReader();
            RevWalk revWalk = new RevWalk(objectReader);
            TreeWalk treeWalk = new TreeWalk(repository, objectReader);) {
            ObjectId objectId = repository.resolve(Constants.HEAD);

            if (objectId == null) {
                return List.of();
            }

            RevCommit revCommit = revWalk.parseCommit(objectId);

            RevTree revTree = revCommit.getTree();

            treeWalk.addTree(revTree);
            treeWalk.setRecursive(true);

            while (treeWalk.next()) {
                String path = treeWalk.getPathString();

                String extension = Optional.of(path)
                    .filter(f -> f.contains("."))
                    .map(f -> f.substring(path.lastIndexOf(".") + 1))
                    .orElse("");

                Optional<String> searchPathOptional = CollectionUtils.findFirst(searchPaths, path::startsWith);

                if ((CollectionUtils.isEmpty(extensions) || extensions.contains(extension)) &&
                    (CollectionUtils.isEmpty(searchPaths) || searchPathOptional.isPresent())) {

                    ObjectId firstObjectId = treeWalk.getObjectId(0);

                    if (log.isDebugEnabled()) {
                        log.debug("Loading {} [{}]", path, firstObjectId.name());
                    }

                    workflowResources.add(
                        readBlob(repository, path.substring(0, path.indexOf('.')), firstObjectId.name()));
                }
            }

            return workflowResources;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Repository getRepository() {
        try {
            LOCK.lock();

            clear();

            if (log.isDebugEnabled()) {
                log.debug("Cloning repository: {}", url);
            }

            CloneCommand cloneCommand = Git.cloneRepository()
                .setURI(url)
                .setBranch(branch)
                .setDirectory(repositoryDir);

            if (username != null && password != null) {
                cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password));
            }

            try (Git git = cloneCommand.call()) {
                return git.getRepository();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } finally {
            LOCK.unlock();
        }
    }

    private WorkflowResource readBlob(Repository repository, String path, String blobId) throws Exception {
        WorkflowResource workflowResource = null;

        try (ObjectReader objectReader = repository.newObjectReader()) {
            if (blobId.equals(LATEST)) {
                List<WorkflowResource> headFiles = getHeadFiles(repository, extensions, List.of(path));

                if (headFiles.isEmpty()) {
                    if (log.isDebugEnabled()) {
                        log.debug("Could not find: " + path + ":" + blobId);
                    }
                } else {
                    workflowResource = headFiles.get(0);
                }
            } else {
                ObjectId objectId = repository.resolve(blobId);

                if (objectId == null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Could not find: " + path + ":" + blobId);
                    }
                } else {
                    ObjectLoader objectLoader = objectReader.open(objectId);

                    AbbreviatedObjectId abbreviatedObjectId = objectReader.abbreviate(objectId);

                    // TODO check, it is maybe the right wat to get commit time

                    RevWalk revWalk = new RevWalk(repository);

                    RevCommit revCommit = revWalk.parseCommit(repository.resolve(Constants.HEAD));

                    workflowResource = new WorkflowResource(
                        path + ":" + abbreviatedObjectId.name(), revCommit.getCommitTime(),
                        Map.of(WorkflowConstants.PATH, path), new ByteArrayResource(objectLoader.getBytes()),
                        Workflow.Format.parse(path));
                }
            }

            return workflowResource;
        }
    }

    private void clear() {
        if (repositoryDir != null) {
            org.springframework.util.FileSystemUtils.deleteRecursively(repositoryDir);
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
