package eu.deustotech.internet.linkedqr.android.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.openrdf.model.Statement;

public class StatementComparator implements Comparator<Statement>{

	@Override
	public int compare(Statement lhs, Statement rhs) {
		List<String> stringList = new ArrayList<String>();
		stringList.add(lhs.getPredicate().stringValue());
		stringList.add(rhs.getPredicate().stringValue());
		
		Comparator<String> comparator = Collections.reverseOrder();
		Collections.sort(stringList, comparator);
		
		if (stringList.get(0).equals(lhs)) {
			return 1;
		} else {
			return -1;
		}
	}

}
