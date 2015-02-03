package control;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import main.OpenBisClient;
import main.ProjectwizardUI;
import model.AOpenbisSample;
import model.ExperimentBean;
import model.ExperimentType;
import model.OpenbisBiologicalEntity;
import model.OpenbisBiologicalSample;
import model.OpenbisExperiment;
import model.OpenbisTestSample;
import parser.Parser;
import properties.Factor;

import org.vaadin.teemu.wizards.WizardStep;

import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Experiment;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Sample;
import ui.EntityStep;
import ui.ExtractionStep;
import ui.ConditionInstanceStep;
import ui.ProjectContextStep;
import ui.TestStep;

/**
 * Aggregates the data from the wizard needed to create the experimental setup in the form of TSVs
 * 
 * @author Andreas Friedrich
 * 
 */
public class WizardDataAggregator {

  private ProjectContextStep s1;
  private EntityStep s2;
  private ConditionInstanceStep s3;
  private ExtractionStep s5;
  private ConditionInstanceStep s6;
  private TestStep s8;

  private File tsv;

  OpenBisClient openbis;
  Parser xmlParser = new Parser();
  Map<String, String> taxMap;
  Map<String, String> tissueMap;
  Map<String, Factor> factorMap;
  int firstFreeExperimentID;
  int firstFreeEntityID;
  int firstFreeBarcodeID;
  char classChar = 'X';

  // mandatory openBIS fields
  private String spaceCode;
  private String projectCode;
  List<OpenbisExperiment> experiments;
  private String species;
  private String tissue;
  private String sampleType;

  // info needed to create samples
  private int bioReps;
  private int extractReps;
  private int techReps;

  private List<List<String>> bioFactors;
  private List<List<String>> extractFactors;
  private boolean inheritEntities;
  private boolean inheritExtracts;

  List<AOpenbisSample> entities = new ArrayList<AOpenbisSample>();
  List<AOpenbisSample> extracts;
  List<AOpenbisSample> tests;
  Map<String, Character> classChars;

  /**
   * Creates a new WizardDataAggregator
   * 
   * @param steps the steps of the Wizard to extract the data from
   * @param openbis openBIS client connection to query for existing experiments
   * @param taxMap mapping between taxonomy IDs and species names
   * @param tissueMap mapping of tissue names and labels
   */
  public WizardDataAggregator(List<WizardStep> steps, OpenBisClient openbis,
      Map<String, String> taxMap, Map<String, String> tissueMap) {
    s1 = (ProjectContextStep) steps.get(0);
    s2 = (EntityStep) steps.get(1);
    s3 = (ConditionInstanceStep) steps.get(2);
    s5 = (ExtractionStep) steps.get(4);
    s6 = (ConditionInstanceStep) steps.get(5);
    s8 = (TestStep) steps.get(7);

    this.openbis = openbis;
    this.taxMap = taxMap;
    this.tissueMap = tissueMap;
  }

  /**
   * Fetches context information like space and project and computes first unused IDs of samples and
   * experiments.
   */
  private void prepareBasics() {
    firstFreeExperimentID = 1;
    firstFreeEntityID = 1;
    firstFreeBarcodeID = 1;
    spaceCode = s1.getSpaceCode();
    projectCode = s1.getProjectCode();

    for (Experiment e : openbis.getExperimentsOfProjectByCode(projectCode)) {
      String code = e.getCode();
      String[] split = code.split(projectCode + "E");
      if (code.startsWith(projectCode + "E") && split.length > 1) {
        int num = Integer.parseInt(split[1]);
        if (firstFreeExperimentID <= num)
          firstFreeExperimentID = num + 1;
      }
    }

    List<Sample> samples = new ArrayList<Sample>();
    if (openbis.projectExists(spaceCode, projectCode)) {
      samples.addAll(openbis.getSamplesOfProject(projectCode));
    }
    for (Sample s : samples) {
      String code = s.getCode();
      if (Functions.isQbicBarcode(code)) {
        int num = Integer.parseInt(code.substring(5, 8));
        if (num >= firstFreeBarcodeID)
          firstFreeBarcodeID = num + 1;
      } else if (s.getSampleTypeCode().equals(("Q_BIOLOGICAL_ENTITY"))) {
        int num = Integer.parseInt(s.getCode().split("-")[1]);
        if (num >= firstFreeEntityID)
          firstFreeEntityID = num + 1;
      }
    }
  }

