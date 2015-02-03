package ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import model.ConditionPropertyPanel;
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
 * Wizard Step to model the biological entities of an experiment
 * 
 * @author Andreas Friedrich
 * 
 */
public class EntityStep implements WizardStep {

  boolean skip = false;
  OptionGroup conditionsSet = new OptionGroup("dummy");

  VerticalLayout main;

  OpenbisInfoComboBox species;
  Map<String, String> speciesMap;

  GridLayout grid;
  ConditionsPanel c;

  String emptyFactor = "Other (please specify)";
  List<String> suggestions = new ArrayList<String>(Arrays.asList("Age", "Genotype", "Health State",
      "Phenotype", "Species", "Treatment", emptyFactor));

  OpenbisInfoTextField speciesNum;

  OpenbisInfoTextField bioReps;

  public ConditionsPanel getCondPanel() {
    return c;
  }

  public OptionGroup conditionsSet() {
    return conditionsSet;
  }

  /**
   * Create a new Entity step for the wizard
   * 
   * @param speciesMap A map of available species (codes and labels)
   */
  public EntityStep(Map<String, String> speciesMap) {
    main = new VerticalLayout();
    main.setMargin(true);
    main.setSpacing(true);
    main.addComponent(new Label(
        "Are there conditions distinguishing the groups before sample extraction?"));
    this.speciesMap = speciesMap;
    ArrayList<String> openbisSpecies = new ArrayList<String>();
    openbisSpecies.addAll(speciesMap.keySet());
    Collections.sort(openbisSpecies);
    species =
        new OpenbisInfoComboBox("Species",
            "If there are samples of different species, leave this empty", openbisSpecies);
    c =
        new ConditionsPanel(suggestions, emptyFactor, "Species",
            (ComboBox) species.getInnerComponent(), true, conditionsSet);
    main.addComponent(c);

    speciesNum =
        new OpenbisInfoTextField("How many different species are there in this project?", "",
            "25px", "2");
    speciesNum.getInnerComponent().setVisible(false);
    speciesNum.getInnerComponent().setEnabled(false);
    main.addComponent(speciesNum.getInnerComponent());
    main.addComponent(species.getInnerComponent());

    bioReps =
        new OpenbisInfoTextField(
            "How many biological replicates (e.g. animals) per condition are there?",
            "Number of (biological) replicates for each condition."
                + "Technical replicates are added later!", "25px", "1");
    main.addComponent(bioReps.getInnerComponent());
  }

  @Override
  public String getCaption() {
    return "Biological Entities";
  }

  @Override
  public Component getContent() {
    return main;
  }

  @Override
  public boolean onAdvance() {
    if (skip || speciesReady() && replicatesReady())
      return true;
    else
      return false;
  }

  private boolean replicatesReady() {
    return !bioReps.getValue().isEmpty();
  }

  private boolean speciesReady() {
    String s = species.getValue();
    return speciesIsFactor() || (s != null && !species.getValue().isEmpty());
  }

  @Override
  public boolean onBack() {
    return true;
  }

  public boolean speciesIsFactor() {
    return !species.getInnerComponent().isEnabled();
  }

  public void enableSpeciesField(boolean enable) {
    species.getInnerComponent().setEnabled(enable);
    speciesNum.getInnerComponent().setEnabled(!enable);
    speciesNum.getInnerComponent().setVisible(!enable);
    if (!enable)
      species.getInnerComponent().setValue(null);
  }

  public List<String> getFactors() {
    return c.getConditions();
  }

  public int getBioRepAmount() {
    return Integer.parseInt(bioReps.getValue());
  }

  public String getSpecies() {
    return species.getValue();
  }

  public boolean factorFieldOther(ComboBox source) {
    return emptyFactor.equals(source.getValue());
  }

  public int getSpeciesAmount() {
    return Integer.parseInt(speciesNum.getValue());
  }

  public void setSkipStep(boolean b) {
    skip = b;
  }

  public boolean isSkipped() {
    return skip;
  }

}
