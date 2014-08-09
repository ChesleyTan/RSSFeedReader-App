package tan.chesley.rssfeedreader;

import android.app.ListActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class ModifySources extends ListActivity {

    private static final String TAG_SOURCES = "tan.chesley.rssfeedreader.sources";
    private ArrayList<String> sources;

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int titleId = getResources().getIdentifier("action_bar_title", "id",
                                                   "android");
        TextView title = (TextView) findViewById(titleId);
        if (title != null) {
            setTitle(getResources().getString(R.string.sources));
            title.setTextColor(getResources().getColor(
                (R.color.AppPrimaryTextColor)));
        }
        setContentView(R.layout.modify_sources);
        if (savedInstanceState != null) {
            sources = savedInstanceState.getStringArrayList(TAG_SOURCES);
        }
        else {
            SourcesManager sm = new SourcesManager(this);
            sources = sm.getSourcesArrayList();
        }
        getListView().setAdapter(new SourcesAdapter(this));
    }

    @Override
    protected void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(TAG_SOURCES, sources);
    }

    private class SourcesAdapter extends ArrayAdapter<String> {
        public SourcesAdapter(ListActivity context) {
            super(context, R.layout.sources_list_item, sources);
        }

        @Override
        public View getView (int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(
                    R.layout.sources_list_item, parent, false);
            }
            TextView sourceTextView = (TextView) convertView.findViewById(R.id.sourceTextView);
            TextView positionTextView = (TextView) convertView.findViewById(R.id.positionTextView);
            sourceTextView.setText(sources.get(position));
            positionTextView.setText(Integer.toString(position + 1)); // Add 1 for One-based indexing
            return convertView;
        }
    }

}
