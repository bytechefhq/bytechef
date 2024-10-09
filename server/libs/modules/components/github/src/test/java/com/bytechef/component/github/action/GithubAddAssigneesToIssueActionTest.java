/*
 * Copyright 2023-present ByteChef Inc.
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
 */

package com.bytechef.component.github.action;

import com.bytechef.component.definition.Context;
import com.bytechef.component.github.util.GithubUtils;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY;
import static com.bytechef.component.github.constant.GithubConstants.ISSUE;
import static com.bytechef.component.github.constant.GithubConstants.ASSIGNEES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author Mayank Madan
 */
public class GithubAddAssigneesToIssueActionTest extends AbstractGithubActionTest{
    private final Map<String, Object> propertyStubsMap = Map.of(REPOSITORY, "testRepo",ISSUE, "testIssue", ASSIGNEES, "githubUsername");

  @Test
  void testPerform() {

    when(mockedParameters.getRequiredString(REPOSITORY))
      .thenReturn((String) propertyStubsMap.get(REPOSITORY));
    when(mockedParameters.getRequiredString(ISSUE))
      .thenReturn((String) propertyStubsMap.get(ISSUE));
    when(mockedParameters.getRequiredString(ASSIGNEES))
      .thenReturn((String) propertyStubsMap.get(ASSIGNEES));
    when(GithubUtils.getOwnerName(mockedContext))
      .thenReturn("testOwner");

    Map<String, Object> result = GithubAddAssigneesToIssueAction
      .perform(mockedParameters, mockedParameters, mockedContext);

    Context.Http.Body body = bodyArgumentCaptor.getValue();
    Map<?, ?> map = (Map<?, ?>) body.getContent();
    assertEquals(propertyStubsMap.get(ASSIGNEES) , map.get(ASSIGNEES));
  }


}
