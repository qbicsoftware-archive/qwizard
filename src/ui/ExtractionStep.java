package ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import model.ConditionsPanel;
import model.OpenbisInfoComboBox;
import model.OpenbisInfoTextField;

import org.vaadin.teemu.wizards.WizardStep;

import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.VerticalLayout;

/**
 * Wizard Step to model the extraction of biological samples from entities
 * 
 * @author Andreas Friedrich
 * 
 */
public class ExtractionStep implements WizardStep {

  boolean skip = false;
  OptionGroup conditionsSet = new OptionGroup("dummy");

  VerticalLayout main;

  OpenbisInfoComboBox tissue;
  Map<String, String> tissueMap;

  GridLayout grid;
  ConditionsPanel c;

  String emptyFactor = "Other (please specify)";
  List<String> suggestions = new ArrayList<String>(Arrays.asList("Extraction time", "Tissue",
      "Growth Medium", "Radiation", "Treatment", emptyFactor));

  OpenbisInfoTextField tissueNum;

  OpenbisInfoTextField extractReps;

  public ConditionsPanel getCondPanel() {
    return c;
  }

  public OptionGroup conditionsSet() {
    return conditionsSet;
  }

  /**
   * Create a new Extraction step for the wizard
   * 
   * @param tissueMap A map of available tissues (codes and labels)
   */
  public ExtractionStep(Map<String, String> tissueMap) {
    main = new VerticalLayout();
    main.setMargin(true);
    main.setSpacing(true);
    main.addComponent(new Label("Are there different types of sample preparation?"));

    this.tissueMap = tissueMap;
    ArrayList<String> tissues = new ArrayList<String>();
    tissues.addAll(tissueMap.keySet());
    Collections.sort(tissues);
    tissue =
        new OpenbisInfoComboBox("Tissue",
            "If different tissues are a study condition, leave this empty", tissues);
    c =
        new ConditionsPanel(suggestions, emptyFactor, "Tissue",
            (ComboBox) tissue.getInnerComponent(), true, conditionsSet);
    main.addComponent(c);

    tissueNum =
        new OpenbisInfoTextField("How many different tissues are there in this sample extraction?",
            "", "25px", "2");
    tissueNum.getInnerComponent().setVisible(false);
    tissueNum.getInnerComponent().setEnabled(false);
    main.addComponent(tissueNum.getInnerComponent());
    main.addComponent(tissue.getInnerComponent());

    extractReps =
        new OpenbisInfoTextField("Extracted replicates per entity",
            "Number of extractions per individual defined in the last step."
                + "Technical replicates are added later!", "25px", "1");
    main.addComponent(extractReps.getInnerComponent());
  }

  @Override
  public String getCaption() {
    return "Sample Extraction";
  }

  @Override
  public Component getContent() {
    return main;
  }

  @Override
  public boolean onAdvance() {
    if (skip || tissueReady() && replicatesReady())
      return true;
    else
      return false;
  }

  private boolean replicatesReady() {
    return !extractReps.getValue().isEmpty();
  }

  private boolean tissueReady() {
    String t = tissue.getValue();
    return tissueIsFactor() || (t != null || !tissue.getValue().isEmpty());
  }

  @Override
  public boolean onBack() {
    return true;
  }

  public boolean tissueIsFactor() {
    return !tissue.getInnerComponent().isEnabled();
  }

  public void enableTissueField(boolean enable) {
    tissue.getInnerComponent().setEnabled(enable);
    tissueNum.getInnerComponent().setEnabled(!enable);
    tissueNum.getInnerComponent().setVisible(!enable);
    if (!enable)
      tissue.getInnerComponent().setValue(null);
  }

  public List<String> getFactors() {
    return c.getConditions();
  }

  public int getExtractRepAmount() {
    return Integer.parseInt(extractReps.getValue());
  }

  public String getTissue() {
    return tissue.getValue();
  }

  public boolean factorFieldOther(ComboBox source) {
    return emptyFactor.equals(source.getValue());
  }

  public int getTissueAmount() {
    return Integer.parseInt(tissueNum.getValue());
  }

  public void setSkipStep(boolean b) {
    skip = b;
  }

  public boolean isSkipped() {
    return skip;
  }
}
