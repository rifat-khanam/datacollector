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
package com.streamsets.pipeline.stage.processor.http;

import com.streamsets.pipeline.api.ConfigDefBean;
import com.streamsets.pipeline.api.ConfigGroups;
import com.streamsets.pipeline.api.ExecutionMode;
import com.streamsets.pipeline.api.GenerateResourceBundle;
import com.streamsets.pipeline.api.HideConfigs;
import com.streamsets.pipeline.api.Processor;
import com.streamsets.pipeline.api.StageDef;
import com.streamsets.pipeline.api.base.configurablestage.DProcessor;
import com.streamsets.pipeline.lib.http.Groups;

@StageDef(
    version = 11,
    label = "HTTP Client",
    description = "Uses an HTTP client to make arbitrary requests.",
    icon = "httpclient.png",
    recordsByRef = true,
    upgrader = HttpProcessorUpgrader.class,
    execution = {
        ExecutionMode.STANDALONE,
        ExecutionMode.CLUSTER_BATCH,
        ExecutionMode.CLUSTER_YARN_STREAMING,
        ExecutionMode.CLUSTER_MESOS_STREAMING,
        ExecutionMode.EDGE,
        ExecutionMode.EMR_BATCH
    },
    onlineHelpRefUrl ="index.html?contextID=task_z54_1qr_fw"
)
@HideConfigs(value = {
    "conf.dataFormatConfig.jsonContent"
})
@ConfigGroups(Groups.class)
@GenerateResourceBundle
public class HttpDProcessor extends DProcessor {

  @ConfigDefBean
  public HttpProcessorConfig conf;

  @Override
  protected Processor createProcessor() {
    return new HttpProcessor(conf);
  }
}
