package edu.cmu.cylab.starslinger.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application.ActivityLifecycleCallbacks;
import android.os.Build;
import android.os.Bundle;
import edu.cmu.cylab.starslinger.SafeSlinger;
import edu.cmu.cylab.starslinger.view.HomeActivity;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class ActivityLifeCallbacks implements ActivityLifecycleCallbacks{

      
    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onActivityStarted(Activity activity) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if(activity.getClass().getSimpleName().compareTo(HomeActivity.class.getSimpleName()) == 0)
           SafeSlinger.getApplication().setMessageFragActive(true);
        
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if(activity.getClass().getSimpleName().compareTo(HomeActivity.class.getSimpleName()) == 0)
            SafeSlinger.getApplication().setMessageFragActive(false);
    }

    @Override
    public void onActivityStopped(Activity activity) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        // TODO Auto-generated method stub
        
    }
 }