  /**
   * Creates the list of biological entities from the input information collected in the aggregator
   * fields and wizard steps and fetches or creates the associated experiments
   * 
   * @return
   * @throws JAXBException
   */
  public List<AOpenbisSample> prepareEntities() throws JAXBException {
    prepareBasics();
    this.factorMap = new HashMap<String, Factor>();
    experiments = new ArrayList<OpenbisExperiment>();
    species = s2.getSpecies();
    bioReps = s2.getBioRepAmount();

    // entities are not created new, but parsed from registered ones
    if (inheritEntities) {
      entities = parseEntities(openbis.getSamplesofExperiment(s1.getExperimentName().getCode()));
      // create new entities and an associated experiment from collected inputs
    } else {
      experiments.add(new OpenbisExperiment(buildExperimentName(),
          ExperimentType.Q_EXPERIMENTAL_DESIGN));

      List<List<Factor>> valueLists = s3.getFactors();
      bioFactors = createFactorInfo(valueLists);

      entities = buildEntities();
    }
    return entities;
  }

  /**
   * Creates the list of biological extracts from the input information collected in the aggregator
   * fields and wizard steps and fetches or creates the associated experiments
   * 
   * @return
   * @throws JAXBException
   */
  public List<AOpenbisSample> prepareExtracts() throws JAXBException {
    tissue = s5.getTissue();
    extractReps = s5.getExtractRepAmount();

    // extracts are not created new, but parsed from registered ones
    if (inheritExtracts) {
      prepareBasics();
      this.factorMap = new HashMap<String, Factor>();
      experiments = new ArrayList<OpenbisExperiment>();
      extracts = parseExtracts(openbis.getSamplesofExperiment(s1.getExperimentName().getCode()));
      // create new entities and an associated experiment from collected inputs
    } else {
      experiments.add(new OpenbisExperiment(buildExperimentName(),
          ExperimentType.Q_SAMPLE_EXTRACTION));
      List<List<Factor>> valueLists = s6.getFactors();
      extractFactors = createFactorInfo(valueLists);

      // keep track of id letters for different conditions
      classChars = new HashMap<String, Character>();
      extracts = buildExtracts(entities, classChars);
    }
    return extracts;
  }

  /**
   * Creates the list of samples prepared for testing from the input information collected in the
   * aggregator fields and wizard steps and fetches or creates the associated experiments
   * 
   * @return
   */
  public List<AOpenbisSample> prepareTestSamples() {
    sampleType = s8.getSampleType();
    techReps = s8.getTechRepAmount();
    if (inheritExtracts) {
      prepareBasics();
      classChars = new HashMap<String, Character>();
      experiments = new ArrayList<OpenbisExperiment>();
    }
    experiments.add(new OpenbisExperiment(buildExperimentName(),
        ExperimentType.Q_SAMPLE_PREPARATION));
    tests = buildTestSamples(extracts, classChars);
    return tests;
  }

  /**
   * Set the list of biological entities (e.g. after filtering it) used in further steps
   * 
   * @param entities
   */
  public void setEntities(List<AOpenbisSample> entities) {
    this.entities = entities;
  }

  /**
   * Set the list of sample extracts (e.g. after filtering it) used in further steps
   * 
   * @param extracts
   */
  public void setExtracts(List<AOpenbisSample> extracts) {
    this.extracts = extracts;
  }

  /**
   * Set the list of test samples
   * 
   * @param tests
   */
  public void setTests(List<AOpenbisSample> tests) {
    this.tests = tests;
  }

  /**
   * Collects conditions as Strings in a list of their instance lists. Also puts the conditions
   * (factors) in a HashMap for later lookup, using value and unit as a key
   * 
   * @param factors List of a list of condition instances (one list per condition)
   * @return List of a list of condition instances (one list per condition) as Strings
   */
  private List<List<String>> createFactorInfo(List<List<Factor>> factors) {
    List<List<String>> res = new ArrayList<List<String>>();
    for (List<Factor> instances : factors) {
      List<String> factorValues = new ArrayList<String>();
      for (Factor f : instances) {
        String name = f.getValue() + f.getUnit();
        factorValues.add(name);
        factorMap.put(name, f);
      }
      res.add(factorValues);
    }
    return res;
  }

