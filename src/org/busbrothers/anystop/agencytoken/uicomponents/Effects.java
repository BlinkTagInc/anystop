package org.busbrothers.anystop.agencytoken.uicomponents;

import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;

public class Effects {

	public static Animation fadeLeft() {
		AnimationSet set = new AnimationSet(true);

        Animation animation = new AlphaAnimation(1.0f, 0.0f);
        animation.setDuration(250);
        set.addAnimation(animation);
        

        animation = new TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, -1.0f,
            Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, 0.0f
        );
        Interpolator i = new DecelerateInterpolator(1.0f);
        animation.setInterpolator(i);
        animation.setDuration(500);
        set.addAnimation(animation);

        return set;
	}
	
	
	public static Animation inFromRight() {
		AnimationSet set = new AnimationSet(true);
		Animation animation;

        animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(500);
        set.addAnimation(animation);
        

//        animation = new TranslateAnimation(
//            Animation.RELATIVE_TO_PARENT, 1.0f, Animation.RELATIVE_TO_SELF, 0.0f,
//            Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, 0.0f
//        );
////        Interpolator i = new DecelerateInterpolator(5.0f);
////        animation.setInterpolator(i);
//        animation.setDuration(500);
//        set.addAnimation(animation);
        
        return set;
	}
	
	public static Animation fadeRight() {
		AnimationSet set = new AnimationSet(true);

        Animation animation = new AlphaAnimation(1.0f, 0.0f);
        animation.setDuration(250);
        set.addAnimation(animation);
        

        animation = new TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, 1.0f,
            Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, 0.0f
        );
        
        
        animation.setDuration(500);
        set.addAnimation(animation);

        return set;
	}
	
	public static Animation goRight() {
		
		AnimationSet set = new AnimationSet(true);

        Animation animation = new AlphaAnimation(1.0f, 1.0f);
        animation.setDuration(250);
        set.addAnimation(animation);
        

        animation = new TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, 1.0f,
            Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, 0.0f
        );
        Interpolator i = new DecelerateInterpolator(1.0f);
        animation.setInterpolator(i);
        animation.setDuration(750);
        set.addAnimation(animation);

        return set;
	}
	
	public static Animation goUp() {
		
		AnimationSet set = new AnimationSet(true);

        Animation animation = new AlphaAnimation(1.0f, 1.0f);
        animation.setDuration(250);
        set.addAnimation(animation);
        

        animation = new TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_PARENT, 0.0f
        );
        Interpolator i = new DecelerateInterpolator(1.0f);
        animation.setInterpolator(i);
        animation.setDuration(750);
        set.addAnimation(animation);

        return set;
	}
	
	public static Animation fadeOut() {
		AnimationSet set = new AnimationSet(true);

        Animation animation = new AlphaAnimation(1.0f, 0.0f);
        animation.setDuration(500);
        return animation;
	}
}
