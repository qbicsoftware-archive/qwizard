package model;

import com.vaadin.ui.AbstractField;

/**
 * Abstract UI component of a Component containing rollover text and other information
 * @author Andreas Friedrich
 *
 */
abstract public class AOpenbisInfoComponent extends AbstractField<Object> {
		
  private static final long serialVersionUID = 377809399988492626L;
  AbstractField<?> inner;
	
	public AOpenbisInfoComponent(String description, AbstractField<?> comp) {
		this.setDescription(description);
		this.inner = comp;
		this.inner.setDescription(description);
	}
	
	public AOpenbisInfoComponent(String description, AbstractField<?> comp,
			String width) {
		this(description, comp);
		comp.setWidth(width);
	}

	@Override
	public Class<String> getType() {
		return String.class;
	}
	
	public AbstractField<?> getInnerComponent() {
		return inner;
	}

	public void setSize(String width, String height) {
		inner.setHeight(height);
		inner.setWidth(width);
	}
	
	public String getValue() {
		return (String) inner.getValue();
	}
}
