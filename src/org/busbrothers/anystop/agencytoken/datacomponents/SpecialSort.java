package org.busbrothers.anystop.agencytoken.datacomponents;

import java.util.Comparator;

public class SpecialSort implements Comparator<SimpleStop> {

	public int compare(SimpleStop one, SimpleStop two) {
		return SmartSort.compare(one.intersection, two.intersection, true);
	}

}
