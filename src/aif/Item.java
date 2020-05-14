package aif;

public final class Item {
	
	private String name;
	private String unit;
	private String value;
	
	public Item(String name, String unit, String value) {
		this.name = name;
		this.unit = unit;
		this.value = value;
	}
	
	public final String getName() {
		return name;
	}
	
	public final String getUnit() {
		return unit;
	}
	
	public final String getValue() {
		return value;
	}
	
	public final String writeItem()
	{
		return this.name + "\t" + this.unit + "\t" + this.value;
	}

	@Override
	public String toString() {
		return this.name + " [" + this.unit + "] : " + this.value;
	}
}
