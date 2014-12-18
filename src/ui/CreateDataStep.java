package ui;

import org.vaadin.teemu.wizards.WizardStep;

import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

public class CreateDataStep implements WizardStep {

  private VerticalLayout main;
  private Button download;
  
  boolean skip = false;

  public CreateDataStep() {
    main = new VerticalLayout();
    download = new Button("Download TSV");
    main.addComponent(download);;
  }
  
  public Button getDownloadButton() {
    return this.download;
  }

  @Override
  public String getCaption() {
    return "Download TSV";
  }

  @Override
  public Component getContent() {
    return main;
  }

  @Override
  public boolean onAdvance() {
    return true;
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
