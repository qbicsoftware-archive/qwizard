package ui;

import java.util.List;


import model.AOpenbisSample;

import org.vaadin.teemu.wizards.WizardStep;


import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

public class NegativeSelectionStep implements WizardStep {

  boolean skip = false;

  VerticalLayout main;
  SummaryTable tab;

  public NegativeSelectionStep(String name, List<AOpenbisSample> samples) {
    this(name);
    setSamples(samples);
  }

  public NegativeSelectionStep(String name) {
    main = new VerticalLayout();
    main.setSpacing(true);
    main.setMargin(true);
    main.addComponent(new Label("Here you can delete " + name
        + " that are not part of the experiment."));
  }

  public void setSamples(List<AOpenbisSample> samples) {
    main.removeAllComponents();
    System.out.println(samples);
    tab = new SummaryTable("Samples", samples);
    main.addComponent(tab);
  }

  public List<AOpenbisSample> getSamples() {
    return tab.getSamples();
  }

  @Override
  public String getCaption() {
    return "Negative Selection";
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

  public void setSkipStep(boolean b) {
    skip = b;
  }

  public boolean isSkipped() {
    return skip;
  }
}
