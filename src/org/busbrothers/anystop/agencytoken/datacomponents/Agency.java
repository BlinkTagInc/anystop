package org.busbrothers.anystop.agencytoken.datacomponents;

import java.io.Serializable;

public class Agency implements Comparable<Agency>, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3409529674733949086L;
	public String name, table, isRTstr, state, city;
	public boolean isRT;
	public boolean isSpecial;
	
	public int compareTo(Agency another) {
		return this.name.compareTo(another.name);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (isRT ? 1231 : 1237);
		result = prime * result + ((isRTstr == null) ? 0 : isRTstr.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((table == null) ? 0 : table.hashCode());
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
		Agency other = (Agency) obj;
		if (isRT != other.isRT)
			return false;
		if (isRTstr == null) {
			if (other.isRTstr != null)
				return false;
		} else if (!isRTstr.equals(other.isRTstr))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (table == null) {
			if (other.table != null)
				return false;
		} else if (!table.equals(other.table))
			return false;
		return true;
	}
}
