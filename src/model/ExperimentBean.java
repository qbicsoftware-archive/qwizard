package model;

/**
 * Bean item representing experiments with their code, type and the number of numOfSamples they contain
 * @author Andreas Friedrich
 *
 */
public class ExperimentBean {
  
  private String code;
  private String experiment_type;
  private String numOfSamples;

  /**
   * Creates a new ExperimentBean
   * @param code experiment code/identifier
   * @param experimentTypeCode the type code of the experiment
   * @param numOfSamples number of samples in this experiment
   */
  public ExperimentBean(String code, String experimentTypeCode, String numOfSamples) {
    this.code = code;
    this.experiment_type = experimentTypeCode;
    this.numOfSamples = numOfSamples;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getExperiment_type() {
    return experiment_type;
  }

  public void setExperiment_type(String experiment_type) {
    this.experiment_type = experiment_type;
  }

  public String getSamples() {
    return numOfSamples;
  }

  public void setSamples(String samples) {
    this.numOfSamples = samples;
  }

}
