package model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import properties.Factor;

public abstract class AOpenbisSample {

  String sampleType;
  String code;
  String experiment;
  String Q_SECONDARY_NAME;
  List<Factor> factors;
  String Q_ADDITIONAL_NOTES;
  String parent;

  public void setSampleType(String sampleType) {
    this.sampleType = sampleType;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public void setExperiment(String experiment) {
    this.experiment = experiment;
  }

  public void setQ_SECONDARY_NAME(String q_SECONDARY_NAME) {
    Q_SECONDARY_NAME = q_SECONDARY_NAME;
  }

  public void setFactors(List<Factor> factors) {
    this.factors = factors;
  }

  public void setQ_ADDITIONAL_NOTES(String q_ADDITIONAL_NOTES) {
    Q_ADDITIONAL_NOTES = q_ADDITIONAL_NOTES;
  }

  public void setParent(String parent) {
    this.parent = parent;
  }

  AOpenbisSample(String code, String experiment, String secondaryName, String additionalNotes,
      List<Factor> factors, String parent) {
    this.code = code;
    this.experiment = experiment;
    this.Q_ADDITIONAL_NOTES = additionalNotes;
    this.Q_SECONDARY_NAME = secondaryName;
    this.factors = factors;
    this.parent = parent;
  }

  public Map<String, String> getValueMap() {
    Map<String, String> res = new HashMap<String, String>();
    res.put("EXPERIMENT", experiment);
    res.put("SAMPLE TYPE", sampleType);
    res.put("code", code);
    res.put("Q_ADDITIONAL_INFO", Q_ADDITIONAL_NOTES);
    res.put("Q_SECONDARY_NAME", Q_SECONDARY_NAME);
    res.put("PARENT", parent);
    fillInFactors(res);
    return res;
  }

  private void fillInFactors(Map<String, String> map) {
    String res = "";
    for (Factor f : factors) {
      res += f.getLabel() + ": " + f.getValue(); //TODO null should be empty list
      if (f.hasUnit())
        res += ":" + f.getUnit();
      res += ";";
    }
    res = res.substring(0, Math.max(res.length()-1,0));
    map.put("XML_FACTORS",res);
  }

  public String getCode() {
    return code;
  }

  public String getQ_SECONDARY_NAME() {
    return Q_SECONDARY_NAME;
  }

  public List<Factor> getFactors() {
    return factors;
  }

  public String getQ_ADDITIONAL_NOTES() {
    return Q_ADDITIONAL_NOTES;
  }

  public String getParent() {
    return parent;
  }
}