  /**
   * Builds an experiment name from the current unused id and increments the id
   * 
   * @return
   */
  private String buildExperimentName() {
    firstFreeExperimentID++;
    return projectCode + "E" + (firstFreeExperimentID - 1);
  }

  /**
   * Generates all permutations of a list of experiment conditions
   * 
   * @param lists Instance lists of different conditions
   * @return List of all possible permutations of the input conditions
   */
  private List<String> generatePermutations(List<List<String>> lists) {
    List<String> res = new ArrayList<String>();
    generatePermutationsHelper(lists, res, 0, "");
    return res;
  }

  /**
   * recursive helper
   */
  private void generatePermutationsHelper(List<List<String>> lists, List<String> result, int depth,
      String current) {
    String separator = "###";
    if (depth == lists.size()) {
      result.add(current);
      return;
    }
    for (int i = 0; i < lists.get(depth).size(); ++i) {
      if (current.equals(""))
        separator = "";
      generatePermutationsHelper(lists, result, depth + 1, current + separator
          + lists.get(depth).get(i));
    }
  }

  /**
   * Build and return a list of all possible biological entities given their conditions, keep track
   * of conditions in a HashMap for later
   * 
   * @return List of AOpenbisSamples containing entity samples
   */
  private List<AOpenbisSample> buildEntities() {
    List<AOpenbisSample> entities = new ArrayList<AOpenbisSample>();
    List<List<String>> factorLists = new ArrayList<List<String>>();
    factorLists.addAll(bioFactors);
    List<String> permutations = generatePermutations(factorLists);
    List<List<String>> permLists = new ArrayList<List<String>>();
    for (String concat : permutations) {
      permLists.add(new ArrayList<String>(Arrays.asList(concat.split("###"))));
    }
    int entityNum = firstFreeEntityID;
    for (int i = bioReps; i > 0; i--) {
      for (List<String> secondaryNameList : permLists) {
        List<Factor> factors = new ArrayList<Factor>();
        for (String name : secondaryNameList) {
          if (factorMap.containsKey(name))
            factors.add(factorMap.get(name));
        }
        if (s2.speciesIsFactor()) {
          for (String factor : secondaryNameList) {
            if (taxMap.containsKey(factor))
              species = factor;
          }
        }
        String taxID = taxMap.get(species);
        String secondaryName = nameListToSecondaryName(secondaryNameList);
        entities.add(new OpenbisBiologicalEntity(projectCode + "ENTITY-" + entityNum, experiments
            .get(0).getOpenbisName(), secondaryName, "", factors, taxID));
        entityNum++;
      }
    }
    return entities;
  }

  /**
   * Build and return a list of all possible biological extracts given their conditions, using
   * existing entities. Keep track of condition in a HashMap for later
   * 
   * @param entities Existing (or prepared) biological entity samples these extracts will be
   *        attached to
   * @param classChars Empty map of different class letters used for the identifiers, to keep track
   *        of for test samples
   * @return List of AOpenbisSamples containing extract samples
   */
  private List<AOpenbisSample> buildExtracts(List<AOpenbisSample> entities,
      Map<String, Character> classChars) {
    List<AOpenbisSample> extracts = new ArrayList<AOpenbisSample>();
    for (AOpenbisSample e : entities) {
      List<List<String>> factorLists = new ArrayList<List<String>>();
      String secName = e.getQ_SECONDARY_NAME();
      if (secName == null)
        secName = "";
      factorLists.add(new ArrayList<String>(Arrays.asList(secName)));

      factorLists.addAll(extractFactors);
      List<String> permutations = generatePermutations(factorLists);
      List<List<String>> permLists = new ArrayList<List<String>>();
      for (String concat : permutations) {
        permLists.add(new ArrayList<String>(Arrays.asList(concat.split("###"))));
      }
      for (List<String> secondaryNameList : permLists) {
        List<Factor> factors = new ArrayList<Factor>();
        factors.addAll(e.getFactors());
        for (String name : secondaryNameList)
          for (String element : name.split(";")) {
            element = element.trim();
            if (factorMap.containsKey(element)) {
              factors.add(factorMap.get(element));
            }
          }
        for (int i = extractReps; i > 0; i--) {
          if (s5.tissueIsFactor()) {
            for (String factor : secondaryNameList) {
              if (tissueMap.containsKey(factor))
                tissue = factor;
            }
          }
          String secondaryName = nameListToSecondaryName(secondaryNameList);
          String tissueCode = tissueMap.get(tissue);
          if (classChars.containsKey(secondaryName)) {
            classChar = classChars.get(secondaryName);
          } else {
            classChar = Functions.incrementUppercase(classChar);
            classChars.put(secondaryName, classChar);
          }
          String code =
              projectCode + Functions.createCountString(firstFreeBarcodeID, 3) + classChar;
          code = code + Functions.checksum(code);
          extracts.add(new OpenbisBiologicalSample(code, experiments.get(experiments.size() - 1)
              .getOpenbisName(), secondaryName, "", factors, tissueCode, "", e.getCode()));
          firstFreeBarcodeID++;
        }
      }
    }
    return extracts;
  }

