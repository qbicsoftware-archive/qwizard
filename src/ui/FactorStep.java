package ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import model.ConditionPropertyPanel;
import properties.Factor;

import org.vaadin.teemu.wizards.WizardStep;

import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

public class FactorStep implements WizardStep {

  boolean skip = false;

  VerticalLayout main;
  List<ConditionPropertyPanel> factorInstances;
  List<ComboBox> optionInstances;
  Set<String> options;
  String optionName;
  String stepName;
  
  public FactorStep(Set<String> options, String optionName, String stepName) {
    main = new VerticalLayout();
    main.setSpacing(true);
    main.setMargin(true);
    main.addComponent(new Label("Please fill in which cases of each condition exist in your study."));
    
    this.options = options;
    this.optionName = optionName;
    this.stepName = stepName;
    factorInstances = new ArrayList<ConditionPropertyPanel>();
    optionInstances = new ArrayList<ComboBox>();
  }

  @Override
  public String getCaption() {
    return stepName;
  }

  public void initFactorFields(List<String> factors) {
    for (int i = 0; i < factors.size(); i++) {
      String f = factors.get(i);
      if (!f.equals(optionName)) {
        EnumSet<properties.Unit> units = EnumSet.noneOf(properties.Unit.class);
        units.addAll(Arrays.asList(properties.Unit.values()));
        ConditionPropertyPanel a =
            new ConditionPropertyPanel(f, units);
        factorInstances.add(a);
        main.addComponent(a);
      }
    }
  }

  public void initOptionsFactorField(int amount) {
    for (int i = 1; i <= amount; i++) {
      ComboBox b = new ComboBox(optionName + " " + i, options);
      optionInstances.add(b);
      main.addComponent(b);
    }
  }

  @Override
  public Component getContent() {
    return main;
  }

  @Override
  public boolean onAdvance() {
    return skip || true;
  }

  @Override
  public boolean onBack() {
    return true;
  }

  public List<List<Factor>> getFactors() {
    List<List<Factor>> res = new ArrayList<List<Factor>>();
    for (ConditionPropertyPanel a : this.factorInstances) {
      res.add(a.getFactors());
    }
    if (this.optionInstances.size() > 0) {
      List<Factor> species = new ArrayList<Factor>();
      for (ComboBox b : this.optionInstances) {
        species.add(new Factor(optionName.toLowerCase(), (String) b.getValue(), ""));
      }
      res.add(species);
    }
    return res;
  }

  public void resetFactorFields() {
    for (ConditionPropertyPanel a : factorInstances) {
      main.removeComponent(a);
    }
    factorInstances = new ArrayList<ConditionPropertyPanel>();
    for (ComboBox b : optionInstances) {
      main.removeComponent(b);
    }
    optionInstances = new ArrayList<ComboBox>();
  }

  public void setSkipStep(boolean b) {
    skip = b;
  }

  public boolean isSkipped() {
    return skip;
  }
}
