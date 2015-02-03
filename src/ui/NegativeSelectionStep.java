package ui;

import java.util.List;

import model.AOpenbisSample;

import org.vaadin.teemu.wizards.WizardStep;

import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * Wizard Step that shows a SummaryTable of the prepared samples and can be used to edit and delete samples 
 * @author Andreas Friedrich
 *
 */
public class NegativeSelectionStep implements WizardStep {

  boolean skip = false;

  private VerticalLayout main;
  private SummaryTable tab;
  private Label info;

  public NegativeSelectionStep(String name, List<AOpenbisSample> samples) {
    this(name);
    setSamples(samples);
  }

  /**
   * Create a new Experiment Tailoring step
   * @param name Title of this step
   */
  public NegativeSelectionStep(String name) {
    main = new VerticalLayout();
    main.setSpacing(true);
    main.setMargin(true);
    info =
        new Label("Here you can delete " + name + " that are not part of the" +
        		" experiment. You can change the secondary name to something" +
        		" more intuitive - conditions will be saved in an additinal column.");
    main.addComponent(info);
  }

  public void setSamples(List<AOpenbisSample> samples) {
    System.out.println(samples);
    main.removeAllComponents();
    main.addComponent(info);
    tab = new SummaryTable("Samples", samples);
    tab.setPageLength(samples.size());
    main.addComponent(tab);
  }

  public List<AOpenbisSample> getSamples() {
    return tab.getSamples();
  }

  @Override
  public String getCaption() {
    return "Experiment Tailoring";
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
