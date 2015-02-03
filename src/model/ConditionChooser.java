package model;

import java.util.List;

import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

/**
 * Composite UI component to choose a single condition of an experiment
 * @author Andreas Friedrich
 *
 */
public class ConditionChooser extends VerticalLayout {

  private static final long serialVersionUID = 7196121933289471757L;
  private ComboBox chooser;
  private String other;
  private String special;
  private boolean isSpecial;
  private TextField freetext;

  /**
   * Creates a new condition chooser component
   * @param options List of different possible conditions
   * @param other Name of the "other" condition, which when selected will enable an input field for free text
   * @param special Name of a "special" condition like species for the entity input, which when selected will disable the normal species input
   * because there is more than one instance
   * @param nullSelectionAllowed true, if the conditions may be empty
   */
  public ConditionChooser(List<String> options, String other, String special,
      boolean nullSelectionAllowed) {
    isSpecial = false;
    this.other = other;
    this.special = special;
    chooser = new ComboBox("Condition", options);
    chooser.setImmediate(true);
    chooser.setNullSelectionAllowed(nullSelectionAllowed);
    addComponent(chooser);
  }

  public void addListener(ValueChangeListener l) {
    this.chooser.addValueChangeListener(l);
  }

  public boolean factorIsSpecial() {
    return isSpecial;
  }

  public void changed() {
    if (chooser.getValue() != null) {
      String val = chooser.getValue().toString();
      if (val.equals(other)) {
        freetext = new TextField();
        addComponent(freetext);
      } else {
        if (this.components.contains(freetext))
          removeComponent(freetext);
        isSpecial = val.equals(special);
      }
    } else {
      if (this.components.contains(freetext))
        removeComponent(freetext);
    }
  }

  public boolean chooserSet() {
    return chooser.getValue() != null;
  }

  public boolean isSet() {
    if (chooser.getValue() == null)
      return false;
    else
      return !chooser.getValue().toString().equals(other) || !freetext.getValue().isEmpty();
  }

  public String getCondition() {
    Object val = chooser.getValue();
    if (val == null)
      return null;
    else if (val.toString().equals(other))
      return freetext.getValue();
    else
      return val.toString();
  }

  public Object getBox() {
    return chooser;
  }
}
