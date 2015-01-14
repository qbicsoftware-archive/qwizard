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
 * 
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

  private void setUpLoadStep() {
    System.out.println("add tsv step");
    w.addStep(steps.get(8)); // tsv upload and registration
    System.out.println(w.getSteps().size() + " steps");
  }

  private void setInheritEntities() {
    System.out.println("add entity negative selection and extract creation");
    w.addStep(steps.get(3)); // entity negative selection
    w.addStep(steps.get(4)); // extract first step
    setInheritExtracts();
  }

  private void setInheritExtracts() {
    System.out.println("add extract neg selection and test samples");
    w.addStep(steps.get(6)); // extracts negative selection
    w.addStep(steps.get(7)); // test samples first step
    setUpLoadStep();
  }

  private void setCreateEntities() {
    System.out.println("add create entities");
    w.addStep(steps.get(1)); // entities first step
    setInheritEntities();
  }

  private void setEntityConditions() {
    System.out.println("add entity conditions");
    w.addStep(steps.get(2)); // entity conditions
    setInheritEntities();
  }

  private void setExtractConditions() {
    System.out.println("add extract conditions");
    w.addStep(steps.get(5)); // extract conditions
    setInheritExtracts();
  }

  private void resetNextSteps() {
    List<WizardStep> steps = w.getSteps();
    List<WizardStep> copy = new ArrayList<WizardStep>();
    copy.addAll(steps);
    boolean isNew = false;
    for (int i = 0; i < copy.size(); i++) {
      WizardStep cur = copy.get(i);
      if (isNew) {
        w.removeStep(cur);
      }
      if (w.isActive(cur))
        isNew = true;
    }
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
    final ProjectContextStep contextStep = new ProjectContextStep(spaces);
    final EntityStep entStep = new EntityStep(taxMap);
    final FactorStep entFactStep =
        new FactorStep(taxMap.keySet(), "Species", "Biological Conditions");
    final NegativeSelectionStep negStep1 = new NegativeSelectionStep("Biological Entities");
    final ExtractionStep extrStep = new ExtractionStep(tissueMap);
    final FactorStep extrFactStep =
        new FactorStep(tissueMap.keySet(), "Tissue", "Extraction Conditions");
    final NegativeSelectionStep negStep2 = new NegativeSelectionStep("Sample Extracts");
    final TestStep techStep = new TestStep(measureTypes);
    final UploadRegisterStep regStep = new UploadRegisterStep();
    steps =
        new ArrayList<WizardStep>(Arrays.asList(contextStep, entStep, entFactStep, negStep1,
            extrStep, extrFactStep, negStep2, techStep, regStep));

    this.dataAggregator = new WizardDataAggregator(steps, openbis, taxMap, tissueMap);
    w.addStep(contextStep);

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
          regStep.clearError();
          try {
            regStep.setRegEnabled(false);
            SamplePreparator prep = new SamplePreparator();
            prep.processTSV(file);
            regStep.setSummary(prep.getSummary());
            regStep.setProcessed(prep.getProcessed());
            regStep.setRegEnabled(true);
          } catch (FileNotFoundException e) {
            e.printStackTrace();
          }
        } else {
          regStep.setError(error);
          if (!file.delete())
            System.err.println("File was not deleted!");
        }
      }
    };
    upload.addFinishedListener(uploadFinListener);
    regStep.initUpload(upload);

    /**
     * Button listeners
     */
    Button.ClickListener cl = new Button.ClickListener() {
      @SuppressWarnings("unchecked")
      @Override
      public void buttonClick(ClickEvent event) {
        String src = event.getButton().getCaption();
        if (src.equals("Download existing project") && contextStep.getProjectCode() != null) {
          List<NewSampleModelBean> beans = new ArrayList<NewSampleModelBean>();
          for (Sample s : openbis.getSamplesOfProject(contextStep.getProjectCode())) {
            beans.add(new NewSampleModelBean(s.getCode(),
                s.getProperties().get("Q_SECONDARY_NAME"), s.getSampleTypeCode()));
          }
        }
        if (src.equals("Download TSV")) {
          FileResource resource = new FileResource(dataAggregator.getTSV());
          Page.getCurrent().open(resource, "Download", true);
        }

        if (src.equals("Jump to TSV upload")) {
          contextStep.getProjectContext().setValue(null);
          resetNextSteps();
          setUpLoadStep();
          contextStep.allowNext(true);
          w.next();
        }

        if (src.equals("Register All")) {
          List<List<ISampleBean>> hierarchy = new ArrayList<List<ISampleBean>>();
          for (List<List<ISampleBean>> midList : regStep.getSamples()) {
            List<ISampleBean> collect = new ArrayList<ISampleBean>();
            for (List<ISampleBean> inner : midList) {
              createExperiment(inner.get(0));
              collect.addAll(inner);
              for (ISampleBean b : inner) {
                fixXMLProps((Map<String, String>) b.getMetadata());
              }
            }
            hierarchy.add(collect);
          }
          openbisCreator.registerSampleBatchLevelWiseWithProgress(hierarchy,
              regStep.getProgressBar(), regStep.getProgressLabel(),
              new RegisteredSamplesReadyRunnable(regStep));
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
    contextStep.getButtons().addClickListener(cl);
    regStep.getDownloadButton().addClickListener(cl);
    regStep.getRegisterButton().addClickListener(cl);

    /**
     * Space selection listener
     */
    ValueChangeListener spaceSelectListener = new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        contextStep.resetProjects();
        String space = contextStep.getSpaceCode();
        if (space != null) {
          List<String> projects = new ArrayList<String>();
          for (Project p : openbis.getProjectsOfSpace(space)) {
            projects.add(p.getCode());
          }
          contextStep.setProjectCodes(projects);
        }
      }

    };
    contextStep.getSpaceBox().addValueChangeListener(spaceSelectListener);

    /**
     * Project selection listener
     */

    ValueChangeListener projectSelectListener = new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        contextStep.resetExperiments();
        String space = contextStep.getSpaceCode();
        String project = contextStep.getProjectCode();
        if (project != null) {
          contextStep.enableNewContextOption(true);
          contextStep.enableExtractContextOption(projectHasBioEntities(space, project));
          contextStep.enableMeasureContextOption(projectHasExtracts(space, project));
          contextStep.enableCopyContextOption(projectHasBioEntities(space, project)
              || projectHasExtracts(space, project));
          List<ExperimentBean> beans = new ArrayList<ExperimentBean>();
          for (Experiment e : openbis.getExperimentsOfProjectByCode(project)) {
            int numOfSamples = openbis.getSamplesofExperiment(e.getCode()).size();
            beans.add(new ExperimentBean(e.getCode(), e.getExperimentTypeCode(), Integer
                .toString(numOfSamples)));
          }
          contextStep.setExperiments(beans);
        }
      }

    };
    contextStep.getProjectBox().addValueChangeListener(projectSelectListener);

    /**
     * Experiment selection listener
     */

    ValueChangeListener expSelectListener = new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        contextStep.resetSamples();
        ExperimentBean exp = contextStep.getExperimentName();
        if (exp != null) {
          String code = exp.getCode();
          List<NewSampleModelBean> beans = new ArrayList<NewSampleModelBean>();
          for (Sample s : openbis.getSamplesofExperiment(code)) {
            beans.add(new NewSampleModelBean(s.getCode(),
                s.getProperties().get("Q_SECONDARY_NAME"), s.getSampleTypeCode()));
          }
          contextStep.setSamples(beans);
        }
      }

    };
    contextStep.getExperimentTable().addValueChangeListener(expSelectListener);

    /**
     * Project context (radio buttons) listener
     */

    ValueChangeListener projectContextListener = new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        if (contextStep.getProjectContext().getValue() != null) {
          resetNextSteps();
          OptionGroup projectContext = contextStep.getProjectContext();
          List<String> contextOptions = contextStep.getContextOptions();
          List<ExperimentBean> experiments = contextStep.getExperiments();
          String context = (String) projectContext.getValue();
          List<ExperimentBean> beans = new ArrayList<ExperimentBean>();
          // inherit from bio entities
          if (contextOptions.get(1).equals(context)) {
            for (ExperimentBean b : experiments) {
              if (b.getExperiment_type().equals(ExperimentType.Q_EXPERIMENTAL_DESIGN.toString()))
                beans.add(b);
            }
            setInheritEntities();
            dataAggregator.setInheritEntities(true);
            dataAggregator.setInheritExtracts(false);
          }
          // inherit from sample extraction
          if (contextOptions.get(2).equals(context)) {
            for (ExperimentBean b : experiments) {
              if (b.getExperiment_type().equals(ExperimentType.Q_SAMPLE_EXTRACTION.toString()))
                beans.add(b);
            }
            setInheritExtracts();
            dataAggregator.setInheritEntities(false);
            dataAggregator.setInheritExtracts(true);
          }
          // new experiments
          if (contextOptions.get(0).equals(context)) {
            setCreateEntities();
            dataAggregator.setInheritEntities(false);
            dataAggregator.setInheritExtracts(false);
          }
          // copy experiments
          if (contextOptions.get(3).equals(context)) {
            beans.addAll(experiments);
            setUpLoadStep();
          }
          contextStep.showExperiments(beans);
        }
      }
    };
    contextStep.getProjectContext().addValueChangeListener(projectContextListener);

    ValueChangeListener entityConditionSetListener = new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        if (entStep.conditionsSet().getValue() != null) {
          resetNextSteps();
          setEntityConditions();
        } else {
          setInheritEntities();
          resetNextSteps();
        }
      }
    };
    entStep.conditionsSet().addValueChangeListener(entityConditionSetListener);

    ValueChangeListener extractConditionSetListener = new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        if (extrStep.conditionsSet().getValue() != null) {
          resetNextSteps();
          setExtractConditions();
        } else {
          setInheritExtracts();
          resetNextSteps();
        }
      }
    };
    extrStep.conditionsSet().addValueChangeListener(extractConditionSetListener);

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
        if (event.getActivatedStep().equals(contextStep)) {
          System.out.println("project context step");
          contextStep.allowNext(false);
        }
        // Entity Setup Step
        if (event.getActivatedStep().equals(entStep)) {
          System.out.println("entity setup step");
          entFactStep.resetFactorFields();
          bioFactorInstancesSet = false;
          // }
        }
        // Entity Factor Instances Step
        if (event.getActivatedStep().equals(entFactStep)) {
          System.out.println("entity factor instance step");
          if (!bioFactorInstancesSet) {
            if (entStep.speciesIsFactor())
              entFactStep.initOptionsFactorField(entStep.getSpeciesAmount());
            entFactStep.initFactorFields(entStep.getFactors());
            bioFactorInstancesSet = true;
          }
        }
        // }
        // Negative Selection of Entities
        if (event.getActivatedStep().equals(negStep1)) {
          System.out.println("entity negative selection step");
          try {
            negStep1.setSamples(dataAggregator.prepareEntities());
          } catch (JAXBException e) {
            e.printStackTrace();
          }
        }
        // }
        // Extract Setup Step
        if (event.getActivatedStep().equals(extrStep)) {
          dataAggregator.setEntities(negStep1.getSamples());
          extrFactStep.resetFactorFields();
          extractFactorInstancesSet = false;
        }
        // }
        // Extract Factor Instances Step
        if (event.getActivatedStep().equals(extrFactStep)) {
          if (!extractFactorInstancesSet) {
            if (extrStep.tissueIsFactor())
              extrFactStep.initOptionsFactorField(extrStep.getTissueAmount());
            extrFactStep.initFactorFields(extrStep.getFactors());
            extractFactorInstancesSet = true;
          }
        }
        // }
        // Negative Selection of Extracts
        if (event.getActivatedStep().equals(negStep2)) {
          try {
            negStep2.setSamples(dataAggregator.prepareExtracts());
          } catch (JAXBException e) {
            e.printStackTrace();
          }
        }
        // Test Setup Step
        if (event.getActivatedStep().equals(techStep)) {
          dataAggregator.setExtracts(negStep2.getSamples());
        }
        // TSV and Registration Step
        if (event.getActivatedStep().equals(regStep)) {
          if (contextStep.copyModeSet()) {
            try {
              dataAggregator.copyExperiment();
            } catch (JAXBException e1) {
              e1.printStackTrace();
            }
            try {
              dataAggregator.createTSV();
            } catch (FileNotFoundException | UnsupportedEncodingException e) {
              e.printStackTrace();
            }
            SamplePreparator prep = new SamplePreparator();
            try {
              prep.processTSV(dataAggregator.getTSV());
            } catch (FileNotFoundException e) {
              e.printStackTrace();
            }
            regStep.setSummary(prep.getSummary());
            regStep.setProcessed(prep.getProcessed());
          }
          // Test samples were filled out
          if (w.getSteps().contains(steps.get(7))) {
            dataAggregator.prepareTestSamples();
            try {
              dataAggregator.createTSV();
            } catch (FileNotFoundException | UnsupportedEncodingException e) {
              e.printStackTrace();
            }
            SamplePreparator prep = new SamplePreparator();
            try {
              prep.processTSV(dataAggregator.getTSV());
            } catch (FileNotFoundException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
            regStep.setSummary(prep.getSummary());
            regStep.setProcessed(prep.getProcessed());
          }
          regStep.setRegEnabled(true);
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
    if (s.getType().equals("Q_BIOLOGICAL_ENTITY"))
      expType = ExperimentType.Q_EXPERIMENTAL_DESIGN.toString();
    else if (s.getType().equals("Q_BIOLOGICAL_SAMPLE"))
      expType = ExperimentType.Q_SAMPLE_EXTRACTION.toString();
    else if (s.getType().equals("Q_TEST_SAMPLE"))
      expType = ExperimentType.Q_SAMPLE_PREPARATION.toString();
    if (!openbis.expExists(space, proj, expCode))
      openbisCreator.registerExperiment(space, proj, expType, expCode);
  }
}
