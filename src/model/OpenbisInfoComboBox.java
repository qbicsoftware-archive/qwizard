package model;

import java.util.List;

import com.vaadin.ui.ComboBox;

/**
 * Composite UI component of a ComboBox containing rollover text and other information
 * @author Andreas Friedrich
 *
 */
public class OpenbisInfoComboBox extends AOpenbisInfoComponent {
	
	public OpenbisInfoComboBox(String label, String description, List<String> data) {
		super(description, new ComboBox(label, data));
	}
}

