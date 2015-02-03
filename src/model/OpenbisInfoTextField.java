package model;

import com.vaadin.ui.TextField;

/**
 * Composite UI component of a TextField containing rollover text and other information
 * @author Andreas Friedrich
 *
 */
public class OpenbisInfoTextField extends AOpenbisInfoComponent {

  private static final long serialVersionUID = -7892628867973563002L;

  public OpenbisInfoTextField(String label, String description) {
    super(description, new TextField(label));
  }

  public OpenbisInfoTextField(String label, String description, String width) {
    super(description, new TextField(label), width);
  }

  public OpenbisInfoTextField(String label, String description, String width, String value) {
    super(description, new TextField(label,value), width);
  }
}
