package ui;

import java.util.List;

import model.OpenbisInfoComboBox;
import model.OpenbisInfoTextField;

import org.vaadin.teemu.wizards.WizardStep;

import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

/**
 * Wizard Step to put in information about the Sample Preparation that leads to a list of Test Samples
 * @author Andreas Friedrich
 *
 */
public class TestStep implements WizardStep {

  private VerticalLayout main;

  private OpenbisInfoTextField techReps;

  private OpenbisInfoComboBox sampleType;

  boolean skip = false;

  /**
   * Create a new Sample Preparation step for the wizard
   * @param sampleTypes Available list of sample types, e.g. Proteins, RNA etc.
   */
  public TestStep(List<String> sampleTypes) {
    main = new VerticalLayout();
    main.setMargin(true);
    main.setSpacing(true);
    main.setSizeUndefined();

    sampleType =
        new OpenbisInfoComboBox("Measurement Type", "What kind of sample type is measured?",
            sampleTypes);
    main.addComponent(sampleType.getInnerComponent());

    techReps =
        new OpenbisInfoTextField("Replicates", "Number of technical replicates for each factor.",
            "25px", "1");

    main.addComponent(new Label("How many biological replicates per factor are there?"));
    main.addComponent(techReps.getInnerComponent());
  }

  @Override
  public String getCaption() {
    return "Technical Replicates";
  }

  @Override
  public Component getContent() {
    return main;
  }

  @Override
  public boolean onAdvance() {
    if (replicatesReady())
      return true;
    else
      return false;
  }

  private boolean replicatesReady() {
    return !techReps.getValue().isEmpty();
  }

  @Override
  public boolean onBack() {
    return true;
  }

  public int getTechRepAmount() {
    return Integer.parseInt(techReps.getValue());
  }

  public String getSampleType() {
    return this.sampleType.getValue();
  }

  public void setSkipStep(boolean b) {
    skip = b;
  }

  public boolean isSkipped() {
    return skip;
  }
}
