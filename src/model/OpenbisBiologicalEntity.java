package model;

import java.util.List;
import java.util.Map;

import properties.Factor;

/**
 * Class representing a biological entity that will be used in an experiment and will be the root of the sample hierarchy
 * @author Andreas Friedrich
 *
 */
public class OpenbisBiologicalEntity extends AOpenbisSample {

  String ncbiOrganism;

   /**
    * Create a new Biological Entity
    * @param openbisName Code of the sample
    * @param experiment Experiment the sample is attached to
    * @param secondaryName Secondary name of the sample (e.g. humanly readable identifier)
    * @param additionalNotes Free text notes for the sample
    * @param factors A list of conditions of this sample
    * @param ncbiOrganism The organism the entity belongs to
    */
  public OpenbisBiologicalEntity(String openbisName, String experiment, String secondaryName, String additionalNotes,
      List<Factor> factors, String ncbiOrganism) {
    super(openbisName, experiment, secondaryName, additionalNotes, factors, "");
    this.ncbiOrganism = ncbiOrganism;
    this.sampleType = "Q_BIOLOGICAL_ENTITY";
  }
  
  public Map<String,String> getValueMap() {
    Map<String,String> res = super.getValueMap();
    res.put("Q_NCBI_ORGANISM", ncbiOrganism);
    return res;
  }
}
