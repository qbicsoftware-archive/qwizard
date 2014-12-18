package control;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import main.OpenBisClient;
import main.OpenbisCreationController;
import main.SamplePreparator;
import model.ExperimentBean;
import model.ExperimentType;
import model.ISampleBean;
import model.NewSampleModelBean;

import org.vaadin.teemu.wizards.Wizard;
import org.vaadin.teemu.wizards.WizardStep;
import org.vaadin.teemu.wizards.event.WizardCancelledEvent;
import org.vaadin.teemu.wizards.event.WizardCompletedEvent;
import org.vaadin.teemu.wizards.event.WizardProgressListener;
import org.vaadin.teemu.wizards.event.WizardStepActivationEvent;
import org.vaadin.teemu.wizards.event.WizardStepSetChangedEvent;

import parser.Parser;

import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Experiment;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Project;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Sample;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.FileResource;
import com.vaadin.server.Page;
import com.vaadin.ui.Button;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Upload.FinishedEvent;
import com.vaadin.ui.Upload.FinishedListener;

import ui.CreateDataStep;
import ui.EntityStep;
import ui.ExtractionStep;
import ui.FactorStep;
import ui.NegativeSelectionStep;
import ui.ProjectContextStep;
import ui.TestStep;
import ui.UploadRegisterStep;
import properties.Factor;

/**
 * Controller for the sample/experiment creation wizard
 * @author Andreas Friedrich
 *
 */
public class WizardController {

  OpenBisClient openbis;
  OpenbisCreationController openbisCreator;
  Wizard w;
  List<WizardStep> steps;
  int lastStep = 1;
  WizardDataAggregator dataAggregator;
  boolean bioFactorInstancesSet = false;
  boolean extractFactorInstancesSet = false;
  Map<String, String> taxMap;
  Map<String, String> tissueMap;
  List<String> measureTypes;
  List<String> spaces;

  /**
   * 
   * @param openbis OpenBisClient API
   * @param taxMap Map containing the NCBI taxonomy (labels and ids) taken from openBIS
   * @param tissueMap Map containing the tissue 
   * @param sampleTypes List containing the different sample (technology) types
   * @param spaces List of space names existing in openBIS
   */
  public WizardController(OpenBisClient openbis, Map<String, String> taxMap,
      Map<String, String> tissueMap, List<String> sampleTypes, List<String> spaces) {
    this.openbis = openbis;
    this.openbisCreator = new OpenbisCreationController(openbis);
    this.taxMap = taxMap;
    this.tissueMap = tissueMap;
    this.measureTypes = sampleTypes;
    this.spaces = spaces;
  }

  private void skipToUpload() {
    w.addStep(steps.get(9)); // tsv upload and registration
  }

  private void setInheritEntities() {
    w.addStep(steps.get(3)); // entity negative selection
    w.addStep(steps.get(4)); // extract first step
    setInheritExtracts();
  }

  private void setInheritExtracts() {
    w.addStep(steps.get(6)); // extracts negative selection
    w.addStep(steps.get(7)); // test samples first step
    setLastSteps();
  }

  private void setCreateEntities() {
    w.addStep(steps.get(1)); // entities first step
    setInheritEntities();
  }

  private void setEntityConditions() {
    w.addStep(steps.get(2)); // entity conditions
    setInheritEntities();
  }

  private void setExtractConditions() {
    w.addStep(steps.get(5)); // extract conditions
    setInheritExtracts();
  }

  private void setLastSteps() {
    w.addStep(steps.get(8));
    w.addStep(steps.get(9));
  }

  private void resetNextSteps() {
    for (int i = lastStep; i < steps.size(); i++)
      w.removeStep(steps.get(i));
  }

  public boolean projectHasBioEntities(String spaceCode, String code) {
    if (!openbis.projectExists(spaceCode, code))
      return false;
    for (Experiment e : openbis.getExperimentsOfProjectByCode(code)) {
      if (e.getExperimentTypeCode().equals("Q_EXPERIMENTAL_DESIGN"))
        return openbis.getSamplesofExperiment(e.getIdentifier()).size() > 0;
    }
    return false;
  }

  public boolean projectHasExtracts(String spaceCode, String code) {
    if (!openbis.projectExists(spaceCode, code))
      return false;
    for (Experiment e : openbis.getExperimentsOfProjectByCode(code)) {
      System.out.println(e.getCode());
      System.out.println(e.getExperimentTypeCode());
      if (e.getExperimentTypeCode().equals("Q_SAMPLE_EXTRACTION"))
        if (openbis.getSamplesofExperiment(e.getIdentifier()).size() > 0)
          return true;
    }
    return false;
  }

  public Wizard getWizard() {
    return w;
  }

