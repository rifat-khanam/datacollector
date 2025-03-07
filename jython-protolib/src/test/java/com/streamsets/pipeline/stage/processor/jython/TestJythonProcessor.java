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
package com.streamsets.pipeline.stage.processor.jython;

import com.streamsets.pipeline.api.Field;
import com.streamsets.pipeline.api.OnRecordError;
import com.streamsets.pipeline.api.Processor;
import com.streamsets.pipeline.api.Record;
import com.streamsets.pipeline.api.StageException;
import com.streamsets.pipeline.sdk.RecordCreator;
import com.streamsets.pipeline.stage.processor.scripting.ProcessingMode;
import com.streamsets.pipeline.stage.processor.scripting.ScriptingProcessorTestUtil;
import com.streamsets.pipeline.stage.processor.scripting.config.ScriptRecordType;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TestJythonProcessor {

  @Test
  public void testJythonMapArray() throws Exception {
    Processor processor = new JythonProcessor(
        ProcessingMode.RECORD,
        "output.write(records[0])\n" +
            "records[0].value = 'Hello'\n" +
            "output.write(records[0])\n" +
            "records[0].value = { 'foo' : 'FOO' };\n" +
            "output.write(records[0])\n" +
            "records[0].value = [ 5 ]\n" +
            "output.write(records[0])\n" +
            ""
    );

    ScriptingProcessorTestUtil.verifyMapAndArray(JythonDProcessor.class, processor);
  }

  @Test
  public void testJythonFileRefField() throws Exception {
    String script = "for record in records:\n" +
        "  try:\n" +
        "    fileRef = record.value['fileRef']\n" +
        "    input_stream = fileRef.getInputStream()\n" +
        "    b = []\n" +
        "    while True:\n" +
        "      read = input_stream.read()\n" +
        "      if read == -1:\n" +
        "        break\n" +
        "      b.append(read)\n" +
        "    input_stream.close()\n" +
        "    record.value['byte_array'] = b\n" +
        "    output.write(record)\n" +
        "\n" +
        "  except Exception as e:\n" +
        "    # Send record to error\n" +
        "    error.write(record, str(e))";

    Processor processor = new JythonProcessor(
        ProcessingMode.RECORD,
        script
    );
    ScriptingProcessorTestUtil.verifyFileRef(JythonDProcessor.class, processor);
  }

  private void testMode(ProcessingMode mode) throws Exception {
    Processor processor = new JythonProcessor(mode,
        "for record in records:\n" +
            "  output.write(record)");

    ScriptingProcessorTestUtil.verifyMode(JythonDProcessor.class, processor);
  }

  @Test
  public void testRecordMode() throws Exception {
    testMode(ProcessingMode.RECORD);
  }

  @Test
  public void testBatchMode() throws Exception {
    testMode(ProcessingMode.BATCH);
  }

  private void testRecordModeOnErrorHandling(OnRecordError onRecordError) throws Exception {
    Processor processor = new JythonProcessor(
        ProcessingMode.RECORD,
        "for record in records:\n" +
            "  if record.value == 'Hello':\n" +
            "    raise Exception()\n" +
            "  output.write(record)"
    );

    ScriptingProcessorTestUtil.verifyRecordModeOnErrorHandling(JythonDProcessor.class, processor, onRecordError);
  }


  @Test
  public void testRecordOnErrorDiscard() throws Exception {
    testRecordModeOnErrorHandling(OnRecordError.DISCARD);
  }

  @Test
  public void testRecordOnErrorToError() throws Exception {
    testRecordModeOnErrorHandling(OnRecordError.TO_ERROR);
  }

  @Test(expected = StageException.class)
  public void testRecordOnErrorStopPipeline() throws Exception {
    testRecordModeOnErrorHandling(OnRecordError.STOP_PIPELINE);
  }

  private void testBatchModeOnErrorHandling(OnRecordError onRecordError) throws Exception {
    Processor processor = new JythonProcessor(
        ProcessingMode.BATCH,
        "for record in records:\n" +
            "  if record.value == 'Hello':\n" +
            "    raise Exception()\n" +
            "  output.write(record)"
    );

    ScriptingProcessorTestUtil.verifyBatchModeOnErrorHandling(JythonDProcessor.class, processor, onRecordError);
  }


  @Test(expected = StageException.class)
  public void testBatchOnErrorDiscard() throws Exception {
    testBatchModeOnErrorHandling(OnRecordError.DISCARD);
  }

  @Test(expected = StageException.class)
  public void testBatchOnErrorToError() throws Exception {
    testBatchModeOnErrorHandling(OnRecordError.TO_ERROR);
  }

  @Test(expected = StageException.class)
  public void testBatchOnErrorStopPipeline() throws Exception {
    testBatchModeOnErrorHandling(OnRecordError.STOP_PIPELINE);
  }

  @Test
  public void testPrimitiveTypesPassthrough() throws Exception {
    Processor processor = new JythonProcessor(
        ProcessingMode.RECORD,
        "import sys\n" +
            "from datetime import datetime\n" + // Verify that site.py was processed properly and std modules on path
            "for record in records:\n" +
            "  output.write(record)\n"
    );

    ScriptingProcessorTestUtil.verifyPrimitiveTypesPassthrough(JythonDProcessor.class, processor);
  }

  @Test
  public void testPrimitiveTypesFromScripting() throws Exception {
    Processor processor = new JythonProcessor(ProcessingMode.RECORD,
        "for record in records:\n" +
            "  record.value = [ 1, 5L, 0.5, True, 'hello' ]\n" +
            "  output.write(record)\n" +
            "  record.value = None\n" +
            "  output.write(record)\n" +
            "");
    ScriptingProcessorTestUtil.verifyPrimitiveTypesFromScripting(JythonDProcessor.class, processor);
  }

  @Test
  public void testStateObject() throws Exception {
    Processor processor = new JythonProcessor(
        ProcessingMode.RECORD,
        "if not 'total_count' in state:\n" +
            "  state['total_count'] = 0\n" +
            "state['total_count'] = state['total_count'] + len(records)\n" +
            "for record in records:\n" +
            "  record.value['count'] = state['total_count']\n" +
            "  output.write(record)\n"
    );
    ScriptingProcessorTestUtil.verifyStateObject(JythonDProcessor.class, processor);
  }

  @Test
  public void testListMap() throws Exception {
    Processor processor = new JythonProcessor(
        ProcessingMode.RECORD,
        "output.write(records[0])\n" +
            "records[0].value['Hello'] = 2\n" +
            "output.write(records[0])\n" +
            ""
    );
    ScriptingProcessorTestUtil.verifyListMap(JythonDProcessor.class, processor);
  }

  @Test
  public void testMapCreation() throws Exception {
    Processor processor = new JythonProcessor(
        ProcessingMode.RECORD,
        "newMap = sdcFunctions.createMap(True)\n" +
            "newMap['Key'] = 'streamsets'\n" +
            "records[0].value['Test'] = newMap\n" +
            "output.write(records[0])\n" +
            "newRecord = sdcFunctions.createRecord('id')\n" +
            "rootMap = sdcFunctions.createMap(True)\n" +
            "rootMap['Hello'] = 2\n" +
            "newRecord.value = rootMap\n" +
            "newMap2 = sdcFunctions.createMap(False)\n" +
            "newMap2['Key'] = 'dpm'\n" +
            "newRecord.value['Test'] = newMap2\n" +
            "output.write(newRecord)"
    );
    ScriptingProcessorTestUtil.verifyMapListMapCreation(JythonDProcessor.class, processor);
  }

  @Test
  public void testEventCreation() throws Exception {
    Processor processor = new JythonProcessor(
        ProcessingMode.RECORD,
        "event = sdcFunctions.createEvent(\"not important\", 1)\n" +
            "event.value = {\"a\": 1, \"b\" :2, \"c\": 3}\n" +
            "sdcFunctions.toEvent(event)"
    );
    ScriptingProcessorTestUtil.verifyEventCreation(JythonDProcessor.class, processor);
  }

  @Test
  public void testTypedNullPassThrough() throws Exception {
    Processor processor = new JythonProcessor(
        ProcessingMode.BATCH,
        "for record in records:\n" +
            "  output.write(record)"
    );
    ScriptingProcessorTestUtil.verifyPreserveTypeForNullValue(JythonDProcessor.class, processor);
  }

  @Test
  public void testAssignNullToTypedField() throws Exception {
    Processor processor = new JythonProcessor(
        ProcessingMode.BATCH,
        // record.value will be a list
        "for record in records:\n" +
            "  for r in record.value:\n" +
            "      r = None\n" +
            "  output.write(record)"
    );
    ScriptingProcessorTestUtil.verifyPreserveTypeForNullValue(JythonDProcessor.class, processor);
  }

  @Test
  public void testNestedMapWithNull() throws Exception {
    Processor processor = new JythonProcessor(
        ProcessingMode.BATCH,
        "for record in records:\n" +
            "  for k in record.value['row1']:\n" +
            "      record.value['row1'][k] = None\n" +
            "  record.value['row2'] = None\n" +
            "  output.write(record)"
    );
    ScriptingProcessorTestUtil.verifyNestedMap(JythonDProcessor.class, processor);
  }

  @Test
  public void testChangeFieldTypeFromScripting() throws Exception {
    Processor processor = new JythonProcessor(
        ProcessingMode.BATCH,
        "from decimal import Decimal\n" +
        "from datetime import date\n" +
        "for record in records:\n" +
            "  record.value['int_long'] = 5L\n" +
            "  record.value['long_bool'] = True\n" +
            "  record.value['str_date'] = date.today()\n" +
            "  record.value['double_decimal'] = Decimal(1235.678)\n" +
            "  output.write(record)"
    );
    ScriptingProcessorTestUtil.verifyChangedTypeFromScripting(JythonDProcessor.class, processor);
  }

  @Test
  public void testListMapOrder() throws Exception {
    Processor processor = new JythonProcessor(ProcessingMode.RECORD,
        "records[0].value['A0'] = 0\n" +
            "records[0].value['A1'] = 1\n" +
            "records[0].value['A2'] = 2\n" +
            "records[0].value['A3'] = 3\n" +
            "records[0].value['A4'] = 4\n" +
            "records[0].value['A5'] = 5\n" +
            "records[0].value['A6'] = 6\n" +
            "records[0].value['A7'] = 7\n" +
            "records[0].value['A8'] = 8\n" +
            "records[0].value['A9'] = 9\n" +
            "records[0].value['A10'] = 10\n" +
            "records[0].value['A11'] = 11\n" +
            "records[0].value['A12'] = 12\n" +
            "records[0].value['A13'] = 13\n" +
            "records[0].value['A14'] = 14\n" +
            "records[0].value['A15'] = 15\n" +
            "records[0].value['A16'] = 16\n" +
            "records[0].value['A17'] = 17\n" +
            "records[0].value['A18'] = 18\n" +
            "records[0].value['A19'] = 19\n" +
            "output.write(records[0])\n" +
            "");
    ScriptingProcessorTestUtil.verifyListMapOrder(JythonDProcessor.class, processor);
  }

  @Test
  public void testNewFieldWithTypedNull() throws Exception {
    // initial data in record is empty
    Record record = RecordCreator.create();
    Map<String, Field> map = new HashMap<>();
    record.set(Field.create(map));

    Processor processor = new JythonProcessor(
        ProcessingMode.RECORD,
        "for record in records:\n" +
            "  record.value['null_int'] = NULL_INTEGER\n" +
            "  record.value['null_long'] = NULL_LONG\n" +
            "  record.value['null_float'] = NULL_FLOAT\n" +
            "  record.value['null_double'] = NULL_DOUBLE\n" +
            "  record.value['null_date'] = NULL_DATE\n" +
            "  record.value['null_datetime'] = NULL_DATETIME\n" +
            "  record.value['null_boolean'] = NULL_BOOLEAN\n" +
            "  record.value['null_decimal'] = NULL_DECIMAL\n" +
            "  record.value['null_byteArray'] = NULL_BYTE_ARRAY\n" +
            "  record.value['null_string'] = NULL_STRING\n" +
            "  record.value['null_list'] = NULL_LIST\n" +
            "  record.value['null_map'] = NULL_MAP\n" +
            "  record.value['null_time'] = NULL_TIME\n" +
            "  output.write(record)\n"
    );

    ScriptingProcessorTestUtil.verifyTypedFieldWithNullValue(JythonDProcessor.class, processor, record);
  }

  @Test
  public void testChangeFieldToTypedNull() throws Exception {
    // initial data in record
    Record record = RecordCreator.create();
    Map<String, Field> map = new HashMap<>();
    map.put("null_int", Field.create("this is string field"));
    map.put("null_string", Field.create(123L));
    map.put("null_date", Field.create(true));
    map.put("null_decimal", Field.createDate(null));
    map.put("null_time", Field.createTime(new Date()));
    // add list field
    List<Field> list1 = new LinkedList<>();
    list1.add(Field.create("dummy field list"));
    map.put("null_list", Field.create(list1));
    // add map field
    Map<String, Field> map1 = new HashMap<>();
    map1.put("dummy", Field.create("dummy field map"));
    map.put("null_map", Field.create(map1));

    record.set(Field.create(map));

    Processor processor = new JythonProcessor(
        ProcessingMode.RECORD,
        "for record in records:\n" +
            "  record.value['null_int'] = NULL_INTEGER\n" +
            "  record.value['null_date'] = NULL_DATE\n" +
            "  record.value['null_decimal'] = NULL_DECIMAL\n" +
            "  record.value['null_string'] = NULL_STRING\n" +
            "  record.value['null_time'] = NULL_TIME\n" +
            "  record.value['null_list'] = NULL_LIST\n" +
            "  record.value['null_map'] = NULL_MAP\n" +
            "  output.write(record)\n"
    );
    ScriptingProcessorTestUtil.verifyTypedFieldWithNullValue(JythonDProcessor.class, processor,record);
  }

  @Test
  public void testGetFieldNull() throws Exception {
    // initial data in record
    Record record = RecordCreator.create();
    Map<String, Field> map = new HashMap<>();
    map.put("null_int", Field.create(Field.Type.INTEGER, null));
    map.put("null_string", Field.create(Field.Type.STRING, null));
    map.put("null_boolean", Field.create(Field.Type.BOOLEAN, null));
    map.put("null_list", Field.create(Field.Type.LIST, null));
    map.put("null_map", Field.create(Field.Type.MAP, null));
    // original record has value in the field, so getFieldNull should return the value
    map.put("null_datetime", Field.createDatetime(new Date()));
    record.set(Field.create(map));

    Processor processor = new JythonProcessor(
        ProcessingMode.RECORD,
        "for record in records:\n" +
            "  if sdcFunctions.getFieldNull(record, '/null_int') == NULL_INTEGER:\n" +
            "      record.value['null_int'] = 123 \n" +
            "  if sdcFunctions.getFieldNull(record, '/null_string') == NULL_STRING:\n" +
            "      record.value['null_string'] = 'test' \n" +
            "  if sdcFunctions.getFieldNull(record, '/null_boolean') == NULL_BOOLEAN:\n" +
            "      record.value['null_boolean'] = True \n" +
            "  if sdcFunctions.getFieldNull(record, '/null_list') is NULL_LIST:\n" +
            "      record.value['null_list'] = ['elem1', 'elem2'] \n" +
            "  if sdcFunctions.getFieldNull(record, '/null_map') == NULL_MAP:\n" +
            "      record.value['null_map'] = {'x': 'X', 'y': 'Y'} \n" +
            "  if sdcFunctions.getFieldNull(record, '/null_datetime') == NULL_DATETIME:\n" + // this should be false
            "      record.value['null_datetime'] = NULL_DATETIME \n" +
            "  output.write(record);\n"
    );

    ScriptingProcessorTestUtil.verifyNullField(JythonDProcessor.class, processor, record);
  }

  @Test
  public void testCreateRecordWithNewRecordId() throws Exception {
    String recordId = "recordId";
    String script = "for record in records:\n" +
        "  try:\n" +
        "    newRecord = sdcFunctions.createRecord('" + recordId + "');\n" +
        "    newRecord.value = {'record_value' : 'record_value'}\n" +
        "    output.write(record)\n" +
        "    output.write(newRecord)\n" +
        "  except Exception as e:\n" +
        "    error.write(record, str(e))";

    Processor processor = new JythonProcessor(
        ProcessingMode.RECORD,
        script
    );
    ScriptingProcessorTestUtil.verifyCreateRecord(JythonDProcessor.class, processor);
  }

  @Test
  public void testRecordHeaderAttributes() throws Exception {
    String headerKey = "key1";
    String value = "value1";
    String script = "for record in records:\n" +
        "  record.attributes['" + headerKey + "'] = '" + value + "'\n" +
        "  record.attributes.remove('remove')\n" +
        "  output.write(record)";

    Processor processor = new JythonProcessor(
        ProcessingMode.RECORD,
        script
    );

    Record record = RecordCreator.create();
    ScriptingProcessorTestUtil.verifyRecordHeaderAttribute(JythonDProcessor.class, processor, record);
  }

  @Test
  public void testAccessSdcRecord() throws Exception {
    String script = "for record in records:\n" +
        "  record.attributes['attr'] = record.sdcRecord.get('/value').getAttribute('attr')\n" +
        "  output.write(record)";

    Processor processor = new JythonProcessor(
        ProcessingMode.RECORD,
        script
    );

    ScriptingProcessorTestUtil.verifyAccessToSdcRecord(JythonDProcessor.class, processor);
  }

  @Test
  public void testInitDestroy() throws Exception {
    String initScript = "state['initValue'] = 'init'";
    String script = "for record in records:\n" +
        "  record.value['initValue'] = state['initValue']\n" +
        "  output.write(record)\n";
    String destroyScript = "event = sdcFunctions.createEvent(\"event\", 1)\n" +
      "sdcFunctions.toEvent(event)";

    Processor processor = new JythonProcessor(
        ProcessingMode.BATCH,
        script,
        initScript,
        destroyScript,
        ScriptRecordType.NATIVE_OBJECTS,
        new HashMap<>()
    );
    ScriptingProcessorTestUtil.verifyInitDestroy(JythonDProcessor.class, processor);
  }

  @Test
  public void testConstants() throws Exception {
    String script = "for record in records:\n" +
        "  record.value['company'] = sdcFunctions.pipelineParameters()['company']\n" +
        "  output.write(record)";
    Processor processor = new JythonProcessor(ProcessingMode.BATCH, script);
    ScriptingProcessorTestUtil.verifyConstants(JythonDProcessor.class, processor);
  }

  @Test
  public void testIsPreview() throws Exception {
    String script = "for record in records:\n" +
        "  record.value['isPreview'] = sdcFunctions.isPreview();\n" +
        "  output.write(record)";
    Processor processor = new JythonProcessor(ProcessingMode.BATCH, script);
    ScriptingProcessorTestUtil.verifyIsPreview(JythonDProcessor.class, processor);
  }

  private static final String WRITE_ERROR_SCRIPT = "for record in records:\n  error.write(record, 'oops')\n";

  @Test
  public void testErrorRecordStopPipeline() throws Exception {
    Processor processor = new JythonProcessor(
      ProcessingMode.RECORD,
      WRITE_ERROR_SCRIPT
    );
    ScriptingProcessorTestUtil.verifyErrorRecordStopPipeline(JythonDProcessor.class, processor);
  }

  @Test
  public void testErrorRecordDiscard() throws Exception {
    Processor processor = new JythonProcessor(
      ProcessingMode.RECORD,
      WRITE_ERROR_SCRIPT
    );
    ScriptingProcessorTestUtil.verifyErrorRecordDiscard(JythonDProcessor.class, processor);
  }

  @Test
  public void testErrorRecordErrorSink() throws Exception {
    Processor processor = new JythonProcessor(
      ProcessingMode.RECORD,
      WRITE_ERROR_SCRIPT
    );
    ScriptingProcessorTestUtil.verifyErrorRecordErrorSink(JythonDProcessor.class, processor);
  }

  @Test
  public void testSdcRecord() throws Exception {
    String script = "from com.streamsets.pipeline.api import Field\n" +
      "for record in records:\n" +
      "  record.sdcRecord.set('/new', Field.create(Field.Type.STRING, 'new-value'))\n" +
      "  record.sdcRecord.get('/old').setAttribute('attr', 'attr-value')\n" +
      "  output.write(record)\n";

    Processor processor = new JythonProcessor(
        ProcessingMode.RECORD,
        script,
        "",
        "",
        ScriptRecordType.SDC_RECORDS,
        new HashMap<>()
     );
    ScriptingProcessorTestUtil.verifySdcRecord(JythonDProcessor.class, processor);
  }

  @Test
  public void testUserParams() throws Exception {
    String script =  "for record in records:\n" +
        "  record.value['user-param-key'] = sdcUserParams['user-param-key']\n" +
        "  output.write(record)";
    Map<String, String> userParams = new HashMap<>();
    userParams.put("user-param-key", "user-param-value");
    Processor processor = new JythonProcessor(
        ProcessingMode.RECORD,
        script,
        "",
        "",
        ScriptRecordType.NATIVE_OBJECTS,
        userParams
     );
    ScriptingProcessorTestUtil.verifyUserParams(JythonDProcessor.class, processor);
  }
}
