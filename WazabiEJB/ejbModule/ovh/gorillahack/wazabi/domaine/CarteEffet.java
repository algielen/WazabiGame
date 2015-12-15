package ovh.gorillahack.wazabi.domaine;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "CARTES_EFFETS", schema = "WAZABI")
public class CarteEffet implements Serializable {
	@Id
	private int code_effet;
	@Column
	private String effet;
	@Column
	private String description;

	public CarteEffet(int code_effet, String effet, String description) {
		super();
		this.code_effet = code_effet;
		this.effet = effet;
		this.description = description;
	}
	
	public CarteEffet() {
		super();
	}

	public int getCode_effet() {
		return code_effet;
	}

	public String getEffet() {
		return effet;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + code_effet;
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((effet == null) ? 0 : effet.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CarteEffet other = (CarteEffet) obj;
		if (code_effet != other.code_effet)
			return false;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (effet == null) {
			if (other.effet != null)
				return false;
		} else if (!effet.equals(other.effet))
			return false;
		return true;
	}

}