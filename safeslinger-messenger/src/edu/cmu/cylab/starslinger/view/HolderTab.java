
package edu.cmu.cylab.starslinger.view;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;
import edu.cmu.cylab.starslinger.R;
import edu.cmu.cylab.starslinger.SafeSlingerConfig;
import edu.cmu.cylab.starslinger.util.ThreadContent;
import edu.cmu.cylab.starslinger.view.HomeActivity.Tabs;

public class HolderTab extends Fragment {

    private FrameLayout mThreadContainer, mMessageContainer;

//    private String mCurrentTabTag = "";
    private boolean dualPane = false;
//    private String mSinglePaneTab = "";
    private Tabs mPrevTab = Tabs.THREADS;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	
    	if(savedInstanceState != null)
    	{
//    		mCurrentTabTag = savedInstanceState.getString("current_tab");
    		mPrevTab = (Tabs)savedInstanceState.getSerializable("prev_tab");
    	}
    	
    	View view = inflater.inflate(R.layout.message_container, container, false);
    	mThreadContainer = (FrameLayout) view.findViewById(R.id.content);
        mMessageContainer = (FrameLayout) view.findViewById(R.id.messageContent);
        
    	if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
    	{
    		dualPane = true;
    		mThreadContainer.setVisibility(View.VISIBLE);
        	mMessageContainer.setVisibility(View.VISIBLE);
    	}
    	else
    	{
    		dualPane = false;
    		mThreadContainer.setVisibility(View.VISIBLE);
        	mMessageContainer.setVisibility(View.GONE);
    	}
   
