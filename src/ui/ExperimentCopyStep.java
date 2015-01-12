package ui;

import java.util.List;

import model.ExperimentBean;
import org.vaadin.teemu.wizards.WizardStep;

import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;

public class ExperimentCopyStep implements WizardStep {

  private VerticalLayout main;

  List<ExperimentBean> experiments;

  private Table experimentTable;

  public ExperimentCopyStep() {
    main = new VerticalLayout();
    main.setMargin(true);
    main.setSpacing(true);
    main.setSizeUndefined();

    experimentTable = new Table("Applicable Experiments");
    experimentTable.setPageLength(6);
    experimentTable.setContainerDataSource(new BeanItemContainer<ExperimentBean>(
        ExperimentBean.class));
    experimentTable.setSelectable(true);

    Label info =
        new Label(
            "Select the experiment(s) whose samples you want to copy. " +
            "Uncopied samples that are higher in the hierarchy will become parents of copied samples.");

    main.addComponent(info);
    main.addComponent(experimentTable);

  }

  public List<ExperimentBean> getExperiments() {
    return experiments;
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

  @Override
  public String getCaption() {
    return "Copy experiments";
  }

  @Override
  public Component getContent() {
    return main;
  }

  @Override
  public boolean onAdvance() {
    if (expSelected())
      return true;
    else
      return false;
  }

  private boolean expSelected() {
    return experimentTable.getValue() != null;
  }

  @Override
  public boolean onBack() {
    return true;
  }

  public Table getExperimentTable() {
    return experimentTable;
  }

  public ExperimentBean getExperimentName() {
    return (ExperimentBean) experimentTable.getValue();
  }

}
