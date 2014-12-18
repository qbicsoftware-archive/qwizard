package control;

import ui.BarcodeView;

/**
 * Class implementing the Runnable interface so it can trigger a response in the view after the barcode creation thread finishes
 * @author Andreas Friedrich
 *
 */
public class BarcodesReadyRunnable implements Runnable {

  private BarcodeView view;

  public BarcodesReadyRunnable(BarcodeView view) {
    this.view = view;
  }

  @Override
  public void run() {
    view.creationDone();
  }
}