        loadBasedOnConfiguration();
        return view;
    }

    private void loadBasedOnConfiguration()
    {
    	if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
    	{
    		
    		if(getChildFragmentManager().findFragmentByTag(Tabs.THREADS.toString()) == null)
    		{
    			FragmentTransaction ft = getChildFragmentManager().beginTransaction();
                ThreadsFragment frag = new ThreadsFragment();
                ft.replace(R.id.content, frag, Tabs.THREADS.toString()).commit();
    		}
            
    		MessagesFragment msgfrag = (MessagesFragment)getChildFragmentManager().findFragmentByTag(Tabs.MESSAGE.toString());
    		getChildFragmentManager().executePendingTransactions();
    		if(msgfrag != null && msgfrag.isAdded()) 
            {
            	getChildFragmentManager().popBackStack();
            	mPrevTab = Tabs.MESSAGE;
            }
            else
            	mPrevTab = Tabs.THREADS;
            
            msgfrag = new MessagesFragment();
//            setmCurrentTabTag(Tabs.MESSAGE.toString());
            
          	int selectedPos = ThreadContent.getInstance().getmSelectedPosition();
        	Bundle bundle = new Bundle();
        	bundle.putParcelable("thread_data", ThreadContent.getInstance().getmThreadList().get(selectedPos));
        	bundle.putBoolean("thread_click", true);
            FragmentTransaction ft = getChildFragmentManager().beginTransaction();
            msgfrag.setArguments(bundle);
            ft.replace(R.id.messageContent, msgfrag, Tabs.MESSAGE.toString()).commit();
    	}
    	else
    	{
    		
//    		System.out.println("Tag : " + getmCurrentTabTag());
    		
    		FragmentTransaction ft = getChildFragmentManager().beginTransaction();
    		
    		MessagesFragment msgfrag = (MessagesFragment)getChildFragmentManager().findFragmentByTag(Tabs.MESSAGE.toString());
	   		if(msgfrag != null && msgfrag.isAdded())
	   			ft.remove(msgfrag).commit();
	   		
    		if(mPrevTab == Tabs.MESSAGE)
    		{
    			ft = getChildFragmentManager().beginTransaction();
//    			setmCurrentTabTag(Tabs.MESSAGE.toString());
    			msgfrag = new MessagesFragment();
    			int selectedPos = ThreadContent.getInstance().getmSelectedPosition();
            	Bundle bundle = new Bundle();
            	bundle.putParcelable("thread_data", ThreadContent.getInstance().getmThreadList().get(selectedPos));
            	bundle.putBoolean("thread_click", true);
                msgfrag.setArguments(bundle);
    			ft.replace(R.id.content, msgfrag, Tabs.MESSAGE.toString()).addToBackStack(null).commit();
    		}
    		else
    		{
    			ft = getChildFragmentManager().beginTransaction();
//    	   		setmCurrentTabTag(Tabs.THREADS.toString());
    			ThreadsFragment frag = new ThreadsFragment();
    			ft.replace(R.id.content, frag, Tabs.THREADS.toString()).commit();
    		}
            
            
            
    	}
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
//    	outState.putString("current_tab", mCurrentTabTag);
    	outState.putSerializable("prev_tab", mPrevTab);
    }
    
    public void updateValues(Bundle bundle, String tag)
    {
    	updateValues(bundle, tag, "");
    }
    
    public void updateBothTabs(Bundle bundle)
    {
        ThreadsFragment frag = (ThreadsFragment) getChildFragmentManager()
                .findFragmentByTag(Tabs.THREADS.toString());
        if (frag != null) {
            frag.updateValues(bundle);
        }
        
        MessagesFragment msgFrag = (MessagesFragment) getChildFragmentManager()
                .findFragmentByTag(Tabs.MESSAGE.toString());
        
        if(msgFrag != null)
        {
        	msgFrag.updateValues(bundle);
        }
    }
    
    public void updateValues(Bundle bundle, String tag, String action) {
    	
        if (Tabs.THREADS.toString().compareTo(tag) == 0) {
//            setmCurrentTabTag(Tabs.THREADS.toString());
            ThreadsFragment frag = (ThreadsFragment) getChildFragmentManager()
                    .findFragmentByTag(tag);
            if (frag != null) {
                frag.updateValues(bundle);
            }
        } else {
        	if(bundle == null)
        	{	
        		int selectedPos = ThreadContent.getInstance().getmSelectedPosition();
        		bundle = new Bundle();
        		bundle.putBoolean("thread_click", true);
        		bundle.putParcelable("thread_data", ThreadContent.getInstance().getmThreadList().get(selectedPos));
        	}
        		
            MessagesFragment frag = (MessagesFragment) getChildFragmentManager()
                    .findFragmentByTag(tag);
            
            if (frag == null || (frag != null && !dualPane && SafeSlingerConfig.Intent.ACTION_MESSAGEOUTGOING.compareTo(action) != 0 && SafeSlingerConfig.Intent.ACTION_MESSAGEINCOMING.compareTo(action) != 0)) {
                FragmentTransaction ft = getChildFragmentManager()
                        .beginTransaction();
                MessagesFragment msgFrag = new MessagesFragment();
                MessagesFragment.setRecip(null);
                msgFrag.setArguments(bundle);
                int containerId = R.id.content;
                if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
                {
                	containerId = R.id.messageContent;
                	ft.replace(containerId, msgFrag, Tabs.MESSAGE.toString())
                    .commit();
                }
                else
                	ft.replace(containerId, msgFrag, Tabs.MESSAGE.toString())
                    .addToBackStack(Tabs.MESSAGE.toString()).commit();
                
            } 
            else if (bundle.getBoolean("thread_click"))
            {
            	MessagesFragment.setRecip(null);
            	frag.onThreadItemClick(bundle);
            }
            else
                frag.updateValues(bundle);
//            setmCurrentTabTag(Tabs.MESSAGE.toString());
        }

    }

//    @Override
//    public void onConfigurationChanged(Configuration newConfig) {
//    	super.onConfigurationChanged(newConfig);
//    	
//    	if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
//    	{
//    		mThreadContainer.setVisibility(View.VISIBLE);
//        	mMessageContainer.setVisibility(View.VISIBLE);
//    	}
//    	else
//    	{
//    		mThreadContainer.setVisibility(View.VISIBLE);
//        	mMessageContainer.setVisibility(View.GONE);
//    	}
//    }
    
    public void updateKeypad() {
        ThreadsFragment frag = (ThreadsFragment) getChildFragmentManager()
                .findFragmentByTag(Tabs.THREADS.toString());
        if (frag != null) {
            frag.updateKeypad();
        }
    }

//    public String getmCurrentTabTag() {
//        return mCurrentTabTag;
//    }
//
//    public void setmCurrentTabTag(String mCurrentTabTag) {
//        this.mCurrentTabTag = mCurrentTabTag;
//    }
}