  public void init() {
    this.w = new Wizard();
    final ProjectContextStep s1 = new ProjectContextStep(spaces);
    final EntityStep s2 = new EntityStep(taxMap);
    final FactorStep s3 = new FactorStep(taxMap.keySet(), "Species", "Biological Conditions");
    final NegativeSelectionStep s4 = new NegativeSelectionStep("Biological Entities");
    final ExtractionStep s5 = new ExtractionStep(tissueMap);
    final FactorStep s6 = new FactorStep(tissueMap.keySet(), "Tissue", "Extraction Conditions");
    final NegativeSelectionStep s7 = new NegativeSelectionStep("Sample Extracts");
    final TestStep s8 = new TestStep(measureTypes);
    final CreateDataStep s9 = new CreateDataStep();
    final UploadRegisterStep s10 = new UploadRegisterStep();
    steps = new ArrayList<WizardStep>(Arrays.asList(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10));

    this.dataAggregator = new WizardDataAggregator(steps, openbis, taxMap, tissueMap);
    w.addStep(s1);
    // for (WizardStep s : steps) {
    // w.addStep(s);
    // }

    final Uploader tsvController = new Uploader();
    Upload upload = new Upload("Upload a tsv here", tsvController);
    // Use a custom button caption instead of plain "Upload".
    upload.setButtonCaption("Upload");
    // Listen for events regarding the success of upload.
    upload.addFailedListener(tsvController);
    upload.addSucceededListener(tsvController);
    FinishedListener uploadFinListener = new FinishedListener() {
      public void uploadFinished(FinishedEvent event) {
        String error = tsvController.getError();
        File file = tsvController.getFile();
        if (error == null || error.isEmpty()) {
          s10.clearError();
          try {
            s10.setRegEnabled(false);
            SamplePreparator prep = new SamplePreparator();
            prep.processTSV(file);
            s10.setSummary(prep.getSummary());
            s10.setProcessed(prep.getProcessed());
            s10.setRegEnabled(true);
          } catch (FileNotFoundException e) {
            e.printStackTrace();
          }
        } else {
          s10.setError(error);
          if (!file.delete())
            System.err.println("File was not deleted!");
        }
      }
    };
    upload.addFinishedListener(uploadFinListener);
    s10.initUpload(upload);

    /**
     * Button listeners
     */
    Button.ClickListener cl = new Button.ClickListener() {
      @SuppressWarnings("unchecked")
      @Override
      public void buttonClick(ClickEvent event) {
        String src = event.getButton().getCaption();
        if (src.equals("Download existing project") && s1.getProjectCode() != null) {
          List<NewSampleModelBean> beans = new ArrayList<NewSampleModelBean>();
          for (Sample s : openbis.getSamplesOfProject(s1.getProjectCode())) {
            beans.add(new NewSampleModelBean(s.getCode(),
                s.getProperties().get("Q_SECONDARY_NAME"), s.getSampleTypeCode()));
          }
        }
        if (src.equals("Download TSV")) {
          FileResource resource = null;
          try {
            File tsv = dataAggregator.createTSV();
            resource = new FileResource(tsv);
          } catch (FileNotFoundException e) {
            e.printStackTrace();
          } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
          }
          Page.getCurrent().open(resource, "Download", true);
        }

        if (src.equals("Jump to TSV upload")) {
          resetNextSteps();
          skipToUpload();
          s1.allowNext(true);
          w.next();
        }

        if (src.equals("Register All")) {
          List<List<ISampleBean>> hierarchy = new ArrayList<List<ISampleBean>>();
          for (List<List<ISampleBean>> midList : s10.getSamples()) {
            List<ISampleBean> collect = new ArrayList<ISampleBean>();
            for (List<ISampleBean> inner : midList) {
              createExperiment(inner.get(0));
              collect.addAll(inner);
              for (ISampleBean b : inner)
                fixXMLProps((Map<String, String>) b.getMetadata());
            }
            hierarchy.add(collect);
          }
          openbisCreator.registerSampleBatchLevelWiseWithProgress(hierarchy, s10.getProgressBar(),
              s10.getProgressLabel(), new RegisteredSamplesReadyRunnable(s10));
        }
      }

      private void fixXMLProps(Map<String, String> metadata) {
        Parser p = new Parser();
        List<Factor> factors = new ArrayList<Factor>();
        if (metadata.get("XML_FACTORS") != null) {
          String[] fStrings = metadata.get("XML_FACTORS").split(";");
          for (String f : fStrings) {
            String[] fields = f.split(":");
            String lab = fields[0].replace(" ", "");
            System.out.println(lab);
            String val = fields[1].replace(" ", "");
            if (fields.length > 2)
              factors.add(new Factor(lab, val, fields[2].replace(" ", "")));
            else
              factors.add(new Factor(lab, val));
          }
          try {
            metadata.put("Q_PROPERTIES", p.toString(p.createXMLFromFactors(factors)));
          } catch (JAXBException e) {
            e.printStackTrace();
          }
        }
        metadata.remove("XML_FACTORS");
      }
    };
    s1.getButtons().addClickListener(cl);
    s9.getDownloadButton().addClickListener(cl);
    s10.getRegisterButton().addClickListener(cl);