  /**
   * Build and return a list of all possible sample preparations (test samples), using existing
   * extracts.
   * 
   * @param extracts Existing (or prepared) sample extracts these test samples will be attached to
   * @param classChars Filled map of different class letters used for the extracts
   * @return List of AOpenbisSamples containing test samples
   */
  private List<AOpenbisSample> buildTestSamples(List<AOpenbisSample> extracts,
      Map<String, Character> classChars) {
    List<AOpenbisSample> tests = new ArrayList<AOpenbisSample>();
    for (AOpenbisSample s : extracts) {
      for (int i = techReps; i > 0; i--) {
        String secondaryName = s.getQ_SECONDARY_NAME();
        if (classChars.containsKey(secondaryName)) {
          classChar = classChars.get(secondaryName);
        } else {
          classChar = Functions.incrementUppercase(classChar);
          classChars.put(secondaryName, classChar);
        }
        String code = projectCode + Functions.createCountString(firstFreeBarcodeID, 3) + classChar;
        code = code + Functions.checksum(code);
        tests.add(new OpenbisTestSample(code, experiments.get(experiments.size() - 1)
            .getOpenbisName(), secondaryName, "", s.getFactors(), sampleType, s.getCode()));
        firstFreeBarcodeID++;
      }
    }
    return tests;
  }

  /**
   * parse secondary name from a list of condition permutations
   * 
   * @param secondaryNameList
   * @return
   */
  private String nameListToSecondaryName(List<String> secondaryNameList) {
    String res = secondaryNameList.toString().replace(", ", " ; ");
    return res.substring(1, res.length() - 1);
  }

  /**
   * set flag denoting the inheritance from entities existing in the system
   * 
   * @param inherit
   */
  public void setInheritEntities(boolean inherit) {
    this.inheritEntities = inherit;
  }

  /**
   * set flag denoting the inheritance from extracts existing in the system
   * 
   * @param inherit
   */
  public void setInheritExtracts(boolean inherit) {
    this.inheritExtracts = inherit;
  }

  /**
   * Parse existing entities from the system
   * 
   * @param entities List of biological entities in the form of openBIS Samples
   * @return List of AOpenbisSamples containing entities
   * @throws JAXBException
   */
  private List<AOpenbisSample> parseEntities(List<Sample> entities) throws JAXBException {
    List<AOpenbisSample> res = new ArrayList<AOpenbisSample>();
    for (Sample s : entities) {
      String code = s.getCode();
      String[] eSplit = s.getExperimentIdentifierOrNull().split("/");
      Map<String, String> p = s.getProperties();
      List<Factor> factors = xmlParser.getFactors(xmlParser.parseXMLString(p.get("Q_PROPERTIES")));
      for (Factor f : factors) {
        String name = f.getValue() + f.getUnit();
        factorMap.put(name, f);
      }
      res.add(new OpenbisBiologicalEntity(code, eSplit[eSplit.length - 1], p
          .get("Q_SECONDARY_NAME"), p.get("Q_ADDITIONAL_INFO"), factors, p.get("Q_NCBI_ORGANISM")));
    }
    return res;
  }

  /**
   * Parse existing extracts from the system
   * 
   * @param entities List of biological extracts in the form of openBIS Samples
   * @return List of AOpenbisSamples containing extracts
   * @throws JAXBException
   */
  private List<AOpenbisSample> parseExtracts(List<Sample> extracts) throws JAXBException {
    List<AOpenbisSample> res = new ArrayList<AOpenbisSample>();
    for (Sample s : extracts) {
      String code = s.getCode();

      String[] eSplit = s.getExperimentIdentifierOrNull().split("/");
      Map<String, String> p = s.getProperties();
      List<Factor> factors = xmlParser.getFactors(xmlParser.parseXMLString(p.get("Q_PROPERTIES")));
      for (Factor f : factors) {
        String name = f.getValue() + f.getUnit();
        factorMap.put(name, f);
      }
      res.add(new OpenbisBiologicalSample(code, eSplit[eSplit.length - 1], p
          .get("Q_SECONDARY_NAME"), p.get("Q_ADDITIONAL_INFO"), factors, p.get("Q_PRIMARY_TISSUE"),
          p.get("Q_TISSUE_DETAILED"), parseParents(code)));
    }
    return res;
  }

