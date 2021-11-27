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

package com.integri.atlas.config;

/**
 * @author Arik Cohen
 */
public class WorkflowRepositoryProperties {

    private GitProperties git;
    private ClasspathProperties classpath;

    public GitProperties getGit() {
        return git;
    }

    public void setGit(GitProperties aGit) {
        git = aGit;
    }

    public ClasspathProperties getClasspath() {
        return classpath;
    }

    public void setClasspath(ClasspathProperties aClasspath) {
        classpath = aClasspath;
    }

    public static class ClasspathProperties {

        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean aEnabled) {
            enabled = aEnabled;
        }
    }

    public static class GitProperties {

        private boolean enabled = false;
        private String url;
        private String[] searchPaths;
        private String branch = "master";
        private String username;
        private String password;

        public String getUrl() {
            return url;
        }

        public void setUrl(String aUrl) {
            url = aUrl;
        }

        public String[] getSearchPaths() {
            return searchPaths;
        }

        public void setSearchPaths(String[] aSearchPaths) {
            searchPaths = aSearchPaths;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean aEnabled) {
            enabled = aEnabled;
        }

        public String getBranch() {
            return branch;
        }

        public void setBranch(String aBranch) {
            branch = aBranch;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String aUsername) {
            username = aUsername;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String aPassword) {
            password = aPassword;
        }
    }
}