    /**
     * Space selection listener
     */
    ValueChangeListener spaceSelectListener = new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        s1.resetProjects();
        String space = s1.getSpaceCode();
        if (space != null) {
          List<String> projects = new ArrayList<String>();
          for (Project p : openbis.getProjectsOfSpace(space)) {
            projects.add(p.getCode());
          }
          s1.setProjectCodes(projects);
        }
      }

    };
    s1.getSpaceBox().addValueChangeListener(spaceSelectListener);

    /**
     * Project selection listener
     */

    ValueChangeListener projectSelectListener = new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        s1.resetExperiments();
        String space = s1.getSpaceCode();
        String project = s1.getProjectCode();
        if (project != null) {
          s1.enableNewContextOption(true);
          s1.enableExtractContextOption(projectHasBioEntities(space, project));
          s1.enableMeasureContextOption(projectHasExtracts(space, project));
          List<ExperimentBean> beans = new ArrayList<ExperimentBean>();
          for (Experiment e : openbis.getExperimentsOfProjectByCode(project)) {
            int numOfSamples = openbis.getSamplesofExperiment(e.getCode()).size();
            beans.add(new ExperimentBean(e.getCode(), e.getExperimentTypeCode(), Integer
                .toString(numOfSamples)));
          }
          s1.setExperiments(beans);
        }
      }

    };
    s1.getProjectBox().addValueChangeListener(projectSelectListener);

    /**
     * Experiment selection listener
     */

    ValueChangeListener expSelectListener = new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        s1.resetSamples();
        ExperimentBean exp = s1.getExperimentName();
        if (exp != null) {
          String code = exp.getCode();
          List<NewSampleModelBean> beans = new ArrayList<NewSampleModelBean>();
          for (Sample s : openbis.getSamplesofExperiment(code)) {
            beans.add(new NewSampleModelBean(s.getCode(),
                s.getProperties().get("Q_SECONDARY_NAME"), s.getSampleTypeCode()));
          }
          s1.setSamples(beans);
        }
      }

    };
    s1.getExperimentTable().addValueChangeListener(expSelectListener);

    /**
     * Project context (radio buttons) listener
     */

    ValueChangeListener projectContextListener = new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        if (s1.getProjectContext().getValue() != null) {
          resetNextSteps();
          OptionGroup projectContext = s1.getProjectContext();
          List<String> contextOptions = s1.getContextOptions();
          List<ExperimentBean> experiments = s1.getExperiments();
          String context = (String) projectContext.getValue();
          List<ExperimentBean> beans = new ArrayList<ExperimentBean>();
          if (contextOptions.get(1).equals(context)) {
            for (ExperimentBean b : experiments) {
              if (b.getExperiment_type().equals(ExperimentType.Q_EXPERIMENTAL_DESIGN.toString()))
                beans.add(b);
            }
            setInheritEntities();
            dataAggregator.setInheritEntities(true);
            dataAggregator.setInheritExtracts(false);
          }
          if (contextOptions.get(2).equals(context)) {
            for (ExperimentBean b : experiments) {
              if (b.getExperiment_type().equals(ExperimentType.Q_SAMPLE_EXTRACTION.toString()))
                beans.add(b);
            }
            setInheritExtracts();
            dataAggregator.setInheritEntities(false);
            dataAggregator.setInheritExtracts(true);
          }
          if (contextOptions.get(0).equals(context)) {
            setCreateEntities();
            dataAggregator.setInheritEntities(false);
            dataAggregator.setInheritExtracts(false);
          }
          s1.showExperiments(beans);
        }
      }
    };
    s1.getProjectContext().addValueChangeListener(projectContextListener);

    ValueChangeListener entityConditionSetListener = new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        if (s2.conditionsSet().getValue() != null) {
          resetNextSteps();
          setEntityConditions();
        } else {
          setInheritEntities();
          resetNextSteps();
        }
      }
    };
    s2.conditionsSet().addValueChangeListener(entityConditionSetListener);

    ValueChangeListener extractConditionSetListener = new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        if (s5.conditionsSet().getValue() != null) {
          resetNextSteps();
          setExtractConditions();
        } else {
          setInheritExtracts();
          resetNextSteps();
        }
      }
    };
    s5.conditionsSet().addValueChangeListener(extractConditionSetListener);

    WizardProgressListener wl = new WizardProgressListener() {
      @Override
      public void wizardCompleted(WizardCompletedEvent event) {}

      @Override
      public void wizardCancelled(WizardCancelledEvent event) {}

      @Override
      public void stepSetChanged(WizardStepSetChangedEvent event) {

      }

      @Override
      public void activeStepChanged(WizardStepActivationEvent event) {
        if (event.getActivatedStep().equals(s1)) {
          lastStep = 1;
          System.out.println("project context step");
          s1.allowNext(false);
        }
        // Entity Setup Step
        if (event.getActivatedStep().equals(s2)) {
          // int last = lastStep;
          lastStep = 2;
          System.out.println("entity setup step");
          s3.resetFactorFields();
          bioFactorInstancesSet = false;
          // }
        }
        // Entity Factor Instances Step
        if (event.getActivatedStep().equals(s3)) {
          System.out.println("entity factor instance step");
          int last = lastStep;
          lastStep = 3;
          if (!bioFactorInstancesSet) {
            if (s2.speciesIsFactor())
              s3.initOptionsFactorField(s2.getSpeciesAmount());
            s3.initFactorFields(s2.getFactors());
            bioFactorInstancesSet = true;
          }
        }
        // }
        // Negative Selection of Entities
        if (event.getActivatedStep().equals(s4)) {
          System.out.println("entity negative selection step");
          int last = lastStep;
          lastStep = 4;
          try {
            s4.setSamples(dataAggregator.prepareEntities());
          } catch (JAXBException e) {
            e.printStackTrace();
          }
        }
        // }
        // Extract Setup Step
        if (event.getActivatedStep().equals(s5)) {
          System.out.println("extract setup step");
          int last = lastStep;
          lastStep = 5;
          dataAggregator.setEntities(s4.getSamples());
          s6.resetFactorFields();
          extractFactorInstancesSet = false;
        }
        // }
        // Extract Factor Instances Step
        if (event.getActivatedStep().equals(s6)) {
          System.out.println("extract factor instance step");
          int last = lastStep;
          lastStep = 6;
          if (!extractFactorInstancesSet) {
            if (s5.tissueIsFactor())
              s6.initOptionsFactorField(s5.getTissueAmount());
            s6.initFactorFields(s5.getFactors());
            extractFactorInstancesSet = true;
          }
        }
        // }
        // Negative Selection of Extracts
        if (event.getActivatedStep().equals(s7)) {
          System.out.println("extract negative selection step");
          int last = lastStep;
          lastStep = 7;
          if (s7.isSkipped()) {
            if (last > 7)
              w.back();
            else
              w.next();
          } else {
            try {
              s7.setSamples(dataAggregator.prepareExtracts());
            } catch (JAXBException e) {
              e.printStackTrace();
            }
          }
        }
        // Test Setup Step
        if (event.getActivatedStep().equals(s8)) {
          System.out.println("test samples step");
          int last = lastStep;
          lastStep = 8;
          dataAggregator.setExtracts(s7.getSamples());
          if (s8.isSkipped()) {
            if (last > 8)
              w.back();
            else
              w.next();
          } else {
          }
        }
        // TSV Download Step
        if (event.getActivatedStep().equals(s9)) {
          System.out.println("tsv step");
          int last = lastStep;
          lastStep = 9;
          if (s9.isSkipped()) {
            if (last > 9)
              w.back();
            else
              w.next();
          } else {
            dataAggregator.prepareTestSamples();
          }
        }
        if (event.getActivatedStep().equals(s10)) {
          System.out.println("register step");
          int last = lastStep;
          lastStep = 10;
        }
      }
    };
    w.addListener(wl);
  }

  protected void createExperiment(ISampleBean s) {
    String space = s.getSpace();
    String proj = s.getProject();
    String expCode = s.getExperiment();
    String expType = "";
    System.out.println(space + proj + expCode);
    if (s.getType().equals("Q_BIOLOGICAL_ENTITY"))
      expType = ExperimentType.Q_EXPERIMENTAL_DESIGN.toString();
    else if (s.getType().equals("Q_BIOLOGICAL_SAMPLE"))
      expType = ExperimentType.Q_SAMPLE_EXTRACTION.toString();
    else if (s.getType().equals("Q_TEST_SAMPLE"))
      expType = ExperimentType.Q_SAMPLE_PREPARATION.toString();
    System.out.println(expType);
    if (!openbis.expExists(space, proj, expCode))
      openbisCreator.registerExperiment(space, proj, expType, expCode);
  }
}
