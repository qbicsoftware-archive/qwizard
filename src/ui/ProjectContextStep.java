package ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import model.ExperimentBean;
import model.NewSampleModelBean;

import org.vaadin.teemu.wizards.WizardStep;

import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;

/**
 * Wizard Step to set the Context of the new experiment and sample creation
 * @author Andreas Friedrich
 *
 */
public class ProjectContextStep implements WizardStep {

  private VerticalLayout main;

  private ComboBox spaceCode;
  private ComboBox projectCode;

  List<ExperimentBean> experiments;

  private Table experimentTable;

  private Table samples;

  List<String> contextOptions = new ArrayList<String>(Arrays.asList(
      "Add a completely new experiment", "Add sample extraction to existing biological entities",
      "Measure existing extracted samples again", "Copy parts of a project"));
  private OptionGroup projectContext;

  private GridLayout grid;

  Button jump;
  boolean jumpAllowed = false;

  /**
   * Create a new Context Step for the wizard
   * @param openbisSpaces List of Spaces to select from in the openBIS instance
   */
  public ProjectContextStep(List<String> openbisSpaces) {
    main = new VerticalLayout();
    main.setMargin(true);
    main.setSpacing(true);
    main.setSizeUndefined();
    Collections.sort(openbisSpaces);
    spaceCode = new ComboBox("Space Name", openbisSpaces);
    spaceCode.setNullSelectionAllowed(false);
    spaceCode.setImmediate(true);

    projectCode = new ComboBox("Project Code");
    projectCode.setEnabled(false);
    projectCode.setImmediate(true);
    projectCode.setNullSelectionAllowed(false);

    projectContext = new OptionGroup("", contextOptions);
    disableContextOptions();

    experimentTable = new Table("Applicable Experiments");
    experimentTable.setPageLength(6);
    experimentTable.setContainerDataSource(new BeanItemContainer<ExperimentBean>(
        ExperimentBean.class));
    experimentTable.setSelectable(true);

    samples = new Table("Existing Samples");
    samples.setPageLength(10);
    samples.setContainerDataSource(new BeanItemContainer<NewSampleModelBean>(
        NewSampleModelBean.class));
//    samples.setSelectable(true);
//    samples.setMultiSelect(true);

    Label info =
        new Label(
            "If you want to add to or copy an existing experiment, please select the experiment. " +
            "When copying children samples, existing samples that are higher in the hierarchy will " +
            "become the new parents.");

    grid = new GridLayout(2, 4);
    grid.setSpacing(true);
    grid.addComponent(spaceCode, 0, 0);
    grid.addComponent(projectCode, 0, 1);
    grid.addComponent(info, 0, 2);
    grid.addComponent(experimentTable, 0, 3);
    grid.addComponent(projectContext, 1, 0, 1, 1);
    grid.addComponent(samples, 1, 2, 1, 3);

    main.addComponent(grid);

    jump = new Button("Jump to TSV upload");
    main.addComponent(jump);
  }

  public Button getButtons() {
    return jump;
  }

  public List<ExperimentBean> getExperiments() {
    return experiments;
  }

  public void setProjectCodes(List<String> projects) {
    projectCode.addItems(projects);
    projectCode.setEnabled(true);
  }

  public void disableContextOptions() {
    for (int i = 0; i < contextOptions.size(); i++)
      projectContext.setItemEnabled(contextOptions.get(i), false);
  }

  public void resetProjects() {
    projectCode.removeAllItems();
    projectCode.setEnabled(false);
    disableContextOptions();
    resetExperiments();
  }

  public void resetContext() {
    projectContext.select(projectContext.getNullSelectionItemId());
  }

  public void resetExperiments() {
    experimentTable.removeAllItems();
    resetContext();
    resetSamples();
  }

  public void resetSamples() {
    samples.removeAllItems();
  }

  public void setExperiments(List<ExperimentBean> beans) {
    experiments = beans;
  }

  public void showExperiments(List<ExperimentBean> beans) {
    BeanItemContainer<ExperimentBean> c =
        new BeanItemContainer<ExperimentBean>(ExperimentBean.class);
    c.addAll(beans);
    experimentTable.setContainerDataSource(c);
    if (c.size() == 1)
      experimentTable.select(c.getIdByIndex(0));
  }

  public void setSamples(List<NewSampleModelBean> beans) {
    BeanItemContainer<NewSampleModelBean> c =
        new BeanItemContainer<NewSampleModelBean>(NewSampleModelBean.class);
    c.addAll(beans);
    samples.setContainerDataSource(c);
  }

  public void enableNewContextOption(boolean enable) {
    projectContext.setItemEnabled(contextOptions.get(0), enable);
  }

  public void enableExtractContextOption(boolean enable) {
    projectContext.setItemEnabled(contextOptions.get(1), enable);
  }

  public void enableMeasureContextOption(boolean enable) {
    projectContext.setItemEnabled(contextOptions.get(2), enable);
  }
  
  public void enableCopyContextOption(boolean enable) {
    projectContext.setItemEnabled(contextOptions.get(3), enable);
    }  
  
  public List<String> getContextOptions() {
    return contextOptions;
  }

  public OptionGroup getProjectContext() {
    return projectContext;
  }

  @Override
  public String getCaption() {
    return "Project context";
  }

  @Override
  public Component getContent() {
    return main;
  }

  @Override
  public boolean onAdvance() {
    if (jumpAllowed)
      return true;
    if (spaceReady() && projectReady()) {
      if (inherit() || copy())
        if (expSelected())
          return true;
        else
          return false;
      else
        return true;
    } else
      return false;
  }

  private boolean expSelected() {
    return (getSamples().size() > 0);
  }

  private boolean inherit() {
    String context = (String) projectContext.getValue();
    return (contextOptions.get(1).equals(context) || contextOptions.get(2).equals(context));
  }

  private boolean copy() {
    String context = (String) projectContext.getValue();
    return contextOptions.get(3).equals(context);
  }

  private boolean projectReady() {
    return projectCode.getValue() != null && !projectCode.getValue().toString().isEmpty();
  }

  private boolean spaceReady() {
    return spaceCode.getValue() != null && !spaceCode.getValue().toString().isEmpty();
  }

  @Override
  public boolean onBack() {
    return true;
  }

  public String getProjectCode() {
    return (String) this.projectCode.getValue();
  }

  public String getSpaceCode() {
    return (String) this.spaceCode.getValue();
  }

  public ComboBox getProjectBox() {
    return projectCode;
  }

  public ComboBox getSpaceBox() {
    return spaceCode;
  }

  public Table getExperimentTable() {
    return experimentTable;
  }

  public ExperimentBean getExperimentName() {
    return (ExperimentBean) experimentTable.getValue();
  }

  @SuppressWarnings("unchecked")
  public List<NewSampleModelBean> getSamples() {
    List<NewSampleModelBean> res = new ArrayList<NewSampleModelBean>();
    samples.setSelectable(true);
    samples.setMultiSelect(true);
    samples.setValue(samples.getItemIds());
    res.addAll((Collection<? extends NewSampleModelBean>) samples.getValue());
    samples.setMultiSelect(false);
    samples.setSelectable(false);
    return res;
  }

  public void allowNext(boolean b) {
    jumpAllowed = b;
  }

  public boolean copyModeSet() {
    String context = (String) projectContext.getValue();
    return contextOptions.get(3).equals(context);
  }

}
