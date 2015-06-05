package edu.cmu.cylab.starslinger.util;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.cylab.starslinger.model.ThreadData;
import edu.cmu.cylab.starslinger.view.HomeActivity.Tabs;

public class ThreadContent {

	private static ThreadContent mInstance;
	
    private List<ThreadData> mThreadList = new ArrayList<ThreadData>();
    private int mSelectedPosition = 0;
    private String mSelectedRecipientId = "";
    
    private Tabs mCurrentTab = Tabs.THREADS;
    
	private ThreadContent()
	{
		
	}
	
	public static ThreadContent getInstance()
	{
		if(mInstance == null)
			mInstance = new ThreadContent();
		
		return mInstance;
	}

	public String getSelectedRecipientId()
	{
		return this.mSelectedRecipientId;
	}
	
	public void setSelectedRecipientId(String keyID)
	{
		this.mSelectedRecipientId = keyID;
	}
	
	public List<ThreadData> getmThreadList() {
		return mThreadList;
	}

	public void setmThreadList(List<ThreadData> mThreadList) {
		this.mThreadList = mThreadList;
	}

	public int getmSelectedPosition() {
		return mSelectedPosition;
	}

	public void setmSelectedPosition(int mSelectedPosition) {
		this.mSelectedPosition = mSelectedPosition;
	}
	
	public int getThreadsCount()
	{
		return mThreadList.size();
	}
	
	public void clearData()
	{
		mThreadList.clear();
		mSelectedPosition = 0;
	}

	public Tabs getmCurrentTab() {
		return mCurrentTab;
	}

	public void setmCurrentTab(Tabs mCurrentTab) {
		this.mCurrentTab = mCurrentTab;
	}
}
