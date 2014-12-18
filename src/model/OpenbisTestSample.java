package model;

import java.util.List;
import java.util.Map;

import properties.Factor;

public class OpenbisTestSample extends AOpenbisSample {

  String Q_SAMPLE_TYPE;

  public OpenbisTestSample(String openbisName, String experiment, String secondaryName, String additionalNotes,
      List<Factor> factors, String sampleType, String parent) {
    super(openbisName, experiment, secondaryName, additionalNotes, factors, parent);
    this.Q_SAMPLE_TYPE = sampleType;
    this.sampleType = "Q_TEST_SAMPLE";
  }

  public Map<String, String> getValueMap() {
    Map<String, String> res = super.getValueMap();
    res.put("Q_SAMPLE_TYPE", Q_SAMPLE_TYPE);
    return res;
  }

}