  /**
   * Parse existing test samples from the system
   * 
   * @param entities List of test samples in the form of openBIS Samples
   * @return List of AOpenbisSamples containing test samples
   * @throws JAXBException
   */
  private List<AOpenbisSample> parseTestSamples(List<Sample> entities) throws JAXBException {
    List<AOpenbisSample> res = new ArrayList<AOpenbisSample>();
    for (Sample s : entities) {
      String code = s.getCode();
      String[] eSplit = s.getExperimentIdentifierOrNull().split("/");
      Map<String, String> p = s.getProperties();
      List<Factor> factors = xmlParser.getFactors(xmlParser.parseXMLString(p.get("Q_PROPERTIES")));
      for (Factor f : factors) {
        String name = f.getValue() + f.getUnit();
        factorMap.put(name, f);
      }
      res.add(new OpenbisTestSample(code, eSplit[eSplit.length - 1], p.get("Q_SECONDARY_NAME"), p
          .get("Q_ADDITIONAL_INFO"), factors, p.get("Q_SAMPLE_TYPE"), parseParents(code)));
    }
    return res;
  }

  /**
   * Get the parents of a sample give its code and return them space delimited so they can be added
   * to a tsv
   * 
   * @param code
   * @return
   */
  private String parseParents(String code) {
    String res = "";
    for (Sample s : openbis.getParents(code))
      res += " " + s.getCode();
    return res.trim();
  }

  /**
   * Copy existing experiments and their samples from the information set in the wizard and the wizard steps. After
   * this function a tsv with the copied experiments can be created
   * 
   * @throws JAXBException
   */
  public void copyExperiment() throws JAXBException {
    prepareBasics();
    factorMap = new HashMap<String, Factor>();
    experiments = new ArrayList<OpenbisExperiment>();

    ExperimentBean exp = s1.getExperimentName();
    String type = exp.getExperiment_type();

    List<Sample> openbisEntities = new ArrayList<Sample>();
    List<Sample> openbisExtracts = new ArrayList<Sample>();
    List<Sample> openbisTests = new ArrayList<Sample>();

    List<Sample> originals = openbis.getSamplesofExperiment(exp.getCode());
    Map<String, String> copies = new HashMap<String, String>();

    if (type.equals(ExperimentType.Q_EXPERIMENTAL_DESIGN.toString())) {

      openbisEntities = originals;
      openbisExtracts = getLowerSamples(openbisEntities);
      openbisTests = getLowerSamples(openbisExtracts);

      entities = copySamples(parseEntities(openbisEntities), copies);
      extracts = copySamples(parseExtracts(openbisExtracts), copies);
      tests = copySamples(parseTestSamples(openbisTests), copies);
    } else if (type.equals(ExperimentType.Q_SAMPLE_EXTRACTION.toString())) {

      openbisExtracts = originals;
      openbisEntities = getUpperSamples(openbisExtracts);
      openbisTests = getLowerSamples(openbisExtracts);

      entities = parseEntities(openbisEntities);

      extracts = copySamples(parseExtracts(openbisExtracts), copies);
      tests = copySamples(parseTestSamples(openbisTests), copies);

    } else if (type.equals(ExperimentType.Q_SAMPLE_PREPARATION.toString())) {

      openbisTests = originals;
      openbisExtracts = getUpperSamples(openbisTests);
      openbisEntities = getUpperSamples(openbisExtracts);

      entities = parseEntities(openbisEntities);
      extracts = parseExtracts(openbisExtracts);

      tests = copySamples(parseTestSamples(openbisTests), copies);
    }
  }

