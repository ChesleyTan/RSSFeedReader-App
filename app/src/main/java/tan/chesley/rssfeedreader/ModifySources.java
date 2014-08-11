package tan.chesley.rssfeedreader;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ModifySources extends ListActivity implements ModifySourceDialogFragment.ModifySourceCallbacks, AddSourceDialogFragment.AddSourceCallbacks{

    private static final String TAG_SOURCES = "tan.chesley.rssfeedreader.sources";
    private static final String TAG_MODIFY_SOURCE_DIALOG_FRAGMENT = "tan.chesley.rssfeedreader.modifysourcedialogfragment";
    private static final String TAG_ADD_SOURCE_DIALOG_FRAGMENT = "tan.chesley.rssfeedreader.addsourcedialogfragment";
    private static final int TAG_IMPORT_FILE_RESULT_CODE = 467678; // "import" in phone number representation
    private static final int TAG_EXPORT_FILE_RESULT_CODE = 397678; // "export" in phone number representation
    private final Context context = this;
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
            SourcesOpenHelper sourceDbHelper = new SourcesOpenHelper(this);
            sources = sourceDbHelper.getSourcesArrayList();
        }
        showListView(false);
    }

    @Override
    protected void onListItemClick (ListView l, View v, final int position, long id) {
        ModifySourceDialogFragment dialog = new ModifySourceDialogFragment();
        Bundle args = new Bundle();
        args.putString(ModifySourceDialogFragment.TAG_SOURCE, sources.get(position));
        dialog.setArguments(args);
        dialog.show(getFragmentManager(), TAG_MODIFY_SOURCE_DIALOG_FRAGMENT);
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
            final TextView sourceTextView = (TextView) convertView.findViewById(R.id.sourceTextView);
            TextView positionTextView = (TextView) convertView.findViewById(R.id.positionTextView);
            ToggleButton disabledToggleButton = (ToggleButton) convertView.findViewById(R.id.disabledToggleButton);
            SourcesOpenHelper dbHelper = new SourcesOpenHelper(context);
            String source = sources.get(position);
            // Check if the source is disabled and change its appearance as appropriate
            if (!dbHelper.isEnabled(source)) {
                sourceTextView.setTextColor(getResources().getColor(R.color.DisabledTextColor));
                disabledToggleButton.setChecked(false);
            }
            sourceTextView.setText(source);
            positionTextView.setText(Integer.toString(position + 1)); // Add 1 for One-based indexing
            disabledToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged (CompoundButton compoundButton, boolean b) {
                    if (b) {
                        sourceTextView.setTextColor(getResources().getColor(R.color.AppPrimaryTextColor));
                    }
                    else {
                        sourceTextView.setTextColor(getResources().getColor(R.color.DisabledTextColor));
                    }
                    SourcesOpenHelper dbHelper = new SourcesOpenHelper(context);
                    dbHelper.setEnabled(sourceTextView.getText().toString(), b);
                }
            });
            return convertView;
        }
    }

    public void showListView(boolean updateSources) {
        if (updateSources) {
            SourcesOpenHelper dbHelper = new SourcesOpenHelper(this);
            sources = dbHelper.getSourcesArrayList();
        }
        getListView().setAdapter(new SourcesAdapter(this));
    }

    public void onModifySourceCallback() {
        showListView(true);
    }

    public void onAddSourceCallback() {
        showListView(true);
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu) {
        getMenuInflater().inflate(R.menu.modifysources, menu);
        return true;
    }

    @Override
    public boolean onMenuItemSelected (int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_new) {
            AddSourceDialogFragment dialog = new AddSourceDialogFragment();
            dialog.show(getFragmentManager(), TAG_ADD_SOURCE_DIALOG_FRAGMENT);
            return true;
        }
        else if (id == R.id.action_import) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("file/*");
            PackageManager pm = getPackageManager();
            List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
            if (activities.size() > 0) {
                startActivityForResult(intent, TAG_IMPORT_FILE_RESULT_CODE);
            } else {
                showToast(getResources().getString(R.string.importFailedNoFileBrowser), Toast.LENGTH_LONG);
            }
            return true;
        }
        else if (id == R.id.action_export) {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                File sdCard = Environment.getExternalStorageDirectory();
                File path = new File(sdCard.getAbsolutePath() + "/tan.chesley.rssfeedreader/feeds");
                path.mkdirs();
                File file = new File(path, "feeds-export.txt");
                try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                    String[] sources = new SourcesOpenHelper(this).getSources();
                    writer.write("tan.chesley.rssfeedreader.export\n");
                    for (String s : sources) {
                        writer.write(s + "\n");
                    }
                    writer.close();
                    showToast(getResources().getString(R.string.successfulExportToFile) + file.getAbsolutePath(), Toast.LENGTH_LONG);
                } catch (IOException e) {
                    showToast(getResources().getString(R.string.errorOccurredDuringWrite), Toast.LENGTH_LONG);
                    e.printStackTrace();
                }
            }
            else {
                showToast(getResources().getString(R.string.noExternalStorageAccess), Toast.LENGTH_LONG);
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        if (requestCode == TAG_IMPORT_FILE_RESULT_CODE) {
            if (resultCode == RESULT_OK) {
                String filePath = data.getData().getPath();
                Log.e("Import", "File chosen: " + filePath);
                File file = new File(filePath);
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(file));
                    String line = reader.readLine();
                    if (line != null && line.equals("tan.chesley.rssfeedreader.export")) {
                        SourcesOpenHelper dbHelper = new SourcesOpenHelper(this);
                        dbHelper.clearAllSources();
                        while ((line = reader.readLine()) != null) {
                            dbHelper.addSource(line, 1);
                        }
                        showToast(getResources().getString(R.string.successfulImportFromFile) + file.getAbsolutePath(), Toast.LENGTH_LONG);
                        // Reload ListView items
                        showListView(true);
                    }
                    else {
                        showToast(getResources().getString(R.string.invalidFileFormat), Toast.LENGTH_LONG);
                    }
                    // TODO validate sources that are imported
                } catch (IOException e) {
                    showToast(getResources().getString(R.string.errorOccurredDuringRead), Toast.LENGTH_LONG);
                    e.printStackTrace();
                }
            }
        }
    }

    public void showToast (String s, int toastDurationFlag) {
        Toast toast = Toast.makeText(this, s,
                                     toastDurationFlag);
        TextView toastTextView = (TextView) toast.getView().findViewById(
            android.R.id.message);
        toastTextView.setTextColor(getResources().getColor(
            R.color.AppPrimaryTextColor));
        toast.getView().getBackground().setAlpha(180);
        toast.show();
    }
}
