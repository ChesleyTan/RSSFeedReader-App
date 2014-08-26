package android.support.v4.app;

import android.os.Bundle;
import android.view.ViewGroup;

/*
This class is a wrapper for the Android Support Library v4's FragmentStatePagerAdapter.
It addresses an issue in which the ClassLoader is not set correctly upon reading a saved state
Source: https://code.google.com/p/android/issues/detail?id=37484
 */

public abstract class FixedFragmentStatePagerAdapter extends FragmentStatePagerAdapter {

    public FixedFragmentStatePagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Fragment f = (Fragment) super.instantiateItem(container, position);
        Bundle savedFragmentState = f.mSavedFragmentState;
        if (savedFragmentState != null) {
            savedFragmentState.setClassLoader(f.getClass().getClassLoader());
        }
        return f;
    }

}