  /**
   * Copy a list of samples, used by the copy experiments function
   * @param samples
   * @param copies
   * @return
   */
  private List<AOpenbisSample> copySamples(List<AOpenbisSample> samples, Map<String, String> copies) {
    String newExp = buildExperimentName();
    String type = samples.get(0).getValueMap().get("SAMPLE TYPE");
    ExperimentType eType = ExperimentType.Q_EXPERIMENTAL_DESIGN;
    if (type.equals("Q_BIOLOGICAL_ENTITY"))
      eType = ExperimentType.Q_EXPERIMENTAL_DESIGN;
    else if (type.equals("Q_BIOLOGICAL_SAMPLE"))
      eType = ExperimentType.Q_SAMPLE_EXTRACTION;
    else if (type.equals("Q_TEST_SAMPLE"))
      eType = ExperimentType.Q_SAMPLE_PREPARATION;
    else
      System.err.println("Unexpected type: " + type);
    experiments.add(new OpenbisExperiment(newExp, eType));

    for (AOpenbisSample s : samples) {
      s.setExperiment(newExp);
      String code = s.getCode();
      String newCode = code;
      if (s instanceof OpenbisBiologicalEntity) {
        newCode = projectCode + "ENTITY-" + firstFreeEntityID;
        firstFreeEntityID++;
      } else {
        newCode =
            projectCode + Functions.createCountString(firstFreeBarcodeID, 3)
                + (code.charAt(code.length() - 2));
        newCode = newCode + Functions.checksum(newCode);
        firstFreeBarcodeID++;
      }
      copies.put(code, newCode);
      s.setCode(newCode);
      String p = s.getParent();
      // change parent if parent was copied
      if (p != null && p.length() > 0)
        if (copies.containsKey(p))
          s.setParent(copies.get(p));
    }
    return samples;
  }

  /**
   * Gets all samples that are one level higher in the sample hierarchy of an attached experiment than a given list of samples
   * @param originals
   * @return
   */
  private List<Sample> getUpperSamples(List<Sample> originals) {
    for (Sample s : originals) {
      List<Sample> parents = openbis.getParents(s.getCode());
      if (parents.size() > 0) {
        return openbis.getSamplesofExperiment(parents.get(0).getExperimentIdentifierOrNull());
      }
    }
    return null;
  }

  /**
   * Gets all samples that are one level lower in the sample hierarchy of an attached experiment than a given list of samples
   * @param originals
   * @return
   */
  private List<Sample> getLowerSamples(List<Sample> originals) {
    for (Sample s : originals) {
      List<Sample> children = openbis.getChildrenSamples(s);
      if (children.size() > 0) {
        return openbis.getSamplesofExperiment(children.get(0).getExperimentIdentifierOrNull());
      }
    }
    return null;
  }

  /**
   * Creates a tab separated values file of the experiments created by the wizard, given that samples have been prepared in the aggregator class
   * @return
   * @throws FileNotFoundException
   * @throws UnsupportedEncodingException
   */
  public File createTSV() throws FileNotFoundException, UnsupportedEncodingException {
    List<AOpenbisSample> samples = new ArrayList<AOpenbisSample>();
    samples.addAll(entities);
    samples.addAll(extracts);
    samples.addAll(tests);
    List<String> rows = new ArrayList<String>();
    List<String> header =
        new ArrayList<String>(Arrays.asList("SAMPLE TYPE", "EXPERIMENT", "Q_SECONDARY_NAME",
            "PARENT", "Q_PRIMARY_TISSUE", "Q_TISSUE_DETAILED", "Q_ADDITIONAL_INFO",
            "Q_NCBI_ORGANISM", "Q_SAMPLE_TYPE", "Q_EXTERNALDB_ID", "XML_FACTORS"));

    String headerLine = "Identifier";
    for (String col : header) {
      headerLine += "\t" + col;
    }
    for (AOpenbisSample s : samples) {
      Map<String, String> data = s.getValueMap();
      String row = "/" + spaceCode + "/" + s.getCode();
      for (String col : header) {
        String val = "";
        if (data.containsKey(col))
          val = data.get(col);
        if (val == null)
          val = "";
        row += "\t" + val;
      }
      rows.add(row);
    }
    String fileName = ProjectwizardUI.tmpFolder + spaceCode + "_" + projectCode + ".tsv";
    PrintWriter writer = new PrintWriter(fileName, "UTF-8");
    writer.println(headerLine);
    for (String line : rows)
      writer.println(line);
    writer.close();
    this.tsv = new File(fileName);
    return tsv;
  }

  public File getTSV() {
    return tsv;
  }
}
