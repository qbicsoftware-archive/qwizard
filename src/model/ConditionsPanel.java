package model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Experiment;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.OptionGroup;

public class ConditionsPanel extends HorizontalLayout {

  List<String> options;
  String other;
  String special;
  ComboBox specialField;
  boolean nullSelectionAllowed;
  List<ConditionChooser> choosers;
  ValueChangeListener conditionChangeListener;
  Button.ClickListener buttonListener;
  GridLayout buttonGrid;
  Button add;
  Button remove;

  OptionGroup conditionsSet;

  public ConditionsPanel(List<String> options, String other, String special, ComboBox specialField,
      boolean nullSelectionAllowed, OptionGroup conditionsSet) {
    this.specialField = specialField;
    this.options = options;
    this.other = other;
    this.special = special;
    this.nullSelectionAllowed = nullSelectionAllowed;

    this.conditionsSet = conditionsSet;
    this.conditionsSet.addItem("set");

    initListener();

    choosers = new ArrayList<ConditionChooser>();
    ConditionChooser c = new ConditionChooser(options, other, special, nullSelectionAllowed);
    c.addListener(conditionChangeListener);
    choosers.add(c);
    addComponent(c);

    buttonGrid = new GridLayout(1, 2);
    buttonGrid.setSpacing(true);
    add = new Button("+");
    add.setWidth("25");
    remove = new Button("-");
    remove.setWidth("25");
    buttonGrid.addComponent(add);
    buttonGrid.addComponent(remove);
    add.addClickListener(buttonListener);
    remove.addClickListener(buttonListener);
    addComponent(buttonGrid);
    setSpacing(true);
  }

  private void initListener() {
    conditionChangeListener = new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        ComboBox source = (ComboBox) event.getProperty();
        boolean special = false;
        conditionsSet.setValue(null);
        for (ConditionChooser c : choosers) {
          if (c.getBox().equals(source)) {
            c.changed();
            if (c.chooserSet())
              conditionsSet.select("set");
          }
          special |= c.factorIsSpecial();
        }
        specialField.setEnabled(!special);
      }
    };

    buttonListener = new Button.ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        if (event.getButton().equals(add))
          add();
        else
          remove();
      }

    };
  }

  public List<String> getConditions() {
    List<String> res = new ArrayList<String>();
    for (ConditionChooser c : choosers) {
      if (c.isSet())
        res.add(c.getCondition());
    }
    return res;
  }

  public void changed() {
    for (ConditionChooser c : choosers)
      c.changed();
  }

  private void add() {
    ConditionChooser c = new ConditionChooser(options, other, special, nullSelectionAllowed);
    c.addListener(conditionChangeListener);
    choosers.add(c);

    removeComponent(buttonGrid);
    addComponent(c);
    addComponent(buttonGrid);
  }

  private void remove() {
    int size = choosers.size();
    if (size > 1) {
      ConditionChooser last = choosers.get(size - 1);
      removeComponent(last);
      choosers.remove(last);
    }
  }

}
