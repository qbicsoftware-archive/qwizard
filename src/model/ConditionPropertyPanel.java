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

@SuppressWarnings("serial")
public class ConditionPropertyPanel extends VerticalLayout {

  Label label;
  OptionGroup type;
  TextArea values;
  ComboBox unit;

  public ConditionPropertyPanel(String condition, EnumSet<properties.Unit> units) {
    label = new Label(condition);
    type = new OptionGroup("", new ArrayList<String>(Arrays.asList("Continuous", "Categorical")));
    values = new TextArea("Values");
//    Collections.sort(units);
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
