/*
 * Copyright 2017 StreamSets Inc.
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
package com.streamsets.pipeline.stage.origin.datalake.gen1;

import com.streamsets.pipeline.api.ConfigDefBean;
import com.streamsets.pipeline.api.ConfigGroups;
import com.streamsets.pipeline.api.ExecutionMode;
import com.streamsets.pipeline.api.GenerateResourceBundle;
import com.streamsets.pipeline.api.HideConfigs;
import com.streamsets.pipeline.api.PushSource;
import com.streamsets.pipeline.api.StageDef;
import com.streamsets.pipeline.api.base.configurablestage.DPushSource;
import com.streamsets.pipeline.lib.dirspooler.SpoolDirConfigBean;
import com.streamsets.pipeline.stage.conf.DataLakeSourceGroups;

@StageDef(
    version = 1,
    label = "Azure Data Lake Storage Gen1",
    description = "Reads data from Azure Data Lake Storage Gen1",
    icon = "data-lake-store-gen1.png",
    producesEvents = true,
    execution = ExecutionMode.STANDALONE,
    recordsByRef = true,
    resetOffset = true,
    onlineHelpRefUrl = "index.html?contextID=task_t13_ht5_5hb",
    beta = true
)
@ConfigGroups(value = DataLakeSourceGroups.class)
@HideConfigs(value = {
    "dataLakeConfig.hdfsUri",
    "dataLakeConfig.hdfsUser",
    "dataLakeConfig.hdfsKerberos",
    "dataLakeConfig.hdfsConfDir",
    "dataLakeConfig.hdfsConfigs"
})
@GenerateResourceBundle
public class DataLakeDSource extends DPushSource {

  @ConfigDefBean
  public DataLakeSourceConfig dataLakeConfig;

  @ConfigDefBean
  public SpoolDirConfigBean spoolDirConfig;

  @Override
  protected PushSource createPushSource() {
    return new DataLakeSource(spoolDirConfig, dataLakeConfig);
  }
}
