/*
 * Copyright 2019 StreamSets Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.streamsets.pipeline.upgrader;

import com.streamsets.pipeline.api.Config;
import com.streamsets.pipeline.api.StageException;
import com.streamsets.pipeline.api.StageUpgrader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class YamlStageUpgrader implements StageUpgrader {
  private static final Logger LOG = LoggerFactory.getLogger(YamlStageUpgrader.class);

  private int upgraderVersion;
  private String stageName;
  private Map<Integer, UpgradeToVersion> toVersionMap = Collections.emptyMap();

  public int getUpgraderVersion() {
    return upgraderVersion;
  }

  public YamlStageUpgrader setUpgraderVersion(int upgraderVersion) {
    this.upgraderVersion = upgraderVersion;
    return this;
  }

  public String getStageName() {
    return stageName;
  }

  public YamlStageUpgrader setStageName(String stageName) {
    this.stageName = stageName;
    return this;
  }

  public Map<Integer, UpgradeToVersion> getToVersionMap() {
    return toVersionMap;
  }

  public YamlStageUpgrader setToVersionMap(Map<Integer, UpgradeToVersion> toVersionMap) {
    this.toVersionMap = toVersionMap;
    return this;
  }

  public List<Config> upgrade(
      String library,
      String stageName,
      String stageInstance,
      int fromVersion,
      int toVersion,
      List<Config> configs
  ) throws StageException {
    LOG.debug(
        "Upgrade '{}' stage from '{}' version to '{}' version",
        stageName,
        fromVersion,
        toVersion
    );
    configs = new ArrayList<>(configs); // making sure it is mutable
    TreeMap<Integer, UpgradeToVersion> upgradesToVersionToUse = toVersionMap
        .entrySet()
        .stream()
        .filter(e -> e.getKey() > fromVersion && e.getKey() <= toVersion)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (o1, o2) -> o1, TreeMap::new)); //dummy merger
    for (Map.Entry<Integer, UpgradeToVersion> upgradeToVersion : upgradesToVersionToUse.entrySet()) {
      LOG.debug("Upgrading '{}' stage to '{}' version", stageName, upgradeToVersion.getKey());
      upgradeToVersion.getValue().upgrade(configs);
    }
    return configs;
  }

}
