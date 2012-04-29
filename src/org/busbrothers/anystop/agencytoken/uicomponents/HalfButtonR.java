package org.busbrothers.anystop.agencytoken.uicomponents;

import org.busbrothers.anystop.agencytoken.R;
import org.busbrothers.anystop.agencytoken.R;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.Button;

public class HalfButtonR extends Button {

	public HalfButtonR(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

	
	public void onDraw(Canvas canvas) {

		// Since this Button now has no background. We must set the text color to indicate focus.
		if (this.isPressed()) {

		// Set the focused text color. In the case of ImageOnlyButton we would .
		// instead do setImageResource(imageResourceFocused);
		this.setBackgroundResource(R.drawable.hbuttonright_pressed);

		} else {

		this.setBackgroundResource(R.drawable.hbuttonright_norm);

		}

		super.onDraw(canvas);

		}

}
