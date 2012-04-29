/** This class implements a TextView element that reduces its font size until it has no more than 
 * maxNumLines lines in it's view, or until the font size has reached minFontSizeInDP. The font size
 * starts at currentFontSizeInDP as set by the constructor.  
 * 
 * @author Ivany
 * @date 2012-01-14
 */

package org.busbrothers.anystop.agencytoken.uicomponents;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.widget.TextView;

public class SelfResizingTextView extends TextView {
	private int currentFontSizeInDP; //!<Current font size of this TextView
	private int minFontSizeInDP; //!<Minimum allowable font size
	private int maxNumLines; //!<Maximum number of lines allowable until we resize
	private int maxWindowHeight;
	private final int fontSizeIncrement = 4; //!<Minimum increment for font size reduction
	
	private final String classNameTag="SelfResizingTextView";
	
	/**Default constructor. See View(Context)
	 * 
	 * @param context The calling Context that is using this View.
	 */
	public SelfResizingTextView(Context context) {
		super(context);
		
		currentFontSizeInDP = -1; //set startingFontSizeInDP to -1 to disable self-resizing behavior
		minFontSizeInDP = -1;
		maxNumLines = -1;
	}
	
	/** Same as the default constructor except that this one allows passing a set of Attributes, so that we can for example 
	 * initialize this View from XML.
	 * @param context The calling Context that is using this View.
	 * @param attributeSet AttributeSet that tells us what (XML, most likely) attributes have been set.
	 */
	public SelfResizingTextView(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);
		
		currentFontSizeInDP = -1; //set startingFontSizeInDP to -1 to disable self-resizing behavior
		minFontSizeInDP = -1;
		maxNumLines = -1;
	}
	
	/** A constructor that allows us to set the class variables for starting font size, minimum font size, and maximum number of lines.
	 * 
	 * @param context The calling Context that is using this View.
	 * @param m_currentFontSizeInDP The starting font size for this View.
	 * @param m_minFontSizeInDP The minimum font size, we don't scale the font down past this size.
	 * @param m_maxNumLines The maximum number of lines allowed until we scale down the font size.
	 * @param m_maxWindowHeight The maximum height (font size * number of lines) of the SelfResizingTextView.
	 */
	public SelfResizingTextView(Context context, int m_currentFontSizeInDP, int m_minFontSizeInDP, int m_maxNumLines, int m_maxWindowHeight) {
		super(context);
		currentFontSizeInDP = m_currentFontSizeInDP;
		minFontSizeInDP = m_minFontSizeInDP;
		maxNumLines = m_maxNumLines;
		maxWindowHeight = m_maxWindowHeight;
		
		setTextSize(TypedValue.COMPLEX_UNIT_SP, currentFontSizeInDP);
	}
	
	/** Allows setting the parameters for this SelfResizingTextView outside of the constructor. Useful when we want to set them
	 *  dynamically but still allow for instantiation of this View in XML.
	 * @param m_currentFontSizeInDP The starting font size for this View.
	 * @param m_minFontSizeInDP The minimum font size, we don't scale the font down past this size.
	 * @param m_maxNumLines The maximum number of lines allowed until we scale down the font size.
	 * @param m_maxWindowHeight The maximum height (font size * number of lines) of the SelfResizingTextView.
	 */
	public void setResizeParams(int m_currentFontSizeInDP, int m_minFontSizeInDP, int m_maxNumLines, int m_maxWindowHeight) {
		currentFontSizeInDP = m_currentFontSizeInDP;
		minFontSizeInDP = m_minFontSizeInDP;
		maxNumLines = m_maxNumLines;
		maxWindowHeight = m_maxWindowHeight;
		
		setTextSize(TypedValue.COMPLEX_UNIT_SP, currentFontSizeInDP);
		invalidate();
	}
	
	/** This method is called when this View gets (re)drawn. It checks its own window height and reduces the font size until it
	 *  fits into the window height (font size * # of lines) or until we hit the minimum allowable font size.
	 * 
	 * @param canvas See View::onDraw(Canvas)
	 */
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		if(currentFontSizeInDP == -1) return; //return if we never set currentFontSizeInDP, i.e. if we constructed this
		//class as a regular TextView
	
		if((getLineCount()*currentFontSizeInDP) > maxWindowHeight && (currentFontSizeInDP-fontSizeIncrement) >= minFontSizeInDP) {
			currentFontSizeInDP -= fontSizeIncrement;
			setTextSize(TypedValue.COMPLEX_UNIT_SP, currentFontSizeInDP);
			invalidate();
			
			Log.v(classNameTag, "Reduced font size to " + currentFontSizeInDP + ", height is " + getHeight());
		}
		else Log.v(classNameTag, "Did not Reduce font size, " + (getLineCount()*currentFontSizeInDP) + " <= " + maxWindowHeight);
	}
	
	/** This method is called when this View gets its size changed. It checks its own window height and reduces the font size until it
	 *  fits into the window height (font size * # of lines) or until we hit the minimum allowable font size.
	 * 
	 * @param w See View::onSizeChanged(int w, int h, int oldw, int oldh)
	 * @param h See View::onSizeChanged(int w, int h, int oldw, int oldh)
	 * @param oldw See View::onSizeChanged(int w, int h, int oldw, int oldh)
	 * @param oldh See View::onSizeChanged(int w, int h, int oldw, int oldh)
	 */
	/*public void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		
		if(currentFontSizeInDP == -1) return; //return if we never set currentFontSizeInDP, i.e. if we constructed this
			//class as a regular TextView
		
		if(getLineCount() > maxNumLines && (currentFontSizeInDP-fontSizeIncrement) >= minFontSizeInDP) {
			currentFontSizeInDP -= fontSizeIncrement;
			setTextSize(TypedValue.COMPLEX_UNIT_SP, currentFontSizeInDP);
			invalidate();
			
			Log.v(classNameTag, "Reduced font size to " + currentFontSizeInDP);
		}
	}*/

}
