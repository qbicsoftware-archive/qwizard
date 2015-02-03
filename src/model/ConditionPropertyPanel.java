package model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import properties.Factor;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;

/**
 * Composite UI component to input values of Property instances and their units
 * @author Andreas Friedrich
 *
 */
public class ConditionPropertyPanel extends VerticalLayout {

  private static final long serialVersionUID = 3320102983685470217L;
  Label label;
  OptionGroup type;
  TextArea values;
  ComboBox unit;

  /**
   * Create a new Condition Property Panel
   * @param condition The name of the condition selected
   * @param units An EnumSet of units (e.g. SI units)
   */
  public ConditionPropertyPanel(String condition, EnumSet<properties.Unit> units) {
    label = new Label(condition);
    type = new OptionGroup("", new ArrayList<String>(Arrays.asList("Continuous", "Categorical")));
    values = new TextArea("Values");
    unit = new ComboBox("Unit", units);
    unit.setEnabled(false);
    unit.setNullSelectionAllowed(false);
    initListener();

    addComponent(label);
    addComponent(type);
    addComponent(values);
    addComponent(unit);
    setSpacing(true);
  }

  private void initListener() {
    ValueChangeListener typeListener = new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        if (type != null && type.getValue().toString().equals("Continuous")) {
          unit.setEnabled(true);
        } else {
          unit.setEnabled(false);
          unit.select(unit.getNullSelectionItemId());
        }
      }
    };
    type.addValueChangeListener(typeListener);
  }
  
  /**
   * Returns all conditions with their units as a list
   * @return
   */
  public List<Factor> getFactors() {
    List<Factor> res = new ArrayList<Factor>();
    String unitVal = "";
    if(unit.getValue()!=null)
      unitVal = ((properties.Unit) unit.getValue()).getValue();
    System.out.println(unitVal);
    for(String val : values.getValue().split("\n")) {
      res.add(new Factor(label.getValue().toLowerCase(), val, unitVal));
    }
    return res;
  }
}
