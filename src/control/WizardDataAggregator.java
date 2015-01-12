package control;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import main.OpenBisClient;
import main.ProjectwizardUI;
import model.AOpenbisSample;
import model.ExperimentBean;
import model.ExperimentType;
import model.NewSampleModelBean;
import model.OpenbisBiologicalEntity;
import model.OpenbisBiologicalSample;
import model.OpenbisExperiment;
import model.OpenbisTestSample;
import parser.Parser;
import properties.Factor;

import org.vaadin.teemu.wizards.WizardStep;

import com.vaadin.sass.internal.parser.function.TypeOfFunctionGenerator;

import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Experiment;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Sample;
import ui.EntityStep;
import ui.ExtractionStep;
import ui.FactorStep;
import ui.ProjectContextStep;
import ui.TestStep;

public class WizardDataAggregator {

  private ProjectContextStep s1;
  private EntityStep s2;
  private FactorStep s3;
  private ExtractionStep s5;
  private FactorStep s6;
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

  public WizardDataAggregator(List<WizardStep> steps, OpenBisClient openbis,
      Map<String, String> taxMap, Map<String, String> tissueMap) {
    s1 = (ProjectContextStep) steps.get(0);
    s2 = (EntityStep) steps.get(1);
    s3 = (FactorStep) steps.get(2);
    s5 = (ExtractionStep) steps.get(4);
    s6 = (FactorStep) steps.get(5);
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

  public List<AOpenbisSample> prepareEntities() throws JAXBException {
    prepareBasics();
    this.factorMap = new HashMap<String, Factor>();
    experiments = new ArrayList<OpenbisExperiment>();
    species = s2.getSpecies();
    bioReps = s2.getBioRepAmount();

    if (inheritEntities) {
      entities = parseEntities(openbis.getSamplesofExperiment(s1.getExperimentName().getCode()));
    } else {
      experiments.add(new OpenbisExperiment(buildExperimentName(),
          ExperimentType.Q_EXPERIMENTAL_DESIGN));

      List<List<Factor>> valueLists = s3.getFactors();
      System.out.println(s3.getFactors());
      bioFactors = createFactorInfo(valueLists);
      System.out.println(bioFactors);

      entities = buildEntities();
    }
    return entities;
  }

  public List<AOpenbisSample> prepareExtracts() throws JAXBException {
    tissue = s5.getTissue();
    extractReps = s5.getExtractRepAmount();

    if (inheritExtracts) {
      prepareBasics();
      this.factorMap = new HashMap<String, Factor>();
      experiments = new ArrayList<OpenbisExperiment>();
      extracts = parseExtracts(openbis.getSamplesofExperiment(s1.getExperimentName().getCode()));
    } else {
      experiments.add(new OpenbisExperiment(buildExperimentName(),
          ExperimentType.Q_SAMPLE_EXTRACTION));
      List<List<Factor>> valueLists = s6.getFactors();
      extractFactors = createFactorInfo(valueLists);

      classChars = new HashMap<String, Character>();
      extracts = buildExtracts(entities, classChars);
    }

    return extracts;
  }

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

  public void setEntities(List<AOpenbisSample> entities) {
    System.out.println(entities);
    this.entities = entities;
  }

  public void setExtracts(List<AOpenbisSample> extracts) {
    this.extracts = extracts;
  }

  public void setTests(List<AOpenbisSample> tests) {
    this.tests = tests;
  }

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

  private String buildExperimentName() {
    firstFreeExperimentID++;
    return projectCode + "E" + (firstFreeExperimentID - 1);
  }

  private void generatePermutations(List<List<String>> lists, List<String> result, int depth,
      String current) {
    String separator = "###";
    if (depth == lists.size()) {
      result.add(current);
      return;
    }
    for (int i = 0; i < lists.get(depth).size(); ++i) {
      if (current.equals(""))
        separator = "";
      generatePermutations(lists, result, depth + 1, current + separator + lists.get(depth).get(i));
    }
  }

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

  private List<AOpenbisSample> buildEntities() {
    List<AOpenbisSample> entities = new ArrayList<AOpenbisSample>();
    List<List<String>> factorLists = new ArrayList<List<String>>();
    factorLists.addAll(bioFactors);
    List<String> permutations = new ArrayList<String>();
    generatePermutations(factorLists, permutations, 0, "");
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
        System.out.println();
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
      List<String> permutations = new ArrayList<String>();
      generatePermutations(factorLists, permutations, 0, "");
      List<List<String>> permLists = new ArrayList<List<String>>();
      for (String concat : permutations) {
        permLists.add(new ArrayList<String>(Arrays.asList(concat.split("###"))));
      }
      for (List<String> secondaryNameList : permLists) {
        List<Factor> factors = new ArrayList<Factor>();
        for (String name : secondaryNameList)
          for (String element : name.split(";"))
            if (factorMap.containsKey(element))
              factors.add(factorMap.get(element));
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

  private String nameListToSecondaryName(List<String> secondaryNameList) {
    String res = secondaryNameList.toString().replace(", ", ";");
    return res.substring(1, res.length() - 1);
  }

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

  public void setInheritEntities(boolean inherit) {
    this.inheritEntities = inherit;
  }

  public void setInheritExtracts(boolean inherit) {
    this.inheritExtracts = inherit;
  }

  private List<String> getSampleCodes(List<NewSampleModelBean> beans) {
    List<String> res = new ArrayList<String>();
    for (NewSampleModelBean b : beans)
      res.add(b.getCode());
    return res;
  }

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
          p.get("Q_TISSUE_DETAILED"), parseParents(code)));// TODO
      // parents
      // if
      // needed
    }
    return res;
  }

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
          .get("Q_ADDITIONAL_INFO"), factors, p.get("Q_SAMPLE_TYPE"), parseParents(code)));// TODO
      // parents
      // if
      // needed
    }
    return res;
  }

  private String parseParents(String code) {
    String res = "";
    for (Sample s : openbis.getParents(code))
      res += " " + s.getCode();
    return res.trim();
  }

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
    System.out.println(entities);
    System.out.println(extracts);
    System.out.println(tests);
  }

  private List<AOpenbisSample> copySamples(List<AOpenbisSample> samples, Map<String, String> copies) {
    String newExp = buildExperimentName();
    String type = samples.get(0).getValueMap().get("SAMPLE TYPE");
    ExperimentType eType = ExperimentType.Q_EXPERIMENTAL_DESIGN;
    if (type.equals("Q_BIOLOGICAL_ENTITY"))
      eType = ExperimentType.Q_EXPERIMENTAL_DESIGN;
    else if (type.equals("Q_BIOLOGICAL_ENTITY"))
      eType = ExperimentType.Q_SAMPLE_EXTRACTION;
    else if (type.equals("Q_BIOLOGICAL_ENTITY"))
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

  private List<Sample> getUpperSamples(List<Sample> originals) {
    for (Sample s : originals) {
      List<Sample> parents = openbis.getParents(s.getCode());
      if (parents.size() > 0) {
        System.out.println(parents.get(0).getExperimentIdentifierOrNull());
        return openbis.getSamplesofExperiment(parents.get(0).getExperimentIdentifierOrNull());
      }
    }
    return null;
  }

  private List<Sample> getLowerSamples(List<Sample> originals) {
    for (Sample s : originals) {
      List<Sample> children = openbis.getChildrenSamples(s);
      if (children.size() > 0) {
        System.out.println(children.get(0).getExperimentIdentifierOrNull());
        return openbis.getSamplesofExperiment(children.get(0).getExperimentIdentifierOrNull());
      }
    }
    return null;
  }

}
