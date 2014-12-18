package model;

import java.util.List;

import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

public class ConditionChooser extends VerticalLayout {

  private ComboBox chooser;
  private String other;
  private String special;
  private boolean isSpecial;
  private TextField freetext;

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
