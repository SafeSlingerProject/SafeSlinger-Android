/*
 * The MIT License (MIT)
 * 
 * Copyright (c) 2010-2015 Carnegie Mellon University
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package edu.cmu.cylab.starslinger.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application.ActivityLifecycleCallbacks;
import android.os.Build;
import android.os.Bundle;
import edu.cmu.cylab.starslinger.SafeSlinger;
import edu.cmu.cylab.starslinger.view.HomeActivity;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class ActivityLifeCallbacks implements ActivityLifecycleCallbacks {

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
        if (activity.getClass().getSimpleName().compareTo(HomeActivity.class.getSimpleName()) == 0)
            SafeSlinger.getApplication().setMessageFragActive(true);

    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (activity.getClass().getSimpleName().compareTo(HomeActivity.class.getSimpleName()) == 0)
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
