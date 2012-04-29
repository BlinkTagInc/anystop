package org.busbrothers.anystop.agencytoken.uicomponents;

import org.busbrothers.anystop.agencytoken.R;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;

public class IndexCheck extends CheckBox {
	
	public int index;
	public boolean stopUpdate=false;
	

	public IndexCheck(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public IndexCheck(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public IndexCheck(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	
	

}
