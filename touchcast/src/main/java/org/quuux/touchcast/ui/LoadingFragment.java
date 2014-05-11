package org.quuux.touchcast.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.quuux.touchcast.R;

public class LoadingFragment extends Fragment {

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_loading, container, false);

        return view;
    }

    public static LoadingFragment newInstance() {
        final LoadingFragment rv = new LoadingFragment();
        return rv;
    }
}
