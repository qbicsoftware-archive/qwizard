package control;

import ui.UploadRegisterStep;

/**
 * Class implementing the Runnable interface so it can be run and trigger a response in the view after the sample creation thread finishes
 * @author Andreas Friedrich
 *
 */
public class RegisteredSamplesReadyRunnable implements Runnable {

  private UploadRegisterStep view;

  public RegisteredSamplesReadyRunnable(UploadRegisterStep view) {
    this.view = view;
  }

  @Override
  public void run() {
    view.registrationDone();
  }
}
