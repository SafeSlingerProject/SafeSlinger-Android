
package edu.cmu.cylab.starslinger.view;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar.Tab;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import edu.cmu.cylab.starslinger.R;
import edu.cmu.cylab.starslinger.view.HomeActivity.Tabs;

public class HolderTab extends Fragment {

    // private final String TAG_THREADS = "threads_frag";

    private FrameLayout mThreadContainer, mMessageContainer;

    private String mCurrentTabTag = "";
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.message_container, container, false);

        mThreadContainer = (FrameLayout) view.findViewById(R.id.content);
        mMessageContainer = (FrameLayout) view.findViewById(R.id.messageContent);

        setmCurrentTabTag(Tabs.THREADS.toString());
        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        ThreadsFragment frag = new ThreadsFragment();
        ft.replace(R.id.content, frag, Tabs.THREADS.toString()).commit();

        return view;
    }

    public void updateValues(Bundle bundle, String tag) {
        if (Tabs.THREADS.toString().compareTo(tag) == 0) {
            setmCurrentTabTag(Tabs.THREADS.toString());
            ThreadsFragment frag = (ThreadsFragment) getActivity().getSupportFragmentManager()
                    .findFragmentByTag(tag);
            if (frag != null) {
                frag.updateValues(bundle);
            }
        } else {
            MessagesFragment frag = (MessagesFragment) getActivity().getSupportFragmentManager()
                    .findFragmentByTag(tag);
            setmCurrentTabTag(Tabs.MESSAGE.toString());
            if (frag == null) {
                FragmentTransaction ft = getActivity().getSupportFragmentManager()
                        .beginTransaction();
                MessagesFragment msgFrag = new MessagesFragment();
                msgFrag.setArguments(bundle);
                ft.replace(R.id.content, msgFrag, Tabs.MESSAGE.toString())
                        .addToBackStack(Tabs.MESSAGE.toString()).commit();
            } else if (bundle.getBoolean("thread_click"))
                frag.onThreadItemClick(bundle);
            else
                frag.updateValues(bundle);

        }

    }

    public void updateKeypad() {
        ThreadsFragment frag = (ThreadsFragment) getActivity().getSupportFragmentManager()
                .findFragmentByTag(Tabs.THREADS.toString());
        if (frag != null) {
            frag.updateKeypad();
        }
    }

    public String getmCurrentTabTag() {
        return mCurrentTabTag;
    }

    public void setmCurrentTabTag(String mCurrentTabTag) {
        this.mCurrentTabTag = mCurrentTabTag;
    }
}
