/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.repository.git.pipeline;

import com.google.common.base.Throwables;
import com.google.common.io.Files;
import com.integri.atlas.engine.coordinator.pipeline.IdentifiableResource;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
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

public class JGitTemplate implements GitOperations {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private static final String LATEST = "latest";

    private File repositoryDir = null;

    private final String url;
    private final String branch;
    private final String[] searchPaths;
    private final String username;
    private final String password;

    public JGitTemplate(String aUrl, String aBranch, String[] aSearchPaths, String aUsername, String aPassword) {
        url = aUrl;
        branch = aBranch;
        searchPaths = aSearchPaths;
        username = aUsername;
        password = aPassword;
    }

    @Override
    public List<IdentifiableResource> getHeadFiles() {
        Repository repo = getRepository();
        return getHeadFiles(repo, searchPaths);
    }

    private List<IdentifiableResource> getHeadFiles(Repository aRepository, String... aSearchPaths) {
        List<String> searchPaths = Arrays.asList(aSearchPaths);
        List<IdentifiableResource> resources = new ArrayList<>();
        try (
            ObjectReader reader = aRepository.newObjectReader();
            RevWalk walk = new RevWalk(reader);
            TreeWalk treeWalk = new TreeWalk(aRepository, reader);
        ) {
            final ObjectId id = aRepository.resolve(Constants.HEAD);
            if (id == null) {
                return List.of();
            }
            RevCommit commit = walk.parseCommit(id);
            RevTree tree = commit.getTree();
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            while (treeWalk.next()) {
                String path = treeWalk.getPathString();
                if (
                    !path.startsWith(".") &&
                    (
                        searchPaths == null ||
                        searchPaths.size() == 0 ||
                        searchPaths.stream().anyMatch(sp -> path.startsWith(sp))
                    )
                ) {
                    ObjectId objectId = treeWalk.getObjectId(0);
                    logger.debug("Loading {} [{}]", path, objectId.name());
                    resources.add(readBlob(aRepository, path.substring(0, path.indexOf('.')), objectId.name()));
                }
            }
            return resources;
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private synchronized Repository getRepository() {
        try {
            clear();
            logger.info("Cloning {} {}", url, branch);
            CloneCommand cmd = Git.cloneRepository().setURI(url).setBranch(branch).setDirectory(repositoryDir);

            if (username != null && password != null) {
                cmd.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password));
            }

            Git git = cmd.call();
            return (git.getRepository());
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public IdentifiableResource getFile(String aFileId) {
        try {
            Repository repository = getRepository();
            int blobIdDelim = aFileId.lastIndexOf(':');
            if (blobIdDelim > -1) {
                String path = aFileId.substring(0, blobIdDelim);
                String blobId = aFileId.substring(blobIdDelim + 1);
                return readBlob(repository, path, blobId);
            } else {
                return readBlob(repository, aFileId, LATEST);
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private IdentifiableResource readBlob(Repository aRepo, String aPath, String aBlobId) throws Exception {
        try (ObjectReader reader = aRepo.newObjectReader()) {
            if (aBlobId.equals(LATEST)) {
                List<IdentifiableResource> headFiles = getHeadFiles(aRepo, aPath);
                Assert.notEmpty(headFiles, "could not find: " + aPath + ":" + aBlobId);
                return headFiles.get(0);
            }
            ObjectId objectId = aRepo.resolve(aBlobId);
            Assert.notNull(objectId, "could not find: " + aPath + ":" + aBlobId);
            byte[] data = reader.open(objectId).getBytes();
            AbbreviatedObjectId abbreviated = reader.abbreviate(objectId);
            return new IdentifiableResource(aPath + ":" + abbreviated.name(), new ByteArrayResource(data));
        }
    }

    private void clear() {
        if (repositoryDir != null) {
            FileUtils.deleteQuietly(repositoryDir);
        }
        repositoryDir = Files.createTempDir();
    }
}
